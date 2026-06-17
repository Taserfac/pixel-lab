<template>
  <article class="skeleton-card" aria-hidden="true">
    <div v-if="image" class="skeleton-cover">
      <div class="skeleton-bone skeleton-cover-bone" />
    </div>

    <div class="skeleton-body">
      <div class="skeleton-title">
        <div class="skeleton-bone skeleton-title-bone" />
      </div>

      <div v-for="i in rows" :key="i" class="skeleton-line">
        <div
          class="skeleton-bone skeleton-text-bone"
          :style="{ width: lineWidth(i) }"
        />
      </div>

      <div class="skeleton-footer">
        <div class="skeleton-author">
          <div
            v-if="avatar"
            class="skeleton-bone skeleton-avatar-bone"
          />
          <div class="skeleton-bone skeleton-name-bone" />
        </div>
        <div class="skeleton-stats">
          <div class="skeleton-bone skeleton-stat-bone" />
          <div class="skeleton-bone skeleton-stat-bone" />
          <div class="skeleton-bone skeleton-stat-bone" />
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
const props = defineProps({
  rows: {
    type: Number,
    default: 3,
  },
  avatar: {
    type: Boolean,
    default: false,
  },
  image: {
    type: Boolean,
    default: true,
  },
})

const widths = ['80%', '60%', '90%', '45%', '70%', '55%', '85%']

function lineWidth(index) {
  return widths[(index - 1) % widths.length]
}
</script>

<style scoped>
/* ── shimmer keyframes ─────────────────────────────────────── */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* ── base bone ─────────────────────────────────────────────── */
.skeleton-bone {
  background-color: var(--background-muted);
  background-image: linear-gradient(
    90deg,
    transparent 0%,
    var(--border-light) 40%,
    var(--border-light) 60%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

/* ── card shell ────────────────────────────────────────────── */
.skeleton-card {
  background: var(--background-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  break-inside: avoid;
  border: 1px solid var(--border);
}

/* ── image area ────────────────────────────────────────────── */
.skeleton-cover {
  width: 100%;
}

.skeleton-cover-bone {
  width: 100%;
  aspect-ratio: var(--post-ratio, 4 / 3);
  border-radius: 0;
}

/* ── body ──────────────────────────────────────────────────── */
.skeleton-body {
  padding: var(--space-4) var(--space-4) var(--space-5);
}

/* ── title placeholder ─────────────────────────────────────── */
.skeleton-title {
  margin-bottom: var(--space-3);
}

.skeleton-title-bone {
  width: 70%;
  height: 18px;
  border-radius: var(--radius-sm);
}

/* ── text lines ────────────────────────────────────────────── */
.skeleton-line {
  margin-bottom: var(--space-2);
}

.skeleton-line:last-of-type {
  margin-bottom: 0;
}

.skeleton-text-bone {
  height: 12px;
  border-radius: var(--radius-sm);
}

/* ── footer ────────────────────────────────────────────────── */
.skeleton-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border);
}

.skeleton-author {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.skeleton-avatar-bone {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.skeleton-name-bone {
  width: 56px;
  height: 12px;
  border-radius: var(--radius-sm);
}

.skeleton-stats {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.skeleton-stat-bone {
  width: 28px;
  height: 12px;
  border-radius: var(--radius-sm);
}
</style>
