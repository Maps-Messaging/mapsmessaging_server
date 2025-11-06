/**
 * MAPS Messaging Server Monitoring Dashboard - Subsystems Module
 * Handles subsystem status display and management
 */

class Subsystems {
    constructor() {
        this.subsystems = [];
        this.refreshInterval = 10000; // 10 seconds
        this.refreshTimer = null;
        this.init();
    }

    init() {
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Auto-refresh when subsystems section is visible
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    this.startAutoRefresh();
                } else {
                    this.stopAutoRefresh();
                }
            });
        });

        const subsystemsSection = document.getElementById('subsystems-section');
        if (subsystemsSection) {
            observer.observe(subsystemsSection);
        }
    }

    async loadSubsystems() {
        try {
            const statusResponse = await window.mapsApi.getServerStatus();
            const subsystems = statusResponse.subsystems || statusResponse || [];
            
            this.subsystems = subsystems;
            this.displaySubsystems();
            
        } catch (error) {
            console.error('Failed to load subsystems:', error);
            this.displayError('Failed to load subsystem status');
        }
    }

    displaySubsystems() {
        const container = document.getElementById('subsystemsGrid');
        if (!container) return;

        if (this.subsystems.length === 0) {
            container.innerHTML = '<div class="loading">No subsystems found</div>';
            return;
        }

        // Group subsystems by status for better organization
        const groupedSubsystems = this.groupSubsystemsByStatus();
        
        const subsystemsHtml = Object.entries(groupedSubsystems).map(([status, subsystems]) => {
            const statusClass = status.toLowerCase();
            const statusIcon = window.mapsApi.getStatusIcon(status);
            
            return `
                <div class="subsystem-group">
                    <div class="subsystem-group-header">
                        <h3>
                            <i class="${statusIcon}"></i>
                            ${status} Subsystems (${subsystems.length})
                        </h3>
                    </div>
                    <div class="subsystem-group-content">
                        ${subsystems.map(subsystem => this.createSubsystemCard(subsystem)).join('')}
                    </div>
                </div>
            `;
        }).join('');

        container.innerHTML = subsystemsHtml;
    }

    groupSubsystemsByStatus() {
        const groups = {
            'OK': [],
            'WARN': [],
            'ERROR': [],
            'STOPPED': [],
            'DISABLED': [],
            'PAUSED': []
        };

        this.subsystems.forEach(subsystem => {
            const status = subsystem.status || 'UNKNOWN';
            if (groups[status]) {
                groups[status].push(subsystem);
            } else {
                groups['ERROR'].push({ ...subsystem, status: 'UNKNOWN', comment: 'Unknown status' });
            }
        });

        // Remove empty groups
        Object.keys(groups).forEach(key => {
            if (groups[key].length === 0) {
                delete groups[key];
            }
        });

        return groups;
    }

    createSubsystemCard(subsystem) {
        const status = subsystem.status || 'UNKNOWN';
        const statusClass = status.toLowerCase();
        const statusColor = window.mapsApi.getStatusColor(status);
        const statusIcon = window.mapsApi.getStatusIcon(status);
        
        return `
            <div class="subsystem-card ${statusClass}">
                <div class="subsystem-header">
                    <div class="subsystem-name">
                        <i class="${statusIcon}" style="color: ${statusColor}"></i>
                        ${this.escapeHtml(subsystem.name || 'Unknown Subsystem')}
                    </div>
                    <div class="subsystem-status ${statusClass}">
                        ${status}
                    </div>
                </div>
                <div class="subsystem-details">
                    <div class="subsystem-comment">
                        ${this.escapeHtml(subsystem.comment || 'No additional information available')}
                    </div>
                    ${this.createSubsystemDetails(subsystem)}
                </div>
                ${this.createSubsystemActions(subsystem)}
            </div>
        `;
    }

    createSubsystemDetails(subsystem) {
        const details = [];
        
        if (subsystem.uptime !== undefined) {
            details.push(`
                <div class="detail-item">
                    <span class="detail-label">Uptime:</span>
                    <span class="detail-value">${window.mapsApi.formatUptime(subsystem.uptime)}</span>
                </div>
            `);
        }
        
        if (subsystem.lastActivity !== undefined) {
            const lastActivity = new Date(subsystem.lastActivity).toLocaleString();
            details.push(`
                <div class="detail-item">
                    <span class="detail-label">Last Activity:</span>
                    <span class="detail-value">${lastActivity}</span>
                </div>
            `);
        }
        
        if (subsystem.metrics !== undefined) {
            Object.entries(subsystem.metrics).forEach(([key, value]) => {
                details.push(`
                    <div class="detail-item">
                        <span class="detail-label">${this.formatLabel(key)}:</span>
                        <span class="detail-value">${this.formatValue(value)}</span>
                    </div>
                `);
            });
        }
        
        if (subsystem.configuration !== undefined) {
            Object.entries(subsystem.configuration).forEach(([key, value]) => {
                details.push(`
                    <div class="detail-item">
                        <span class="detail-label">${this.formatLabel(key)}:</span>
                        <span class="detail-value">${this.formatValue(value)}</span>
                    </div>
                `);
            });
        }
        
        return details.length > 0 ? `<div class="subsystem-metrics">${details.join('')}</div>` : '';
    }

    createSubsystemActions(subsystem) {
        const actions = [];
        
        // Add refresh action for all subsystems
        actions.push(`
            <button class="btn btn-small btn-secondary" onclick="window.subsystems.refreshSubsystem('${subsystem.name}')">
                <i class="fas fa-sync"></i> Refresh
            </button>
        `);
        
        // Add restart action for subsystems that support it
        if (this.supportsRestart(subsystem)) {
            actions.push(`
                <button class="btn btn-small btn-warning" onclick="window.subsystems.restartSubsystem('${subsystem.name}')">
                    <i class="fas fa-redo"></i> Restart
                </button>
            `);
        }
        
        // Add stop/start actions for subsystems that support it
        if (this.supportsStartStop(subsystem)) {
            if (subsystem.status === 'STOPPED') {
                actions.push(`
                    <button class="btn btn-small btn-success" onclick="window.subsystems.startSubsystem('${subsystem.name}')">
                        <i class="fas fa-play"></i> Start
                    </button>
                `);
            } else {
                actions.push(`
                    <button class="btn btn-small btn-error" onclick="window.subsystems.stopSubsystem('${subsystem.name}')">
                        <i class="fas fa-stop"></i> Stop
                    </button>
                `);
            }
        }
        
        return actions.length > 0 ? `<div class="subsystem-actions">${actions.join('')}</div>` : '';
    }

    formatLabel(key) {
        return key.replace(/([A-Z])/g, ' $1')
                   .replace(/^./, str => str.toUpperCase())
                   .trim();
    }

    formatValue(value) {
        if (typeof value === 'number') {
            if (value > 1024 * 1024 * 1024) {
                return (value / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
            } else if (value > 1024 * 1024) {
                return (value / (1024 * 1024)).toFixed(2) + ' MB';
            } else if (value > 1024) {
                return (value / 1024).toFixed(2) + ' KB';
            }
            return value.toLocaleString();
        } else if (typeof value === 'boolean') {
            return value ? 'Yes' : 'No';
        } else if (typeof value === 'object' && value !== null) {
            return JSON.stringify(value);
        }
        return String(value);
    }

    supportsRestart(subsystem) {
        // Check if subsystem supports restart based on its type or configuration
        const restartableTypes = ['DestinationManager', 'SessionManager', 'NetworkManager'];
        return restartableTypes.some(type => subsystem.name?.includes(type));
    }

    supportsStartStop(subsystem) {
        // Check if subsystem supports start/stop operations
        const controllableTypes = ['DiscoveryManager', 'SchemaManager', 'DeviceManager'];
        return controllableTypes.some(type => subsystem.name?.includes(type));
    }

    async refreshSubsystem(subsystemName) {
        try {
            // This would call a hypothetical API to refresh a specific subsystem
            // For now, we'll just reload all subsystems
            await this.loadSubsystems();
            this.showNotification(`Subsystem ${subsystemName} refreshed`, 'success');
        } catch (error) {
            console.error(`Failed to refresh subsystem ${subsystemName}:`, error);
            this.showNotification(`Failed to refresh subsystem ${subsystemName}`, 'error');
        }
    }

    async restartSubsystem(subsystemName) {
        if (!confirm(`Are you sure you want to restart the ${subsystemName} subsystem?`)) {
            return;
        }
        
        try {
            // This would call a hypothetical API to restart a specific subsystem
            // For now, we'll just show a notification
            this.showNotification(`Subsystem ${subsystemName} restart initiated`, 'warning');
            setTimeout(() => this.loadSubsystems(), 2000); // Reload after a delay
        } catch (error) {
            console.error(`Failed to restart subsystem ${subsystemName}:`, error);
            this.showNotification(`Failed to restart subsystem ${subsystemName}`, 'error');
        }
    }

    async startSubsystem(subsystemName) {
        try {
            // This would call a hypothetical API to start a specific subsystem
            this.showNotification(`Subsystem ${subsystemName} start initiated`, 'success');
            setTimeout(() => this.loadSubsystems(), 2000);
        } catch (error) {
            console.error(`Failed to start subsystem ${subsystemName}:`, error);
            this.showNotification(`Failed to start subsystem ${subsystemName}`, 'error');
        }
    }

    async stopSubsystem(subsystemName) {
        if (!confirm(`Are you sure you want to stop the ${subsystemName} subsystem?`)) {
            return;
        }
        
        try {
            // This would call a hypothetical API to stop a specific subsystem
            this.showNotification(`Subsystem ${subsystemName} stop initiated`, 'warning');
            setTimeout(() => this.loadSubsystems(), 2000);
        } catch (error) {
            console.error(`Failed to stop subsystem ${subsystemName}:`, error);
            this.showNotification(`Failed to stop subsystem ${subsystemName}`, 'error');
        }
    }

    startAutoRefresh() {
        if (this.refreshTimer) return;
        
        this.refreshTimer = setInterval(() => {
            this.loadSubsystems();
        }, this.refreshInterval);
    }

    stopAutoRefresh() {
        if (this.refreshTimer) {
            clearInterval(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    showNotification(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
        // You could implement a toast notification system here
    }

    displayError(message) {
        const container = document.getElementById('subsystemsGrid');
        if (container) {
            container.innerHTML = `<div class="error-message">${message}</div>`;
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Public method to get subsystem statistics
    getSubsystemStats() {
        const stats = {
            total: this.subsystems.length,
            byStatus: {}
        };
        
        this.subsystems.forEach(subsystem => {
            const status = subsystem.status || 'UNKNOWN';
            stats.byStatus[status] = (stats.byStatus[status] || 0) + 1;
        });
        
        return stats;
    }

    // Cleanup method
    destroy() {
        this.stopAutoRefresh();
        this.subsystems = [];
        console.log('Subsystems module cleaned up');
    }
}

// Initialize subsystems module
document.addEventListener('DOMContentLoaded', () => {
    window.subsystems = new Subsystems();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.subsystems) {
        window.subsystems.destroy();
    }
});