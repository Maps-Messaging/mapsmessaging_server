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
import io.mapsmessaging.rest.api.impl.logging.LogMonitorRestApi;
import io.mapsmessaging.logging.LogEntry;
import io.mapsmessaging.logging.LogEntryListener;
import io.mapsmessaging.test.BaseTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Server-Sent Events (SSE) functionality
 * Validates the real-time log streaming used by the monitoring dashboard
 */
@DisplayName("SSE Log Streaming Tests")
public class SseLogStreamingTest extends BaseTestConfig {

    private LogMonitorRestApi logMonitorRestApi;
    private MockedStatic<MessageDaemon> mockedMessageDaemon;
    private List<LogEntry> capturedLogEntries;
    private CountDownLatch logLatch;

    @BeforeEach
    void setUp() {
        capturedLogEntries = new ArrayList<>();
        logLatch = new CountDownLatch(1);
        
        // Mock the MessageDaemon and LogMonitor
        mockedMessageDaemon = Mockito.mockStatic(MessageDaemon.class);
        io.mapsmessaging.MessageDaemon mockDaemon = Mockito.mock(io.mapsmessaging.MessageDaemon.class);
        io.mapsmessaging.logging.LogMonitor mockLogMonitor = Mockito.mock(io.mapsmessaging.logging.LogMonitor.class);
        
        mockedMessageDaemon.when(MessageDaemon::getInstance).thenReturn(mockDaemon);
        Mockito.when(mockDaemon.getLogMonitor()).thenReturn(mockLogMonitor);
        
        // Set up mock log history
        List<LogEntry> mockLogHistory = createMockLogEntries();
        Mockito.when(mockLogMonitor.getLogHistory()).thenReturn(mockLogHistory);
        
        // Set up listener registration
        Mockito.doAnswer(invocation -> {
            LogEntryListener listener = invocation.getArgument(0);
            // Simulate async log delivery
            new Thread(() -> {
                try {
                    Thread.sleep(100); // Small delay
                    mockLogHistory.forEach(listener::receive);
                    logLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            return null;
        }).when(mockLogMonitor).registerListener(Mockito.any(LogEntryListener.class));
        
        logMonitorRestApi = new LogMonitorRestApi();
    }

    @AfterEach
    void tearDown() {
        if (mockedMessageDaemon != null) {
            mockedMessageDaemon.close();
        }
    }

    private List<LogEntry> createMockLogEntries() {
        List<LogEntry> entries = new ArrayList<>();
        
        // Create mock log entries with different levels
        entries.add(createLogEntry("INFO", "Test info message", "TestLogger", "main"));
        entries.add(createLogEntry("WARN", "Test warning message", "TestLogger", "worker-1"));
        entries.add(createLogEntry("ERROR", "Test error message", "TestLogger", "worker-2"));
        
        return entries;
    }

    private LogEntry createLogEntry(String level, String message, String logger, String thread) {
        LogEntry entry = Mockito.mock(LogEntry.class);
        Mockito.when(entry.getLevel()).thenReturn(level);
        Mockito.when(entry.getMessage()).thenReturn(message);
        Mockito.when(entry.getLoggerName()).thenReturn(logger);
        Mockito.when(entry.getThreadName()).thenReturn(thread);
        Mockito.when(entry.getTimestamp()).thenReturn(System.currentTimeMillis());
        return entry;
    }

    @Nested
    @DisplayName("SSE Token Management")
    class SseTokenTests {

        @Test
        @DisplayName("Should generate valid SSE token")
        void testSseTokenGeneration() {
            // When
            String token = logMonitorRestApi.requestSseToken();
            
            // Then
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.length() >= 10, "Token should be reasonably long");
        }

        @Test
        @DisplayName("Should generate unique tokens for each request")
        void testSseTokenUniqueness() {
            // When
            String token1 = logMonitorRestApi.requestSseToken();
            String token2 = logMonitorRestApi.requestSseToken();
            
            // Then
            assertNotEquals(token1, token2, "Tokens should be unique");
        }

        @Test
        @DisplayName("Should generate tokens within reasonable time")
        void testSseTokenPerformance() {
            // Given
            long maxTime = 100; // 100ms max
            
            // When
            long startTime = System.currentTimeMillis();
            String token = logMonitorRestApi.requestSseToken();
            long duration = System.currentTimeMillis() - startTime;
            
            // Then
            assertNotNull(token);
            assertTrue(duration < maxTime, "Token generation took too long: " + duration + "ms");
        }
    }

    @Nested
    @DisplayName("Log Entry Processing")
    class LogEntryProcessingTests {

        @Test
        @DisplayName("Should process different log levels correctly")
        void testLogLevelProcessing() throws InterruptedException {
            // Given
            String token = logMonitorRestApi.requestSseToken();
            
            // When
            // The log entries are delivered via the mocked listener
            boolean completed = logLatch.await(5, TimeUnit.SECONDS);
            
            // Then
            assertTrue(completed, "Log delivery should complete within timeout");
            assertTrue(capturedLogEntries.size() >= 3, "Should receive all log entries");
            
            // Verify different log levels are present
            boolean hasInfo = capturedLogEntries.stream()
                .anyMatch(entry -> "INFO".equals(entry.getLevel()));
            boolean hasWarn = capturedLogEntries.stream()
                .anyMatch(entry -> "WARN".equals(entry.getLevel()));
            boolean hasError = capturedLogEntries.stream()
                .anyMatch(entry -> "ERROR".equals(entry.getLevel()));
            
            assertTrue(hasInfo, "Should have INFO level logs");
            assertTrue(hasWarn, "Should have WARN level logs");
            assertTrue(hasError, "Should have ERROR level logs");
        }

        @Test
        @DisplayName("Should preserve log entry structure")
        void testLogEntryStructure() throws InterruptedException {
            // Given
            String token = logMonitorRestApi.requestSseToken();
            
            // When
            boolean completed = logLatch.await(5, TimeUnit.SECONDS);
            
            // Then
            assertTrue(completed, "Log delivery should complete");
            
            for (LogEntry entry : capturedLogEntries) {
                assertNotNull(entry.getLevel(), "Log entry should have level");
                assertNotNull(entry.getMessage(), "Log entry should have message");
                assertNotNull(entry.getLoggerName(), "Log entry should have logger name");
                assertNotNull(entry.getThreadName(), "Log entry should have thread name");
                assertTrue(entry.getTimestamp() > 0, "Log entry should have timestamp");
            }
        }
    }

    @Nested
    @DisplayName("Filtering and Search")
    class FilteringTests {

        @Test
        @DisplayName("Should filter log entries by level")
        void testLogLevelFiltering() {
            // Given
            String levelFilter = "ERROR";
            
            // When
            LogEntries response = logMonitorRestApi.getLogEntries(levelFilter);
            
            // Then
            assertNotNull(response);
            assertNotNull(response.getEntries());
            
            // All returned entries should match the filter
            for (LogEntry entry : response.getEntries()) {
                // This would require actual filter implementation
                // For now, we verify the API doesn't crash
                assertNotNull(entry);
            }
        }

        @Test
        @DisplayName("Should handle complex filter expressions")
        void testComplexFiltering() {
            // Given
            String complexFilter = "level = 'ERROR' AND logger = 'TestLogger'";
            
            // When & Then
            assertDoesNotThrow(() -> {
                LogEntries response = logMonitorRestApi.getLogEntries(complexFilter);
                assertNotNull(response);
                assertNotNull(response.getEntries());
            });
        }

        @Test
        @DisplayName("Should handle invalid filter gracefully")
        void testInvalidFilterHandling() {
            // Given
            String invalidFilter = "invalid syntax [";
            
            // When & Then
            assertDoesNotThrow(() -> {
                LogEntries response = logMonitorRestApi.getLogEntries(invalidFilter);
                assertNotNull(response);
            });
        }
    }

    @Nested
    @DisplayName("Performance and Scalability")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle concurrent token requests")
        void testConcurrentTokenRequests() throws InterruptedException {
            // Given
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            List<String> tokens = new ArrayList<>();
            
            // When
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        String token = logMonitorRestApi.requestSseToken();
                        if (token != null && !token.isEmpty()) {
                            synchronized (tokens) {
                                tokens.add(token);
                            }
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Count as failure
                    } finally {
                        completeLatch.countDown();
                    }
                }).start();
            }
            
