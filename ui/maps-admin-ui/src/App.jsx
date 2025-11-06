import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { useConfig } from './hooks/useConfig'
import './App.css'

function App() {
  const { config, loading, error } = useConfig()

  if (loading) {
    return (
      <div className="App">
        <div className="loading">Loading configuration...</div>
      </div>
    )
  }

  return (
    <Router>
      <div className="App">
        <header className="App-header">
          <h1>{config.title}</h1>
          <p>
            Administrative interface for Maps Messaging Server
          </p>
          <div className="version-info">
            <small>Version: {config.version} | Build: {config.buildTime}</small>
            {error && <small className="error"> | Config: {error}</small>}
          </div>
        </header>
        <main>
          <Routes>
            <Route path="/" element={
              <div>
                <h2>Welcome to Maps Messaging Admin</h2>
                <p>This is the administrative interface for the Maps Messaging Server.</p>
                <div className="api-info">
                  <h3>API Configuration</h3>
                  <p><strong>Base URL:</strong> {config.apiBaseUrl}</p>
                  <p><strong>API Version:</strong> {config.apiVersion}</p>
                  <p><strong>OpenAPI Documentation:</strong> 
                    <a href={`${config.apiBaseUrl}/api/docs`} target="_blank" rel="noopener noreferrer">
                      {config.apiBaseUrl}/api/docs
                    </a>
                  </p>
                </div>
              </div>
            } />
          </Routes>
        </main>
      </div>
    </Router>
  )
}

export default App