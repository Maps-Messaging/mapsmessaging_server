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

package io.mapsmessaging.rest.api.impl.logging;

import com.google.gson.Gson;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.LogEntry;
import io.mapsmessaging.logging.LogEntryListener;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.LogEntries;
import io.mapsmessaging.rest.token.TokenManager;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.GsonFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Logging Monitor")
@Path(URI_PATH+"/server/log")
public class LogMonitorRestApi extends BaseRestApi {
  private static final String RESOURCE = "logging";
  private final Map<SseEventSink, LogEntryListener> activeSinks = new ConcurrentHashMap<>();
  @Context
  private Sse sse;

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get last stored log entries",
      description = "Retrieve the last configured number of log entries from the server",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LogEntries.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public LogEntries getLogEntries(@QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    List<LogEntry> initialLogs = MessageDaemon.getInstance().getLogMonitor().getLogHistory();
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;

    return new LogEntries(initialLogs.stream()
        .filter(logEntry -> parser == null || parser.evaluate(logEntry))
        .collect(Collectors.toList()));
  }


  @GET
  @Path("/sse")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @Operation(
      summary = "Request a temporary token to access the server side logs",
      description = "Retrieve a temporary token that allows access to the server side log stream",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "String token to use to access the log SSE",
              content = @Content(mediaType = "text")
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public String requestSseToken() {
    hasAccess(RESOURCE);
    return TokenManager.getInstance().generateToken(getSession());
  }

  @GET
  @Path("/sse/stream/{token}")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @Operation(
      summary = "Stream live log entries",
      description = "Subscribe to dynamic log events using Server-Sent Events",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "SSE stream of LogEntry events",
              content = @Content(mediaType = "text/event-stream", schema = @Schema(implementation = LogEntry.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public void streamLogs(
      @PathParam("token") String token,
      @Context SseEventSink eventSink,
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    if(!TokenManager.getInstance().useToken(getSession(), token)){
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<LogEntry> initialLogs = MessageDaemon.getInstance().getLogMonitor().getLogHistory();

    // Send the initial log history
    try {
      for (LogEntry logEntry : initialLogs) {
        if (eventSink.isClosed()) {
          return; // Stop if the sink is closed
        }
        if (parser != null && !parser.evaluate(logEntry)) {
          continue;
        }
        Gson gson = GsonFactory.getInstance().getPrettyGson();
        String json = gson.toJson(logEntry);

        OutboundSseEvent event = sse.newEventBuilder()
            .name("logEvent")
            .data(String.class, json)
            .build();
        eventSink.send(event);
      }
    } catch (Exception e) {
      try {
        eventSink.close();
      } catch (IOException ex) {
        // ignore we are in an exception
      }
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
          if (parser != null && !parser.evaluate(logEntry)) {
            return;
          }
          Gson gson = GsonFactory.getInstance().getPrettyGson();
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
          try {
            eventSink.close();
          } catch (IOException ex) {
            // ignore we are in an exception
          }
        }
      } else {
        // Clean up if sink is closed
        activeSinks.remove(eventSink);
        MessageDaemon.getInstance().getLogMonitor().unregisterListener(this);
      }

    }
  }
}
