const CACHE_NAME = 'pixel-lab-v3'
const APP_SHELL = ['/']

self.addEventListener('install', event => {
  // 强制跳过等待，新版 SW 立即接管
  self.skipWaiting()
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL)))
})

self.addEventListener('fetch', event => {
  const { request } = event
  const url = new URL(request.url)

  if (request.method !== 'GET') return

  // API 与页面导航优先走网络，避免离线缓存污染登录状态或保留旧版 index.html。
  if (url.pathname.startsWith('/api/') || request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then(response => {
          if (request.mode === 'navigate' && response.ok) {
            caches.open(CACHE_NAME).then(cache => cache.put('/', response.clone()))
          }
          return response
        })
        .catch(() => request.mode === 'navigate' ? caches.match('/') : caches.match(request))
    )
    return
  }

  // 带内容哈希的静态资源适合缓存优先。
  event.respondWith(
    caches.match(request).then(cached => cached || fetch(request).then(response => {
      if (response.ok && response.type === 'basic') {
        caches.open(CACHE_NAME).then(cache => cache.put(request, response.clone()))
      }
      return response
    }))
  )
})

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(names => Promise.all(names.filter(name => name.startsWith('pixel-lab-') && name !== CACHE_NAME).map(name => caches.delete(name))))
      .then(() => self.clients.claim())
  )
})
