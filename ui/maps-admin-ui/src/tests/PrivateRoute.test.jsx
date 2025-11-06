import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { PrivateRoute } from '../components/PrivateRoute'
import { AuthContext } from '../contexts/AuthContext'

const TestContent = () => <div>Protected Content</div>

const renderWithAuth = (authValue) => {
  return render(
    <BrowserRouter>
      <AuthContext.Provider value={authValue}>
        <PrivateRoute>
          <TestContent />
        </PrivateRoute>
      </AuthContext.Provider>
    </BrowserRouter>
  )
}

describe('PrivateRoute', () => {
  it('should show loading state when isLoading is true', () => {
    const authValue = {
      isLoading: true,
      isAuthenticated: false,
    }

    renderWithAuth(authValue)
    expect(screen.getByText('Loading...')).toBeInTheDocument()
  })

  it('should show protected content when authenticated', () => {
    const authValue = {
      isLoading: false,
      isAuthenticated: true,
    }

    renderWithAuth(authValue)
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('should redirect to login when not authenticated', () => {
    const authValue = {
      isLoading: false,
      isAuthenticated: false,
    }

    renderWithAuth(authValue)
    // Should redirect to /login (component will show Navigate)
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })
})
