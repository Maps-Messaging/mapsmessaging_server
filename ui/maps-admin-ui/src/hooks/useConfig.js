import { useState, useEffect } from 'react'

export const useConfig = () => {
  const [config, setConfig] = useState({
    apiBaseUrl: 'http://localhost:8080',
    apiVersion: 'v1',
    title: 'Maps Messaging Admin',
    version: '4.1.1',
    buildTime: 'unknown'
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadConfig = async () => {
      try {
        const response = await fetch('/config.json')
        if (!response.ok) {
          throw new Error(`Failed to load config: ${response.status}`)
        }
        const configData = await response.json()
        setConfig(configData)
      } catch (err) {
        console.warn('Failed to load config.json, using defaults:', err)
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    loadConfig()
  }, [])

  return { config, loading, error }
}