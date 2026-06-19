const CACHE_NAME = 'pixel-lab-v2'

self.addEventListener('install', () => {
  self.skipWaiting()
})

self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)

  if (url.origin !== self.location.origin) return

  // Never cache authenticated API responses.
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(fetch(request))
    return
  }

  // A deployed index.html may reference a completely new set of hashed assets,
  // so navigation must always prefer the current server response.
  if (request.mode === 'navigate') {
    event.respondWith(fetch(request, { cache: 'no-store' }))
    return
  }

  // Hashed Vite assets are safe to cache; a new build produces new URLs.
  if (url.pathname.startsWith('/assets/')) {
    event.respondWith(
      caches.match(request).then((cachedResponse) => {
        if (cachedResponse) return cachedResponse

        return fetch(request).then((response) => {
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response
          }
          const responseClone = response.clone()
          caches.open(CACHE_NAME).then(cache => cache.put(request, responseClone))
          return response
        })
      })
    )
  }
})

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const cacheNames = await caches.keys()
    await Promise.all(
      cacheNames
        .filter(cacheName => cacheName !== CACHE_NAME)
        .map(cacheName => caches.delete(cacheName))
    )
    await self.clients.claim()

    // Recover tabs that were opened through the old cache-first worker.
    const clients = await self.clients.matchAll({ type: 'window' })
    await Promise.all(clients.map(client => client.navigate(client.url)))
  })())
})
