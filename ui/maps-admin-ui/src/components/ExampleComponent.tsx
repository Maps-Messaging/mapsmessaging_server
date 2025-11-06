/**
 * Example component demonstrating the usage of the generated API client
 * This component will work once the OpenAPI client is generated
 */

import React from 'react';

// These imports will be available after OpenAPI code generation
// import { getApiClient } from '../api/ApiClient';
// import { createQueryHooks } from '../api/ReactQueryHelpers';
// import { DefaultApi } from '../api/generated';

interface ExampleComponentProps {
  // Define component props here
}

export const ExampleComponent: React.FC<ExampleComponentProps> = () => {
  // Example usage (will work after API generation):
  
  /*
  const { useQuery } = createQueryHooks(
    DefaultApi.prototype.someEndpoint,
    { queryKeyPrefix: ['example'] }
  );
  
  const { data, isLoading, error } = useQuery({ param: 'value' });
  
  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  */

  return (
    <div className="example-component">
      <h2>Maps Admin UI Example</h2>
      <p>
        This component demonstrates how to use the generated API client.
        Once you run <code>npm run openapi:ensure</code>, the API client
        will be generated and you can uncomment the code above to see it in action.
      </p>
      
      <div className="usage-example">
        <h3>Usage Steps:</h3>
        <ol>
          <li>Run <code>npm install</code> to install dependencies</li>
          <li>Run <code>npm run openapi:download</code> to get the latest spec</li>
          <li>Run <code>npm run openapi:generate</code> to create the client</li>
          <li>Import and use the generated APIs in your components</li>
        </ol>
      </div>
      
      <div className="api-client-info">
        <h3>API Client Features:</h3>
        <ul>
          <li>Type-safe API calls</li>
          <li>Automatic authentication handling</li>
          <li>Error handling and retry logic</li>
          <li>React Query integration</li>
          <li>Server-Sent Events support</li>
        </ul>
      </div>
    </div>
  );
};

export default ExampleComponent;