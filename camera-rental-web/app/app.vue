<template>
  <NuxtRouteAnnouncer />
  <main class="site-shell">
    <section class="hero">
      <div class="hero__copy">
        <p class="eyebrow">捷租达 · Camera Rental Platform</p>
        <h1>相机与无人机租赁，一套系统管订单、设备和排期。</h1>
        <p class="lede">
          客户官网正在逐步接入商品、租期、下单与支付能力。运营后台已独立部署，可处理设备台账、
          闲鱼订单、扫码发货和排期中心。
        </p>
        <div class="actions">
          <a class="button button--primary" href="/admin/">进入管理后台</a>
          <a class="button button--ghost" href="/admin/schedule-center/">打开设备排期中心</a>
        </div>
      </div>
      <div class="hero__panel" aria-label="平台模块">
        <div class="metric">
          <span>后台</span>
          <strong>订单与设备</strong>
        </div>
        <div class="metric">
          <span>排期</span>
          <strong>单台 SN 占用</strong>
        </div>
        <div class="metric">
          <span>渠道</span>
          <strong>闲鱼 / 闲管家</strong>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.site-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 48px 24px;
  color: #10201a;
  background:
    radial-gradient(circle at 12% 16%, rgba(31, 178, 122, 0.22), transparent 32%),
    radial-gradient(circle at 86% 18%, rgba(230, 168, 62, 0.2), transparent 30%),
    linear-gradient(135deg, #f4f0e6 0%, #eef7ef 46%, #dfece5 100%);
}

.hero {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
  gap: 32px;
  align-items: center;
}

.hero__copy,
.hero__panel {
  border: 1px solid rgba(16, 32, 26, 0.12);
  background: rgba(255, 252, 244, 0.82);
  box-shadow: 0 24px 80px rgba(33, 54, 44, 0.16);
  backdrop-filter: blur(18px);
}

.hero__copy {
  padding: clamp(32px, 5vw, 64px);
  border-radius: 32px;
}

.eyebrow {
  margin: 0 0 18px;
  color: #13754f;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  max-width: 780px;
  font-size: clamp(38px, 6vw, 72px);
  line-height: 0.98;
  letter-spacing: -0.06em;
}

.lede {
  margin: 24px 0 0;
  max-width: 660px;
  color: #4b5c53;
  font-size: 17px;
  line-height: 1.8;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 34px;
}

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 20px;
  border-radius: 999px;
  color: inherit;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.button--primary {
  color: white;
  background: #10201a;
}

.button--ghost {
  border: 1px solid rgba(16, 32, 26, 0.18);
  background: rgba(255, 255, 255, 0.52);
}

.hero__panel {
  display: grid;
  gap: 14px;
  padding: 22px;
  border-radius: 28px;
}

.metric {
  padding: 24px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(241, 247, 239, 0.72));
}

.metric span {
  display: block;
  margin-bottom: 8px;
  color: #6a7b72;
  font-size: 12px;
  font-weight: 800;
}

.metric strong {
  display: block;
  font-size: 24px;
  letter-spacing: -0.04em;
}

@media (max-width: 860px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
