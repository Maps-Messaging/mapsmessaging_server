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

package io.mapsmessaging.rest.api.impl.session;

import io.mapsmessaging.dto.rest.endpoint.EndPointDetailsDTO;
import io.mapsmessaging.dto.rest.endpoint.EndPointSummaryDTO;
import io.mapsmessaging.rest.api.impl.destination.BaseDestinationApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.EndPointDetailResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.handler.SessionTracker;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Session Management")
@Path(URI_PATH)
public class SessionManagementApi extends BaseDestinationApi {

  @GET
  @Path("/session")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all active sessions",
      description = "Retrieve a list of all active REST sessions. Can be filtered with the optional filter string. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all sessions was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointDetailResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public EndPointDetailResponse getAllSessions(
      @Parameter(
          description = "Optional filter string for selecting specific sessions",
          schema = @Schema(type = "String", example = "user = 'admin' AND connectedTimeMs > 60000")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    EndPointDetailResponse cachedResponse = getFromCache(key, EndPointDetailResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<EndPointSummaryDTO> sessions = SessionTracker.getConnections();
    
    List<EndPointSummaryDTO> filteredSessions = sessions.stream()
        .filter(session -> parser == null || parser.evaluate(session))
        .collect(Collectors.toList());

    EndPointDetailResponse response = new EndPointDetailResponse(filteredSessions);
    putToCache(key, response);
    return response;
  }

  @GET
  @Path("/session/{sessionId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get session details",
      description = "Retrieve detailed information about a specific session. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get session details was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = EndPointDetailsDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Session not found"),
      }
  )
  public EndPointDetailsDTO getSessionDetails(
      @Parameter(
          description = "The unique identifier of the session",
          required = true,
          schema = @Schema(type = "long", example = "12345")
      )
      @PathParam("sessionId") long sessionId
  ) {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "" + sessionId);
    EndPointDetailsDTO cachedResponse = getFromCache(key, EndPointDetailsDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    EndPointDetailsDTO session = SessionTracker.getConnection(sessionId);
    if (session == null) {
      throw new WebApplicationException("Session not found", Response.Status.NOT_FOUND);
    }

    putToCache(key, session);
    return session;
  }

  @DELETE
  @Path("/session/{sessionId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Terminate a session",
      description = "Terminate the specified session and clean up associated resources. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Session termination was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Session not found"),
      }
  )
  public StatusResponse terminateSession(
      @Parameter(
          description = "The unique identifier of the session to terminate",
          required = true,
          schema = @Schema(type = "long", example = "12345")
      )
      @PathParam("sessionId") long sessionId
  ) {
    hasAccess(RESOURCE);
    
    // Find the session by ID
    List<EndPointSummaryDTO> sessions = SessionTracker.getConnections();
    HttpSession sessionToTerminate = null;
    
    for (EndPointSummaryDTO sessionSummary : sessions) {
      if (sessionSummary.getId() == sessionId) {
        // Find the actual HttpSession
        for (HttpSession session : SessionTracker.getSessions()) {
          Object connectionId = session.getAttribute("connectionId");
          if (connectionId != null && connectionId.equals(sessionId)) {
            sessionToTerminate = session;
            break;
          }
        }
        break;
      }
    }

    if (sessionToTerminate == null) {
      throw new WebApplicationException("Session not found", Response.Status.NOT_FOUND);
    }

    try {
      // Invalidate the session
      sessionToTerminate.invalidate();
      
      // Clear from cache
      CacheKey key = new CacheKey(uriInfo.getPath(), "" + sessionId);
      invalidateCache(key);
      
      return new StatusResponse("Session terminated successfully");
    } catch (Exception e) {
      throw new WebApplicationException("Failed to terminate session: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("/session/terminateAll")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Terminate all sessions except current",
      description = "Terminate all active sessions except the current session. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "All sessions terminated successfully",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse terminateAllSessions(
      @Parameter(
          description = "Whether to include the current session in termination",
          schema = @Schema(type = "boolean", example = "false", defaultValue = "false")
      )
      @QueryParam("includeCurrent") @DefaultValue("false") boolean includeCurrent
  ) {
    hasAccess(RESOURCE);
    
    try {
      List<EndPointSummaryDTO> sessions = SessionTracker.getConnections();
      int terminatedCount = 0;
      
      for (EndPointSummaryDTO sessionSummary : sessions) {
        // Skip current session unless explicitly included
        if (!includeCurrent && isCurrentSession(sessionSummary.getId())) {
          continue;
        }
        
        // Find and terminate the session
        for (HttpSession session : SessionTracker.getSessions()) {
          Object connectionId = session.getAttribute("connectionId");
          if (connectionId != null && connectionId.equals(sessionSummary.getId())) {
            session.invalidate();
            terminatedCount++;
            break;
          }
        }
      }
      
      // Clear session cache
      CacheKey key = new CacheKey(uriInfo.getPath(), "");
      invalidateCache(key);
      
      return new StatusResponse("Terminated " + terminatedCount + " sessions successfully");
    } catch (Exception e) {
      throw new WebApplicationException("Failed to terminate sessions: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isCurrentSession(long sessionId) {
    // Check if this is the current session by comparing with the request's session
    try {
      HttpSession currentSession = request.getSession(false);
      if (currentSession != null) {
        Object currentConnectionId = currentSession.getAttribute("connectionId");
        return currentConnectionId != null && currentConnectionId.equals(sessionId);
      }
    } catch (Exception e) {
      // Ignore exceptions, return false
    }
    return false;
  }
}