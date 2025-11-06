// Resource Admin JavaScript

class ResourceAdmin {
    constructor() {
        this.currentResource = 'destinations';
        this.currentPage = 1;
        this.pageSize = 20;
        this.totalItems = 0;
        this.selectedItems = new Set();
        this.autoRefreshInterval = null;
        this.apiBase = '/api/v1';
        this.editingItem = null;
        
        this.resourceConfig = {
            destinations: {
                title: 'Destinations',
                apiEndpoint: '/server/destinations',
                detailEndpoint: '/server/destination',
                createEndpoint: '/server/destination',
                updateEndpoint: '/server/destination',
                deleteEndpoint: '/server/destination',
                fields: [
                    { name: 'name', label: 'Name', type: 'text', required: true },
                    { name: 'type', label: 'Type', type: 'select', options: ['queue', 'topic'], required: true },
                    { name: 'schemaId', label: 'Schema ID', type: 'text' }
                ],
                displayFields: [
                    { name: 'name', label: 'Name' },
                    { name: 'type', label: 'Type' },
                    { name: 'status', label: 'Status', type: 'badge' },
                    { name: 'storedMessages', label: 'Messages', type: 'number' },
                    { name: 'created', label: 'Created', type: 'date' }
                ]
            },
            connections: {
                title: 'Connections',
                apiEndpoint: '/server/connections',
                detailEndpoint: '/server/connection',
                closeEndpoint: '/server/connection/close',
                fields: [],
                displayFields: [
                    { name: 'id', label: 'ID', type: 'number' },
                    { name: 'name', label: 'Name' },
                    { name: 'user', label: 'User' },
                    { name: 'protocolName', label: 'Protocol' },
                    { name: 'adapter', label: 'Adapter' },
                    { name: 'connectedTimeMs', label: 'Connected', type: 'duration' }
                ],
                actions: ['close']
            },
            interfaces: {
                title: 'Interfaces',
                apiEndpoint: '/server/interfaces',
                detailEndpoint: '/server/interface/{endpoint}',
                statusEndpoint: '/server/interface/{endpoint}/status',
                actions: ['start', 'stop', 'pause', 'resume'],
                bulkActions: ['startAll', 'stopAll', 'pauseAll', 'resumeAll'],
                fields: [
                    { name: 'name', label: 'Name', type: 'text', required: true },
                    { name: 'protocol', label: 'Protocol', type: 'text', required: true },
                    { name: 'port', label: 'Port', type: 'number', required: true }
                ],
                displayFields: [
                    { name: 'name', label: 'Name' },
                    { name: 'protocol', label: 'Protocol' },
                    { name: 'state', label: 'Status', type: 'badge' },
                    { name: 'port', label: 'Port' }
                ]
            },
            integrations: {
                title: 'Integrations',
                apiEndpoint: '/server/integration',
                detailEndpoint: '/server/integration/{name}',
                connectionEndpoint: '/server/integration/{name}/connection',
                actions: ['start', 'stop', 'pause', 'resume'],
                bulkActions: ['startAll', 'stopAll', 'pauseAll', 'resumeAll'],
                fields: [
                    { name: 'name', label: 'Name', type: 'text', required: true },
                    { name: 'type', label: 'Type', type: 'text', required: true },
                    { name: 'hostname', label: 'Hostname', type: 'text', required: true }
                ],
                displayFields: [
                    { name: 'name', label: 'Name' },
                    { name: 'type', label: 'Type' },
                    { name: 'state', label: 'Status', type: 'badge' },
                    { name: 'hostname', label: 'Hostname' }
                ]
            },
            sessions: {
                title: 'Sessions',
                apiEndpoint: '/session',
                detailEndpoint: '/session/{sessionId}',
                deleteEndpoint: '/session/{sessionId}',
                bulkDeleteEndpoint: '/session/terminateAll',
                fields: [],
                displayFields: [
                    { name: 'id', label: 'ID', type: 'number' },
                    { name: 'user', label: 'User' },
                    { name: 'name', label: 'Name' },
                    { name: 'protocolName', label: 'Protocol' },
                    { name: 'connectedTimeMs', label: 'Connected', type: 'duration' }
                ],
                actions: ['terminate']
            }
        };
        
        this.init();
    }
    
    init() {
        this.setupEventListeners();
        this.loadResource('destinations');
        this.checkConnectionStatus();
    }
    
