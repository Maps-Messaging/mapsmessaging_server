import {
  useQuery,
  useMutation,
  useQueryClient,
  UseQueryOptions,
  UseMutationOptions,
  QueryKey,
} from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { getApiClient } from './ApiClient';

// Generic type for API functions
type ApiFunction<TParams = any, TResult = any> = (
  params?: TParams,
  init?: RequestInit
) => Promise<TResult>;

// Generic type for mutation functions
type MutationFunction<TParams = any, TResult = any> = (
  params?: TParams,
  init?: RequestInit
) => Promise<TResult>;

export interface CreateQueryHooksConfig {
  queryKeyPrefix: string[];
  defaultOptions?: Partial<UseQueryOptions<any, any, any, any>>;
}

export interface CreateMutationHooksConfig {
  mutationKeyPrefix: string[];
  defaultOptions?: Partial<UseMutationOptions<any, any, any, any>>;
  invalidateQueries?: QueryKey[];
}

/**
 * Creates typed React Query hooks for a given API endpoint
 */
export function createQueryHooks<TParams, TResult>(
  apiFunction: ApiFunction<TParams, TResult>,
  config: CreateQueryHooksConfig
) {
  const { queryKeyPrefix, defaultOptions = {} } = config;

  const useQueryHook = (
    params?: TParams,
    options?: Partial<UseQueryOptions<TResult, Error>>
  ) => {
    const apiClient = getApiClient();
    
    return useQuery<TResult, Error>({
      queryKey: [...queryKeyPrefix, params],
      queryFn: () => apiFunction(params, {
        headers: apiClient.getConfiguration().headers,
      }),
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes
      retry: (failureCount, error) => {
        // Don't retry on 4xx errors
        if (error.message.includes('401') || error.message.includes('403') || 
            error.message.includes('404') || error.message.includes('422')) {
          return false;
        }
        return failureCount < 3;
      },
      ...defaultOptions,
      ...options,
    });
  };

  return { useQuery: useQueryHook };
}

/**
 * Creates typed React Query mutation hooks for a given API endpoint
 */
export function createMutationHooks<TParams, TResult>(
  mutationFunction: MutationFunction<TParams, TResult>,
  config: CreateMutationHooksConfig
) {
  const { mutationKeyPrefix, defaultOptions = {}, invalidateQueries = [] } = config;

  const useMutationHook = (
    options?: Partial<UseMutationOptions<TResult, Error, TParams>>
  ) => {
    const queryClient = useQueryClient();
    const apiClient = getApiClient();
    
    return useMutation<TResult, Error, TParams>({
      mutationKey: mutationKeyPrefix,
      mutationFn: (params: TParams) => mutationFunction(params, {
        headers: apiClient.getConfiguration().headers,
      }),
      onSuccess: (data, variables, context) => {
        // Invalidate related queries after successful mutation
        invalidateQueries.forEach(queryKey => {
          queryClient.invalidateQueries({ queryKey });
        });
        
        // Call user-provided onSuccess if provided
        if (defaultOptions.onSuccess) {
          (defaultOptions.onSuccess as any)(data, variables, context);
        }
        if (options?.onSuccess) {
          (options.onSuccess as any)(data, variables, context, undefined);
        }
      },
      ...defaultOptions,
      ...options,
    });
  };

  return { useMutation: useMutationHook };
}

/**
 * Helper for creating both query and mutation hooks for the same resource
 */
export function createResourceHooks<TParams, TResult, TCreateParams = Partial<TParams>, TUpdateParams = Partial<TParams>>(
  api: {
    get: ApiFunction<TParams, TResult>;
    create: MutationFunction<TCreateParams, TResult>;
    update: MutationFunction<TUpdateParams, TResult>;
    delete: MutationFunction<{ id: string }, void>;
    list?: ApiFunction<TParams, TResult[]>;
  },
  resourceName: string
) {
  const queryHooks = createQueryHooks(api.get, {
    queryKeyPrefix: [resourceName],
  });

  const listHooks = api.list ? createQueryHooks(api.list, {
    queryKeyPrefix: [resourceName, 'list'],
  }) : null;

  const createHooks = createMutationHooks(api.create, {
    mutationKeyPrefix: [resourceName, 'create'],
    invalidateQueries: [[resourceName, 'list']],
  });

  const updateHooks = createMutationHooks(api.update, {
    mutationKeyPrefix: [resourceName, 'update'],
    invalidateQueries: [[resourceName]],
  });

  const deleteHooks = createMutationHooks(api.delete, {
    mutationKeyPrefix: [resourceName, 'delete'],
    invalidateQueries: [[resourceName, 'list']],
  });

  return {
    useGet: queryHooks.useQuery,
    useList: listHooks?.useQuery,
    useCreate: createHooks.useMutation,
    useUpdate: updateHooks.useMutation,
    useDelete: deleteHooks.useMutation,
  };
}

/**
 * SSE (Server-Sent Events) helper for streaming endpoints
 */
export function useSSE<T = any>(url: string, options?: {
  onMessage?: (data: T) => void;
  onError?: (error: Event) => void;
  reconnect?: boolean;
  reconnectInterval?: number;
}) {
  const [data, setData] = useState<T | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<Event | null>(null);

  const {
    onMessage,
    onError,
    reconnect = true,
    reconnectInterval = 3000,
  } = options || {};

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let reconnectTimeout: NodeJS.Timeout | null = null;

    const connect = () => {
      try {
        const apiClient = getApiClient();
        const baseUrl = apiClient.getConfiguration().basePath;
        const fullUrl = `${baseUrl}/${url.replace(/^\//, '')}`;

        eventSource = new EventSource(fullUrl);
        setIsConnected(true);
        setError(null);

        eventSource.onmessage = (event) => {
          try {
            const parsedData = JSON.parse(event.data);
            setData(parsedData);
            onMessage?.(parsedData);
          } catch (e) {
            console.error('Failed to parse SSE data:', e);
          }
        };

        eventSource.onerror = (err) => {
          console.error('SSE error:', err);
          setIsConnected(false);
          setError(err);
          onError?.(err);

          if (reconnect && eventSource?.readyState === EventSource.CLOSED) {
            reconnectTimeout = setTimeout(connect, reconnectInterval);
          }
        };

        eventSource.onopen = () => {
          setIsConnected(true);
          setError(null);
        };
      } catch (e) {
        console.error('Failed to create SSE connection:', e);
        setError(e as Event);
      }
    };

    connect();

    return () => {
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [url, onMessage, onError, reconnect, reconnectInterval]);

  return { data, isConnected, error };
}