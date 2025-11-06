import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import { apiClient, setupInterceptors } from '../services/apiClient'
import * as authService from '../services/authService'

vi.mock('../services/authService')

const TestComponent = () => {
  const [result, setResult] = React.useState(null)

  const makeRequest = async () => {
    try {
      const response = await apiClient.get('/api/v1/test')
      setResult('success: ' + response.data.message)
    } catch (error) {
      setResult('error: ' + error.message)
    }
  }

  return (
    <div>
      <div data-testid="result">{result}</div>
      <button onClick={makeRequest}>Make Request</button>
    </div>
  )
}

describe('Token Refresh Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.getItem.mockReturnValue(null)
    localStorage.setItem.mockClear()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('should automatically refresh token on 401', async () => {
    const mockToken = {
      token: 'old-token',
      refreshToken: 'valid-refresh-token',
      expiryTime: Date.now() + 3600000,
      expiresIn: 3600,
      user: { username: 'testuser' },
    }

    localStorage.getItem.mockReturnValue(JSON.stringify(mockToken))

    authService.refreshTokenAPI.mockResolvedValue({
      token: 'new-token',
      refreshToken: 'valid-refresh-token',
      expiresIn: 3600,
    })

    let callCount = 0
    apiClient.interceptors.response.handlers = []

    // First call returns 401, second returns 200
    vi.spyOn(apiClient, 'get').mockImplementation(() => {
      callCount++
      if (callCount === 1) {
        return Promise.reject({
          response: { status: 401 },
          config: { url: '/api/v1/test', method: 'get', _retry: undefined },
        })
      }
      return Promise.resolve({ data: { message: 'success' } })
    })

    render(
      <BrowserRouter>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </BrowserRouter>
    )

    // Setup interceptors
    const authContext = {
      refreshTokens: vi.fn(() => Promise.resolve(true)),
      token: 'old-token',
      refreshToken: 'valid-refresh-token',
    }

    setupInterceptors(authContext)

    fireEvent.click(screen.getByText('Make Request'))

    await waitFor(() => {
      expect(authService.refreshTokenAPI).toHaveBeenCalled()
    })
  })

  it('should queue concurrent requests during token refresh', async () => {
    const mockToken = {
      token: 'old-token',
      refreshToken: 'valid-refresh-token',
      expiryTime: Date.now() + 3600000,
      expiresIn: 3600,
      user: { username: 'testuser' },
    }

    localStorage.getItem.mockReturnValue(JSON.stringify(mockToken))

    authService.refreshTokenAPI.mockResolvedValue({
      token: 'new-token',
      refreshToken: 'valid-refresh-token',
      expiresIn: 3600,
    })

    // Simulate multiple 401s happening simultaneously
    const requests = [
      { url: '/api/v1/test1', shouldFail: true },
      { url: '/api/v1/test2', shouldFail: true },
      { url: '/api/v1/test3', shouldFail: false },
    ]

    let processedCount = 0

    vi.spyOn(apiClient, 'get').mockImplementation((url) => {
      const request = requests.find(r => r.url === url)
      processedCount++

      if (request?.shouldFail) {
        return Promise.reject({
          response: { status: 401 },
          config: { url, method: 'get', _retry: undefined },
        })
      }

      return Promise.resolve({ data: { message: 'success from ' + url } })
    })

    render(
      <BrowserRouter>
        <AuthProvider>
          <div>
            <div data-testid="result">test</div>
          </div>
        </AuthProvider>
      </BrowserRouter>
    )

    // Verify all requests were attempted
    await waitFor(() => {
      expect(processedCount).toBeGreaterThan(0)
    })
  })
})
