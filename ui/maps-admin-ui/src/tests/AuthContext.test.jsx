import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { AuthProvider, AuthContext } from '../contexts/AuthContext'
import { useAuth } from '../hooks/useAuth'
import * as authService from '../services/authService'

vi.mock('../services/authService')

const TestComponent = () => {
  const auth = useAuth()
  return (
    <div>
      <div data-testid="auth-status">
        {auth.isAuthenticated ? 'Authenticated' : 'Not Authenticated'}
      </div>
      <div data-testid="username">{auth.user?.username || 'No User'}</div>
      <button onClick={() => auth.login('testuser', 'password')}>
        Login
      </button>
      <button onClick={() => auth.logout()}>Logout</button>
      <button onClick={() => auth.refreshTokens()}>Refresh</button>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.getItem.mockReturnValue(null)
    localStorage.setItem.mockClear()
    localStorage.removeItem.mockClear()
  })

  it('should provide initial unauthenticated state', () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    expect(screen.getByTestId('auth-status')).toHaveTextContent(
      'Not Authenticated'
    )
    expect(screen.getByTestId('username')).toHaveTextContent('No User')
  })

  it('should handle successful login', async () => {
    authService.loginAPI.mockResolvedValue({
      token: 'test-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 3600,
      user: { username: 'testuser' },
    })

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('Login'))

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent(
        'Authenticated'
      )
    })

    expect(screen.getByTestId('username')).toHaveTextContent('testuser')
  })

  it('should handle failed login', async () => {
    authService.loginAPI.mockRejectedValue({
      response: { data: { message: 'Invalid credentials' } },
    })

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('Login'))

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent(
        'Not Authenticated'
      )
    })
  })

  it('should handle logout', async () => {
    authService.loginAPI.mockResolvedValue({
      token: 'test-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 3600,
      user: { username: 'testuser' },
    })

    authService.logoutAPI.mockResolvedValue({})

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('Login'))

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent(
        'Authenticated'
      )
    })

    fireEvent.click(screen.getByText('Logout'))

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent(
        'Not Authenticated'
      )
    })
  })

  it('should handle token refresh', async () => {
    const mockToken = {
      token: 'test-token',
      refreshToken: 'test-refresh-token',
      expiryTime: Date.now() + 3600000,
      expiresIn: 3600,
      user: { username: 'testuser' },
    }

    localStorage.getItem.mockReturnValue(JSON.stringify(mockToken))

    authService.refreshTokenAPI.mockResolvedValue({
      token: 'new-token',
      refreshToken: 'new-refresh-token',
      expiresIn: 3600,
    })

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent(
        'Authenticated'
      )
    })

    fireEvent.click(screen.getByText('Refresh'))

    await waitFor(() => {
      expect(authService.refreshTokenAPI).toHaveBeenCalled()
    })
  })
})
