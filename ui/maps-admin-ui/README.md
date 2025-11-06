# Maps Admin UI

A modern React-based admin interface for the Maps Messaging Server, featuring OpenAPI-driven client generation and type-safe API interactions.

## Features

- **Type-safe API client**: Automatically generated from OpenAPI specification
- **React Query integration**: Optimistic updates, caching, and background refetching
- **Authentication handling**: Built-in support for API keys and bearer tokens
- **Error handling**: Centralized error handling with automatic retry logic
- **Server-Sent Events**: Real-time streaming support
- **TypeScript**: Full type safety throughout the application

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Download the latest OpenAPI specification
npm run openapi:download

# Generate the TypeScript client
npm run openapi:generate
```

### Development

```bash
# Start development server
npm run dev

# Run type checking
npm run type-check

# Run linting
npm run lint

# Fix linting issues
npm run lint:fix
```

### Building

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

### Testing

```bash
# Run all tests
npm test

# Run smoke tests to validate generated API
npm run test:smoke
```

## OpenAPI Client Generation

This project uses OpenAPI generator to create type-safe API clients from the Maps Messaging Server OpenAPI specification.

### Scripts

- `npm run openapi:download` - Downloads the latest OpenAPI spec from `http://cloud.kritikal.org:8080/openapi.json`
- `npm run openapi:generate` - Generates TypeScript client code using the `typescript-fetch` template
- `npm run openapi:ensure` - Runs both download and generate in sequence

### Configuration

The OpenAPI generator is configured via `openapi-generator.json`:

- Uses `typescript-fetch` template
- Generates separate API and model packages
- Enables strict TypeScript settings
- Uses camelCase for properties and parameters
- Supports string enums

### Generated Code Structure

```
src/api/generated/
├── api/           # Generated API classes
├── models/        # Generated model interfaces
└── index.ts       # Main exports
```

## Usage

### API Client

```typescript
import { getApiClient } from '@/api/ApiClient';

// Get the singleton API client instance
const apiClient = getApiClient();

// Or create with custom configuration
const customClient = new ApiClient({
  baseUrl: 'https://api.example.com',
  bearerToken: 'your-token',
});
```

### React Query Hooks

```typescript
import { createQueryHooks } from '@/api/ReactQueryHooks';
import { UserApi } from '@/api/generated';

// Create typed hooks for a specific API endpoint
const { useQuery } = createQueryHooks(
  UserApi.prototype.getUserById,
  { queryKeyPrefix: ['user'] }
);

// Use in a component
function UserComponent({ userId }: { userId: string }) {
  const { data: user, isLoading, error } = useQuery({ id: userId });
  
  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  
  return <div>{user?.name}</div>;
}
```

### Resource Hooks

```typescript
import { createResourceHooks } from '@/api/ReactQueryHooks';
import { UserApi } from '@/api/generated';

// Create complete CRUD hooks for a resource
const userHooks = createResourceHooks(
  {
    get: UserApi.prototype.getUserById,
    list: UserApi.prototype.getUsers,
    create: UserApi.prototype.createUser,
    update: UserApi.prototype.updateUser,
    delete: UserApi.prototype.deleteUser,
  },
  'user'
);

// Use in components
function UserManagement() {
  const { data: users } = userHooks.useList();
  const createUser = userHooks.useCreate();
  
  const handleCreate = (userData) => {
    createUser.mutate(userData);
  };
  
  return (
    // Your UI here
  );
}
```

### Server-Sent Events

```typescript
import { useSSE } from '@/api/ReactQueryHooks';

function RealtimeUpdates() {
  const { data, isConnected, error } = useSSE('events/stream', {
    onMessage: (data) => {
      console.log('Received:', data);
    },
    onError: (error) => {
      console.error('SSE error:', error);
    },
  });
  
  return (
    <div>
      <div>Status: {isConnected ? 'Connected' : 'Disconnected'}</div>
      <div>Latest data: {JSON.stringify(data)}</div>
    </div>
  );
}
```

## CI/CD Integration

### Pre-build Checks

Add this to your CI pipeline to ensure the API client is up-to-date:

```bash
# Ensure OpenAPI spec is downloaded and client is generated
npm run openapi:ensure

# Run smoke tests to validate generated code
npm run test:smoke
```

### Generated Code

The generated API client code is committed to the repository to ensure:

- Reproducible builds
- Type checking without requiring generation during build
- Code review of generated changes
- Offline development capability

When the OpenAPI specification changes:

1. Run `npm run openapi:ensure`
2. Review the generated changes
3. Update any custom code that uses the modified APIs
4. Commit the changes

## Project Structure

```
ui/maps-admin-ui/
├── src/
│   ├── api/
│   │   ├── generated/          # Auto-generated API client
│   │   ├── ApiClient.ts       # API client wrapper
│   │   └── ReactQueryHelpers.ts # React Query utilities
│   ├── components/            # React components
│   ├── hooks/                 # Custom React hooks
│   ├── pages/                 # Page components
│   ├── test/                  # Test utilities and setup
│   └── test/smoke/            # Smoke tests for generated API
├── openapi/
│   └── openapi.json          # Downloaded OpenAPI specification
├── openapi-generator.json    # Generator configuration
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## Configuration

### Environment Variables

- `VITE_API_BASE_URL` - Override the default API base URL
- `VITE_API_KEY` - Default API key for development

### Customizing Generation

To customize the generated client:

1. Modify `openapi-generator.json`
2. Add custom templates to a `templates/` directory
3. Update the generation script in `package.json`

## Troubleshooting

### Common Issues

1. **Generated code doesn't compile**: 
   - Ensure the OpenAPI spec is valid
   - Check for breaking changes in the API specification
   - Run `npm run openapi:ensure` to regenerate

2. **Type errors with generated code**:
   - Check TypeScript version compatibility
   - Verify the OpenAPI generator configuration
   - Review the generated model types

3. **API requests failing**:
   - Verify the base URL configuration
   - Check authentication setup
   - Review network requests in browser dev tools

### Debug Mode

Enable debug logging for API requests:

```typescript
const apiClient = new ApiClient({
  baseUrl: 'http://localhost:8080/api',
});

// Console will show request timing and errors
```

## Contributing

1. Always run `npm run openapi:ensure` after API changes
2. Update smoke tests when adding new API usage patterns
3. Keep generated code committed for reproducible builds
4. Document any custom API client configurations

## License

This project is part of the Maps Messaging Server and follows the same licensing terms.