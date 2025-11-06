/**
 * MAPS Messaging Server Monitoring Dashboard - Metrics Module
 * Handles detailed metrics visualization and performance statistics
 */

class Metrics {
    constructor() {
        this.charts = {};
        this.metricsData = {};
        this.init();
    }

    init() {
        this.initializeCharts();
    }

    initializeCharts() {
        // Thread Distribution Chart
        const threadCtx = document.getElementById('threadChart');
        if (threadCtx) {
            this.charts.thread = new Chart(threadCtx.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: ['RUNNABLE', 'BLOCKED', 'WAITING', 'TIMED_WAITING', 'TERMINATED'],
                    datasets: [{
                        data: [0, 0, 0, 0, 0],
                        backgroundColor: [
                            '#27ae60',  // Runnable - Green
                            '#e74c3c',  // Blocked - Red
                            '#f39c12',  // Waiting - Orange
                            '#3498db',  // Timed Waiting - Blue
                            '#95a5a6'   // Terminated - Gray
                        ],
                        borderWidth: 2,
                        borderColor: '#fff'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                padding: 15,
                                font: {
                                    size: 12
                                }
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const label = context.label || '';
                                    const value = context.parsed || 0;
                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                                    return `${label}: ${value} (${percentage}%)`;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    async loadMetricsData() {
        try {
            await Promise.all([
                this.loadServerInfo(),
                this.loadPerformanceStats(),
                this.loadCacheInfo()
            ]);
        } catch (error) {
            console.error('Failed to load metrics data:', error);
            this.showError('Failed to load metrics data');
        }
    }

    async loadServerInfo() {
        try {
            const info = await window.mapsApi.getServerInfo();
            this.displayServerInfo(info);
        } catch (error) {
            console.error('Failed to load server info:', error);
            this.displayError('serverInfo', 'Failed to load server information');
        }
    }

    displayServerInfo(info) {
        const container = document.getElementById('serverInfo');
        if (!container) return;

        const infoHtml = `
            <div class="info-grid">
                <div class="info-item">
                    <strong>Server Version:</strong>
                    <span>${info.version || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <strong>Build Date:</strong>
                    <span>${info.buildDate || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <strong>Java Version:</strong>
                    <span>${info.javaVersion || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <strong>Uptime:</strong>
                    <span>${info.uptime ? window.mapsApi.formatUptime(info.uptime) : 'N/A'}</span>
                </div>
                <div class="info-item">
                    <strong>Start Time:</strong>
                    <span>${info.startTime ? new Date(info.startTime).toLocaleString() : 'N/A'}</span>
                </div>
                <div class="info-item">
                    <strong>Operating System:</strong>
                    <span>${info.osName || 'N/A'} ${info.osVersion || ''}</span>
                </div>
                <div class="info-item">
                    <strong>Architecture:</strong>
                    <span>${info.osArch || 'N/A'}</span>
                </div>
                <div class="info-item">
                    <strong>Available Processors:</strong>
                    <span>${info.availableProcessors || 'N/A'}</span>
                </div>
            </div>
        `;

        container.innerHTML = infoHtml;
    }

    async loadPerformanceStats() {
        try {
            const statsResponse = await window.mapsApi.getServerStats();
            const stats = statsResponse.data || statsResponse;
            this.displayPerformanceStats(stats);
            this.updateThreadChart(stats);
        } catch (error) {
            console.error('Failed to load performance stats:', error);
            this.displayError('performanceStats', 'Failed to load performance statistics');
        }
    }

    displayPerformanceStats(stats) {
        const container = document.getElementById('performanceStats');
        if (!container) return;

        const statsHtml = `
            <div class="stats-grid">
                <div class="stat-group">
                    <h4>Memory</h4>
                    <div class="stat-item">
                        <span>Heap Used:</span>
                        <span>${stats.memory?.heapUsed ? window.mapsApi.formatBytes(stats.memory.heapUsed) : 'N/A'}</span>
                    </div>
                    <div class="stat-item">
                        <span>Heap Max:</span>
                        <span>${stats.memory?.heapMax ? window.mapsApi.formatBytes(stats.memory.heapMax) : 'N/A'}</span>
                    </div>
                    <div class="stat-item">
                        <span>Non-Heap Used:</span>
                        <span>${stats.memory?.nonHeapUsed ? window.mapsApi.formatBytes(stats.memory.nonHeapUsed) : 'N/A'}</span>
                    </div>
                    <div class="stat-item">
                        <span>Direct Memory:</span>
                        <span>${stats.memory?.directMemoryUsed ? window.mapsApi.formatBytes(stats.memory.directMemoryUsed) : 'N/A'}</span>
                    </div>
                </div>
                
                <div class="stat-group">
                    <h4>Performance</h4>
                    <div class="stat-item">
                        <span>CPU Usage:</span>
                        <span>${stats.cpu?.usage ? stats.cpu.usage.toFixed(2) + '%' : 'N/A'}</span>
                    </div>
                    <div class="stat-item">
                        <span>System Load:</span>
                        <span>${stats.cpu?.systemLoad ? stats.cpu.systemLoad.toFixed(2) : 'N/A'}</span>
                    </div>
                    <div class="stat-item">
                        <span>GC Count:</span>
                        <span>${stats.gc?.collectionCount || 'N/A'}</span>
                    </div>
                    <div class="stat-item">
                        <span>GC Time:</span>
                        <span>${stats.gc?.collectionTime ? stats.gc.collectionTime + 'ms' : 'N/A'}</span>
                    </div>
                </div>
                
                <div class="stat-group">
                    <h4>Connections</h4>
                    <div class="stat-item">
                        <span>Active:</span>
                        <span>${stats.connections?.active || 0}</span>
                    </div>
                    <div class="stat-item">
                        <span>Total:</span>
                        <span>${stats.connections?.total || 0}</span>
                    </div>
                    <div class="stat-item">
                        <span>Peak:</span>
                        <span>${stats.connections?.peak || 0}</span>
                    </div>
                    <div class="stat-item">
                        <span>Failed:</span>
                        <span>${stats.connections?.failed || 0}</span>
                    </div>
                </div>
                
                <div class="stat-group">
                    <h4>Messages</h4>
                    <div class="stat-item">
                        <span>Publish Rate:</span>
                        <span>${stats.messageRates?.publish || 0} msg/s</span>
                    </div>
                    <div class="stat-item">
                        <span>Subscribe Rate:</span>
                        <span>${stats.messageRates?.subscribe || 0} msg/s</span>
                    </div>
                    <div class="stat-item">
                        <span>Total Published:</span>
                        <span>${stats.messageStats?.totalPublished || 0}</span>
                    </div>
                    <div class="stat-item">
                        <span>Total Consumed:</span>
                        <span>${stats.messageStats?.totalConsumed || 0}</span>
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = statsHtml;
    }

    updateThreadChart(stats) {
        const chart = this.charts.thread;
        if (!chart || !stats.threads) return;

        const threadStates = stats.threads.states || {};
        const data = [
            threadStates.RUNNABLE || 0,
            threadStates.BLOCKED || 0,
            threadStates.WAITING || 0,
            threadStates.TIMED_WAITING || 0,
            threadStates.TERMINATED || 0
        ];

        chart.data.datasets[0].data = data;
        chart.update();
    }

    async loadCacheInfo() {
        try {
            const cacheInfo = await window.mapsApi.getCacheInfo();
            this.updateCacheDisplay(cacheInfo);
        } catch (error) {
            console.error('Failed to load cache info:', error);
            this.displayError('cacheInfo', 'Failed to load cache information');
        }
    }

    updateCacheDisplay(cacheInfo) {
        const container = document.getElementById('cacheInfo');
        if (!container) return;

        const cacheHtml = `
            <div class="cache-grid">
                <div class="cache-item">
                    <strong>Cache Size:</strong>
                    <span>${cacheInfo.size || 0} entries</span>
                </div>
                <div class="cache-item">
                    <strong>Max Size:</strong>
                    <span>${cacheInfo.maxSize || 'Unlimited'}</span>
                </div>
                <div class="cache-item">
                    <strong>Memory Used:</strong>
                    <span>${cacheInfo.memoryUsed ? window.mapsApi.formatBytes(cacheInfo.memoryUsed) : 'N/A'}</span>
                </div>
                <div class="cache-item">
                    <strong>Hit Rate:</strong>
                    <span>${cacheInfo.hitRate ? (cacheInfo.hitRate * 100).toFixed(2) + '%' : 'N/A'}</span>
                </div>
                <div class="cache-item">
                    <strong>Miss Rate:</strong>
                    <span>${cacheInfo.missRate ? (cacheInfo.missRate * 100).toFixed(2) + '%' : 'N/A'}</span>
                </div>
                <div class="cache-item">
                    <strong>Evictions:</strong>
                    <span>${cacheInfo.evictions || 0}</span>
                </div>
                <div class="cache-item">
                    <strong>Requests:</strong>
                    <span>${cacheInfo.requests || 0}</span>
                </div>
                <div class="cache-item">
                    <strong>Clear Operations:</strong>
                    <span>${cacheInfo.clearOperations || 0}</span>
                </div>
            </div>
        `;

        container.innerHTML = cacheHtml;
    }

    displayError(containerId, message) {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = `<div class="error-message">${message}</div>`;
        }
    }

    showError(message) {
        console.error(message);
        // You could implement a toast notification here
    }

    // Utility method to format metrics data for export
    exportMetrics() {
        const exportData = {
            timestamp: new Date().toISOString(),
            serverInfo: this.metricsData.serverInfo,
            performanceStats: this.metricsData.performanceStats,
            cacheInfo: this.metricsData.cacheInfo
        };
        
        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `maps-metrics-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    // Cleanup method
    destroy() {
        // Destroy charts
        Object.values(this.charts).forEach(chart => {
            if (chart && chart.destroy) {
                chart.destroy();
            }
        });
        
        this.charts = {};
        this.metricsData = {};
    }
}

// Initialize metrics module
document.addEventListener('DOMContentLoaded', () => {
    window.metrics = new Metrics();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.metrics) {
        window.metrics.destroy();
    }
});