<template>
  <span
    class="brand-logo"
    :class="[
      `brand-logo--${size}`,
      `brand-logo--${tone}`,
      { 'brand-logo--shadow': shadow }
    ]"
    aria-hidden="true"
  >
    <svg
      class="brand-logo__mark"
      viewBox="0 0 64 64"
      focusable="false"
    >
      <defs>
        <linearGradient
          id="brandLogoSurface"
          x1="10"
          y1="7"
          x2="54"
          y2="58"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="#23df9a" />
          <stop
            offset="0.56"
            stop-color="#16c784"
          />
          <stop
            offset="1"
            stop-color="#0ba46d"
          />
        </linearGradient>
        <linearGradient
          id="brandLogoStroke"
          x1="20"
          y1="18"
          x2="48"
          y2="48"
          gradientUnits="userSpaceOnUse"
        >
          <stop stop-color="#ffffff" />
          <stop
            offset="1"
            stop-color="#dffcef"
          />
        </linearGradient>
      </defs>
      <rect
        class="brand-logo__surface"
        x="4"
        y="4"
        width="56"
        height="56"
        rx="17"
      />
      <path
        class="brand-logo__shine"
        d="M14 10H36C47 10 55 18 55 29V50C46 31 33 18 14 10Z"
      />
      <g class="brand-logo__pixels">
        <rect
          x="18"
          y="17"
          width="9"
          height="9"
          rx="2.2"
        />
        <rect
          x="30"
          y="17"
          width="9"
          height="9"
          rx="2.2"
        />
        <rect
          x="42"
          y="17"
          width="9"
          height="9"
          rx="2.2"
        />
        <rect
          x="18"
          y="29"
          width="9"
          height="9"
          rx="2.2"
        />
        <rect
          x="42"
          y="29"
          width="9"
          height="9"
          rx="2.2"
        />
        <rect
          x="18"
          y="41"
          width="9"
          height="9"
          rx="2.2"
        />
      </g>
      <rect
        class="brand-logo__pixel brand-logo__pixel--blue"
        x="34"
        y="32"
        width="7"
        height="7"
        rx="2"
      />
      <rect
        class="brand-logo__pixel brand-logo__pixel--gold"
        x="42"
        y="41"
        width="6"
        height="6"
        rx="1.8"
      />
      <path
        class="brand-logo__pen"
        d="M34 48L51 31C53 29 56 31 54 34L37 51L30 54L34 48Z"
      />
      <path
        class="brand-logo__pen-line"
        d="M37 47L50 34"
      />
    </svg>
  </span>
</template>

<script setup>
defineProps({
  size: {
    type: String,
    default: 'md',
    validator: value => ['sm', 'md', 'lg'].includes(value)
  },
  tone: {
    type: String,
    default: 'light',
    validator: value => ['light', 'solid', 'panel'].includes(value)
  },
  shadow: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.brand-logo {
  --logo-size: 40px;
  --logo-border: 0;
  --logo-radius: 12px;
  --logo-bg: transparent;
  --logo-ring: transparent;
  width: var(--logo-size);
  height: var(--logo-size);
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  overflow: hidden;
  border: var(--logo-border) solid var(--border);
  border-radius: var(--logo-radius);
  background: var(--logo-bg);
}

.brand-logo--sm {
  --logo-size: 36px;
  --logo-radius: 10px;
}

.brand-logo--md {
  --logo-size: 40px;
}

.brand-logo--lg {
  --logo-size: 54px;
  --logo-radius: 16px;
}

.brand-logo--solid {
  --logo-bg: transparent;
}

.brand-logo--panel {
  --logo-bg: transparent;
}

.brand-logo--shadow {
  filter: drop-shadow(0 12px 24px rgba(22, 199, 132, 0.24));
}

.brand-logo__mark {
  width: 100%;
  height: 100%;
  display: block;
}

.brand-logo__surface {
  fill: url("#brandLogoSurface");
}

.brand-logo__shine {
  fill: rgba(255, 255, 255, 0.17);
}

.brand-logo__pixels {
  fill: url("#brandLogoStroke");
  filter: drop-shadow(0 1px 1px rgba(4, 75, 47, 0.2));
}

.brand-logo__pixel--blue {
  fill: #5b8def;
}

.brand-logo__pixel--gold {
  fill: #ffb454;
}

.brand-logo__pen {
  fill: #ffffff;
  opacity: 0.95;
}

.brand-logo__pen-line {
  fill: none;
  stroke: #0ba46d;
  stroke-width: 2.4;
  stroke-linecap: round;
}
</style>
