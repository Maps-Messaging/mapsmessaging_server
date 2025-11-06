# MAPS Admin UI

A modern React + TypeScript admin dashboard for the MAPS (Messaging Server) platform, built with Vite.

## Prerequisites

- **Node.js**: v18.0.0 or later (v20.0.0 or later recommended)
- **npm**: v9.0.0 or later (or your preferred package manager)

## Project Structure

```
src/
├── components/       # Reusable UI components
├── features/         # Feature-specific modules (Dashboard, Monitoring, etc.)
├── routes/          # Route definitions
├── providers/       # Context and provider components
├── utils/           # Utility functions and helpers
├── test/            # Test setup and utilities
├── App.tsx          # Main App component with routing
├── main.tsx         # Application entry point
└── index.css        # Global styles
```

## Getting Started

### Install Dependencies

```bash
npm install
```

### Development Server

Start the development server with hot-module replacement:

```bash
npm run dev
```

The server will start at `http://localhost:5173` by default.

To connect to a remote API server, set the `VITE_API_BASE_URL` environment variable:

```bash
VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

Or create a `.env.local` file:

```
VITE_API_BASE_URL=http://localhost:8080/api
```

### Build for Production

Create a minified production build optimized for static hosting:

```bash
npm run build
```

The build output will be generated in the `dist/` directory and is ready to be deployed under the server's `www` directory. All assets use relative paths for proper static site hosting.

### Preview Production Build

Preview the production build locally:

```bash
npm run preview
```

## Development

### Type Checking

Run TypeScript type checking:

```bash
npm run type-check
```

### Linting

Run ESLint to check code quality:

```bash
npm run lint
```

Fix linting issues automatically:

```bash
npm run lint:fix
```

### Code Formatting

Format code with Prettier:

```bash
npm run format
```

### Testing

Run tests with Vitest:

```bash
npm run test
```

Run tests with UI:

```bash
npm run test:ui
```

## Features

The admin dashboard includes routing placeholders for:

- **Dashboard**: Overview and key metrics
- **Monitoring**: System monitoring and metrics
- **Messaging**: Message configuration and management
- **Resources**: Resource allocation and management
- **Schemas**: Schema management and validation
- **Auth**: Authentication and authorization settings
- **Settings**: System configuration

## Configuration

### Environment Variables

Copy `.env.example` to `.env.local` to set environment variables:

```bash
cp .env.example .env.local
```

Available variables:

- `VITE_API_BASE_URL`: Base URL for API requests (default: `http://localhost:8080/api`)
- `VITE_APP_NAME`: Application name
- `VITE_APP_VERSION`: Application version
- `VITE_ENABLE_ANALYTICS`: Enable analytics tracking
- `VITE_ENABLE_DEBUGGING`: Enable debug logging

### Path Aliases

TypeScript path aliases are configured for cleaner imports:

- `@/*` → `src/*`
- `@components/*` → `src/components/*`
- `@features/*` → `src/features/*`
- `@routes/*` → `src/routes/*`
- `@providers/*` → `src/providers/*`
- `@utils/*` → `src/utils/*`

## Dependencies

### Core
- **React 18**: UI library
- **React Router v6**: Client-side routing
- **TypeScript**: Type-safe development

### State Management & Data Fetching
- **TanStack React Query v5**: Server state management and caching

### UI & Styling
- **Material-UI (MUI) v5**: Component library
- **Emotion**: CSS-in-JS styling

### Charting
- **Recharts**: Chart and visualization library

### Notifications
- **Sonner**: Toast notification library

### Testing
- **Vitest**: Unit testing framework
- **React Testing Library**: Component testing utilities
- **jsdom**: DOM simulation for testing

### Development Tools
- **Vite**: Build tool and dev server
- **ESLint**: Code linting
- **Prettier**: Code formatting

## Build Output

The production build (`npm run build`) generates:

- Minified JavaScript with tree-shaking
- Optimized CSS with code splitting
- Relative asset paths suitable for deployment under a static `www` directory
- Source maps disabled for production (security/size)

Assets are organized as:

```
dist/
├── index.html
├── assets/
│   ├── [name]-[hash].js
│   ├── [name]-[hash].css
│   └── ...
└── vite.svg
```

## Deployment

The built `dist/` directory is ready for deployment:

1. Run `npm run build` to create the production bundle
2. Copy the contents of `dist/` to your server's `www` directory
3. Configure your web server to serve `index.html` for all routes (for SPA routing support)

### Static Hosting Configuration

The built assets use root-relative paths (`/assets/...`), making them suitable for serving from the web root. Configure your web server to serve `index.html` for all non-existent files (for SPA routing).

**For Nginx:**

```nginx
server {
    listen 80;
    server_name example.com;

    root /path/to/www;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**For Apache:**

```apache
<Directory /path/to/www>
    <IfModule mod_rewrite.c>
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </IfModule>
</Directory>

# Cache static assets
<FilesMatch "\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$">
    Header set Cache-Control "max-age=31536000, public, immutable"
</FilesMatch>
```

### Custom Base Path

If deploying under a sub-path (e.g., `/admin/ui/`), update the `base` setting in `vite.config.ts`:

```typescript
build: {
  base: '/admin/ui/',  // Trailing slash required
  // ... rest of config
}
```

Then rebuild with `npm run build`.

## License

See the LICENSE file in the repository root.
