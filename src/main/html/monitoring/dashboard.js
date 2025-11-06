/**
 * MAPS Messaging Server Monitoring Dashboard - Main Dashboard
 * Handles the main dashboard functionality, charts, and real-time updates
 */

class Dashboard {
    constructor() {
        this.autoRefresh = true;
        this.refreshInterval = 5000; // 5 seconds
        this.refreshTimer = null;
        this.charts = {};
        this.updatesLastChecked = null;
        
        this.init();
    }

    async init() {
        this.setupEventListeners();
        this.initializeCharts();
        await this.loadInitialData();
        this.startAutoRefresh();
        this.checkForUpdates();
    }

    setupEventListeners() {
        // Auto refresh controls
        document.getElementById('autoRefreshBtn').addEventListener('click', () => {
            this.toggleAutoRefresh();
        });

        document.getElementById('refreshIntervalBtn').addEventListener('click', () => {
            this.changeRefreshInterval();
        });

        document.getElementById('manualRefreshBtn').addEventListener('click', () => {
            this.refreshAllData();
        });

        // Navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.switchSection(link.dataset.section);
            });
        });

        // Updates banner
        document.getElementById('dismissUpdatesBtn')?.addEventListener('click', () => {
            this.dismissUpdates();
        });

        // Visibility change handling
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                this.pauseAutoRefresh();
            } else {
                this.resumeAutoRefresh();
            }
        });
    }

    initializeCharts() {
        // Message Rates Chart
        const messageRatesCtx = document.getElementById('messageRatesChart').getContext('2d');
        this.charts.messageRates = new Chart(messageRatesCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    {
                        label: 'Publish Rate',
                        data: [],
                        borderColor: '#3498db',
                        backgroundColor: 'rgba(52, 152, 219, 0.1)',
                        tension: 0.4
                    },
                    {
                        label: 'Subscribe Rate',
                        data: [],
                        borderColor: '#27ae60',
                        backgroundColor: 'rgba(39, 174, 96, 0.1)',
                        tension: 0.4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Messages/sec'
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Time'
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    }
                }
            }
        });

        // CPU Usage Chart
        const cpuCtx = document.getElementById('cpuChart').getContext('2d');
        this.charts.cpu = new Chart(cpuCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'CPU Usage (%)',
                    data: [],
                    borderColor: '#e74c3c',
                    backgroundColor: 'rgba(231, 76, 60, 0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        title: {
                            display: true,
                            text: 'CPU Usage (%)'
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Time'
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: false
                    }
                }
            }
        });
    }

    async loadInitialData() {
        try {
            await Promise.all([
                this.updateServerHealth(),
                this.updateServerInfo(),
                this.updateServerStats(),
                this.updateCacheInfo()
            ]);
        } catch (error) {
            console.error('Failed to load initial data:', error);
            this.showError('Failed to load initial dashboard data');
        }
    }

    async updateServerHealth() {
        try {
            const health = await window.mapsApi.getServerHealth();
            const status = await window.mapsApi.getServerStatus();

            // Update health card
            const healthStatus = document.getElementById('serverHealthStatus');
            const healthDetail = document.getElementById('serverHealthDetail');
            const healthCard = document.getElementById('serverHealthCard');

            if (health.state === 'Ok') {
                healthStatus.textContent = 'HEALTHY';
                healthStatus.style.color = window.mapsApi.getStatusColor('OK');
                healthCard.style.borderColor = window.mapsApi.getStatusColor('OK');
            } else {
                healthStatus.textContent = health.state.toUpperCase();
                healthStatus.style.color = window.mapsApi.getStatusColor('ERROR');
                healthCard.style.borderColor = window.mapsApi.getStatusColor('ERROR');
            }

            healthDetail.textContent = health.issueCount > 0 
                ? `${health.issueCount} issues detected` 
                : 'All systems operational';

        } catch (error) {
            console.error('Failed to update server health:', error);
            document.getElementById('serverHealthStatus').textContent = 'ERROR';
            document.getElementById('serverHealthDetail').textContent = 'Failed to fetch health status';
        }
    }

    async updateServerInfo() {
        try {
            const info = await window.mapsApi.getServerInfo();
            
            // Update uptime
            if (info.uptime) {
                const uptimeElement = document.getElementById('uptimeValue');
                uptimeElement.textContent = window.mapsApi.formatUptime(info.uptime);
            }

        } catch (error) {
            console.error('Failed to update server info:', error);
        }
    }

    async updateServerStats() {
        try {
            const statsResponse = await window.mapsApi.getServerStats();
            const stats = statsResponse.data || statsResponse;

            // Update connections
            if (stats.connections) {
                const connectionsElement = document.getElementById('connectionsValue');
                const connectionsDetail = document.getElementById('connectionsDetail');
                
                const active = stats.connections.active || 0;
                const total = stats.connections.total || 0;
                
                connectionsElement.textContent = active.toString();
                connectionsDetail.textContent = `Active: ${active} / Total: ${total}`;
            }

            // Update memory
            if (stats.memory) {
                const memoryElement = document.getElementById('memoryValue');
                const memoryDetail = document.getElementById('memoryDetail');
                
                const used = stats.memory.used || 0;
                const total = stats.memory.total || 0;
                const percentage = total > 0 ? ((used / total) * 100).toFixed(1) : 0;
                
                memoryElement.textContent = `${percentage}%`;
                memoryDetail.textContent = `${window.mapsApi.formatBytes(used)} / ${window.mapsApi.formatBytes(total)}`;
            }

            // Update message rates chart
            if (stats.messageRates) {
                this.updateMessageRatesChart(stats.messageRates);
            }

            // Update CPU chart
            if (stats.cpu) {
                this.updateCpuChart(stats.cpu);
            }

        } catch (error) {
            console.error('Failed to update server stats:', error);
        }
    }

    async updateCacheInfo() {
        try {
            const cacheInfo = await window.mapsApi.getCacheInfo();
            // Cache info is handled by the metrics section
            if (window.metrics) {
                window.metrics.updateCacheDisplay(cacheInfo);
            }
        } catch (error) {
            console.error('Failed to update cache info:', error);
        }
    }

    updateMessageRatesChart(messageRates) {
        const chart = this.charts.messageRates;
        const now = new Date().toLocaleTimeString();

        // Keep only last 20 data points
        if (chart.data.labels.length > 20) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
            chart.data.datasets[1].data.shift();
        }

        chart.data.labels.push(now);
        chart.data.datasets[0].data.push(messageRates.publish || 0);
        chart.data.datasets[1].data.push(messageRates.subscribe || 0);
        
        chart.update('none'); // Update without animation for performance
    }

    updateCpuChart(cpuData) {
        const chart = this.charts.cpu;
        const now = new Date().toLocaleTimeString();

        // Keep only last 20 data points
        if (chart.data.labels.length > 20) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
        }

        chart.data.labels.push(now);
        chart.data.datasets[0].data.push(cpuData.usage || 0);
        
        chart.update('none'); // Update without animation for performance
    }

    async refreshAllData() {
        try {
            await Promise.all([
                this.updateServerHealth(),
                this.updateServerInfo(),
                this.updateServerStats()
            ]);
        } catch (error) {
            console.error('Failed to refresh data:', error);
        }
    }

    startAutoRefresh() {
        if (this.autoRefresh && !this.refreshTimer) {
            this.refreshTimer = setInterval(() => {
                this.refreshAllData();
            }, this.refreshInterval);
        }
    }

    pauseAutoRefresh() {
        if (this.refreshTimer) {
            clearInterval(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    resumeAutoRefresh() {
        if (this.autoRefresh) {
            this.startAutoRefresh();
        }
    }

    toggleAutoRefresh() {
        this.autoRefresh = !this.autoRefresh;
        const btn = document.getElementById('autoRefreshBtn');
        const status = document.getElementById('refreshStatus');
        
        if (this.autoRefresh) {
            status.textContent = 'ON';
            btn.classList.remove('btn-secondary');
            btn.classList.add('btn-primary');
            this.startAutoRefresh();
        } else {
            status.textContent = 'OFF';
            btn.classList.remove('btn-primary');
            btn.classList.add('btn-secondary');
            this.pauseAutoRefresh();
        }
    }

    changeRefreshInterval() {
        const intervals = [
            { label: '1s', value: 1000 },
            { label: '5s', value: 5000 },
            { label: '10s', value: 10000 },
            { label: '30s', value: 30000 },
            { label: '1m', value: 60000 }
        ];

        const currentIndex = intervals.findIndex(i => i.value === this.refreshInterval);
        const nextIndex = (currentIndex + 1) % intervals.length;
        
        this.refreshInterval = intervals[nextIndex].value;
        document.getElementById('intervalDisplay').textContent = intervals[nextIndex].label;
        
        // Restart auto refresh with new interval
        this.pauseAutoRefresh();
        if (this.autoRefresh) {
            this.startAutoRefresh();
        }
    }

    switchSection(sectionName) {
        // Update navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });
        document.querySelector(`[data-section="${sectionName}"]`).classList.add('active');

        // Update sections
        document.querySelectorAll('.dashboard-section').forEach(section => {
            section.classList.remove('active');
        });
        document.getElementById(`${sectionName}-section`).classList.add('active');

        // Load section-specific data
        this.loadSectionData(sectionName);
    }

    async loadSectionData(sectionName) {
        switch (sectionName) {
            case 'dashboard':
                await this.refreshAllData();
                break;
            case 'metrics':
                if (window.metrics) {
                    window.metrics.loadMetricsData();
                }
                break;
            case 'logs':
                if (window.logs) {
                    window.logs.initializeLogs();
                }
                break;
            case 'subsystems':
                if (window.subsystems) {
                    window.subsystems.loadSubsystems();
                }
                break;
        }
    }

    async checkForUpdates() {
        try {
            // Check for updates every 5 minutes
            const updateInterval = 5 * 60 * 1000;
            
            const checkUpdates = async () => {
                try {
                    const updates = await window.mapsApi.getUpdates();
                    if (updates && Object.keys(updates).length > 0) {
                        this.showUpdatesBanner(updates);
                    }
                } catch (error) {
                    console.log('No updates available or failed to check updates');
                }
            };

            // Initial check
            await checkUpdates();
            
            // Periodic checks
            setInterval(checkUpdates, updateInterval);
            
        } catch (error) {
            console.error('Failed to setup update checking:', error);
        }
    }

    showUpdatesBanner(updates) {
        const banner = document.getElementById('updatesBanner');
        const message = document.getElementById('updatesMessage');
        
        let updateText = 'Updates available: ';
        if (updates.schemaUpdate) {
            updateText += 'Schema updates available';
        }
        if (updates.serverUpdate) {
            updateText += 'Server updates available';
        }
        
        message.textContent = updateText;
        banner.style.display = 'flex';
    }

    dismissUpdates() {
        const banner = document.getElementById('updatesBanner');
        banner.style.display = 'none';
    }

    showError(message) {
        console.error(message);
        // You could implement a toast notification here
    }

    showSuccess(message) {
        console.log(message);
        // You could implement a toast notification here
    }

    // Cleanup method
    destroy() {
        this.pauseAutoRefresh();
        
        // Destroy charts
        Object.values(this.charts).forEach(chart => {
            if (chart && chart.destroy) {
                chart.destroy();
            }
        });
        
        this.charts = {};
    }
}

// Initialize dashboard when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.dashboard = new Dashboard();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.dashboard) {
        window.dashboard.destroy();
    }
});