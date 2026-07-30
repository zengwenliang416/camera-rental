import { pick } from '../state.js';

export function renderLogin() {
  return `
    <section class="login-screen" data-specnav-screen="login" data-specnav-component="UnifiedAdminLogin">
      <div class="login-atmosphere"><i></i><i></i><i></i></div>
      <div class="login-shell">
        <section class="login-story">
          <header>
            <div class="login-brand"><span><i></i><i></i><i></i><i></i></span><div><strong>${pick('设备排期中心', 'Equipment Schedule Center')}</strong><small>RENTAL OPERATIONS</small></div></div>
            <div class="login-preview-controls"><em>ADMIN SSO</em><button data-theme-toggle aria-label="${pick('切换主题', 'Toggle theme')}">◐</button><button data-locale-toggle>EN</button></div>
          </header>
          <div class="login-intro">
            <p>PHYSICAL ASSET CONTROL</p>
            <h1>${pick('让每一台设备，进入可追踪的履约周期。', 'Every physical device, tracked through fulfillment.')}</h1>
            <span>${pick('与相机租赁管理后台使用同一账号、租户和角色权限。排期、设备、订单和发货状态始终由服务端确认。', 'Use the same admin account, tenant, and roles. Schedule, device, order, and shipment state remain server-authoritative.')}</span>
          </div>
          <div class="login-capabilities">
            <article><b>01</b><div><strong>${pick('统一身份与租户', 'Unified identity and tenant')}</strong><span>${pick('复用管理端登录接口，不创建第二套账号。', 'Reuses admin authentication; no second account system.')}</span></div></article>
            <article><b>02</b><div><strong>${pick('单台设备级控制', 'Physical-device control')}</strong><span>${pick('登录后按设备编号与 SN 查看排期、出库和回仓。', 'Review schedules, dispatch, and returns by device ID and SN.')}</span></div></article>
            <article><b>03</b><div><strong>${pick('隐私与写操作隔离', 'Private data and write isolation')}</strong><span>${pick('普通视图默认脱敏，业务写入必须通过服务端权限校验。', 'Operational views are masked; writes require server authorization.')}</span></div></article>
          </div>
          <footer><span>● ${pick('管理端认证服务', 'Admin authentication service')}</span><small>${pick('当前原型不提交账号或密码', 'Prototype submits no credentials')}</small></footer>
        </section>
        <section class="login-form-panel">
          <div class="login-form-heading">
            <button data-screen-target="workbench" aria-label="${pick('返回工作台原型', 'Back to workbench prototype')}">←</button>
            <div><p>WELCOME BACK</p><h2>${pick('登录设备排期中心', 'Sign in to schedule center')}</h2><span>${pick('使用相机租赁管理后台账号继续', 'Continue with your rental admin account')}</span></div>
          </div>
          <div class="login-methods">
            <button class="is-active"><span>⌁</span>${pick('管理端账号', 'Admin account')}</button>
            <button data-prototype-action="unsupported-login"><span>◇</span>${pick('手机验证码', 'SMS code')}<small>${pick('未接入', 'Unavailable')}</small></button>
            <button data-prototype-action="unsupported-login"><span>▦</span>${pick('扫码', 'QR')}<small>${pick('未接入', 'Unavailable')}</small></button>
          </div>
          <form class="login-form" onsubmit="return false">
            <label><span>${pick('租户名称', 'Tenant name')}</span><div><i>⌂</i><input value="捷租达" aria-label="${pick('租户名称', 'Tenant name')}" /></div></label>
            <label><span>${pick('管理端账号', 'Admin username')}</span><div><i>◎</i><input value="" placeholder="${pick('请输入管理端用户名', 'Enter admin username')}" aria-label="${pick('管理端账号', 'Admin username')}" /></div></label>
            <label><span>${pick('登录密码', 'Password')}</span><div><i>◇</i><input type="password" value="" placeholder="${pick('请输入登录密码', 'Enter password')}" aria-label="${pick('登录密码', 'Password')}" /><button type="button" data-prototype-action="show-password">◉</button></div></label>
            <div class="login-options"><label><input type="checkbox" checked /><span>${pick('记住登录状态', 'Remember session')}</span></label><button type="button" data-prototype-action="help">${pick('登录遇到问题？', 'Need help?')}</button></div>
            <button class="login-submit" type="button" data-screen-target="workbench"><span>${pick('安全登录', 'Secure sign in')}</span><b>→</b></button>
          </form>
          <div class="login-security">
            <span>▣</span>
            <p><strong>${pick('服务端安全边界', 'Server security boundary')}</strong><small>${pick('登录态、租户和角色权限由管理端接口返回；前端不会自行授予业务权限。', 'Session, tenant, and roles come from admin APIs; the client never grants business permissions.')}</small></p>
          </div>
          <footer>${pick('登录即表示进入内部运营系统。请勿在公共设备保存登录状态。', 'Internal operations system. Do not save sessions on shared devices.')}</footer>
        </section>
      </div>
    </section>`;
}
