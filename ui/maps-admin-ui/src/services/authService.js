import axios from 'axios'
import { apiClient } from './apiClient'

const API_BASE_URL = '/api/v1'

export async function loginAPI(username, password, useBasicAuth = false) {
  try {
    const config = {
      withCredentials: true,
    }

    if (useBasicAuth) {
      config.auth = {
        username,
        password,
      }
      const response = await axios.post(
        `${API_BASE_URL}/login`,
        {},
        config
      )
      return response.data
    } else {
      const response = await apiClient.post(`${API_BASE_URL}/login`, {
        username,
        password,
      })
      return response.data
    }
  } catch (error) {
    throw error
  }
}

export async function logoutAPI(token) {
  try {
    const response = await apiClient.post(`${API_BASE_URL}/logout`, {}, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
    return response.data
  } catch (error) {
    throw error
  }
}

export async function refreshTokenAPI(refreshToken) {
  try {
    const response = await apiClient.post(`${API_BASE_URL}/refreshToken`, {
      refreshToken,
    })
    return response.data
  } catch (error) {
    throw error
  }
}
