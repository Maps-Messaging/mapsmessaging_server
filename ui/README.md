# Maps Messaging Admin UI

A modern React-based administrative interface for the Maps Messaging Server, built with Vite, React Router, and React Query.

## Features

### Authentication
- **Credential-based Login**: POST `/api/v1/login` with username/password
- **Basic Auth Support**: Optional basic authentication fallback
- **Token Management**: Secure token and refresh token storage with expiry tracking
- **Automatic Token Refresh**: Interceptor automatically refreshes expired tokens
- **Session Management**: Session timeout detection and manual token clearing
- **Logout**: POST `/api/v1/logout` with automatic state cleanup

### Routing & Protection
- **PrivateRoute Component**: Guards feature routes from unauthenticated access
- **Public Login Route**: Redirects authenticated users to dashboard
- **Session Timeout Handling**: Displays notice and redirects to login

### UI/Layout
- **Responsive Sidebar Navigation**: Collapsible navigation with grouped menu items
- **Organized Navigation Groups**:
  - Monitoring (Dashboard, Metrics, Logs)
  - Messaging (Topics, Queues, Subscriptions)
  - Resources (Connections, Sessions, Clients)
  - Schemas (Schema Registry, Validators)
  - Security (Users, Groups, Roles, ACLs)
  - Settings (Configuration, Plugins, About)
- **Page Header**: Breadcrumbs and user display
- **Responsive Design**: Mobile-friendly layout with collapsible sidebar

### API Integration
- **Axios HTTP Client**: With request/response interceptors
- **Automatic Token Injection**: Authorization header added to all requests
- **401 Handling**: Automatic token refresh and request retry
- **Queue Management**: Failed requests queued during token refresh
- **React Query**: Server state management with caching and synchronization

### Developer Experience
- **Storybook**: Component library and visual testing
- **Unit Tests**: Vitest-based tests for auth, routing, and components
- **ESLint**: Code quality enforcement
- **Hot Module Replacement**: Fast development with Vite

## Project Structure

```
maps-admin-ui/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── Layout.jsx       # Main layout with sidebar
│   │   ├── PrivateRoute.jsx # Route protection
│   │   └── *.stories.jsx    # Storybook stories
│   ├── contexts/            # React contexts
│   │   └── AuthContext.jsx  # Authentication state management
│   ├── hooks/               # Custom React hooks
│   │   ├── useAuth.js       # Auth context hook
│   │   └── useConfig.js     # Configuration loading
│   ├── pages/               # Page components
│   │   ├── LoginPage.jsx    # Login form
│   │   ├── DashboardPage.jsx # Dashboard
│   │   └── *.stories.jsx    # Page stories
│   ├── services/            # API services
│   │   ├── authService.js   # Authentication API calls
│   │   └── apiClient.js     # Axios client with interceptors
│   ├── styles/              # CSS modules and global styles
│   │   ├── layout.css
│   │   ├── login.css
│   │   └── dashboard.css
│   ├── tests/               # Unit tests
│   │   ├── setup.js
│   │   ├── AuthContext.test.jsx
│   │   ├── LoginPage.test.jsx
│   │   └── PrivateRoute.test.jsx
│   ├── App.jsx              # Main App component
│   ├── App.css              # App styles
│   ├── main.jsx             # Entry point
│   └── index.css            # Global styles
├── .storybook/              # Storybook configuration
├── public/
│   └── config.json          # Runtime configuration
├── scripts/
│   └── process-config.js    # Config generation script
├── vite.config.js           # Vite configuration
├── vitest.config.js         # Test configuration
├── package.json
└── index.html               # HTML entry point
```

## Setup & Development

### Prerequisites
- Node.js 16+ or 18+
- npm or yarn

### Installation

```bash
cd ui/maps-admin-ui
npm install
```

### Development Server

```bash
npm run dev
```

Opens at `http://localhost:3000` with proxy to `http://localhost:8080/api`

### Build

```bash
npm run build
```

Outputs to `../../target/classes/html/admin`

### Production Build

```bash
npm run build:prod
```

Processes environment variables and generates config.json

### Testing

```bash
# Run tests
npm test

# Run tests with UI
npm run test:ui

# Run tests in watch mode
npm test -- --watch
```

### Storybook

```bash
npm run storybook
```

Opens at `http://localhost:6006`

```bash
npm run build-storybook
```

## Authentication Flow

### Login Flow

1. User submits credentials (username/password) on LoginPage
2. `AuthContext.login()` calls `POST /api/v1/login`
3. Server returns: `{ token, refreshToken, expiresIn, user }`
4. Tokens stored in localStorage with expiry calculation
5. User redirected to `/dashboard`
6. AuthContext state updated: `isAuthenticated = true`

### Token Refresh Flow

