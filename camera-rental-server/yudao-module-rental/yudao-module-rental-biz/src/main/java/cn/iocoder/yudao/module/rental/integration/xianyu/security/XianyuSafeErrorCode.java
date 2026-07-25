package cn.iocoder.yudao.module.rental.integration.xianyu.security;

import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;

/**
 * Converts integration failures into diagnostics that cannot contain payload or credential values.
 */
public final class XianyuSafeErrorCode {

    private XianyuSafeErrorCode() {
    }

    public static String from(Throwable throwable) {
        if (throwable instanceof XianyuClientException clientException) {
            StringBuilder code = new StringBuilder("XGJ_").append(clientException.getKind().name());
            if (clientException.getHttpStatus() != null) {
                code.append("_HTTP_").append(clientException.getHttpStatus());
            }
            if (clientException.getRemoteCode() != null) {
                code.append("_REMOTE_").append(clientException.getRemoteCode());
            }
            return code.toString();
        }
        return throwable == null ? "UNKNOWN" : throwable.getClass().getSimpleName();
    }

}
