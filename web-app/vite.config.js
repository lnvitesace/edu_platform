import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Decode JWT payload without verification (for local dev proxy only)
function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(Buffer.from(payload, 'base64url').toString())
  } catch { return null }
}

// Inject X-User-Id and X-User-Role from JWT for local dev (replaces gateway auth filter)
function injectUserHeaders(proxyReq, req) {
  const auth = req.headers['authorization']
  if (auth && auth.startsWith('Bearer ')) {
    const claims = decodeJwt(auth.slice(7))
    if (claims) {
      proxyReq.setHeader('X-User-Id', claims.sub)
      if (claims.role) proxyReq.setHeader('X-User-Role', claims.role)
    }
  }
}

const serviceProxy = (target) => ({
  target,
  changeOrigin: true,
  configure: (proxy) => { proxy.on('proxyReq', injectUserHeaders) }
})

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/auth': serviceProxy('http://localhost:8001'),
      '/api/users': serviceProxy('http://localhost:8001'),
      '/api/courses': serviceProxy('http://localhost:8002'),
      '/api/enrollments': serviceProxy('http://localhost:8002'),
      '/api/progress': serviceProxy('http://localhost:8002'),
      '/api/search': serviceProxy('http://localhost:8005'),
      '/api': serviceProxy('http://localhost:8080'),
    }
  }
})
