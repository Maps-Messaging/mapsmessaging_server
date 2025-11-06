import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import { LoginPage } from '../pages/LoginPage'
import * as authService from '../services/authService'

vi.mock('../services/authService')
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => vi.fn(),
  }
})

const renderWithAuth = (component) => {
  return render(
    <BrowserRouter>
      <AuthProvider>{component}</AuthProvider>
    </BrowserRouter>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.getItem.mockReturnValue(null)
    localStorage.setItem.mockClear()
  })

  it('should render login form', () => {
    renderWithAuth(<LoginPage />)

    expect(screen.getByText('Maps Messaging Admin')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument()
  })

  it('should show error when credentials are empty', async () => {
    renderWithAuth(<LoginPage />)

    const loginButton = screen.getByRole('button', { name: 'Login' })
    fireEvent.click(loginButton)

    await waitFor(() => {
      expect(
        screen.getByText('Please enter both username and password')
      ).toBeInTheDocument()
    })
  })

  it('should handle successful login', async () => {
    authService.loginAPI.mockResolvedValue({
      token: 'test-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 3600,
      user: { username: 'testuser' },
    })

    renderWithAuth(<LoginPage />)

    fireEvent.change(screen.getByLabelText('Username'), {
      target: { value: 'testuser' },
    })
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'password' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Login' }))

    await waitFor(() => {
      expect(authService.loginAPI).toHaveBeenCalledWith(
        'testuser',
        'password',
        false
      )
    })
  })

  it('should display error message on login failure', async () => {
    authService.loginAPI.mockRejectedValue({
      response: { data: { message: 'Invalid credentials' } },
    })

    renderWithAuth(<LoginPage />)

    fireEvent.change(screen.getByLabelText('Username'), {
      target: { value: 'testuser' },
    })
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'wrongpassword' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Login' }))

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument()
    })
  })

  it('should support basic auth toggle', async () => {
    renderWithAuth(<LoginPage />)

    const basicAuthCheckbox = screen.getByLabelText('Use Basic Authentication')
    expect(basicAuthCheckbox).not.toBeChecked()

    fireEvent.click(basicAuthCheckbox)
    expect(basicAuthCheckbox).toBeChecked()
  })
})
