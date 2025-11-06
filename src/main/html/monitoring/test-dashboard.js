/**
 * Simple test to verify monitoring dashboard components
 * This file can be run in a browser console to test the dashboard
 */

// Test API Client
function testApiClient() {
    console.log('Testing API Client...');
    
    if (typeof window.mapsApi === 'undefined') {
        console.error('API Client not found');
        return false;
    }
    
    // Test basic functionality
    const tests = [
        () => typeof window.mapsApi.request === 'function',
        () => typeof window.mapsApi.getServerStatus === 'function',
        () => typeof window.mapsApi.getServerHealth === 'function',
        () => typeof window.mapsApi.getServerInfo === 'function',
        () => typeof window.mapsApi.getServerStats === 'function',
        () => typeof window.mapsApi.getCacheInfo === 'function',
        () => typeof window.mapsApi.getLogEntries === 'function',
        () => typeof window.mapsApi.getSseToken === 'function',
        () => typeof window.mapsApi.createSseConnection === 'function'
    ];
    
    const results = tests.map((test, index) => {
        try {
            return test();
        } catch (error) {
            console.error(`Test ${index} failed:`, error);
            return false;
        }
    });
    
    const passed = results.filter(r => r).length;
    console.log(`API Client Tests: ${passed}/${results.length} passed`);
    return passed === results.length;
}

// Test Dashboard Components
function testDashboardComponents() {
    console.log('Testing Dashboard Components...');
    
    const components = [
        { name: 'Dashboard', element: '.dashboard-container' },
        { name: 'Navigation', element: '.dashboard-nav' },
        { name: 'Dashboard Section', element: '#dashboard-section' },
        { name: 'Metrics Section', element: '#metrics-section' },
        { name: 'Logs Section', element: '#logs-section' },
        { name: 'Subsystems Section', element: '#subsystems-section' },
        { name: 'Status Cards', element: '.status-card' },
        { name: 'Charts', element: '.chart-container' }
    ];
    
    const results = components.map(comp => {
        const element = document.querySelector(comp.element);
        const exists = element !== null;
        if (!exists) {
            console.warn(`Component ${comp.name} not found: ${comp.element}`);
        }
        return exists;
    });
    
    const passed = results.filter(r => r).length;
    console.log(`Dashboard Component Tests: ${passed}/${results.length} passed`);
    return passed === results.length;
}

// Test Charts
function testCharts() {
    console.log('Testing Charts...');
    
    if (typeof Chart === 'undefined') {
        console.error('Chart.js not loaded');
        return false;
    }
    
    const chartCanvases = [
        'messageRatesChart',
        'cpuChart',
        'threadChart'
    ];
    
    const results = chartCanvases.map(id => {
        const canvas = document.getElementById(id);
        const exists = canvas !== null;
        if (!exists) {
            console.warn(`Chart canvas ${id} not found`);
        }
        return exists;
    });
    
    const passed = results.filter(r => r).length;
    console.log(`Chart Tests: ${passed}/${results.length} passed`);
    return passed === results.length;
}

// Test Event Listeners
function testEventListeners() {
    console.log('Testing Event Listeners...');
    
    const buttons = [
        { id: 'autoRefreshBtn', name: 'Auto Refresh' },
        { id: 'refreshIntervalBtn', name: 'Refresh Interval' },
        { id: 'manualRefreshBtn', name: 'Manual Refresh' },
        { id: 'pauseLogsBtn', name: 'Pause Logs' },
        { id: 'resumeLogsBtn', name: 'Resume Logs' },
        { id: 'downloadLogsBtn', name: 'Download Logs' },
        { id: 'clearLogsBtn', name: 'Clear Logs' }
    ];
    
    const results = buttons.map(btn => {
        const element = document.getElementById(btn.id);
        const exists = element !== null;
        if (!exists) {
            console.warn(`Button ${btn.name} not found: ${btn.id}`);
        }
        return exists;
    });
    
    const passed = results.filter(r => r).length;
    console.log(`Event Listener Tests: ${passed}/${results.length} passed`);
    return passed === results.length;
}

// Test Navigation
function testNavigation() {
    console.log('Testing Navigation...');
    
    const navLinks = document.querySelectorAll('.nav-link');
    if (navLinks.length === 0) {
        console.error('No navigation links found');
        return false;
    }
    
    console.log(`Found ${navLinks.length} navigation links`);
    
    // Test navigation switching
    const firstLink = navLinks[0];
    if (firstLink && firstLink.dataset.section) {
        console.log(`Navigation links have data-section attributes`);
        return true;
    }
    
    console.warn('Navigation links missing data-section attributes');
    return false;
}

// Test API Connectivity (if server is available)
async function testApiConnectivity() {
    console.log('Testing API Connectivity...');
    
    try {
        const response = await window.mapsApi.getServerHealth();
        console.log('API Connectivity: SUCCESS');
        console.log('Server Health Response:', response);
        return true;
    } catch (error) {
        console.warn('API Connectivity: FAILED (server may not be running)');
        console.warn('Error:', error.message);
        return false;
    }
}

// Run all tests
async function runAllTests() {
    console.log('=== MAPS Monitoring Dashboard Tests ===');
    
    const tests = [
        { name: 'API Client', fn: testApiClient },
        { name: 'Dashboard Components', fn: testDashboardComponents },
        { name: 'Charts', fn: testCharts },
        { name: 'Event Listeners', fn: testEventListeners },
        { name: 'Navigation', fn: testNavigation },
        { name: 'API Connectivity', fn: testApiConnectivity }
    ];
    
    const results = [];
    
    for (const test of tests) {
        try {
            const result = await test.fn();
            results.push({ name: test.name, passed: result });
        } catch (error) {
            console.error(`Test ${test.name} failed with exception:`, error);
            results.push({ name: test.name, passed: false, error: error.message });
        }
    }
    
    // Summary
    console.log('\n=== Test Summary ===');
    const passed = results.filter(r => r.passed).length;
    const total = results.length;
    
    results.forEach(result => {
        const status = result.passed ? '✅ PASS' : '❌ FAIL';
        console.log(`${status} ${result.name}${result.error ? ': ' + result.error : ''}`);
    });
    
    console.log(`\nOverall: ${passed}/${total} tests passed`);
    
    if (passed === total) {
        console.log('🎉 All tests passed! Dashboard is working correctly.');
    } else {
        console.log('⚠️  Some tests failed. Please check the issues above.');
    }
    
    return results;
}

// Export test functions for manual testing
window.dashboardTests = {
    runAllTests,
    testApiClient,
    testDashboardComponents,
    testCharts,
    testEventListeners,
    testNavigation,
    testApiConnectivity
};

console.log('Dashboard tests loaded. Run dashboardTests.runAllTests() to execute all tests.');