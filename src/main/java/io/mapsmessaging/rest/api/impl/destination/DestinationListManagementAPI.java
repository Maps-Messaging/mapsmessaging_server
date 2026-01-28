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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Destination Management")
@Path(URI_PATH+"/server/destination/list")
public class DestinationListManagementAPI extends BaseDestinationApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve a paginated list of destinations/folders at the specified namespace",
      description = "Fetch a paginated list of all known destinations. You can filter the list using a selector string, limit the number of returned entries using the 'size' parameter, and sort the results by attributes such as Name, Published Messages, or Stored Messages. Cached results are returned if available to enhance performance. Authentication is required if the server configuration mandates it.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get page of destinations was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DestinationPageResponseDTO.class))
          ),
          @ApiResponse(responseCode = "304", description = "No change detected"),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DestinationPageResponseDTO getDestinationPage(
      @BeanParam DestinationPageRequestDTO request,
      @HeaderParam("If-None-Match") String ifNoneMatch
  ) throws LoginException, IOException {
    hasAccess(RESOURCE);

    String prefix = request.getPrefix();
    if(prefix == null) prefix="";
    int pageSize = request.getPageSize();
    if(pageSize <= 10) pageSize = 10;
    if(pageSize > 1000) pageSize = 1000;

    CacheKey key = new CacheKey(uriInfo.getPath(), request.toString());
    // Try to retrieve from cache
    DestinationPageResponseDTO cachedResponse = getFromCache(key, DestinationPageResponseDTO.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    HttpSession httpSession = getSession();

    NamespaceTree destinationTree = (NamespaceTree) httpSession.getAttribute("DestinationTree");
    if(destinationTree == null) {
      Session session = getAuthenticatedSession();
      destinationTree  = NamespaceTree.buildFromPaths(session.getAllDestinationNames());
      httpSession.setAttribute("DestinationTree", destinationTree);
    }
    NamespaceNode node = destinationTree.findNode(prefix);
    if(node == null){
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return null;
    }
    String etag = computeETag(node, prefix);
    if (etag.equals(ifNoneMatch)) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      response.setHeader("ETag", etag);
      return null;
    }

    DestinationPageResponseDTO dto = new  DestinationPageResponseDTO();
    dto.setTotalEntries(node.getEntryCount());
    List<Entry> page =  node.pageEntries(request.getPageNumber(), pageSize);
    dto.setEntries(page.toArray(new Entry[0]));
    dto.setTotalPages((node.getEntryCount() / pageSize)+1);
    putToCache(key, dto);
    response.setHeader("ETag", etag);
    return dto;
  }

  private String computeETag(NamespaceNode node, String prefix) {
    return "\"" + prefix + ":" + node.getLastUpdateMillis() + ":" + node.getEntryCount() + "\"";
  }
}
