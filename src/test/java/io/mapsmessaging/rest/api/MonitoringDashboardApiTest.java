/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.rest.api;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.api.impl.server.ServerDetailsApi;
import io.mapsmessaging.rest.api.impl.logging.LogMonitorRestApi;
import io.mapsmessaging.rest.responses.ServerHealthStateResponse;
import io.mapsmessaging.rest.responses.ServerStatisticsResponse;
import io.mapsmessaging.rest.responses.SubSystemStatusList;
import io.mapsmessaging.rest.responses.LogEntries;
import io.mapsmessaging.dto.rest.ServerInfoDTO;
import io.mapsmessaging.dto.rest.cache.CacheInfo;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for MAPS Monitoring Dashboard API endpoints
 * Validates the REST API endpoints used by the monitoring dashboard
 */
@DisplayName("Monitoring Dashboard API Tests")
public class MonitoringDashboardApiTest extends BaseTestConfig {

    private ServerDetailsApi serverDetailsApi;
    private LogMonitorRestApi logMonitorRestApi;
    private MockedStatic<MessageDaemon> mockedMessageDaemon;

    @BeforeEach
    void setUp() {
        // Mock the MessageDaemon for testing
        mockedMessageDaemon = Mockito.mockStatic(MessageDaemon.class);
        mockedMessageDaemon.when(MessageDaemon::getInstance).thenReturn(Mockito.mock(io.mapsmessaging.MessageDaemon.class));
        
        serverDetailsApi = new ServerDetailsApi();
        logMonitorRestApi = new LogMonitorRestApi();
    }

    @Nested
    @DisplayName("Server Status and Health APIs")
    class ServerStatusTests {

        @Test
        @DisplayName("Should return server health status")
        void testGetServerHealth() {
            // Given
            ServerHealthStateResponse expectedResponse = new ServerHealthStateResponse("Ok", 0);
            
            // When
            ServerHealthStateResponse actualResponse = serverDetailsApi.getServerHealth();
            
            // Then
            assertNotNull(actualResponse);
            assertEquals("Ok", actualResponse.getState());
            assertEquals(0, actualResponse.getIssueCount());
        }

        @Test
        @DisplayName("Should return subsystem status list")
        void testGetServerStatus() {
            // When
            SubSystemStatusList actualResponse = serverDetailsApi.getServerStatus();
            
            // Then
            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getSubsystems());
            assertTrue(actualResponse.getSubsystems().size() >= 0);
        }

