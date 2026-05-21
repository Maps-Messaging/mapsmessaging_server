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

package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.rest.api.impl.destination.context.Entry;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceNode;
import io.mapsmessaging.rest.api.impl.destination.context.NamespaceTree;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Destination Management")
@Path(URI_PATH + "/server/destination/list")
public class DestinationListManagementAPI extends BaseDestinationApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Retrieve a paginated list of destinations/folders at the specified namespace",
      description = "Fetch a paginated list of all known destinations. You can filter the list using a selector string, limit the number of returned entries using the 'size' parameter, and sort the results by attributes such as Name, Published Messages, or Stored Messages. Cached results are returned if available to enhance performance. Authentication is required if the server configuration mandates it.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get page of destinations was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DestinationPageResponseDTO.class))
          ),
          @ApiResponse(
              responseCode = "304",
              description = "No change detected"
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
              responseCode = "404",
              description = "Namespace not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getDestinationPage(
      @BeanParam DestinationPageRequestDTO request,
      @HeaderParam("If-None-Match") String ifNoneMatch
  ) {
    hasAccess(RESOURCE);

    if (request == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("request is required"))
          .build();
    }

    String normalizedPrefix = normalizePrefix(request.getPrefix());
    if (normalizedPrefix == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("prefix is invalid"))
          .build();
    }

    int pageNumber = request.getPageNumber();
    if (pageNumber < 0) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("pageNumber must be >= 0"))
          .build();
    }

    int pageSize = request.getPageSize();
    if (pageSize < 10) {
      pageSize = 10;
    }
    if (pageSize > 1000) {
      pageSize = 1000;
    }

    try {
      NamespaceTree destinationTree = getOrBuildDestinationTree();
      NamespaceNode namespaceNode = destinationTree.findNode(normalizedPrefix);
      if (namespaceNode == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(new StatusResponse("namespace not found"))
            .build();
      }

      String etag = computeETag(namespaceNode, normalizedPrefix);
      if (etagMatches(ifNoneMatch, etag)) {
        return Response.status(Response.Status.NOT_MODIFIED)
            .header("ETag", etag)
            .build();
      }

      CacheKey cacheKey = new CacheKey(uriInfo.getPath(), request.toString());
      DestinationPageResponseDTO cachedResponse = getFromCache(cacheKey, DestinationPageResponseDTO.class);
      if (cachedResponse != null) {
        return Response.ok(cachedResponse, MediaType.APPLICATION_JSON)
            .header("ETag", etag)
            .build();
      }

      DestinationPageResponseDTO destinationPageResponse = new DestinationPageResponseDTO();
      destinationPageResponse.setTotalEntries(namespaceNode.getEntryCount());

      List<Entry> pageEntries = namespaceNode.pageEntries(pageNumber, pageSize);
      destinationPageResponse.setEntries(pageEntries.toArray(new Entry[0]));

      destinationPageResponse.setTotalPages((namespaceNode.getEntryCount() / pageSize) + 1);
      destinationPageResponse.setPageNo(pageNumber);

      putToCache(cacheKey, destinationPageResponse);

      return Response.ok(destinationPageResponse, MediaType.APPLICATION_JSON)
          .header("ETag", etag)
          .build();
    } catch (LoginException exception) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failed to build destination list: " + exception.getMessage()))
          .build();
    } catch (IOException | RuntimeException exception) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failed to build destination list: " + exception.getMessage()))
          .build();
    }
  }

  private NamespaceTree getOrBuildDestinationTree() throws LoginException, IOException {
    jakarta.servlet.http.HttpSession httpSession = getSession();
    Object storedDestinationTree = httpSession.getAttribute("DestinationTree");
    if (storedDestinationTree instanceof NamespaceTree destinationTree) {
      return destinationTree;
    }

    Session session = getAuthenticatedSession();
    NamespaceTree destinationTree = NamespaceTree.buildFromPaths(session.getAllDestinationNames());
    httpSession.setAttribute("DestinationTree", destinationTree);
    return destinationTree;
  }

  private String normalizePrefix(String prefix) {
    if (prefix == null) {
      return "";
    }

    String trimmedPrefix = prefix.trim();
    if (trimmedPrefix.startsWith("\"")) {
      trimmedPrefix = trimmedPrefix.substring(1);
    }
    if (trimmedPrefix.endsWith("\"")) {
      trimmedPrefix = trimmedPrefix.substring(0, trimmedPrefix.length() - 1);
    }

    if (trimmedPrefix.isEmpty()) {
      return "";
    }

    try {
      String decoded = URLDecoder.decode(trimmedPrefix, StandardCharsets.UTF_8);
      return decoded.trim();
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private boolean etagMatches(String ifNoneMatch, String etag) {
    if (ifNoneMatch == null) {
      return false;
    }

    String headerValue = ifNoneMatch.trim();
    if (headerValue.equals(etag)) {
      return true;
    }

    if (headerValue.startsWith("W/")) {
      String weakTag = headerValue.substring(2).trim();
      return weakTag.equals(etag);
    }

    return false;
  }

  private String computeETag(NamespaceNode node, String prefix) {
    return "\"" + prefix + ":" + node.getLastUpdateMillis() + ":" + node.getEntryCount() + "\"";
  }
}