1. API interceptor detects 401 response
2. If refresh token available, calls `POST /api/v1/refreshToken`
3. New tokens received and stored
4. Original request retried with new token
5. If refresh fails, user redirected to login

### Session Timeout

- Detected when stored token expiry time has passed
- `sessionTimeout` flag set on auth context
- User notified and redirected to login
- Can be cleared manually with `clearSessionTimeout()`

## API Endpoints

### Authentication Endpoints

#### Login
```
POST /api/v1/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}

Response:
{
  "token": "string",
  "refreshToken": "string",
  "expiresIn": 3600,
  "user": {
    "username": "string",
    ...
  }
}
```

#### Logout
```
POST /api/v1/logout
Authorization: Bearer {token}

Response:
{ "success": true }
```

#### Refresh Token
```
POST /api/v1/refreshToken
Content-Type: application/json

{
  "refreshToken": "string"
}

Response:
{
  "token": "string",
  "refreshToken": "string",
  "expiresIn": 3600
}
```

## Configuration

### Environment Variables

Create `.env` based on `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_API_VERSION=v1
VITE_APP_TITLE=Maps Messaging Admin
VITE_SESSION_TIMEOUT_MS=1800000
```

### Runtime Configuration

`public/config.json` is loaded at runtime and can be populated via Maven during build:

```json
{
  "title": "Maps Messaging Admin",
  "version": "1.0.0",
  "buildTime": "2024-01-01T00:00:00Z",
  "apiEndpoint": "/api/v1"
}
```

## Interceptor Architecture

The `apiClient.js` implements a queue-based token refresh mechanism:

1. **Request Interceptor**
   - Adds authorization header with current token
   - Updates token from localStorage if available

2. **Response Interceptor**
   - Catches 401 responses
   - Initiates token refresh if not already in progress
   - Queues failed requests during refresh
   - Retries queued requests after successful refresh
   - Cleans up on failure

3. **Queue Management**
   - `failedQueue[]` stores promises for retry
   - `isRefreshing` flag prevents multiple refresh attempts
   - `processQueue()` resolves/rejects queued requests

## Components

### PrivateRoute

Wrapper component for protected routes:

```jsx
<PrivateRoute>
  <DashboardPage />
</PrivateRoute>
```

Features:
- Checks `isAuthenticated` before rendering
- Shows loading state during auth check
- Redirects unauthenticated users to `/login`

### Layout

Main layout component with sidebar navigation:

```jsx
<Layout>
  <Outlet />
</Layout>
```

Features:
- Collapsible sidebar with grouped navigation
- Responsive design (mobile-friendly)
- Page header with breadcrumbs
- User display and logout button

## Testing

### Auth Context Tests

- Initial unauthenticated state
- Successful login flow
- Failed login handling
- Logout flow
- Token refresh logic

### Login Page Tests

- Form rendering
- Validation (empty fields)
- Successful login submission
- Error message display
- Basic auth toggle

### Route Protection Tests

- Loading state display
- Protected content access when authenticated
- Redirect to login when not authenticated

## Styling

CSS variables defined in `index.css`:

- **Colors**: primary, secondary, success, warning, danger, grays
- **Spacing**: Consistent 8px base unit
- **Shadows**: sm, md, lg variants
- **Border Radius**: 8px default
- **Responsive**: Mobile-first with breakpoints at 768px and 480px

## Best Practices

1. **Authentication**
   - Always use `useAuth()` hook, never access context directly
   - Check `isLoading` before rendering protected content
   - Handle session timeout gracefully

2. **API Calls**
   - Use React Query for server state
   - Leverage automatic retry and caching
   - Handle 401s via interceptor

3. **Components**
   - Keep components focused and single-purpose
   - Use TypeScript for larger apps
   - Add Storybook stories for visual regression testing

4. **Testing**
   - Test happy path and error scenarios
   - Mock API calls with `vi.mock()`
   - Use React Testing Library for component tests

## Troubleshooting

### Session expires too quickly
- Check `expiresIn` returned by server
- Verify localStorage is not being cleared
- Check browser dev tools Network tab for 401s

### Token refresh loops
- Enable debug logs in `apiClient.js`
- Verify refresh token is valid
- Check server logs for refresh endpoint errors

### Sidebar not responsive
- Test on mobile devices or use browser dev tools
- Check media queries are applied
- Verify CSS is loaded (check for CSS errors)

### Tests failing
- Run `npm test -- --reporter=verbose` for details
- Ensure mocks are set up in `beforeEach`
- Check localStorage mock is configured

## Contributing

When adding new features:

1. Create components in `src/components/`
2. Add tests in `src/tests/`
3. Add Storybook stories for UI components
4. Update this README if adding new sections/pages
5. Follow existing code style and patterns

## License

Apache License 2.0 with Commons Clause
