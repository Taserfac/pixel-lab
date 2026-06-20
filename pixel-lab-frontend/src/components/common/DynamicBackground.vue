<template>
  <div
    class="dynamic-bg"
    :class="[styleClass, motionClass, modeClass, { 'is-paused': isPaused }]"
    :style="pointerStyle"
    aria-hidden="true"
  >
    <div class="ambient-layer ambient-base" />
    <div class="ambient-layer ambient-grid" />
    <div class="ambient-layer ambient-scan" />
    <div class="ambient-layer ambient-glow primary" />
    <div class="ambient-layer ambient-glow accent" />
    <canvas ref="canvasRef" class="ambient-canvas" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  styleName: { type: String, default: 'pixel-grid' },
  motion: { type: String, default: 'reduced' },
  mode: { type: String, default: 'full' }
})

const canvasRef = ref(null)
const pointerX = ref(0.5)
const pointerY = ref(0.45)
const isPaused = ref(false)

let ctx
let animationFrame = 0
let resizeObserver
let particles = []
let targetPointer = { x: 0.5, y: 0.45 }
let currentPointer = { x: 0.5, y: 0.45 }
let pointerFrame = 0
let reduceMotionQuery
let reduceMotionHandler

const effectiveMotion = computed(() => (
  reduceMotionQuery?.matches && props.motion === 'on' ? 'reduced' : props.motion
))
const styleClass = computed(() => `style-${props.styleName}`)
const motionClass = computed(() => `motion-${effectiveMotion.value}`)
const modeClass = computed(() => `mode-${props.mode}`)
const pointerStyle = computed(() => ({
  '--bg-pointer-x': `${pointerX.value * 100}%`,
  '--bg-pointer-y': `${pointerY.value * 100}%`
}))

const particleCount = () => {
  if (typeof window === 'undefined') return 0
  const width = window.innerWidth || 1280
  if (effectiveMotion.value === 'off' || props.mode === 'hidden') return 0
  if (width < 720) return effectiveMotion.value === 'on' ? 34 : 18
  return effectiveMotion.value === 'on' ? 86 : 42
}

const createParticles = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const count = particleCount()
  particles = Array.from({ length: count }, (_, index) => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    baseSize: index % 7 === 0 ? 2.2 : 1 + Math.random() * 1.4,
    vx: (Math.random() - 0.5) * 0.18,
    vy: (Math.random() - 0.5) * 0.14,
    phase: Math.random() * Math.PI * 2,
    hue: index % 5 === 0 ? 'accent' : 'primary'
  }))
}

const resizeCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas || typeof window === 'undefined') return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const { innerWidth, innerHeight } = window
  canvas.width = Math.max(1, Math.floor(innerWidth * dpr))
  canvas.height = Math.max(1, Math.floor(innerHeight * dpr))
  canvas.style.width = `${innerWidth}px`
  canvas.style.height = `${innerHeight}px`
  ctx = canvas.getContext('2d')
  ctx?.setTransform(dpr, 0, 0, dpr, 0, 0)
  createParticles()
}

const drawParticles = (time) => {
  const canvas = canvasRef.value
  if (!canvas || !ctx || isPaused.value || props.mode === 'hidden') return
  const width = window.innerWidth || canvas.width
  const height = window.innerHeight || canvas.height
  ctx.clearRect(0, 0, width, height)

  const mouseX = currentPointer.x * width
  const mouseY = currentPointer.y * height
  const isStarCanvas = props.styleName === 'star-canvas'
  const isSpectrum = props.styleName === 'spectrum'
  const alpha = props.mode === 'subtle' ? 0.16 : 0.34

  for (const particle of particles) {
    const dx = mouseX - particle.x
    const dy = mouseY - particle.y
    const distance = Math.max(80, Math.sqrt(dx * dx + dy * dy))
    const pull = isStarCanvas ? 0.018 : 0.006
    particle.x += particle.vx + (dx / distance) * pull
    particle.y += particle.vy + (dy / distance) * pull
    particle.phase += 0.012

    if (particle.x < -20) particle.x = width + 20
    if (particle.x > width + 20) particle.x = -20
    if (particle.y < -20) particle.y = height + 20
    if (particle.y > height + 20) particle.y = -20

    const pulse = 0.65 + Math.sin(time * 0.0012 + particle.phase) * 0.35
    const size = particle.baseSize * (isSpectrum ? 1.4 : 1) * (0.85 + pulse * 0.35)
    ctx.beginPath()
    ctx.fillStyle = particle.hue === 'accent'
      ? `rgba(124, 90, 239, ${alpha * (0.45 + pulse * 0.5)})`
      : `rgba(22, 199, 132, ${alpha * (0.4 + pulse * 0.5)})`
    ctx.arc(particle.x, particle.y, size, 0, Math.PI * 2)
    ctx.fill()

    if (isStarCanvas && distance < 190) {
      ctx.beginPath()
      ctx.strokeStyle = `rgba(22, 199, 132, ${alpha * (1 - distance / 190) * 0.45})`
      ctx.lineWidth = 1
      ctx.moveTo(particle.x, particle.y)
      ctx.lineTo(mouseX, mouseY)
      ctx.stroke()
    }
  }
}

