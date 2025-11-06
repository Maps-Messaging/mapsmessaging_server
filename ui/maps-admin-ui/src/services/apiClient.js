import axios from 'axios'

const STORAGE_KEY = 'auth_tokens'

export const apiClient = axios.create({
  baseURL: '/',
  withCredentials: true,
})

let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

export function setupInterceptors(authContext) {
  apiClient.interceptors.request.use(
    (config) => {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        try {
          const { token } = JSON.parse(stored)
          if (token) {
            config.headers.Authorization = `Bearer ${token}`
          }
        } catch (error) {
          console.error('Failed to parse stored auth:', error)
        }
      }
      return config
    },
    (error) => Promise.reject(error),
  )

  apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
      const originalRequest = error.config

      if (
        error.response?.status === 401 &&
        !originalRequest._retry &&
        authContext
      ) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          })
            .then(token => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              return apiClient(originalRequest)
            })
            .catch(err => Promise.reject(err))
        }

        originalRequest._retry = true
        isRefreshing = true

        return authContext.refreshTokens()
          .then((success) => {
            if (success) {
              const stored = localStorage.getItem(STORAGE_KEY)
              if (stored) {
                const { token } = JSON.parse(stored)
                processQueue(null, token)
                originalRequest.headers.Authorization = `Bearer ${token}`
                return apiClient(originalRequest)
              }
            } else {
              processQueue(new Error('Token refresh failed'), null)
              return Promise.reject(error)
            }
          })
          .catch((err) => {
            processQueue(err, null)
            return Promise.reject(error)
          })
          .finally(() => {
            isRefreshing = false
          })
      }

      return Promise.reject(error)
    },
  )
}

export default apiClient
