import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import '../styles/login.css'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [useBasicAuth, setUseBasicAuth] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const { login, isAuthenticated, sessionTimeout, clearSessionTimeout } = useAuth()

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true })
    }
  }, [isAuthenticated, navigate])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)

    if (!username || !password) {
      setError('Please enter both username and password')
      setIsLoading(false)
      return
    }

    const result = await login(username, password, useBasicAuth)
    if (result.success) {
      navigate('/dashboard', { replace: true })
    } else {
      setError(result.error || 'Login failed')
    }
    setIsLoading(false)
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>Maps Messaging Admin</h1>
          <p>Server Administration Console</p>
        </div>

        {sessionTimeout && (
          <div className="session-timeout-notice">
            <p>Your session has expired. Please log in again.</p>
            <button
              onClick={clearSessionTimeout}
              className="close-notice"
            >
              ×
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              disabled={isLoading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              disabled={isLoading}
              required
            />
          </div>

          <div className="form-group checkbox">
            <label htmlFor="basicAuth">
              <input
                id="basicAuth"
                type="checkbox"
                checked={useBasicAuth}
                onChange={(e) => setUseBasicAuth(e.target.checked)}
                disabled={isLoading}
              />
              Use Basic Authentication
            </label>
          </div>

          {error && <div className="error-message">{error}</div>}

          <button
            type="submit"
            className="login-button"
            disabled={isLoading}
          >
            {isLoading ? 'Logging in...' : 'Login'}
          </button>
        </form>
      </div>
    </div>
  )
}