const animate = (time = 0) => {
  currentPointer.x += (targetPointer.x - currentPointer.x) * 0.08
  currentPointer.y += (targetPointer.y - currentPointer.y) * 0.08
  drawParticles(time)
  if (effectiveMotion.value !== 'off' && props.mode !== 'hidden') {
    animationFrame = window.requestAnimationFrame(animate)
  }
}

const startAnimation = () => {
  if (typeof window === 'undefined') return
  window.cancelAnimationFrame(animationFrame)
  if (effectiveMotion.value === 'off' || props.mode === 'hidden') {
    ctx?.clearRect(0, 0, window.innerWidth, window.innerHeight)
    return
  }
  createParticles()
  animationFrame = window.requestAnimationFrame(animate)
}

const updatePointer = (event) => {
  if (props.mode === 'hidden' || typeof window === 'undefined') return
  targetPointer = {
    x: event.clientX / Math.max(1, window.innerWidth),
    y: event.clientY / Math.max(1, window.innerHeight)
  }
  if (pointerFrame) return
  pointerFrame = window.requestAnimationFrame(() => {
    pointerX.value = targetPointer.x
    pointerY.value = targetPointer.y
    pointerFrame = 0
  })
}

const handleVisibility = () => {
  if (typeof document === 'undefined') return
  isPaused.value = document.hidden
  if (!isPaused.value) startAnimation()
}

onMounted(() => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return
  try {
    reduceMotionQuery = window.matchMedia?.('(prefers-reduced-motion: reduce)')
    reduceMotionHandler = () => startAnimation()
    window.addEventListener('pointermove', updatePointer, { passive: true })
    document.addEventListener('visibilitychange', handleVisibility)
    reduceMotionQuery?.addEventListener?.('change', reduceMotionHandler)
    window.requestAnimationFrame(() => {
      resizeCanvas()
      if (typeof ResizeObserver !== 'undefined') {
        resizeObserver = new ResizeObserver(resizeCanvas)
        resizeObserver.observe(document.body)
      } else {
        window.addEventListener('resize', resizeCanvas, { passive: true })
      }
      startAnimation()
    })
  } catch (error) {
    console.warn('[Pixel Lab] Dynamic background disabled:', error)
  }
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(animationFrame)
  window.cancelAnimationFrame(pointerFrame)
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('pointermove', updatePointer)
  document.removeEventListener('visibilitychange', handleVisibility)
  if (reduceMotionHandler) {
    reduceMotionQuery?.removeEventListener?.('change', reduceMotionHandler)
  }
})

watch(() => [props.styleName, props.motion, props.mode], () => {
  resizeCanvas()
  startAnimation()
})
</script>

<style scoped>
.dynamic-bg {
  --ambient-primary: rgba(22, 199, 132, 0.28);
  --ambient-accent: rgba(124, 90, 239, 0.22);
  --ambient-warm: rgba(255, 180, 84, 0.15);
  --ambient-grid: rgba(20, 26, 23, 0.045);
  --ambient-grid-strong: rgba(22, 199, 132, 0.11);
  --ambient-opacity: 1;

  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  opacity: var(--ambient-opacity);
  transition: opacity var(--transition-slow), filter var(--transition-slow);
  contain: layout paint style;
}

:global([data-theme='dark']) .dynamic-bg {
  --ambient-primary: rgba(25, 212, 142, 0.22);
  --ambient-accent: rgba(151, 117, 255, 0.2);
  --ambient-warm: rgba(255, 180, 84, 0.1);
  --ambient-grid: rgba(255, 255, 255, 0.045);
  --ambient-grid-strong: rgba(25, 212, 142, 0.12);
}

.mode-subtle {
  --ambient-opacity: 0.24;
  filter: saturate(0.85);
}

.mode-hidden {
  --ambient-opacity: 0;
}

.ambient-layer,
.ambient-canvas {
  position: absolute;
  inset: 0;
}

.ambient-base {
  background:
    radial-gradient(circle at var(--bg-pointer-x) var(--bg-pointer-y), rgba(22, 199, 132, 0.14), transparent 28vw),
    radial-gradient(circle at 16% 22%, var(--ambient-primary), transparent 32vw),
    radial-gradient(circle at 78% 18%, var(--ambient-accent), transparent 30vw),
    radial-gradient(circle at 72% 82%, var(--ambient-warm), transparent 26vw),
    linear-gradient(180deg, rgba(255, 255, 255, 0.44), transparent 42%);
  transform: translate3d(calc((50% - var(--bg-pointer-x)) * 0.02), calc((50% - var(--bg-pointer-y)) * 0.02), 0);
}

:global([data-theme='dark']) .ambient-base {
  background:
    radial-gradient(circle at var(--bg-pointer-x) var(--bg-pointer-y), rgba(25, 212, 142, 0.1), transparent 28vw),
    radial-gradient(circle at 16% 22%, var(--ambient-primary), transparent 32vw),
    radial-gradient(circle at 78% 18%, var(--ambient-accent), transparent 30vw),
    radial-gradient(circle at 72% 82%, rgba(255, 180, 84, 0.08), transparent 26vw),
    linear-gradient(180deg, rgba(255, 255, 255, 0.025), transparent 48%);
}

