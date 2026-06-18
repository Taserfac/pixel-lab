<template>
  <div class="auth-page">
    <main class="auth-shell">
      <section class="auth-visual" aria-label="Pixel Lab 功能概览">
        <img
          src="@/assets/images/loginbg.png"
          alt="Pixel Lab 社区、创意画布、图像工坊和个人中心功能展示"
        >
      </section>
      <section class="auth-panel">
        <slot />
      </section>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  box-sizing: border-box;
  padding: clamp(16px, 3vw, 42px);
  color-scheme: light;
  background: #f3f7f5;
  --background: #ffffff;
  --background-card: #ffffff;
  --background-muted: #f4f7f6;
  --foreground: #111c19;
  --foreground-muted: #6d7a76;
  --border: #dfe8e4;
  --primary: #16c784;
  --primary-hover: #12ac72;
  --primary-muted: #e8faf3;
  --el-bg-color: #ffffff;
  --el-bg-color-overlay: #ffffff;
  --el-fill-color-blank: #ffffff;
  --el-fill-color-light: #f4f7f6;
  --el-text-color-primary: #111c19;
  --el-text-color-regular: #4e5d58;
  --el-text-color-placeholder: #9aa7a3;
  --el-border-color: #dfe8e4;
  --el-border-color-hover: #a9bbb4;
  --el-color-primary: #16c784;
}

.auth-shell {
  width: min(1480px, 100%);
  min-height: min(780px, calc(100vh - clamp(32px, 6vw, 84px)));
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(380px, 0.8fr);
  overflow: hidden;
  border: 1px solid rgba(17, 28, 25, 0.08);
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgba(26, 72, 55, 0.12);
}

.auth-visual {
  min-width: 0;
  overflow: hidden;
  background: #eef8f4;
}

.auth-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center;
}

.auth-panel {
  display: grid;
  place-items: center;
  padding: clamp(30px, 5vw, 72px);
  background: #ffffff;
}

@media (max-width: 980px) {
  .auth-shell {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .auth-visual {
    height: clamp(220px, 35vw, 330px);
  }

  .auth-visual img {
    object-position: center 18%;
  }
}

@media (max-width: 560px) {
  .auth-page {
    padding: 0;
    background: #ffffff;
  }

  .auth-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .auth-visual {
    height: 180px;
  }

  .auth-panel {
    place-items: start center;
    padding: 30px 24px 44px;
  }
}
</style>
