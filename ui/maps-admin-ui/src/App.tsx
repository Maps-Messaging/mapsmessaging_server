import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import ExampleComponent from './components/ExampleComponent';
import ApiUsageExample from './components/ApiUsageExample';
import './App.css';

// Create a client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes
      retry: (failureCount, error) => {
        // Don't retry on 4xx errors
        if (error instanceof Error && 
            (error.message.includes('401') || error.message.includes('403') || 
             error.message.includes('404') || error.message.includes('422'))) {
          return false;
        }
        return failureCount < 3;
      },
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="App">
        <header className="App-header">
          <h1>Maps Messaging Server Admin UI</h1>
          <p>Admin interface with OpenAPI-driven client generation</p>
        </header>
        
        <main className="App-main">
          <ExampleComponent />
          <ApiUsageExample />
        </main>
        
        <footer className="App-footer">
          <p>&copy; 2024 Maps Messaging. All rights reserved.</p>
        </footer>
      </div>
      
      {/* React Query DevTools for development */}
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}

export default App;