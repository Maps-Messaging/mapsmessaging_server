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

package io.mapsmessaging.rest.api.impl.logging;

import com.google.gson.Gson;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.LogEntry;
import io.mapsmessaging.logging.LogEntryListener;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.LogEntries;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.token.TokenManager;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.GsonFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Logging Monitor")
@Path(URI_PATH + "/server/log")
public class LogMonitorRestApi extends BaseRestApi {

  private static final String RESOURCE = "logging";

  private final Map<SseEventSink, LogEntryListener> activeSinks = new ConcurrentHashMap<>();

  @Context
  private Sse sse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get last stored log entries",
      description = "Retrieve the last configured number of log entries from the server",
      parameters = {
          @Parameter(
              name = "filter",
              description = "Optional selector filter applied to log entries",
              required = false,
              schema = @Schema(type = "String", example = "level = 'ERROR'", minLength = 1)
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LogEntries.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getLogEntries(@QueryParam("filter") String filter) {
    hasAccess(RESOURCE);

    String normalizedFilter = normalizeOptionalFilter(filter);
    if (filter != null && normalizedFilter == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Filter must not be blank"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    List<LogEntry> initialLogs = MessageDaemon.getInstance().getLogMonitor().getLogHistory();

    ParserExecutor parserExecutor = null;
    if (normalizedFilter != null) {
      try {
        parserExecutor = SelectorParser.compile(normalizedFilter);
      } catch (Exception ex) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new StatusResponse("Invalid filter"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
    }
    ParserExecutor parserExecutor2 = parserExecutor;
    LogEntries responseBody = new LogEntries(
        initialLogs.stream()
            .filter(logEntry -> parserExecutor2 == null || parserExecutor2.evaluate(logEntry))
            .collect(Collectors.toList())
    );

    return Response.ok(responseBody, MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/sse")
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(
      summary = "Request a temporary token to access the server side logs",
      description = "Retrieve a temporary token that allows access to the server side log stream",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "String token to use to access the log SSE",
              content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response requestSseToken() {
    hasAccess(RESOURCE);

    try {
      String token = TokenManager.getInstance().generateToken(getSession());
      if (token == null || token.trim().isEmpty()) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new StatusResponse("Failed to generate token"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
      return Response.ok(token, MediaType.TEXT_PLAIN).build();
    } catch (Exception ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Failed to generate token"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  @GET
  @Path("/sse/stream/{token}")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @Operation(
      summary = "Stream live log entries",
      description = "Subscribe to dynamic log events using Server-Sent Events",
      parameters = {
          @Parameter(
              name = "filter",
              description = "Optional selector filter applied to log entries",
              required = false,
              schema = @Schema(type = "String", example = "level = 'ERROR'", minLength = 1)
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "SSE stream of LogEntry events",
              content = @Content(mediaType = "text/event-stream")
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource, or token is invalid"
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error"
          )
      }
  )
  public void streamLogs(
      @PathParam("token") String token,
      @Context SseEventSink eventSink,
      @QueryParam("filter") String filter
  ) {
    hasAccess(RESOURCE);

    if (eventSink == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (token == null || token.trim().isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      safeClose(eventSink);
      return;
    }

    if (!TokenManager.getInstance().useToken(getSession(), token)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      safeClose(eventSink);
      return;
    }

    String normalizedFilter = normalizeOptionalFilter(filter);
    if (filter != null && normalizedFilter == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      safeClose(eventSink);
      return;
    }

    ParserExecutor parserExecutor = null;
    if (normalizedFilter != null) {
      try {
        parserExecutor = SelectorParser.compile(normalizedFilter);
      } catch (Exception ex) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        safeClose(eventSink);
        return;
      }
    }

    List<LogEntry> initialLogs = MessageDaemon.getInstance().getLogMonitor().getLogHistory();

    try {
      for (LogEntry logEntry : initialLogs) {
        if (eventSink.isClosed()) {
          return;
        }
        if (parserExecutor != null && !parserExecutor.evaluate(logEntry)) {
          continue;
        }
        OutboundSseEvent event = buildLogEvent(logEntry);
        eventSink.send(event);
      }
    } catch (Exception ex) {
      safeClose(eventSink);
      return;
    }

    LogEntryListener listener = new RestLogListener(eventSink, parserExecutor, activeSinks, sse);
    MessageDaemon.getInstance().getLogMonitor().registerListener(listener);
    activeSinks.put(eventSink, listener);
  }

  private OutboundSseEvent buildLogEvent(LogEntry logEntry) {
    Gson gson = GsonFactory.getInstance().getPrettyGson();
    String json = gson.toJson(logEntry);
    return sse.newEventBuilder()
        .name("logEvent")
        .data(String.class, json)
        .build();
  }

  private void safeClose(SseEventSink eventSink) {
    try {
      eventSink.close();
    } catch (Exception ex) {
      // ignore
    }
  }

  private String normalizeOptionalFilter(String filter) {
    if (filter == null) {
      return null;
    }
    String trimmed = filter.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed;
  }
}

class RestLogListener implements LogEntryListener {

  private final SseEventSink eventSink;
  private final ParserExecutor parserExecutor;
  private final Map<SseEventSink, LogEntryListener> activeSinks;
  private final Sse sse;

  RestLogListener(
      SseEventSink eventSink,
      ParserExecutor parserExecutor,
      Map<SseEventSink, LogEntryListener> activeSinks,
      Sse sse
  ) {
    this.eventSink = eventSink;
    this.parserExecutor = parserExecutor;
    this.activeSinks = activeSinks;
    this.sse = sse;
  }

  @Override
  public void receive(LogEntry logEntry) {
    if (eventSink.isClosed()) {
      cleanup();
      return;
    }

    try {
      if (parserExecutor != null && !parserExecutor.evaluate(logEntry)) {
        return;
      }

      Gson gson = GsonFactory.getInstance().getPrettyGson();
      String json = gson.toJson(logEntry);

      OutboundSseEvent event = sse.newEventBuilder()
          .name("logEvent")
          .data(String.class, json)
          .build();

      eventSink.send(event);
    } catch (Exception ex) {
      try {
        eventSink.close();
      } catch (IOException ioException) {
        // ignore
      }
      cleanup();
    }
  }

  private void cleanup() {
    activeSinks.remove(eventSink);
    MessageDaemon.getInstance().getLogMonitor().unregisterListener(this);
  }
}
