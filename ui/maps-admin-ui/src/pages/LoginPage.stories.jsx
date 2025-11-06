import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import { LoginPage } from './LoginPage'
import '../styles/login.css'

export default {
  title: 'Pages/LoginPage',
  component: LoginPage,
  decorators: [
    (Story) => (
      <BrowserRouter>
        <AuthProvider>
          <Story />
        </AuthProvider>
      </BrowserRouter>
    ),
  ],
}

export const Default = {}

export const WithError = {
  parameters: {
    mockData: {
      error: 'Invalid credentials',
    },
  },
}

export const WithSessionTimeout = {
  parameters: {
    mockData: {
      sessionTimeout: true,
    },
  },
}
