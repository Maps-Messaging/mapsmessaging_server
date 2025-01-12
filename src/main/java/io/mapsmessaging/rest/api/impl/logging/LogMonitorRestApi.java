/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.rest.api.impl.logging;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.LogEntry;
import io.mapsmessaging.logging.LogEntryListener;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@Tag(name = "Logging Monitor")
@Path(URI_PATH)
public class LogMonitorRestApi extends BaseRestApi {
  private static final String RESOURCE = "logging";

  @Context
  private Sse sse;

  private final Map<SseEventSink, LogEntryListener> activeSinks = new ConcurrentHashMap<>();


  @GET
  @Path("/server/log")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get last stored log entries",
      description = "Retrieve the last configured number of log entries from the server"
  )
  public List<LogEntry> getLogEntries(@QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    List<LogEntry> initialLogs = MessageDaemon.getInstance().getLogMonitor().getLogHistory();
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;

    return initialLogs.stream()
        .filter(logEntry -> parser == null || parser.evaluate(logEntry))
        .collect(Collectors.toList());
  }


  @GET
  @Path("/server/log/sse")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @Operation(
      summary = "Stream live log entries",
      description = "Subscribe to dynamic log events using Server-Sent Events"
  )
  public void streamLogs(
      @Context SseEventSink eventSink,
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;

    List<LogEntry> initialLogs = MessageDaemon.getInstance().getLogMonitor().getLogHistory();

    // Send the initial log history
    try {
      for (LogEntry logEntry : initialLogs) {
        if (eventSink.isClosed()) {
          return; // Stop if the sink is closed
        }
        if(parser != null && !parser.evaluate(logEntry)) {
          continue;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(logEntry);

        OutboundSseEvent event = sse.newEventBuilder()
            .name("logEvent")
            .data(String.class, json)
            .build();
        eventSink.send(event);
      }
    } catch (Exception e) {
      e.printStackTrace();
      eventSink.close();
      return;
    }

    // Register a new listener
    LogEntryListener listener = new RestLogListener(eventSink, parser);

    // Add the listener to LogMonitor
    MessageDaemon.getInstance().getLogMonitor().registerListener(listener);

    // Track the active sink
    activeSinks.put(eventSink, listener);
  }

  @AllArgsConstructor
  private class RestLogListener implements LogEntryListener {

    private final SseEventSink eventSink;
    private ParserExecutor parser;


    @Override
    public void receive(LogEntry logEntry) {
      if (!eventSink.isClosed()) { // Check if the sink is still open
        try {
          if(parser != null && !parser.evaluate(logEntry)) {
            return;
          }
          Gson gson = new GsonBuilder().setPrettyPrinting().create();
          String json = gson.toJson(logEntry);
          OutboundSseEvent event = sse.newEventBuilder()
              .name("logEvent")
              .data(String.class, json)
              .build();
          eventSink.send(event);
        } catch (Exception e) {
          // Handle any send errors
          activeSinks.remove(eventSink);
          MessageDaemon.getInstance().getLogMonitor().unregisterListener(this);
          eventSink.close();
        }
      } else {
        // Clean up if sink is closed
        activeSinks.remove(eventSink);
        MessageDaemon.getInstance().getLogMonitor().unregisterListener(this);
      }

    }
  }
}
