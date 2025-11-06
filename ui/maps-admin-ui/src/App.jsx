import React, { useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClientProvider, QueryClient } from '@tanstack/react-query'
import { SnackbarProvider } from 'notistack'
import { AuthProvider, AuthContext } from './contexts/AuthContext'
import { setupInterceptors } from './services/apiClient'
import { PrivateRoute } from './components/PrivateRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { useConfig } from './hooks/useConfig'
import './App.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      gcTime: 1000 * 60 * 10,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function AppContent() {
  const { config, loading, error } = useConfig()
  const authContext = React.useContext(AuthContext)

  useEffect(() => {
    if (authContext) {
      setupInterceptors(authContext)
    }
  }, [authContext])

  if (loading) {
    return (
      <div className="app-loading">
        <div className="loader">
          <p>Loading...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="app">
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/*"
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route path="" element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          {/* Additional routes will be added here */}
        </Route>
      </Routes>
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SnackbarProvider maxSnack={3}>
        <AuthProvider>
          <Router>
            <AppContent />
          </Router>
        </AuthProvider>
      </SnackbarProvider>
    </QueryClientProvider>
  )
}

export default App
