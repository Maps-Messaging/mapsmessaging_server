import { createConfiguration, BaseAPI, Configuration as IConfiguration } from './generated';

export interface ApiClientConfig {
  baseUrl?: string;
  apiKey?: string;
  bearerToken?: string;
}

export class ApiClient {
  private configuration: IConfiguration;
  private baseApi: BaseAPI;

  constructor(config: ApiClientConfig = {}) {
    const {
      baseUrl = typeof window !== 'undefined' 
        ? `${window.location.protocol}//${window.location.host}/api`
        : 'http://cloud.kritikal.org:8080/api',
      apiKey,
      bearerToken,
    } = config;

    this.configuration = createConfiguration({
      basePath: baseUrl,
      apiKey,
      headers: bearerToken ? {
        'Authorization': `Bearer ${bearerToken}`,
      } : {},
      middleware: [
        {
          pre: async (context: any) => {
            // Add request timing
            const startTime = Date.now();
            return {
              ...context,
              init: {
                ...context.init,
                headers: {
                  ...context.init?.headers,
                  'X-Request-Started': startTime.toString(),
                },
              },
            };
          },
          post: async (context: any) => {
            const startTime = context.init?.headers?.['X-Request-Started'];
            if (startTime) {
              const duration = Date.now() - parseInt(startTime as string);
              console.debug(`API request to ${context.url} took ${duration}ms`);
            }
            return context.response;
          },
          onError: async (context: any) => {
            console.error('API request failed:', {
              url: context.url,
              method: context.init?.method,
              status: context.response?.status,
              statusText: context.response?.statusText,
            });
            
            // Handle common error scenarios
            if (context.response?.status === 401) {
              // Handle unauthorized - could redirect to login
              if (typeof window !== 'undefined') {
                window.location.href = '/login';
              }
            }
            
            throw context.error || new Error('API request failed');
          },
        },
      ],
    });

    this.baseApi = new BaseAPI(this.configuration);
  }

  getConfiguration(): IConfiguration {
    return this.configuration;
  }

  getBaseApi(): BaseAPI {
    return this.baseApi;
  }

  // Helper method to update authentication
  updateAuth(config: { apiKey?: string; bearerToken?: string }): void {
    const headers: Record<string, string> = {};
    
    if (config.apiKey) {
      this.configuration.apiKey = config.apiKey;
    }
    
    if (config.bearerToken) {
      headers['Authorization'] = `Bearer ${config.bearerToken}`;
    }
    
    this.configuration.headers = headers;
  }

  // Helper method to update base URL
  updateBaseUrl(baseUrl: string): void {
    this.configuration.basePath = baseUrl;
  }
}

// Singleton instance for the application
let apiClientInstance: ApiClient | null = null;

export function getApiClient(config?: ApiClientConfig): ApiClient {
  if (!apiClientInstance || config) {
    apiClientInstance = new ApiClient(config);
  }
  return apiClientInstance;
}

export function resetApiClient(): void {
  apiClientInstance = null;
}