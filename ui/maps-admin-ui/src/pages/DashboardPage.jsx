import React from 'react'
import { useAuth } from '../hooks/useAuth'
import '../styles/dashboard.css'

export function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="dashboard-page">
      <div className="page-title">
        <h1>Dashboard</h1>
        <p>Welcome back, {user?.username}!</p>
      </div>

      <div className="dashboard-grid">
        <div className="dashboard-card">
          <h2>Connected Clients</h2>
          <div className="stat-value">--</div>
          <p className="stat-label">Active connections</p>
        </div>

        <div className="dashboard-card">
          <h2>Message Throughput</h2>
          <div className="stat-value">--</div>
          <p className="stat-label">Messages/sec</p>
        </div>

        <div className="dashboard-card">
          <h2>Server Status</h2>
          <div className="stat-value">Healthy</div>
          <p className="stat-label">All systems operational</p>
        </div>

        <div className="dashboard-card">
          <h2>Uptime</h2>
          <div className="stat-value">--</div>
          <p className="stat-label">Time since startup</p>
        </div>
      </div>

      <div className="dashboard-section">
        <h2>Recent Activity</h2>
        <p>Recent activity will be displayed here</p>
      </div>
    </div>
  )
}
