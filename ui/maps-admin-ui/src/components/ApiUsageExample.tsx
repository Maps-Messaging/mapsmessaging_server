/**
 * Example component showing how to use the generated API client
 * This will work once the OpenAPI client is generated
 */

import React, { useState } from 'react';

// These imports will be available after OpenAPI code generation
// Uncomment these lines after running `npm run openapi:ensure`
// import { getApiClient } from '../api/ApiClient';
// import { createResourceHooks } from '../api/ReactQueryHelpers';
// import { DefaultApi, User, CreateUserRequest } from '../api/generated';

interface ApiUsageExampleProps {}

export const ApiUsageExample: React.FC<ApiUsageExampleProps> = () => {
  // Example state for demonstration
  const [isGenerating, setIsGenerating] = useState(false);

  // Example usage (will work after API generation):
  
  /*
  // Create resource hooks for user management
  const userHooks = createResourceHooks(
    {
      get: DefaultApi.prototype.getUserById,
      list: DefaultApi.prototype.getUsers,
      create: DefaultApi.prototype.createUser,
      update: DefaultApi.prototype.updateUser,
      delete: DefaultApi.prototype.deleteUser,
    },
    'user'
  );

  // Use the hooks in your component
  const { data: users, isLoading: loadingUsers } = userHooks.useList();
  const createUser = userHooks.useCreate();
  const deleteUser = userHooks.useDelete();

  const handleCreateUser = (userData: CreateUserRequest) => {
    createUser.mutate(userData, {
      onSuccess: () => {
        console.log('User created successfully');
      },
      onError: (error) => {
        console.error('Failed to create user:', error);
      },
    });
  };

  const handleDeleteUser = (userId: string) => {
    deleteUser.mutate({ id: userId });
  };

  // Example of using SSE for real-time updates
  const { data: realtimeData, isConnected } = useSSE('events/stream', {
    onMessage: (data) => {
      console.log('Real-time update:', data);
    },
  });
  */

  const handleGenerateApi = async () => {
    setIsGenerating(true);
    try {
      // This would trigger the API generation
      await fetch('/api/generate-openapi', { method: 'POST' });
    } catch (error) {
      console.error('Failed to generate API:', error);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="api-usage-example">
      <h2>API Usage Examples</h2>
      
      <div className="code-examples">
        <h3>1. Basic Query Usage</h3>
        <pre>
{`// Import the generated API and hooks
import { createQueryHooks } from '@/api/ReactQueryHelpers';
import { DefaultApi } from '@/api/generated';

// Create typed hooks
const { useQuery } = createQueryHooks(
  DefaultApi.prototype.getUsers,
  { queryKeyPrefix: ['users'] }
);

// Use in component
function UserList() {
  const { data: users, isLoading, error } = useQuery();
  
  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  
  return (
    <ul>
      {users?.map(user => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}`}
        </pre>

        <h3>2. CRUD Operations with Resource Hooks</h3>
        <pre>
{`// Create complete CRUD hooks
const userHooks = createResourceHooks(
  {
    get: DefaultApi.prototype.getUserById,
    list: DefaultApi.prototype.getUsers,
    create: DefaultApi.prototype.createUser,
    update: DefaultApi.prototype.updateUser,
    delete: DefaultApi.prototype.deleteUser,
  },
  'user'
);

// Use in component
function UserManagement() {
  const { data: users } = userHooks.useList();
  const createUser = userHooks.useCreate();
  
  const handleCreate = (userData) => {
    createUser.mutate(userData);
  };
  
  return (
    <div>
      {/* Your UI here */}
    </div>
  );
}`}
        </pre>

        <h3>3. Real-time Updates with SSE</h3>
        <pre>
{`// Use Server-Sent Events for streaming
import { useSSE } from '@/api/ReactQueryHelpers';

function RealtimeDashboard() {
  const { data, isConnected, error } = useSSE('events/stream', {
    onMessage: (data) => {
      console.log('New event:', data);
    },
  });
  
  return (
    <div>
      <div>Status: {isConnected ? 'Connected' : 'Disconnected'}</div>
      <div>Latest: {JSON.stringify(data)}</div>
    </div>
  );
}`}
        </pre>

        <h3>4. Custom API Client Configuration</h3>
        <pre>
{`// Configure the API client
import { getApiClient } from '@/api/ApiClient';

const apiClient = getApiClient({
  baseUrl: 'https://api.example.com',
  bearerToken: 'your-jwt-token',
});

// Update authentication dynamically
apiClient.updateAuth({
  bearerToken: 'new-token',
});

// Update base URL
apiClient.updateBaseUrl('https://new-api.example.com');
}`}
        </pre>
      </div>

      <div className="generation-status">
        <h3>Current Status</h3>
        <p>
          The API client generation is set up but not yet generated. 
          Run the following commands to get started:
        </p>
        
        <div className="command-block">
          <code>npm run openapi:ensure</code>
          <button 
            onClick={handleGenerateApi}
            disabled={isGenerating}
          >
            {isGenerating ? 'Generating...' : 'Generate API Client'}
          </button>
        </div>
        
        <p>
          After generation, uncomment the import statements at the top of this file 
          to see the examples in action.
        </p>
      </div>
    </div>
  );
};

export default ApiUsageExample;