<template>
  <main class="landing">
    <section>
      <p>捷租达 · CAMERA RENTAL</p>
      <h1>摄影设备租赁与归还服务</h1>
      <span>请使用客服发送的专属链接登记设备退回信息。</span>
    </section>
  </main>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  color: #1d2922;
  background: #f3f0e8;
}

section {
  width: min(680px, 100%);
  padding: clamp(32px, 8vw, 72px);
  border: 1px solid #d8d1c3;
  border-radius: 32px;
  background: #fffdf8;
}

p {
  color: #287052;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

h1 {
  margin: 16px 0;
  font-size: clamp(36px, 8vw, 68px);
  line-height: 1;
  letter-spacing: -0.055em;
}

span {
  color: #6f746f;
  line-height: 1.8;
}
</style>
