import { BrowserRouter } from 'react-router-dom'
import { AuthProvider, AuthContext } from '../contexts/AuthContext'
import { Layout } from './Layout'
import '../styles/layout.css'

export default {
  title: 'Components/Layout',
  component: Layout,
  decorators: [
    (Story) => (
      <BrowserRouter>
        <AuthProvider>
          <div style={{ display: 'flex', height: '100vh' }}>
            <Story />
          </div>
        </AuthProvider>
      </BrowserRouter>
    ),
  ],
}

export const Default = {
  args: {
    user: {
      username: 'admin',
    },
  },
}

export const SidebarCollapsed = {
  parameters: {
    sidebarOpen: false,
  },
}
