import React, { useState } from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import '../styles/layout.css'

const navigationGroups = [
  {
    title: 'Monitoring',
    icon: '📊',
    items: [
      { label: 'Dashboard', path: '/dashboard' },
      { label: 'Metrics', path: '/metrics' },
      { label: 'Logs', path: '/logs' },
    ],
  },
  {
    title: 'Messaging',
    icon: '💬',
    items: [
      { label: 'Topics', path: '/messaging/topics' },
      { label: 'Queues', path: '/messaging/queues' },
      { label: 'Subscriptions', path: '/messaging/subscriptions' },
    ],
  },
  {
    title: 'Resources',
    icon: '📦',
    items: [
      { label: 'Connections', path: '/resources/connections' },
      { label: 'Sessions', path: '/resources/sessions' },
      { label: 'Clients', path: '/resources/clients' },
    ],
  },
  {
    title: 'Schemas',
    icon: '📋',
    items: [
      { label: 'Schema Registry', path: '/schemas/registry' },
      { label: 'Validators', path: '/schemas/validators' },
    ],
  },
  {
    title: 'Security',
    icon: '🔒',
    items: [
      { label: 'Users', path: '/security/users' },
      { label: 'Groups', path: '/security/groups' },
      { label: 'Roles', path: '/security/roles' },
      { label: 'ACLs', path: '/security/acls' },
    ],
  },
  {
    title: 'Settings',
    icon: '⚙️',
    items: [
      { label: 'Configuration', path: '/settings/configuration' },
      { label: 'Plugins', path: '/settings/plugins' },
      { label: 'About', path: '/settings/about' },
    ],
  },
]

export function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [expandedGroups, setExpandedGroups] = useState({})
  const location = useLocation()
  const { logout, user } = useAuth()

  const toggleGroup = (groupTitle) => {
    setExpandedGroups(prev => ({
      ...prev,
      [groupTitle]: !prev[groupTitle],
    }))
  }

  const handleLogout = async () => {
    await logout()
  }

  const isActive = (path) => location.pathname === path

  return (
    <div className="layout-container">
      <aside className={`sidebar ${sidebarOpen ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          <h1 className="sidebar-title">Maps Admin</h1>
          <button
            className="sidebar-toggle"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label="Toggle sidebar"
          >
            {sidebarOpen ? '←' : '→'}
          </button>
        </div>

        <nav className="sidebar-nav">
          {navigationGroups.map(group => (
            <div key={group.title} className="nav-group">
              <button
                className="nav-group-title"
                onClick={() => toggleGroup(group.title)}
              >
                <span className="nav-group-icon">{group.icon}</span>
                {sidebarOpen && (
                  <>
                    <span className="nav-group-label">{group.title}</span>
                    <span className="nav-group-toggle">
                      {expandedGroups[group.title] ? '▼' : '▶'}
                    </span>
                  </>
                )}
              </button>

              {(expandedGroups[group.title] || !sidebarOpen) && sidebarOpen && (
                <div className="nav-group-items">
                  {group.items.map(item => (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`nav-item ${isActive(item.path) ? 'active' : ''}`}
                    >
                      {item.label}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            {sidebarOpen && (
              <>
                <div className="user-name">{user?.username || 'User'}</div>
                <button className="logout-btn" onClick={handleLogout}>
                  Logout
                </button>
              </>
            )}
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="page-header">
          <div className="breadcrumbs">
            {/* Breadcrumbs will be populated by page components */}
          </div>
          <div className="header-right">
            <span className="user-display">{user?.username || 'Guest'}</span>
          </div>
        </header>

        <div className="page-content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