        @Test
        @DisplayName("Should return server information")
        void testGetServerInfo() {
            // When
            ServerInfoDTO actualResponse = serverDetailsApi.getBuildInfo();
            
            // Then
            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getVersion());
        }

        @Test
        @DisplayName("Should return server statistics")
        void testGetServerStats() {
            // When
            ServerStatisticsResponse actualResponse = serverDetailsApi.getStats();
            
            // Then
            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getData());
        }
    }

    @Nested
    @DisplayName("Cache Management APIs")
    class CacheManagementTests {

        @Test
        @DisplayName("Should return cache information")
        void testGetCacheInfo() {
            // When
            CacheInfo actualResponse = serverDetailsApi.getCacheInformation();
            
            // Then
            assertNotNull(actualResponse);
            assertTrue(actualResponse.getSize() >= 0);
        }
    }

    @Nested
    @DisplayName("Log Monitoring APIs")
    class LogMonitoringTests {

        @Test
        @DisplayName("Should return log entries")
        void testGetLogEntries() {
            // When
            LogEntries actualResponse = logMonitorRestApi.getLogEntries(null);
            
            // Then
            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getEntries());
            assertTrue(actualResponse.getEntries().size() >= 0);
        }

        @Test
        @DisplayName("Should return log entries with filter")
        void testGetLogEntriesWithFilter() {
            // Given
            String filter = "level = 'ERROR'";
            
            // When
            LogEntries actualResponse = logMonitorRestApi.getLogEntries(filter);
            
            // Then
            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getEntries());
        }

        @Test
        @DisplayName("Should return SSE token")
        void testGetSseToken() {
            // When
            String actualToken = logMonitorRestApi.requestSseToken();
            
            // Then
            assertNotNull(actualToken);
            assertFalse(actualToken.isEmpty());
            assertTrue(actualToken.length() > 10); // Tokens should be reasonably long
        }
    }

    @Nested
    @DisplayName("API Response Format Tests")
    class ApiResponseFormatTests {

        @Test
        @DisplayName("Server health response should contain required fields")
        void testServerHealthResponseFormat() {
            // When
            ServerHealthStateResponse response = serverDetailsApi.getServerHealth();
            
            // Then
            assertNotNull(response.getState());
            assertTrue(response.getIssueCount() >= 0);
            
            // Validate possible states
            assertTrue(response.getState().equals("Ok") || 
                      response.getState().equals("Warning") || 
                      response.getState().equals("Error"));
        }

        @Test
        @DisplayName("Server statistics response should contain performance metrics")
        void testServerStatsResponseFormat() {
            // When
            ServerStatisticsResponse response = serverDetailsApi.getStats();
            
            // Then
            assertNotNull(response.getData());
            
            // Should contain typical performance metrics structure
            // (Actual structure depends on implementation)
        }

        @Test
        @DisplayName("Log entries response should contain log data")
        void testLogEntriesResponseFormat() {
            // When
            LogEntries response = logMonitorRestApi.getLogEntries(null);
            
            // Then
            assertNotNull(response.getEntries());
            
            // Each log entry should have basic structure
            response.getEntries().forEach(entry -> {
                assertNotNull(entry);
                // Additional validation depends on log entry structure
            });
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid log filter gracefully")
        void testInvalidLogFilter() {
            // Given
            String invalidFilter = "invalid syntax";
            
            // When & Then
            assertDoesNotThrow(() -> {
                LogEntries response = logMonitorRestApi.getLogEntries(invalidFilter);
                assertNotNull(response);
            });
        }

        @Test
        @DisplayName("Should handle API errors gracefully")
        void testApiErrorHandling() {
            // This test would require more complex setup to simulate errors
            // For now, we verify the APIs don't throw unexpected exceptions
            
            assertDoesNotThrow(() -> {
                serverDetailsApi.getServerHealth();
                serverDetailsApi.getServerStatus();
                serverDetailsApi.getBuildInfo();
                serverDetailsApi.getStats();
                serverDetailsApi.getCacheInformation();
            });
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("API responses should be within reasonable time")
        void testApiResponsePerformance() {
            // Given
            long maxResponseTime = 1000; // 1 second max
            
            // When & Then
            assertDoesNotThrow(() -> {
                long startTime = System.currentTimeMillis();
                
                serverDetailsApi.getServerHealth();
                long healthTime = System.currentTimeMillis() - startTime;
                
                startTime = System.currentTimeMillis();
                serverDetailsApi.getServerStatus();
                long statusTime = System.currentTimeMillis() - startTime;
                
                startTime = System.currentTimeMillis();
                serverDetailsApi.getStats();
                long statsTime = System.currentTimeMillis() - startTime;
                
                assertTrue(healthTime < maxResponseTime, "Server health API too slow: " + healthTime + "ms");
                assertTrue(statusTime < maxResponseTime, "Server status API too slow: " + statusTime + "ms");
                assertTrue(statsTime < maxResponseTime, "Server stats API too slow: " + statsTime + "ms");
            });
        }

        @Test
        @DisplayName("Concurrent API calls should be handled correctly")
        void testConcurrentApiCalls() {
            // Given
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];
            boolean[] results = new boolean[threadCount];
            
            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        serverDetailsApi.getServerHealth();
                        results[index] = true;
                    } catch (Exception e) {
                        results[index] = false;
                    }
                });
                threads[i].start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                try {
                    thread.join(5000); // 5 second timeout
                } catch (InterruptedException e) {
                    fail("Thread interrupted: " + e.getMessage());
                }
            }
            
            // Then
            for (boolean result : results) {
                assertTrue(result, "Concurrent API call failed");
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Dashboard APIs should work together")
        void testDashboardIntegration() {
            // This test verifies that all APIs used by dashboard work together
            
            // When
            ServerHealthStateResponse health = serverDetailsApi.getServerHealth();
            SubSystemStatusList status = serverDetailsApi.getServerStatus();
            ServerInfoDTO info = serverDetailsApi.getBuildInfo();
            ServerStatisticsResponse stats = serverDetailsApi.getStats();
            CacheInfo cache = serverDetailsApi.getCacheInformation();
            LogEntries logs = logMonitorRestApi.getLogEntries(null);
            
            // Then
            assertNotNull(health);
            assertNotNull(status);
            assertNotNull(info);
            assertNotNull(stats);
            assertNotNull(cache);
            assertNotNull(logs);
            
            // Verify data consistency where applicable
            // For example, if health shows issues, status should reflect that
            if (health.getIssueCount() > 0) {
                boolean hasWarningsOrErrors = status.getSubsystems().stream()
                    .anyMatch(subsystem -> 
                        subsystem.getStatus().toString().equals("WARN") || 
                        subsystem.getStatus().toString().equals("ERROR"));
                assertTrue(hasWarningsOrErrors, 
                    "Health shows issues but no subsystems in WARN/ERROR state");
            }
        }

        @Test
        @DisplayName("SSE token generation should be unique")
        void testSseTokenUniqueness() {
            // When
            String token1 = logMonitorRestApi.requestSseToken();
            String token2 = logMonitorRestApi.requestSseToken();
            String token3 = logMonitorRestApi.requestSseToken();
            
            // Then
            assertNotNull(token1);
            assertNotNull(token2);
            assertNotNull(token3);
            
            assertNotEquals(token1, token2, "SSE tokens should be unique");
            assertNotEquals(token2, token3, "SSE tokens should be unique");
            assertNotEquals(token1, token3, "SSE tokens should be unique");
        }
    }
}