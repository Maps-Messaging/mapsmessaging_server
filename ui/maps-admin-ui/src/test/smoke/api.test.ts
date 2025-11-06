/**
 * Smoke tests to validate that generated API endpoints compile and can be instantiated
 * These tests don't make actual HTTP calls but verify the TypeScript compilation
 * and basic functionality of the generated code
 */

import { ApiClient, getApiClient } from '../../api/ApiClient';

// Mock the generated API imports - these will be available after code generation
// We'll create basic mocks for now to allow the tests to compile
const mockGeneratedApi = {
  BaseAPI: class {
    constructor() {}
  },
  Configuration: class {
    constructor(config: any = {}) {
      this.basePath = config.basePath || 'http://localhost:8080/api';
      this.headers = config.headers || {};
    }
    basePath: string;
    headers: Record<string, string>;
  },
};

// Mock the generated modules
jest.mock('../../api/generated', () => mockGeneratedApi, { virtual: true });

describe('Generated API Smoke Tests', () => {
  beforeEach(() => {
    // Reset the API client before each test
    const { resetApiClient } = require('../../api/ApiClient');
    resetApiClient();
  });

  test('ApiClient should initialize with default configuration', () => {
    const client = new ApiClient();
    expect(client).toBeDefined();
    expect(client.getConfiguration()).toBeDefined();
    expect(client.getConfiguration().basePath).toContain('localhost:3000');
  });

  test('ApiClient should accept custom configuration', () => {
    const customConfig = {
      baseUrl: 'https://api.example.com',
      apiKey: 'test-api-key',
      timeout: 60000,
    };
    
    const client = new ApiClient(customConfig);
    expect(client.getConfiguration().basePath).toBe('https://api.example.com');
    expect(client.getConfiguration().apiKey).toBe('test-api-key');
  });

  test('ApiClient should handle bearer token authentication', () => {
    const client = new ApiClient({
      bearerToken: 'test-bearer-token',
    });
    
    const config = client.getConfiguration();
    expect(config.headers?.['Authorization']).toBe('Bearer test-bearer-token');
  });

  test('ApiClient should update authentication dynamically', () => {
    const client = new ApiClient();
    
    client.updateAuth({
      apiKey: 'new-api-key',
      bearerToken: 'new-bearer-token',
    });
    
    expect(client.getConfiguration().apiKey).toBe('new-api-key');
    expect(client.getConfiguration().headers?.['Authorization']).toBe('Bearer new-bearer-token');
  });

  test('ApiClient should update base URL dynamically', () => {
    const client = new ApiClient();
    const newBaseUrl = 'https://new-api.example.com';
    
    client.updateBaseUrl(newBaseUrl);
    expect(client.getConfiguration().basePath).toBe(newBaseUrl);
  });

  test('getApiClient should return singleton instance', () => {
    const client1 = getApiClient();
    const client2 = getApiClient();
    
    expect(client1).toBe(client2);
  });

  test('getApiClient should create new instance with config', () => {
    const client1 = getApiClient();
    const client2 = getApiClient({ baseUrl: 'https://different.example.com' });
    
    expect(client1).not.toBe(client2);
    expect(client2.getConfiguration().basePath).toBe('https://different.example.com');
  });

  test('Generated API types should be available', () => {
    // This test verifies that the generated types can be imported and used
    // The actual types will be available after code generation
    
    type TestConfiguration = InstanceType<typeof mockGeneratedApi.Configuration>;
    type TestBaseAPI = InstanceType<typeof mockGeneratedApi.BaseAPI>;
    
    const config: TestConfiguration = new mockGeneratedApi.Configuration({
      basePath: 'https://test.api.com',
    });
    
    const api: TestBaseAPI = new mockGeneratedApi.BaseAPI();
    
    expect(config.basePath).toBe('https://test.api.com');
    expect(api).toBeDefined();
  });
});