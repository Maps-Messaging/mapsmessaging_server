/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class LogMonitorTest {

  private final LoggerContext loggerContext = new LoggerContext();

  @AfterEach
  void stopLoggerContext() {
    loggerContext.stop();
  }

  @Test
  void append_formatsEventAndNotifiesRegisteredListener() {
    LogMonitor logMonitor = new LogMonitor();
    List<LogEntry> receivedEntries = new ArrayList<>();
    long timestamp = 1_700_000_000_000L;
    LoggingEvent loggingEvent = createEvent(Level.WARN, "sensor {} is {}", new Object[]{"alpha", "offline"}, timestamp);
    String formattedTimestamp = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    logMonitor.registerListener(receivedEntries::add);
    logMonitor.append(loggingEvent);

    List<LogEntry> history = logMonitor.getLogHistory();
    Assertions.assertEquals(1, history.size());
    Assertions.assertEquals(history, receivedEntries);
    LogEntry logEntry = history.get(0);
    Assertions.assertTrue(logEntry.getLogNumber() > 0);
    Assertions.assertEquals(Level.WARN_INT, logEntry.getLevel());
    Assertions.assertEquals(
        "[" + formattedTimestamp + "] WARN  sensor alpha is offline (test.logger)",
        logEntry.getMessage()
    );
  }

  @Test
  void unregisterListener_preventsFurtherNotifications() {
    LogMonitor logMonitor = new LogMonitor();
    List<LogEntry> receivedEntries = new ArrayList<>();
    LogEntryListener listener = receivedEntries::add;
    logMonitor.registerListener(listener);

    logMonitor.append(createEvent(Level.INFO, "first", null, 1L));
    logMonitor.unregisterListener(listener);
    logMonitor.append(createEvent(Level.INFO, "second", null, 2L));

    Assertions.assertEquals(1, receivedEntries.size());
    Assertions.assertTrue(receivedEntries.get(0).getMessage().contains("first"));
    Assertions.assertEquals(2, logMonitor.getLogHistory().size());
  }

  @Test
  void getLogHistory_retainsLatestTwentyEntriesAndReturnsSnapshot() {
    LogMonitor logMonitor = new LogMonitor();
    for (int eventNumber = 1; eventNumber <= 25; eventNumber++) {
      logMonitor.append(createEvent(Level.DEBUG, "event " + eventNumber, null, eventNumber));
    }

    List<LogEntry> history = logMonitor.getLogHistory();
    Assertions.assertEquals(20, history.size());
    Assertions.assertTrue(history.get(0).getMessage().contains("event 6"));
    Assertions.assertTrue(history.get(19).getMessage().contains("event 25"));

    history.clear();
    Assertions.assertEquals(20, logMonitor.getLogHistory().size());
  }

  private LoggingEvent createEvent(Level level, String message, Object[] arguments, long timestamp) {
    Logger logger = loggerContext.getLogger("test.logger");
    LoggingEvent loggingEvent = new LoggingEvent(
        LogMonitorTest.class.getName(),
        logger,
        level,
        message,
        null,
        arguments
    );
    loggingEvent.setTimeStamp(timestamp);
    return loggingEvent;
  }
}
