import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { setToken } from '../api/client'

const API_ORIGIN = import.meta.env.VITE_API_BASE_URL ?? ''

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await fetch(`${API_ORIGIN}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        setError(body.message || 'Invalid credentials')
        return
      }
      const { token } = await res.json()
      setToken(token)
      navigate('/', { replace: true })
    } catch {
      setError('Network error — check your connection')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--canvas, #f5f7fa)' }}>
      <form onSubmit={(e) => void handleSubmit(e)} style={{ background: '#fff', padding: '2rem', borderRadius: 12, boxShadow: '0 2px 16px rgba(0,0,0,0.1)', width: '100%', maxWidth: 360, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h1 style={{ margin: 0, fontSize: 22, fontWeight: 700 }}>Job Helper</h1>
        {error && (
          <div style={{ background: '#fef2f2', color: '#991b1b', padding: '10px 12px', borderRadius: 6, fontSize: 14 }}>
            {error}
          </div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={{ fontSize: 13, fontWeight: 500 }}>Username</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
            style={{ padding: '8px 12px', border: '1px solid var(--line, #e2e8f0)', borderRadius: 6, fontSize: 14 }}
          />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label style={{ fontSize: 13, fontWeight: 500 }}>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
            style={{ padding: '8px 12px', border: '1px solid var(--line, #e2e8f0)', borderRadius: 6, fontSize: 14 }}
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          style={{ padding: '10px', background: 'var(--primary, #1a4c38)', color: '#fff', border: 'none', borderRadius: 6, fontSize: 15, fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer' }}
        >
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
