package cn.iocoder.yudao.framework.tenant.core.security;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link TenantSecurityWebFilter} 单元测试
 *
 * @author Codex
 */
class TenantSecurityWebFilterTest extends BaseMockitoUnitTest {

    @Mock
    private GlobalExceptionHandler globalExceptionHandler;
    @Mock
    private TenantFrameworkService tenantFrameworkService;

    private TenantSecurityWebFilter filter;

    @BeforeEach
    public void setUp() {
        WebProperties webProperties = new WebProperties();
        TenantProperties tenantProperties = new TenantProperties();
        filter = new TenantSecurityWebFilter(webProperties, tenantProperties, Set.of(),
                globalExceptionHandler, tenantFrameworkService);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test // 已登录租户不能通过请求上下文越权访问其它租户
    public void testDoFilterInternal_forbiddenWhenLoginTenantDiffersFromRequestTenant() throws Exception {
        // 准备参数
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/rental/xianyu/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        LoginUser loginUser = new LoginUser()
                .setId(10L)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setTenantId(1001L);
        SecurityFrameworkUtils.setLoginUser(loginUser, request);
        TenantContextHolder.setTenantId(2002L);

        // 调用
        filter.doFilter(request, response, chain);

        // 断言
        String content = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(content.contains("\"code\":403"));
        assertTrue(content.contains("您无权访问该租户的数据"));
        assertFalse(chainCalled.get());
        verify(tenantFrameworkService, never()).validTenant(2002L);
    }

}
