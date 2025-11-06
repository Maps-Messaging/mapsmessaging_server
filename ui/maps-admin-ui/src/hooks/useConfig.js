import { useState, useEffect } from 'react'

export function useConfig() {
  const [config, setConfig] = useState({
    title: 'Maps Messaging',
    version: 'Unknown',
    buildTime: 'Unknown',
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadConfig = async () => {
      try {
        const response = await fetch('./config.json')
        if (!response.ok) {
          throw new Error('Failed to load config')
        }
        const data = await response.json()
        setConfig(prev => ({ ...prev, ...data }))
        setError(null)
      } catch (err) {
        console.error('Error loading config:', err)
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    loadConfig()
  }, [])

  return { config, loading, error }
}