.ambient-grid {
  opacity: 0.78;
  background-image:
    linear-gradient(var(--ambient-grid) 1px, transparent 1px),
    linear-gradient(90deg, var(--ambient-grid) 1px, transparent 1px),
    linear-gradient(135deg, transparent 0 46%, var(--ambient-grid-strong) 47% 53%, transparent 54% 100%);
  background-position:
    calc(var(--bg-pointer-x) * -0.08) calc(var(--bg-pointer-y) * -0.08),
    calc(var(--bg-pointer-x) * -0.08) calc(var(--bg-pointer-y) * -0.08),
    0 0;
  background-size: 28px 28px, 28px 28px, 180px 180px;
  mask-image: radial-gradient(circle at 50% 34%, black, transparent 78%);
}

.ambient-scan {
  opacity: 0;
  background:
    repeating-linear-gradient(90deg, transparent 0 18px, rgba(22, 199, 132, 0.055) 18px 19px, transparent 19px 42px),
    linear-gradient(110deg, transparent 25%, rgba(124, 90, 239, 0.1) 48%, transparent 70%);
  transform: translateX(-8%);
}

.ambient-glow {
  width: 42vw;
  height: 42vw;
  min-width: 460px;
  min-height: 460px;
  border-radius: 50%;
  filter: blur(36px);
  opacity: 0.38;
  mix-blend-mode: multiply;
}

:global([data-theme='dark']) .ambient-glow {
  mix-blend-mode: screen;
  opacity: 0.22;
}

.ambient-glow.primary {
  left: calc(var(--bg-pointer-x) - 26vw);
  top: calc(var(--bg-pointer-y) - 24vw);
  background: var(--ambient-primary);
}

.ambient-glow.accent {
  right: -12vw;
  bottom: -18vw;
  background: var(--ambient-accent);
}

.ambient-canvas {
  opacity: 0.82;
}

.style-aurora .ambient-base {
  background:
    radial-gradient(circle at var(--bg-pointer-x) var(--bg-pointer-y), rgba(255, 255, 255, 0.36), transparent 18vw),
    conic-gradient(from 140deg at 42% 34%, rgba(22, 199, 132, 0.22), rgba(124, 90, 239, 0.2), rgba(91, 141, 239, 0.12), rgba(22, 199, 132, 0.22)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.56), transparent 55%);
  filter: blur(24px) saturate(1.15);
}

.style-aurora .ambient-grid {
  opacity: 0.24;
}

.style-star-canvas .ambient-grid {
  background-size: 36px 36px, 36px 36px, 220px 220px;
  opacity: 0.42;
}

.style-star-canvas .ambient-base {
  background:
    radial-gradient(circle at var(--bg-pointer-x) var(--bg-pointer-y), rgba(124, 90, 239, 0.14), transparent 24vw),
    radial-gradient(circle at 18% 24%, rgba(22, 199, 132, 0.13), transparent 28vw),
    radial-gradient(circle at 82% 72%, rgba(91, 141, 239, 0.12), transparent 28vw);
}

.style-spectrum .ambient-base {
  background:
    radial-gradient(circle at var(--bg-pointer-x) var(--bg-pointer-y), rgba(22, 199, 132, 0.12), transparent 26vw),
    linear-gradient(120deg, rgba(22, 199, 132, 0.14), transparent 32%),
    linear-gradient(240deg, rgba(124, 90, 239, 0.13), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.56), transparent 44%);
}

.style-spectrum .ambient-scan {
  opacity: 0.7;
}

.style-spectrum .ambient-grid {
  background-size: 22px 22px, 22px 22px, 180px 180px;
  opacity: 0.52;
}

.motion-on .ambient-base {
  animation: ambient-drift 18s ease-in-out infinite alternate;
}

.motion-on .ambient-scan {
  animation: spectrum-slide 16s linear infinite;
}

.motion-reduced .ambient-base {
  animation: ambient-drift 32s ease-in-out infinite alternate;
}

.motion-off .ambient-base,
.motion-off .ambient-scan,
.is-paused .ambient-base,
.is-paused .ambient-scan {
  animation: none;
}

@keyframes ambient-drift {
  from {
    transform: translate3d(-1.5%, -1%, 0) scale(1);
  }
  to {
    transform: translate3d(1.5%, 1%, 0) scale(1.04);
  }
}

@keyframes spectrum-slide {
  from {
    transform: translateX(-10%);
  }
  to {
    transform: translateX(10%);
  }
}

@media (max-width: 720px) {
  .dynamic-bg {
    --ambient-opacity: 0.72;
  }

  .mode-subtle {
    --ambient-opacity: 0.16;
  }

  .ambient-grid {
    background-size: 34px 34px, 34px 34px, 220px 220px;
  }

  .ambient-glow {
    min-width: 320px;
    min-height: 320px;
    filter: blur(28px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .ambient-base,
  .ambient-scan {
    animation: none !important;
  }
}
</style>