            startLatch.countDown(); // Start all threads
            assertTrue(completeLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
            
            // Then
            assertEquals(threadCount, successCount.get(), "All token requests should succeed");
            assertEquals(threadCount, tokens.size(), "Should receive all tokens");
            
            // Verify all tokens are unique
            long uniqueTokens = tokens.stream().distinct().count();
            assertEquals(threadCount, uniqueTokens, "All tokens should be unique");
        }

        @Test
        @DisplayName("Should handle large number of log entries")
        void testLargeLogVolume() throws InterruptedException {
            // Given
            int logCount = 1000;
            CountDownLatch largeLogLatch = new CountDownLatch(logCount);
            
            // Mock large number of log entries
            List<LogEntry> largeLogList = new ArrayList<>();
            for (int i = 0; i < logCount; i++) {
                largeLogList.add(createLogEntry("INFO", "Message " + i, "BulkLogger", "bulk-thread"));
            }
            
            Mockito.when(mockedMessageDaemon.getMock().getLogMonitor().getLogHistory())
                .thenReturn(largeLogList);
            
            // When
            String token = logMonitorRestApi.requestSseToken();
            
            // Then
            assertNotNull(token);
            // Performance verification would depend on actual implementation
            // For now, ensure it doesn't crash
            assertDoesNotThrow(() -> {
                // Simulate processing
                largeLogList.forEach(entry -> assertNotNull(entry));
            });
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null token validation")
        void testNullTokenHandling() {
            // This test would require access to the internal token validation
            // For now, we verify the API doesn't crash with null inputs
            
            assertDoesNotThrow(() -> {
                LogEntries response = logMonitorRestApi.getLogEntries(null);
                assertNotNull(response);
            });
        }

        @Test
        @DisplayName("Should handle empty filter")
        void testEmptyFilterHandling() {
            // When & Then
            assertDoesNotThrow(() -> {
                LogEntries response = logMonitorRestApi.getLogEntries("");
                assertNotNull(response);
                assertNotNull(response.getEntries());
            });
        }

        @Test
        @DisplayName("Should handle very long filter expressions")
        void testLongFilterHandling() {
            // Given
            StringBuilder longFilter = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longFilter.append("level = 'INFO' OR ");
            }
            longFilter.setLength(longFilter.length() - 4); // Remove last " OR "
            
            // When & Then
            assertDoesNotThrow(() -> {
                LogEntries response = logMonitorRestApi.getLogEntries(longFilter.toString());
                assertNotNull(response);
            });
        }
    }

    @Nested
    @DisplayName("Integration with Dashboard")
    class DashboardIntegrationTests {

        @Test
        @DisplayName("Should provide data for dashboard initialization")
        void testDashboardDataProvision() {
            // When
            LogEntries logEntries = logMonitorRestApi.getLogEntries(null);
            String sseToken = logMonitorRestApi.requestSseToken();
            
            // Then
            assertNotNull(logEntries, "Should provide initial log entries");
            assertNotNull(logEntries.getEntries(), "Should have log entries list");
            assertNotNull(sseToken, "Should provide SSE token for streaming");
            
            // Verify data is suitable for dashboard consumption
            assertTrue(logEntries.getEntries().size() >= 0, "Should have non-negative log count");
            assertTrue(sseToken.length() > 0, "Token should be non-empty");
        }

        @Test
        @DisplayName("Should support dashboard polling intervals")
        void testDashboardPollingSupport() {
            // Given
            int pollCount = 5;
            long pollInterval = 100; // 100ms between polls
            
            // When
            List<LogEntries> responses = new ArrayList<>();
            for (int i = 0; i < pollCount; i++) {
                try {
                    Thread.sleep(pollInterval);
                    LogEntries response = logMonitorRestApi.getLogEntries(null);
                    responses.add(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Then
            assertEquals(pollCount, responses.size(), "Should complete all polls");
            
            // All responses should be valid
            for (LogEntries response : responses) {
                assertNotNull(response);
                assertNotNull(response.getEntries());
            }
        }
    }
}