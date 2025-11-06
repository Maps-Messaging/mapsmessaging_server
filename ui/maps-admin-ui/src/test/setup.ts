import '@testing-library/jest-dom';

// Mock fetch globally
global.fetch = jest.fn();

// Mock EventSource for SSE tests
const mockEventSource = {
  CONNECTING: 0,
  OPEN: 1,
  CLOSED: 2,
} as const;

const mockEventSourceImpl = jest.fn().mockImplementation((url) => ({
  url,
  readyState: mockEventSource.OPEN,
  close: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn(),
  dispatchEvent: jest.fn(),
  ...mockEventSource,
}));

(global as any).EventSource = Object.assign(mockEventSourceImpl, mockEventSource);

// Mock window.location
Object.defineProperty(window, 'location', {
  value: {
    protocol: 'http:',
    host: 'localhost:3000',
    href: 'http://localhost:3000',
  },
  writable: true,
});

// Reset all mocks before each test
beforeEach(() => {
  jest.clearAllMocks();
});