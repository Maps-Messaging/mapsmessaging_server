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
 */

package io.mapsmessaging.rest.api.impl.auth;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.dto.rest.auth.GroupDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.GroupListResponse;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH)
public class GroupManagementApi extends BaseAuthRestApi {

  @GET
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all groups",
      description = "Retrieves all currently known groups. Requires authentication if enabled in the configuration.",
      operationId = "getAllGroups"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of groups returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Unexpected error")
  })
  public GroupListResponse getAllGroups(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "name = 'admin'")
      )
      @QueryParam("filter") String filter
  ) throws ParseException {
    hasAccess(RESOURCE);

    ParserExecutor parser = (filter != null && !filter.isEmpty())
        ? SelectorParser.compile(filter)
        : null;

    CacheKey key = new CacheKey(
        uriInfo.getPath(),
        (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : ""
    );
    GroupListResponse cachedResponse = getFromCache(key, GroupListResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    AuthManager authManager = AuthManager.getInstance();
    List<GroupDetails> groups = authManager.getGroups();

    List<GroupDTO> results =
        groups.stream()
            .map(group -> new GroupDTO(group.getName(), group.getGroupId(), group.getUsers()))
            .filter(g -> filterGroup(parser, g))
            .collect(Collectors.toList());

    GroupListResponse result = new GroupListResponse(request, results);
    putToCache(key, result);
    return result;
  }

  @GET
  @Path("/auth/groups/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get group by UUID",
      description = "Retrieve the group by its UUID. Requires authentication if enabled in the configuration.",
      operationId = "getGroupById"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Group returned"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Group not found"),
      @ApiResponse(responseCode = "500", description = "Unexpected error")
  })
  public GroupDTO getGroupById(@PathParam("groupUuid") String groupUuid) {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);

    if (groupDetails != null) {
      return new GroupDTO(groupDetails.getName(), groupDetails.getGroupId(), groupDetails.getUsers());
    }
    throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
  }

  @POST
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add new group",
      description = "Adds a new group to the group list. Requires authentication if enabled in the configuration.",
      operationId = "addGroup"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "New group created"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "Unexpected error")
  })
  public GroupDTO addGroup(String groupName) throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupIdMap groupIdMap = authManager.addGroup(groupName);
    return new GroupDTO(groupName, groupIdMap.getAuthId(), new ArrayList<>());
  }

  @POST
  @Path("/auth/groups/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add user to group",
      description = "Adds a user to a group. Requires authentication if enabled in the configuration.",
      operationId = "addUserToGroup"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User added to group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Group or user not found"),
      @ApiResponse(responseCode = "500", description = "Unexpected error")
  })
  public BaseResponse addUserToGroup(
      @PathParam("groupUuid") String groupUuid,
      @PathParam("userUuid") String userUuid
  ) throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);
    UserDetails userDetails =
        authManager.getUsers().stream()
            .filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid))
            .findFirst()
            .orElse(null);

    if (groupDetails != null && userDetails != null) {
      authManager.addUserToGroup(
          userDetails.getIdentityEntry().getUsername(), groupDetails.getName()
      );
    }
    return new BaseResponse();
  }

  @DELETE
  @Path("/auth/groups/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Remove user from group",
      description = "Removes a user from a group. Requires authentication if enabled in the configuration.",
      operationId = "removeUserFromGroup"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User removed from group"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Group or user not found"),
      @ApiResponse(responseCode = "500", description = "Unexpected error")
  })
  public BaseResponse removeUserFromGroup(
      @PathParam("groupUuid") String groupUuid,
      @PathParam("userUuid") String userUuid
  ) throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);
    UserDetails userDetails =
        authManager.getUsers().stream()
            .filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid))
            .findFirst()
            .orElse(null);

    if (groupDetails != null && userDetails != null) {
      authManager.removeUserFromGroup(
          userDetails.getIdentityEntry().getUsername(), groupDetails.getName()
      );
    }
    return new BaseResponse();
  }

  @DELETE
  @Path("/auth/groups/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete a group",
      description = "Deletes a group by its UUID and removes all user memberships. Requires authentication if enabled in the configuration.",
      operationId = "deleteGroup"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Group deleted"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Group not found"),
      @ApiResponse(responseCode = "500", description = "Unexpected error")
  })
  public BaseResponse deleteGroup(@PathParam("groupUuid") String groupUuid) throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);

    if (groupDetails != null) {
      authManager.delGroup(groupDetails.getName());
      return new BaseResponse();
    }
    response.setStatus(500);
    return new BaseResponse();
  }

  // Helper method
  private boolean filterGroup(ParserExecutor parser, GroupDTO group) {
    return parser == null || parser.evaluate(group);
  }
}