    setupEventListeners() {
        // Navigation
        document.querySelectorAll('.sidebar .nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const resource = e.currentTarget.dataset.resource;
                this.loadResource(resource);
            });
        });
        
        // Buttons
        document.getElementById('refreshBtn').addEventListener('click', () => this.refreshData());
        document.getElementById('createBtn').addEventListener('click', () => this.showCreateForm());
        document.getElementById('bulkDeleteBtn').addEventListener('click', () => this.bulkDelete());
        document.getElementById('selectAll').addEventListener('change', (e) => this.selectAll(e.target.checked));
        
        // Search and Filters
        document.getElementById('searchInput').addEventListener('input', (e) => this.handleSearch(e.target.value));
        document.getElementById('filterSelect').addEventListener('change', () => this.refreshData());
        document.getElementById('sortBySelect').addEventListener('change', () => this.refreshData());
        
        // Auto Refresh
        document.getElementById('autoRefresh').addEventListener('change', (e) => this.toggleAutoRefresh(e.target.checked));
        
        // Drawer
        document.getElementById('closeDrawer').addEventListener('click', () => this.closeDrawer());
        document.getElementById('cancelEdit').addEventListener('click', () => this.closeDrawer());
        document.getElementById('saveChanges').addEventListener('click', () => this.saveChanges());
        
        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') this.closeDrawer();
            if (e.ctrlKey && e.key === 'r') {
                e.preventDefault();
                this.refreshData();
            }
        });
    }
    
    async loadResource(resourceType) {
        this.currentResource = resourceType;
        this.selectedItems.clear();
        this.currentPage = 1;
        
        // Update navigation
        document.querySelectorAll('.sidebar .nav-link').forEach(link => {
            link.classList.remove('active');
        });
        document.querySelector(`[data-resource="${resourceType}"]`).classList.add('active');
        
        // Update title
        const config = this.resourceConfig[resourceType];
        document.getElementById('resourceTitle').textContent = config.title;
        
        // Update create button visibility
        const createBtn = document.getElementById('createBtn');
        createBtn.style.display = config.fields.length > 0 ? 'inline-block' : 'none';
        
        // Update filters
        this.updateFilters();
        
        // Load data
        await this.refreshData();
    }
    
    async refreshData() {
        try {
            this.showLoading(true);
            const config = this.resourceConfig[this.currentResource];
            const params = new URLSearchParams();
            
            // Add search parameter
            const searchValue = document.getElementById('searchInput').value;
            if (searchValue) {
                if (this.currentResource === 'destinations') {
                    params.append('filter', `name LIKE '%${searchValue}%'`);
                } else {
                    params.append('filter', searchValue);
                }
            }
            
            // Add pagination
            params.append('size', this.pageSize);
            
            // Add sorting
            const sortBy = document.getElementById('sortBySelect').value;
            if (sortBy) {
                params.append('sortBy', sortBy);
            }
            
            const response = await fetch(`${this.apiBase}${config.apiEndpoint}?${params}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const data = await response.json();
            this.renderTable(data);
            this.updatePagination(data);
            
        } catch (error) {
            console.error('Error loading data:', error);
            this.showToast('Error loading data: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
        }
    }
    
    renderTable(data) {
        const config = this.resourceConfig[this.currentResource];
        const tbody = document.getElementById('dataTableBody');
        const thead = document.querySelector('#dataTable thead tr');
        
        // Update table headers
        thead.innerHTML = `
            <th><input type="checkbox" class="form-check-input" id="selectAll"></th>
            ${config.displayFields.map(field => `<th>${field.label}</th>`).join('')}
            <th>Actions</th>
        `;
        
        // Clear table body
        tbody.innerHTML = '';
        
        // Get data array based on response structure
        let items = [];
        if (data.destinations) items = data.destinations;
        else if (data.endPoints) items = data.endPoints;
        else if (data.interfaces) items = data.interfaces;
        else if (data.integrations) items = data.integrations;
        else if (data.endPointDetails) items = data.endPointDetails;
        else items = data || [];
        
        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="${config.displayFields.length + 2}" class="text-center text-muted">
                        <i class="bi bi-inbox" style="font-size: 2rem;"></i>
                        <p>No ${config.title.toLowerCase()} found</p>
                    </td>
                </tr>
            `;
            return;
        }
        
        // Render rows
        items.forEach(item => {
            const row = document.createElement('tr');
            const itemId = item.id || item.name;
            
            row.innerHTML = `
                <td><input type="checkbox" class="form-check-input item-select" data-id="${itemId}"></td>
                ${config.displayFields.map(field => this.renderFieldValue(item, field)).join('')}
                <td>${this.renderActions(item, config)}</td>
            `;
            
            tbody.appendChild(row);
        });
        
        // Re-attach select all listener
        document.getElementById('selectAll').addEventListener('change', (e) => this.selectAll(e.target.checked));
        
        // Attach item selection listeners
        document.querySelectorAll('.item-select').forEach(checkbox => {
            checkbox.addEventListener('change', (e) => {
                const id = e.target.dataset.id;
                if (e.target.checked) {
                    this.selectedItems.add(id);
                } else {
                    this.selectedItems.delete(id);
                }
                this.updateBulkActions();
            });
        });
    }
    
    renderFieldValue(item, field) {
        let value = item[field.name];
        
        if (field.type === 'badge') {
            const statusClass = this.getStatusClass(value);
            return `<td><span class="status-badge ${statusClass}">${value}</span></td>`;
        } else if (field.type === 'duration') {
            const duration = this.formatDuration(value);
            return `<td>${duration}</td>`;
        } else if (field.type === 'date') {
            const date = new Date(value).toLocaleString();
            return `<td>${date}</td>`;
        } else if (field.type === 'number') {
            return `<td>${this.formatNumber(value)}</td>`;
        } else {
            return `<td>${value || '-'}</td>`;
        }
    }
    
    renderActions(item, config) {
        if (!config.actions && !config.bulkActions) return '<td>-</td>';
        
        let actions = '';
        
        if (config.actions) {
            config.actions.forEach(action => {
                const itemId = item.id || item.name;
                const endpointParam = this.currentResource === 'interfaces' || this.currentResource === 'integrations' ? itemId : 'connectionId';
                
                if (action === 'close') {
                    actions += `<button class="btn btn-sm btn-outline-danger btn-action" onclick="resourceAdmin.performAction('${action}', '${itemId}')" title="Close">
                        <i class="bi bi-x-circle"></i>
                    </button>`;
                } else if (action === 'terminate') {
                    actions += `<button class="btn btn-sm btn-outline-danger btn-action" onclick="resourceAdmin.performAction('${action}', '${itemId}')" title="Terminate">
                        <i class="bi bi-person-x"></i>
                    </button>`;
                } else {
                    actions += `<button class="btn btn-sm btn-outline-secondary btn-action" onclick="resourceAdmin.performAction('${action}', '${itemId}')" title="${action.charAt(0).toUpperCase() + action.slice(1)}">
                        <i class="bi bi-${this.getActionIcon(action)}"></i>
                    </button>`;
                }
            });
        }
        
        // Add view details button
        actions += `<button class="btn btn-sm btn-outline-primary btn-action" onclick="resourceAdmin.showDetails('${item.id || item.name}')" title="View Details">
            <i class="bi bi-eye"></i>
        </button>`;
        
        return `<td><div class="action-buttons">${actions}</div></td>`;
    }
    
    getActionIcon(action) {
        const icons = {
            start: 'play-circle',
            stop: 'stop-circle',
            pause: 'pause-circle',
            resume: 'play-circle'
        };
        return icons[action] || 'gear';
    }
    
    getStatusClass(status) {
        const statusMap = {
            'running': 'status-running',
            'started': 'status-running',
            'active': 'status-running',
            'stopped': 'status-stopped',
            'inactive': 'status-stopped',
            'paused': 'status-paused',
            'unknown': 'status-unknown'
        };
        return statusMap[status?.toLowerCase()] || 'status-unknown';
    }
    
    formatDuration(ms) {
        if (!ms) return '-';
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        
        if (days > 0) return `${days}d ${hours % 24}h`;
        if (hours > 0) return `${hours}h ${minutes % 60}m`;
        if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
        return `${seconds}s`;
    }
    
    formatNumber(num) {
        if (!num) return '0';
        return new Intl.NumberFormat().format(num);
    }
    
    async performAction(action, itemId) {
        const config = this.resourceConfig[this.currentResource];
        let endpoint;
        
        if (action === 'close') {
            endpoint = `${config.closeEndpoint}?connectionId=${itemId}`;
        } else if (action === 'terminate') {
            endpoint = `${config.deleteEndpoint.replace('{sessionId}', itemId)}`;
        } else {
            endpoint = `${config.detailEndpoint.replace('{endpoint}', itemId)}/${action}`;
        }
        
        try {
            const response = await fetch(`${this.apiBase}${endpoint}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            this.showToast(`${action.charAt(0).toUpperCase() + action.slice(1)} operation completed successfully`, 'success');
            await this.refreshData();
            
        } catch (error) {
            console.error(`Error performing ${action}:`, error);
            this.showToast(`Error performing ${action}: ${error.message}`, 'error');
        }
    }
    
    async showDetails(itemId) {
        const config = this.resourceConfig[this.currentResource];
        let endpoint;
        
        if (this.currentResource === 'destinations') {
            endpoint = `${config.detailEndpoint}?destinationName=${itemId}`;
        } else if (this.currentResource === 'connections') {
            endpoint = `${config.detailEndpoint}?connectionId=${itemId}`;
        } else if (this.currentResource === 'sessions') {
            endpoint = config.detailEndpoint.replace('{sessionId}', itemId);
        } else {
            endpoint = config.detailEndpoint.replace('{endpoint}', itemId);
        }
        
        try {
            const response = await fetch(`${this.apiBase}${endpoint}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const details = await response.json();
            this.renderDetails(details);
            this.openDrawer();
            
        } catch (error) {
            console.error('Error loading details:', error);
            this.showToast('Error loading details: ' + error.message, 'error');
        }
    }
    
    renderDetails(details) {
        const config = this.resourceConfig[this.currentResource];
        const drawerTitle = document.getElementById('drawerTitle');
        const drawerBody = document.getElementById('drawerBody');
        
        drawerTitle.textContent = `Details - ${details.name || details.id || 'Unknown'}`;
        
        let html = '';
        
        // Render metrics if available
        if (details.storedMessages !== undefined || details.publishedMessages !== undefined) {
            html += '<div class="resource-metrics">';
            if (details.storedMessages !== undefined) {
                html += `
                    <div class="metric-card">
                        <div class="metric-value">${this.formatNumber(details.storedMessages)}</div>
                        <div class="metric-label">Stored Messages</div>
                    </div>
                `;
            }
            if (details.publishedMessages !== undefined) {
                html += `
                    <div class="metric-card">
                        <div class="metric-value">${this.formatNumber(details.publishedMessages)}</div>
                        <div class="metric-label">Published Messages</div>
                    </div>
                `;
            }
            if (details.deliveredMessages !== undefined) {
                html += `
                    <div class="metric-card">
                        <div class="metric-value">${this.formatNumber(details.deliveredMessages)}</div>
                        <div class="metric-label">Delivered Messages</div>
                    </div>
                `;
            }
            html += '</div>';
        }
        
        // Render fields
        if (config.fields && config.fields.length > 0) {
            html += '<form id="detailForm">';
            config.fields.forEach(field => {
                const value = details[field.name] || '';
                html += this.renderFormField(field, value, true);
            });
            html += '</form>';
        } else {
            // Display read-only details
            html += '<div class="detail-fields">';
            Object.keys(details).forEach(key => {
                if (typeof details[key] !== 'object' && key !== 'id') {
                    const label = key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1');
                    let value = details[key];
                    
                    if (key.includes('Time') || key.includes('Created')) {
                        value = new Date(value).toLocaleString();
                    } else if (key.includes('Ms')) {
                        value = this.formatDuration(value);
                    } else if (typeof value === 'number') {
                        value = this.formatNumber(value);
                    }
                    
                    html += `
                        <div class="form-group">
                            <label class="form-label">${label}</label>
                            <div class="form-control-plaintext">${value || '-'}</div>
                        </div>
                    `;
                }
            });
            html += '</div>';
        }
        
        drawerBody.innerHTML = html;
        this.editingItem = details;
    }
    
    showCreateForm() {
        const config = this.resourceConfig[this.currentResource];
        const drawerTitle = document.getElementById('drawerTitle');
        const drawerBody = document.getElementById('drawerBody');
        
        drawerTitle.textContent = `Create New ${config.title.slice(0, -1)}`;
        
        let html = '<form id="createForm">';
        config.fields.forEach(field => {
            html += this.renderFormField(field, '', false);
        });
        html += '</form>';
        
        drawerBody.innerHTML = html;
        this.editingItem = null;
        this.openDrawer();
    }
    
    renderFormField(field, value, readonly) {
        let html = '<div class="form-group">';
        html += `<label class="form-label">${field.label}${field.required ? ' *' : ''}</label>`;
        
        if (readonly) {
            html += `<div class="form-control-plaintext">${value}</div>`;
        } else if (field.type === 'select') {
            html += `<select class="form-select" name="${field.name}" ${field.required ? 'required' : ''}>`;
            field.options.forEach(option => {
                html += `<option value="${option}" ${value === option ? 'selected' : ''}>${option}</option>`;
            });
            html += '</select>';
        } else {
            html += `<input type="${field.type}" class="form-control" name="${field.name}" value="${value}" ${field.required ? 'required' : ''}>`;
        }
        
        html += '</div>';
        return html;
    }
    
    async saveChanges() {
        const form = document.getElementById('detailForm') || document.getElementById('createForm');
        if (!form) return;
        
        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());
        
        const config = this.resourceConfig[this.currentResource];
        let endpoint;
        let method;
        
        if (this.editingItem) {
            // Update existing item
            endpoint = `${this.apiBase}${config.updateEndpoint}?destinationName=${this.editingItem.name}`;
            method = 'PUT';
        } else {
            // Create new item
            endpoint = `${this.apiBase}${config.createEndpoint}`;
            method = 'POST';
        }
        
        try {
            const response = await fetch(endpoint, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            this.showToast(`Item ${this.editingItem ? 'updated' : 'created'} successfully`, 'success');
            this.closeDrawer();
            await this.refreshData();
            
        } catch (error) {
            console.error('Error saving changes:', error);
            this.showToast('Error saving changes: ' + error.message, 'error');
        }
    }
    
    selectAll(selectAll) {
        document.querySelectorAll('.item-select').forEach(checkbox => {
            checkbox.checked = selectAll;
            const id = checkbox.dataset.id;
            if (selectAll) {
                this.selectedItems.add(id);
            } else {
                this.selectedItems.delete(id);
            }
        });
        this.updateBulkActions();
    }
    
    updateBulkActions() {
        const bulkDeleteBtn = document.getElementById('bulkDeleteBtn');
        bulkDeleteBtn.style.display = this.selectedItems.size > 0 ? 'inline-block' : 'none';
    }
    
    async bulkDelete() {
        if (this.selectedItems.size === 0) return;
        
        const confirmed = await this.showConfirm(
            `Are you sure you want to delete ${this.selectedItems.size} item(s)?`,
            'Delete Items'
        );
        
        if (!confirmed) return;
        
        const config = this.resourceConfig[this.currentResource];
        let endpoint;
        
        if (config.bulkDeleteEndpoint) {
            endpoint = `${this.apiBase}${config.bulkDeleteEndpoint}`;
        } else {
            // Delete items one by one
            const promises = Array.from(this.selectedItems).map(itemId => {
                const deleteEndpoint = config.deleteEndpoint.replace('{sessionId}', itemId);
                return fetch(`${this.apiBase}${deleteEndpoint}`, { method: 'DELETE' });
            });
            
            try {
                await Promise.all(promises);
                this.showToast(`${this.selectedItems.size} item(s) deleted successfully`, 'success');
                this.selectedItems.clear();
                await this.refreshData();
            } catch (error) {
                console.error('Error deleting items:', error);
                this.showToast('Error deleting items: ' + error.message, 'error');
            }
            return;
        }
        
        try {
            const response = await fetch(endpoint, { method: 'PUT' });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            this.showToast('Bulk delete completed successfully', 'success');
            this.selectedItems.clear();
            await this.refreshData();
            
        } catch (error) {
            console.error('Error performing bulk delete:', error);
            this.showToast('Error performing bulk delete: ' + error.message, 'error');
        }
    }
    
    handleSearch(searchTerm) {
        clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
            this.refreshData();
        }, 300);
    }
    
    updateFilters() {
        const config = this.resourceConfig[this.currentResource];
        const filterSelect = document.getElementById('filterSelect');
        
        // Update filter options based on resource type
        let options = '<option value="">All Filters</option>';
        
        if (this.currentResource === 'destinations') {
            options += `
                <option value="type='queue'">Queues Only</option>
                <option value="type='topic'">Topics Only</option>
                <option value="storedMessages>0">With Messages</option>
                <option value="storedMessages=0">Empty</option>
            `;
        } else if (this.currentResource === 'connections') {
            options += `
                <option value="protocolName='MQTT'">MQTT</option>
                <option value="protocolName='AMQP'">AMQP</option>
                <option value="protocolName='REST'">REST</option>
            `;
        }
        
        filterSelect.innerHTML = options;
    }
    
    updatePagination(data) {
        const pagination = document.getElementById('pagination');
        const totalPages = Math.ceil(this.totalItems / this.pageSize);
        
        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }
        
        let html = '';
        
        // Previous button
        html += `
            <li class="page-item ${this.currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="resourceAdmin.goToPage(${this.currentPage - 1})">Previous</a>
            </li>
        `;
        
        // Page numbers
        const startPage = Math.max(1, this.currentPage - 2);
        const endPage = Math.min(totalPages, this.currentPage + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            html += `
                <li class="page-item ${i === this.currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="resourceAdmin.goToPage(${i})">${i}</a>
                </li>
            `;
        }
        
        // Next button
        html += `
            <li class="page-item ${this.currentPage === totalPages ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="resourceAdmin.goToPage(${this.currentPage + 1})">Next</a>
            </li>
        `;
        
        pagination.innerHTML = html;
    }
    
    goToPage(page) {
        this.currentPage = page;
        this.refreshData();
    }
    
    toggleAutoRefresh(enabled) {
        if (enabled) {
            this.autoRefreshInterval = setInterval(() => {
                this.refreshData();
            }, 30000); // Refresh every 30 seconds
        } else {
            if (this.autoRefreshInterval) {
                clearInterval(this.autoRefreshInterval);
                this.autoRefreshInterval = null;
            }
        }
    }
    
    openDrawer() {
        document.getElementById('detailDrawer').classList.add('open');
    }
    
    closeDrawer() {
        document.getElementById('detailDrawer').classList.remove('open');
        this.editingItem = null;
    }
    
    showLoading(show) {
        const spinner = document.querySelector('.loading-spinner');
        if (spinner) {
            spinner.classList.toggle('show', show);
        }
    }
    
    showToast(message, type = 'info') {
        const toastContainer = document.getElementById('toastContainer');
        const toastId = 'toast-' + Date.now();
        
        const toastHtml = `
            <div id="${toastId}" class="toast ${type}" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="toast-header">
                    <i class="bi bi-${this.getToastIcon(type)} me-2"></i>
                    <strong class="me-auto">${type.charAt(0).toUpperCase() + type.slice(1)}</strong>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;
        
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
        
        // Remove toast element after it's hidden
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
    
    getToastIcon(type) {
        const icons = {
            success: 'check-circle-fill',
            error: 'exclamation-triangle-fill',
            warning: 'exclamation-triangle-fill',
            info: 'info-circle-fill'
        };
        return icons[type] || 'info-circle-fill';
    }
    
    async showConfirm(message, title = 'Confirm Action') {
        return new Promise((resolve) => {
            document.getElementById('confirmModalTitle').textContent = title;
            document.getElementById('confirmModalBody').textContent = message;
            
            const modal = new bootstrap.Modal(document.getElementById('confirmModal'));
            
            document.getElementById('confirmAction').onclick = () => {
                modal.hide();
                resolve(true);
            };
            
            modal.show();
            
            // Handle modal close without confirmation
            document.getElementById('confirmModal').addEventListener('hidden.bs.modal', () => {
                resolve(false);
            }, { once: true });
        });
    }
    
    async checkConnectionStatus() {
        try {
            const response = await fetch(`${this.apiBase}/server/status`);
            const statusElement = document.getElementById('connectionStatus');
            
            if (response.ok) {
                statusElement.innerHTML = '<i class="bi bi-circle-fill text-success"></i> Connected';
                statusElement.className = 'connection-status connected';
            } else {
                statusElement.innerHTML = '<i class="bi bi-circle-fill text-danger"></i> Disconnected';
                statusElement.className = 'connection-status disconnected';
            }
        } catch (error) {
            const statusElement = document.getElementById('connectionStatus');
            statusElement.innerHTML = '<i class="bi bi-circle-fill text-warning"></i> Connection Error';
            statusElement.className = 'connection-status connecting';
        }
    }
}

// Initialize the application
const resourceAdmin = new ResourceAdmin();

// Add loading spinner to the page
document.addEventListener('DOMContentLoaded', () => {
    const table = document.querySelector('.table-responsive');
    if (table) {
        table.insertAdjacentHTML('beforebegin', `
            <div class="loading-spinner">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
            </div>
        `);
    }
});