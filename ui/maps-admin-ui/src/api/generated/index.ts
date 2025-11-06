// Mock generated API module
// This file will be replaced by actual generated code when you run `npm run openapi:generate`

export interface Configuration {
  basePath?: string;
  headers?: Record<string, string>;
  apiKey?: string;
  middleware?: any[];
}

export class BaseAPI {
  constructor(_config?: Configuration) {
    // Mock implementation
  }
}

// Export a constructor function for Configuration
export function createConfiguration(config?: Partial<Configuration>): Configuration {
  return {
    basePath: config?.basePath,
    headers: config?.headers,
    apiKey: config?.apiKey,
    middleware: config?.middleware,
  };
}