import React, { createContext, useCallback, useEffect, useReducer } from 'react'
import { refreshTokenAPI, loginAPI, logoutAPI } from '../services/authService'

export const AuthContext = createContext(null)

const STORAGE_KEY = 'auth_tokens'

const initialState = {
  isAuthenticated: false,
  user: null,
  token: null,
  refreshToken: null,
  expiresIn: null,
  isLoading: true,
  error: null,
  sessionTimeout: false,
}

function authReducer(state, action) {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload }

    case 'LOGIN_SUCCESS': {
      const { token, refreshToken, expiresIn, user } = action.payload
      const now = new Date().getTime()
      const expiryTime = now + expiresIn * 1000
      const tokenData = {
        token,
        refreshToken,
        expiryTime,
        expiresIn,
        user,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(tokenData))
      return {
        ...state,
        isAuthenticated: true,
        user,
        token,
        refreshToken,
        expiresIn,
        error: null,
        sessionTimeout: false,
        isLoading: false,
      }
    }

    case 'LOGIN_FAILURE':
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        token: null,
        refreshToken: null,
        error: action.payload,
        isLoading: false,
      }

    case 'REFRESH_SUCCESS': {
      const { token, refreshToken, expiresIn } = action.payload
      const now = new Date().getTime()
      const expiryTime = now + expiresIn * 1000
      const tokenData = {
        token,
        refreshToken,
        expiryTime,
        expiresIn,
        user: state.user,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(tokenData))
      return {
        ...state,
        token,
        refreshToken,
        expiresIn,
        error: null,
        sessionTimeout: false,
      }
    }

    case 'REFRESH_FAILURE':
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        token: null,
        refreshToken: null,
        error: action.payload,
        sessionTimeout: true,
      }

    case 'LOGOUT_SUCCESS':
      localStorage.removeItem(STORAGE_KEY)
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        token: null,
        refreshToken: null,
        sessionTimeout: false,
        error: null,
        isLoading: false,
      }

    case 'RESTORE_SESSION': {
      const { token, refreshToken, expiryTime, expiresIn, user } = action.payload
      return {
        ...state,
        isAuthenticated: true,
        user,
        token,
        refreshToken,
        expiresIn,
        isLoading: false,
      }
    }

    case 'SESSION_EXPIRED':
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        token: null,
        refreshToken: null,
        sessionTimeout: true,
        isLoading: false,
      }

    case 'CLEAR_ERROR':
      return { ...state, error: null }

    case 'CLEAR_SESSION_TIMEOUT':
      return { ...state, sessionTimeout: false }

    default:
      return state
  }
}

export function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(authReducer, initialState)

  const restoreSession = useCallback(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      try {
        const data = JSON.parse(stored)
        const now = new Date().getTime()
        if (data.expiryTime > now) {
          dispatch({ type: 'RESTORE_SESSION', payload: data })
        } else {
          localStorage.removeItem(STORAGE_KEY)
          dispatch({ type: 'SET_LOADING', payload: false })
        }
      } catch (error) {
        console.error('Failed to restore session:', error)
        dispatch({ type: 'SET_LOADING', payload: false })
      }
    } else {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [])

  useEffect(() => {
    restoreSession()
  }, [restoreSession])

  const login = useCallback(
    async (username, password, useBasicAuth = false) => {
      dispatch({ type: 'SET_LOADING', payload: true })
      try {
        const response = await loginAPI(username, password, useBasicAuth)
        const {
          token,
          refreshToken,
          expiresIn = 3600,
          user = { username },
        } = response

        dispatch({
          type: 'LOGIN_SUCCESS',
          payload: { token, refreshToken, expiresIn, user },
        })

        return { success: true }
      } catch (error) {
        const errorMessage =
          error.response?.data?.message ||
          error.message ||
          'Login failed'
        dispatch({ type: 'LOGIN_FAILURE', payload: errorMessage })
        return { success: false, error: errorMessage }
      }
    },
    [],
  )

  const refreshTokens = useCallback(async () => {
    if (!state.refreshToken) {
      dispatch({
        type: 'REFRESH_FAILURE',
        payload: 'No refresh token available',
      })
      return false
    }

    try {
      const response = await refreshTokenAPI(state.refreshToken)
      const { token, refreshToken, expiresIn = 3600 } = response

      dispatch({
        type: 'REFRESH_SUCCESS',
        payload: { token, refreshToken, expiresIn },
      })

      return true
    } catch (error) {
      const errorMessage =
        error.response?.data?.message ||
        error.message ||
        'Token refresh failed'
      dispatch({ type: 'REFRESH_FAILURE', payload: errorMessage })
      return false
    }
  }, [state.refreshToken])

  const logout = useCallback(async () => {
    try {
      await logoutAPI(state.token)
    } catch (error) {
      console.error('Logout API error:', error)
    } finally {
      dispatch({ type: 'LOGOUT_SUCCESS' })
    }
  }, [state.token])

  const clearError = useCallback(() => {
    dispatch({ type: 'CLEAR_ERROR' })
  }, [])

  const clearSessionTimeout = useCallback(() => {
    dispatch({ type: 'CLEAR_SESSION_TIMEOUT' })
  }, [])

  const value = {
    ...state,
    login,
    refreshTokens,
    logout,
    clearError,
    clearSessionTimeout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
