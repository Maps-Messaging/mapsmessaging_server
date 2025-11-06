/**
 * MAPS Messaging Server Monitoring Dashboard - API Client
 * Handles all communication with the REST API endpoints
 */

class MapsApiClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
        this.cache = new Map();
        this.cacheTimeout = 5000; // 5 seconds cache
    }

    /**
     * Generic API request method
     */
    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}/api/v1${endpoint}`;
        const cacheKey = `${endpoint}_${JSON.stringify(options)}`;
        
        // Check cache for GET requests
        if (!options.method || options.method === 'GET') {
            const cached = this.getCachedData(cacheKey);
            if (cached) {
                return cached;
            }
        }

        try {
            const response = await fetch(url, {
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    ...options.headers
                },
                ...options
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            
            // Cache successful GET requests
            if (!options.method || options.method === 'GET') {
                this.setCachedData(cacheKey, data);
            }

            return data;
        } catch (error) {
            console.error(`API request failed for ${endpoint}:`, error);
            throw error;
        }
    }

    /**
     * Cache management
     */
    getCachedData(key) {
        const cached = this.cache.get(key);
        if (cached && Date.now() - cached.timestamp < this.cacheTimeout) {
            return cached.data;
        }
        if (cached) {
            this.cache.delete(key);
        }
        return null;
    }

    setCachedData(key, data) {
        this.cache.set(key, {
            data: data,
            timestamp: Date.now()
        });
    }

    clearCache() {
        this.cache.clear();
    }

    // Server Status and Health APIs
    async getServerStatus() {
        return this.request('/server/status');
    }

    async getServerHealth() {
        return this.request('/server/health');
    }

    async getServerInfo() {
        return this.request('/server/details/info');
    }

    async getServerStats() {
        return this.request('/server/details/stats');
    }

    async getCacheInfo() {
        return this.request('/server/cache');
    }

    // Log APIs
    async getLogEntries(filter = '') {
        const endpoint = filter ? `/server/log?filter=${encodeURIComponent(filter)}` : '/server/log';
        return this.request(endpoint);
    }

    async getSseToken() {
        return this.request('/server/log/sse');
    }

    /**
     * Create SSE connection for live log streaming
     */
    createSseConnection(token, filter = '', onMessage, onError, onClose) {
        const endpoint = `/api/v1/server/log/sse/stream/${token}`;
        const url = filter ? `${this.baseUrl}${endpoint}?filter=${encodeURIComponent(filter)}` : `${this.baseUrl}${endpoint}`;
        
        const eventSource = new EventSource(url);
        
        eventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                onMessage(data);
            } catch (error) {
                console.error('Failed to parse SSE message:', error);
                onMessage({ message: event.data, raw: true });
            }
        };

        eventSource.onerror = (error) => {
            console.error('SSE connection error:', error);
            if (onError) onError(error);
        };

        eventSource.onclose = () => {
            console.log('SSE connection closed');
            if (onClose) onClose();
        };

        return eventSource;
    }

    // Updates API
    async getUpdates() {
        return this.request('/updates');
    }

    // Utility methods
    async checkServerConnectivity() {
        try {
            await this.getServerHealth();
            return true;
        } catch (error) {
            return false;
        }
    }

    /**
     * Format bytes to human readable format
     */
    formatBytes(bytes, decimals = 2) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }

    /**
     * Format uptime to human readable format
     */
    formatUptime(milliseconds) {
        const seconds = Math.floor(milliseconds / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) {
            return `${days}d ${hours % 24}h ${minutes % 60}m`;
        } else if (hours > 0) {
            return `${hours}h ${minutes % 60}m`;
        } else if (minutes > 0) {
            return `${minutes}m ${seconds % 60}s`;
        } else {
            return `${seconds}s`;
        }
    }

    /**
     * Get status color based on status value
     */
    getStatusColor(status) {
        const statusColors = {
            'OK': '#27ae60',
            'WARN': '#f39c12',
            'ERROR': '#e74c3c',
            'STOPPED': '#6c757d',
            'DISABLED': '#6c757d',
            'PAUSED': '#f39c12'
        };
        return statusColors[status?.toUpperCase()] || '#6c757d';
    }

    /**
     * Get status icon based on status value
     */
    getStatusIcon(status) {
        const statusIcons = {
            'OK': 'fas fa-check-circle',
            'WARN': 'fas fa-exclamation-triangle',
            'ERROR': 'fas fa-times-circle',
            'STOPPED': 'fas fa-stop-circle',
            'DISABLED': 'fas fa-ban',
            'PAUSED': 'fas fa-pause-circle'
        };
        return statusIcons[status?.toUpperCase()] || 'fas fa-question-circle';
    }

    /**
     * Parse log level from message
     */
    parseLogLevel(message) {
        const levelRegex = /\b(ERROR|WARN|INFO|DEBUG|TRACE)\b/i;
        const match = message.match(levelRegex);
        return match ? match[1].toUpperCase() : 'INFO';
    }

    /**
     * Parse timestamp from log entry
     */
    parseTimestamp(logEntry) {
        if (logEntry.timestamp) {
            return new Date(logEntry.timestamp);
        }
        if (logEntry.time) {
            return new Date(logEntry.time);
        }
        return new Date();
    }

    /**
     * Create a retry mechanism for failed requests
     */
    async withRetry(requestFn, maxRetries = 3, delay = 1000) {
        let lastError;
        for (let i = 0; i < maxRetries; i++) {
            try {
                return await requestFn();
            } catch (error) {
                lastError = error;
                if (i < maxRetries - 1) {
                    await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i)));
                }
            }
        }
        throw lastError;
    }
}

// Global API client instance
window.mapsApi = new MapsApiClient();

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = MapsApiClient;
}