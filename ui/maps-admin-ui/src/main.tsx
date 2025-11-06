import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

// Initialize the API client with default configuration
import { getApiClient } from './api/ApiClient';

// This will be available once the OpenAPI client is generated
try {
  getApiClient({
    baseUrl: import.meta.env.VITE_API_BASE_URL || 
             (typeof window !== 'undefined' 
               ? `${window.location.protocol}//${window.location.host}/api`
               : 'http://cloud.kritikal.org:8080/api'),
    bearerToken: import.meta.env.VITE_API_BEARER_TOKEN,
  });
} catch (error) {
  console.warn('API client initialization failed (expected before code generation):', error);
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);