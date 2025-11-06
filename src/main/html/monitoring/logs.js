/**
 * MAPS Messaging Server Monitoring Dashboard - Logs Module
 * Handles live log streaming via Server-Sent Events and log management
 */

class Logs {
    constructor() {
        this.sseConnection = null;
        this.sseToken = null;
        this.isPaused = false;
        this.logEntries = [];
        this.maxLogEntries = 1000;
        this.filters = {
            text: '',
            level: ''
        };
        this.logBuffer = [];
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
        
        this.init();
    }

    init() {
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Filter controls
        document.getElementById('logFilter')?.addEventListener('input', (e) => {
            this.filters.text = e.target.value;
            this.applyFilters();
        });

        document.getElementById('logLevel')?.addEventListener('change', (e) => {
            this.filters.level = e.target.value;
            this.applyFilters();
        });

        // Control buttons
        document.getElementById('pauseLogsBtn')?.addEventListener('click', () => {
            this.pauseLogs();
        });

        document.getElementById('resumeLogsBtn')?.addEventListener('click', () => {
            this.resumeLogs();
        });

        document.getElementById('downloadLogsBtn')?.addEventListener('click', () => {
            this.downloadLogs();
        });

        document.getElementById('clearLogsBtn')?.addEventListener('click', () => {
            this.clearLogs();
        });
    }

    async initializeLogs() {
        try {
            // Load initial log entries
            await this.loadInitialLogs();
            
            // Start SSE streaming
            await this.startSseStreaming();
            
        } catch (error) {
            console.error('Failed to initialize logs:', error);
            this.showLogStatus('Failed to initialize logs: ' + error.message, 'error');
        }
    }

    async loadInitialLogs() {
        try {
            const logsResponse = await window.mapsApi.getLogEntries(this.filters.text);
            const logs = logsResponse.entries || logsResponse || [];
            
            this.logEntries = logs.map(log => this.normalizeLogEntry(log));
            this.displayLogs();
            
            this.showLogStatus(`Loaded ${this.logEntries.length} log entries`, 'success');
        } catch (error) {
            console.error('Failed to load initial logs:', error);
            this.showLogStatus('Failed to load initial logs', 'error');
        }
    }

    async startSseStreaming() {
        try {
            // Get SSE token
            this.sseToken = await window.mapsApi.getSseToken();
            
            // Create SSE connection
            this.sseConnection = window.mapsApi.createSseConnection(
                this.sseToken,
                this.filters.text,
                (logData) => this.handleSseMessage(logData),
                (error) => this.handleSseError(error),
                () => this.handleSseClose()
            );
            
            this.showLogStatus('Connected to log stream', 'success');
            this.reconnectAttempts = 0;
            
        } catch (error) {
            console.error('Failed to start SSE streaming:', error);
            this.handleSseError(error);
        }
    }

    handleSseMessage(logData) {
        if (this.isPaused) {
            this.logBuffer.push(logData);
            return;
        }

        try {
            const logEntry = this.normalizeLogEntry(logData);
            this.addLogEntry(logEntry);
        } catch (error) {
            console.error('Failed to process log message:', error);
        }
    }

