/**
 * Smoke tests for React Query helpers
 * These tests verify that the React Query hooks can be created and used
 */

import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createQueryHooks, createMutationHooks, createResourceHooks } from '../../api/ReactQueryHelpers';

// Mock API functions
const mockGetFunction = jest.fn().mockResolvedValue({ id: 1, name: 'Test Resource' });
const mockListFunction = jest.fn().mockResolvedValue([{ id: 1, name: 'Test Resource' }]);
const mockCreateFunction = jest.fn().mockResolvedValue({ id: 2, name: 'New Resource' });
const mockUpdateFunction = jest.fn().mockResolvedValue({ id: 1, name: 'Updated Resource' });
const mockDeleteFunction = jest.fn().mockResolvedValue(undefined);

// Wrapper component for React Query
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('React Query Helpers Smoke Tests', () => {
  let wrapper: ReturnType<typeof createWrapper>;

  beforeEach(() => {
    wrapper = createWrapper();
    jest.clearAllMocks();
  });

  test('createQueryHooks should create working useQuery hook', async () => {
    const { useQuery } = createQueryHooks(mockGetFunction, {
      queryKeyPrefix: ['test'],
    });

    const { result } = renderHook(() => useQuery({ id: 1 }), { wrapper });

    expect(result.current.isLoading).toBe(true);
    
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual({ id: 1, name: 'Test Resource' });
    expect(mockGetFunction).toHaveBeenCalledWith({ id: 1 }, expect.any(Object));
  });

  test('createMutationHooks should create working useMutation hook', async () => {
    const { useMutation } = createMutationHooks(mockCreateFunction, {
      mutationKeyPrefix: ['create'],
    });

    const { result } = renderHook(() => useMutation(), { wrapper });

    expect(result.current.isIdle).toBe(true);

    result.current.mutate({ name: 'New Resource' });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual({ id: 2, name: 'New Resource' });
    expect(mockCreateFunction).toHaveBeenCalledWith({ name: 'New Resource' }, expect.any(Object));
  });

  test('createResourceHooks should create complete set of hooks', async () => {
    const hooks = createResourceHooks(
      {
        get: mockGetFunction,
        list: mockListFunction,
        create: mockCreateFunction,
        update: mockUpdateFunction,
        delete: mockDeleteFunction,
      },
      'testResource'
    );

    expect(hooks.useGet).toBeDefined();
    expect(hooks.useList).toBeDefined();
    expect(hooks.useCreate).toBeDefined();
    expect(hooks.useUpdate).toBeDefined();
    expect(hooks.useDelete).toBeDefined();

    // Test useGet
    const { result: getResult } = renderHook(() => hooks.useGet({ id: 1 }), { wrapper });
    
    await waitFor(() => {
      expect(getResult.current.isSuccess).toBe(true);
    });
    expect(getResult.current.data).toEqual({ id: 1, name: 'Test Resource' });

    // Test useList
    const { result: listResult } = renderHook(() => hooks.useList!(), { wrapper });
    
    await waitFor(() => {
      expect(listResult.current.isSuccess).toBe(true);
    });
    expect(listResult.current.data).toEqual([{ id: 1, name: 'Test Resource' }]);

    // Test useCreate
    const { result: createResult } = renderHook(() => hooks.useCreate(), { wrapper });
    
    createResult.current.mutate({ name: 'New Resource' });
    
    await waitFor(() => {
      expect(createResult.current.isSuccess).toBe(true);
    });
    expect(createResult.current.data).toEqual({ id: 2, name: 'New Resource' });

    // Test useUpdate
    const { result: updateResult } = renderHook(() => hooks.useUpdate(), { wrapper });
    
    updateResult.current.mutate({ id: 1, name: 'Updated Resource' });
    
    await waitFor(() => {
      expect(updateResult.current.isSuccess).toBe(true);
    });
    expect(updateResult.current.data).toEqual({ id: 1, name: 'Updated Resource' });

    // Test useDelete
    const { result: deleteResult } = renderHook(() => hooks.useDelete(), { wrapper });
    
    deleteResult.current.mutate({ id: '1' });
    
    await waitFor(() => {
      expect(deleteResult.current.isSuccess).toBe(true);
    });
    expect(mockDeleteFunction).toHaveBeenCalledWith({ id: 1 }, expect.any(Object));
  });

  test('query hooks should use custom options', () => {
    const { useQuery } = createQueryHooks(mockGetFunction, {
      queryKeyPrefix: ['test'],
      defaultOptions: {
        staleTime: 1000,
        gcTime: 2000,
      },
    });

    const { result } = renderHook(
      () => useQuery({ id: 1 }, { staleTime: 5000 }),
      { wrapper }
    );

    // The hook should be created without errors
    expect(result.current).toBeDefined();
    expect(result.current.isLoading).toBe(true);
  });

  test('mutation hooks should handle invalidation queries', async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
      },
    });

    const invalidateQueries = jest.fn();
    jest.spyOn(queryClient, 'invalidateQueries').mockImplementation(invalidateQueries);

    const customWrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { useMutation } = createMutationHooks(mockCreateFunction, {
      mutationKeyPrefix: ['create'],
      invalidateQueries: [['testList']],
    });

    const { result } = renderHook(() => useMutation(), { wrapper: customWrapper });

    result.current.mutate({ name: 'Test' });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['testList'] });
  });
});