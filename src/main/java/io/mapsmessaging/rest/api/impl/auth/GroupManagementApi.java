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

package io.mapsmessaging.rest.api.impl.auth;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.dto.rest.auth.GroupDTO;
import io.mapsmessaging.dto.rest.auth.UserDTO;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH+"/auth/groups")
public class GroupManagementApi extends BaseAuthRestApi {

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all groups",
      description = "Retrieves all currently known groups. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all groups was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDTO[].class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public GroupDTO[] getAllGroups(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type = "String", example = "name = 'admin'")
      )
      @QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? "" + filter.hashCode() : "");
    GroupDTO[] cachedResponse = getFromCache(key, GroupDTO[].class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch groups
    AuthManager authManager = AuthManager.getInstance();
    List<GroupDetails> groups = authManager.getGroups();

    // Transform and filter groups
    GroupDTO[] results =
        groups.stream()
            .map(groupDetails -> createGroupDto(groupDetails))
            .filter(group -> filterGroup(parser, group))
            .toList().toArray(new GroupDTO[0]);
    putToCache(key, results);
    return results;
  }

  @GET
  @Path("/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get group by UUID",
      description = "Retrieve the group using the UUID of the specific group. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get groupby id was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public GroupDTO getGroupById(@PathParam("groupUuid") String groupUuid) {
    hasAccess(RESOURCE);

    // Fetch the group by UUID
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);

    if (groupDetails != null) {
      return createGroupDto(groupDetails);
    }

    // Return a 404 if the group is not found
    throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
  }

  @POST
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add new group",
      description = "Adds a new group to the group list. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Add group was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public StatusResponse addGroup(String groupName) throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    if (authManager.getGroupIdentity(groupName) == null) {
      authManager.addGroup(groupName);
      response.setStatus(HttpServletResponse.SC_CREATED);
      return new StatusResponse("Successfully added group " + groupName);
    }
    response.setStatus(HttpServletResponse.SC_CONFLICT);
    return new StatusResponse("Group " + groupName + " already exists");
  }

  @POST
  @Path("/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add user to group",
      description = "Adds a user to a group using the UUID of the user and UUID of the group . Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Add group to user was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public StatusResponse addUserToGroup(
      @PathParam("groupUuid") String groupUuid, @PathParam("userUuid") String userUuid)
      throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);
    if (groupDetails == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return new StatusResponse("Group " + groupUuid + " does not exist");
    }

    UserDetails userDetails =
        authManager.getUsers().stream()
            .filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid))
            .findFirst()
            .orElse(null);
    if (userDetails == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return new StatusResponse("User " + userUuid + " does not exist");
    }
    authManager.addUserToGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    return new StatusResponse("Successfully added user " + userDetails.getIdentityEntry().getUsername() + " to group " + groupDetails.getName());
  }

  @DELETE
  @Path("/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Removes a user from group",
      description = "Removes a user from a group using the users UUID and the groups UUID . Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Remove user from group was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public StatusResponse removeUserFromGroup(
      @PathParam("groupUuid") String groupUuid, @PathParam("userUuid") String userUuid)
      throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);
    if (groupDetails == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return new StatusResponse("Group " + groupUuid + " does not exist");
    }
    UserDetails userDetails =
        authManager.getUsers().stream()
            .filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid))
            .findFirst()
            .orElse(null);
    if (userDetails == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return new StatusResponse("User " + userUuid + " does not exist");
    }
    authManager.removeUserFromGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    return new StatusResponse("User " + userDetails.getIdentityEntry().getUsername() + " removed from group " + groupDetails.getName());
  }

  @DELETE
  @Path("/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete a group",
      description = "Deletes a group from the list and removes all user memberships. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Delete group was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public StatusResponse deleteGroup(@PathParam("groupUuid") String groupUuid) throws IOException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails =
        authManager.getGroups().stream()
            .filter(g -> g.getGroupId().toString().equals(groupUuid))
            .findFirst()
            .orElse(null);
    if (groupDetails != null) {
      authManager.delGroup(groupDetails.getName());
      return new StatusResponse("Successfully deleted group " + groupDetails.getName());
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new StatusResponse("Failed to delete group " + groupDetails.getName());
  }

  // Helper methods
  private boolean filterGroup(ParserExecutor parser, GroupDTO group) {
    return parser == null || parser.evaluate(group);
  }


  private GroupDTO createGroupDto(GroupDetails groupDetails){
    List<UserDTO> userList = new ArrayList<>();
    for(UUID userId : groupDetails.getUsers()){
      Identity identity = AuthManager.getInstance().getUserIdentity(userId);
      UserDTO user = new UserDTO(identity.getUsername(), identity.getId(), null, null);
      userList.add(user);
    }
    return new GroupDTO(groupDetails.getName(), groupDetails.getGroupId(),userList.toArray(new UserDTO[0]));
  }
}