    handleSseError(error) {
        console.error('SSE connection error:', error);
        this.showLogStatus('Connection to log stream lost', 'error');
        
        // Attempt to reconnect
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
            
            this.showLogStatus(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`, 'warning');
            
            setTimeout(() => {
                this.startSseStreaming();
            }, delay);
        } else {
            this.showLogStatus('Failed to reconnect to log stream after multiple attempts', 'error');
        }
    }

    handleSseClose() {
        console.log('SSE connection closed');
        this.sseConnection = null;
        this.showLogStatus('Log stream connection closed', 'warning');
    }

    normalizeLogEntry(logData) {
        // Handle different log entry formats
        if (logData.raw) {
            // Raw text message
            return {
                timestamp: new Date(),
                level: this.parseLogLevel(logData.message),
                message: logData.message,
                logger: 'unknown',
                thread: 'unknown'
            };
        }

        return {
            timestamp: logData.timestamp ? new Date(logData.timestamp) : new Date(),
            level: logData.level || 'INFO',
            message: logData.message || logData.toString(),
            logger: logData.logger || logData.category || 'unknown',
            thread: logData.thread || 'unknown',
            exception: logData.exception || null
        };
    }

    parseLogLevel(message) {
        const levelRegex = /\b(ERROR|WARN|WARNING|INFO|DEBUG|TRACE)\b/i;
        const match = message.match(levelRegex);
        if (match) {
            const level = match[1].toUpperCase();
            return level === 'WARNING' ? 'WARN' : level;
        }
        return 'INFO';
    }

    addLogEntry(logEntry) {
        // Add to beginning of array (newest first)
        this.logEntries.unshift(logEntry);
        
        // Limit the number of entries
        if (this.logEntries.length > this.maxLogEntries) {
            this.logEntries = this.logEntries.slice(0, this.maxLogEntries);
        }
        
        this.displayLogs();
    }

    displayLogs() {
        const container = document.getElementById('logsList');
        if (!container) return;

        const filteredLogs = this.getFilteredLogs();
        
        if (filteredLogs.length === 0) {
            container.innerHTML = '<div class="loading">No log entries match the current filters</div>';
            return;
        }

        const logsHtml = filteredLogs.map(log => this.createLogEntryHtml(log)).join('');
        container.innerHTML = logsHtml;
        
        // Auto-scroll to top for newest entries
        container.scrollTop = 0;
    }

    createLogEntryHtml(logEntry) {
        const timestamp = logEntry.timestamp.toLocaleString();
        const levelClass = logEntry.level.toLowerCase();
        const hasException = logEntry.exception ? 'has-exception' : '';
        
        return `
            <div class="log-entry ${levelClass} ${hasException}">
                <div class="log-timestamp">${timestamp}</div>
                <div class="log-level">${logEntry.level}</div>
                <div class="log-message">
                    <div class="log-text">${this.escapeHtml(logEntry.message)}</div>
                    ${logEntry.exception ? `<div class="log-exception">${this.escapeHtml(logEntry.exception)}</div>` : ''}
                    <div class="log-meta">
                        <span class="log-logger">${logEntry.logger}</span>
                        ${logEntry.thread ? `<span class="log-thread">[${logEntry.thread}]</span>` : ''}
                    </div>
                </div>
            </div>
        `;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    getFilteredLogs() {
        return this.logEntries.filter(log => {
            // Text filter
            if (this.filters.text) {
                const searchText = this.filters.text.toLowerCase();
                const messageMatch = log.message.toLowerCase().includes(searchText);
                const loggerMatch = log.logger.toLowerCase().includes(searchText);
                const threadMatch = log.thread.toLowerCase().includes(searchText);
                
                if (!messageMatch && !loggerMatch && !threadMatch) {
                    return false;
                }
            }
            
            // Level filter
            if (this.filters.level && log.level !== this.filters.level) {
                return false;
            }
            
            return true;
        });
    }

    applyFilters() {
        this.displayLogs();
    }

    pauseLogs() {
        this.isPaused = true;
        document.getElementById('pauseLogsBtn').style.display = 'none';
        document.getElementById('resumeLogsBtn').style.display = 'inline-flex';
        this.showLogStatus('Log streaming paused', 'warning');
    }

    resumeLogs() {
        this.isPaused = false;
        document.getElementById('pauseLogsBtn').style.display = 'inline-flex';
        document.getElementById('resumeLogsBtn').style.display = 'none';
        this.showLogStatus('Log streaming resumed', 'success');
        
        // Process buffered logs
        while (this.logBuffer.length > 0) {
            const logData = this.logBuffer.shift();
            this.handleSseMessage(logData);
        }
    }

    clearLogs() {
        this.logEntries = [];
        this.logBuffer = [];
        this.displayLogs();
        this.showLogStatus('Logs cleared', 'success');
    }

    downloadLogs() {
        const filteredLogs = this.getFilteredLogs();
        const logText = filteredLogs.map(log => {
            const timestamp = log.timestamp.toISOString();
            const exception = log.exception ? `\nException: ${log.exception}` : '';
            return `[${timestamp}] ${log.level} ${log.logger} [${log.thread}] - ${log.message}${exception}`;
        }).join('\n');

        const blob = new Blob([logText], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `maps-logs-${new Date().toISOString().replace(/[:.]/g, '-')}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        
        this.showLogStatus(`Downloaded ${filteredLogs.length} log entries`, 'success');
    }

    showLogStatus(message, type = 'info') {
        const statusElement = document.getElementById('logsStatus');
        const statusText = document.getElementById('logsStatusText');
        
        if (statusElement && statusText) {
            statusText.textContent = message;
            statusElement.className = `logs-status ${type}`;
            
            // Auto-hide success messages after 3 seconds
            if (type === 'success') {
                setTimeout(() => {
                    statusText.textContent = 'Connected';
                    statusElement.className = 'logs-status';
                }, 3000);
            }
        }
    }

    // Public method to check if logs are currently being streamed
    isStreaming() {
        return this.sseConnection !== null && !this.isPaused;
    }

    // Public method to get current log statistics
    getLogStats() {
        const levelCounts = {};
        this.logEntries.forEach(log => {
            levelCounts[log.level] = (levelCounts[log.level] || 0) + 1;
        });
        
        return {
            total: this.logEntries.length,
            filtered: this.getFilteredLogs().length,
            levelCounts: levelCounts,
            isStreaming: this.isStreaming(),
            isPaused: this.isPaused
        };
    }

    // Cleanup method
    destroy() {
        // Close SSE connection
        if (this.sseConnection) {
            this.sseConnection.close();
            this.sseConnection = null;
        }
        
        // Clear data
        this.logEntries = [];
        this.logBuffer = [];
        this.isPaused = false;
        
        console.log('Logs module cleaned up');
    }
}

// Initialize logs module
document.addEventListener('DOMContentLoaded', () => {
    window.logs = new Logs();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.logs) {
        window.logs.destroy();
    }
});