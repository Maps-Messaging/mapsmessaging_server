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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH + "/auth/groups")
public class GroupManagementApi extends BaseAuthRestApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all groups",
      description = "Retrieves all currently known groups. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all groups was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDTO[].class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public Response getAllGroups(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "name = 'admin'")
      )
      @QueryParam("filter") String filter
  ) {

    hasAccess(RESOURCE);

    ParserExecutor parserExecutor;
    try {
      parserExecutor = (filter != null && !filter.isBlank()) ? SelectorParser.compile(filter) : null;
    } catch (ParseException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid filter expression"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isBlank()) ? Integer.toString(filter.hashCode()) : "");
    GroupDTO[] cachedResponse = getFromCache(key, GroupDTO[].class);
    if (cachedResponse != null) {
      return Response.ok(cachedResponse).type(MediaType.APPLICATION_JSON).build();
    }

    AuthManager authManager = AuthManager.getInstance();
    List<GroupDetails> groups = authManager.getGroups();

    GroupDTO[] results = groups.stream()
        .map(this::createGroupDto)
        .filter(groupDto -> parserExecutor == null || parserExecutor.evaluate(groupDto))
        .toList()
        .toArray(new GroupDTO[0]);

    putToCache(key, results);
    return Response.ok(results).type(MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/{groupUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get group by UUID",
      description = "Retrieve the group using the UUID of the specific group. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get group by id was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "404",
              description = "Group not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response getGroupById(
      @Parameter(
          required = true,
          description = "Group unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("groupUuid") String groupUuid) {

    hasAccess(RESOURCE);

    UUID uuid;
    try {
      uuid = UUID.fromString(groupUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream()
        .filter(group -> group.getGroupId().equals(uuid))
        .findFirst()
        .orElse(null);

    if (groupDetails == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Group not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    return Response.ok(createGroupDto(groupDetails)).type(MediaType.APPLICATION_JSON).build();
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add new group",
      description = "Adds a new group to the group list. Requires authentication if enabled in the configuration.",
      requestBody = @RequestBody(
          required = true,
          content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "it_group_1700000000"))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "Group created",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "409",
              description = "Group already exists",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Group creation failed",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response addGroup(String groupName) {

    hasAccess(RESOURCE);

    if (groupName == null || groupName.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Group name is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();
    if (authManager.getGroupIdentity(groupName) != null) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new StatusResponse("Group " + groupName + " already exists"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      authManager.addGroup(groupName);
      removeUriFromCache(URI_PATH + "/auth/groups");
    } catch (IOException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Group creation failed"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    return Response.status(Response.Status.CREATED)
        .entity(new StatusResponse("Successfully added group " + groupName))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @POST
  @Path("/{groupUuid}/{userUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add user to group",
      description = "Adds a user to a group using the UUID of the user and UUID of the group. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Add user to group was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "404",
              description = "User or group not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Group membership update failed",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response addUserToGroup(
      @Parameter(
          required = true,
          description = "Group unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("groupUuid") String groupUuid,
      @Parameter(
          required = true,
          description = "User unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("userUuid") String userUuid
  ) {

    hasAccess(RESOURCE);

    UUID userId;
    UUID groupId;
    try {
      userId = UUID.fromString(userUuid);
      groupId = UUID.fromString(groupUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();

    GroupDetails groupDetails = authManager.getGroups().stream()
        .filter(group -> group.getGroupId().equals(groupId))
        .findFirst()
        .orElse(null);

    if (groupDetails == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Group not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    UserDetails userDetails = authManager.getUsers().stream()
        .filter(user -> user.getIdentityEntry().getId().equals(userId))
        .findFirst()
        .orElse(null);

    if (userDetails == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("User not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      authManager.addUserToGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    } catch (IOException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Group membership update failed"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    removeUriFromCache(URI_PATH + "/auth/groups");
    return Response.ok(new StatusResponse(
            "Successfully added user " + userDetails.getIdentityEntry().getUsername() + " to group " + groupDetails.getName()
        ))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @DELETE
  @Path("/{groupUuid}/{userUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Removes a user from group",
      description = "Removes a user from a group using the users UUID and the groups UUID. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Remove user from group was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "404",
              description = "User or group not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Group membership update failed",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response removeUserFromGroup(
      @Parameter(
          required = true,
          description = "Group unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("groupUuid") String groupUuid,
      @Parameter(
          required = true,
          description = "User unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("userUuid") String userUuid
  ) {

    hasAccess(RESOURCE);

    UUID userId;
    UUID groupId;
    try {
      userId = UUID.fromString(userUuid);
      groupId = UUID.fromString(groupUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();

    GroupDetails groupDetails = authManager.getGroups().stream()
        .filter(group -> group.getGroupId().equals(groupId))
        .findFirst()
        .orElse(null);

    if (groupDetails == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Group not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    UserDetails userDetails = authManager.getUsers().stream()
        .filter(user -> user.getIdentityEntry().getId().equals(userId))
        .findFirst()
        .orElse(null);

    if (userDetails == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("User not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      authManager.removeUserFromGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    } catch (IOException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Group membership update failed"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    removeUriFromCache(URI_PATH + "/auth/groups");

    return Response.ok(new StatusResponse(
            "User " + userDetails.getIdentityEntry().getUsername() + " removed from group " + groupDetails.getName()
        ))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @DELETE
  @Path("/{groupUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a group",
      description = "Deletes a group from the list and removes all user memberships. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Group deleted"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(
              responseCode = "404",
              description = "Group not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Group deletion failed",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response deleteGroup(
      @Parameter(
          required = true,
          description = "Group unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("groupUuid") String groupUuid) {

    hasAccess(RESOURCE);

    UUID groupId;
    try {
      groupId = UUID.fromString(groupUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream()
        .filter(group -> group.getGroupId().equals(groupId))
        .findFirst()
        .orElse(null);

    if (groupDetails == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("Group not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      authManager.delGroup(groupDetails.getName());
    } catch (IOException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Group deletion failed"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    removeUriFromCache(URI_PATH + "/auth/groups");

    return Response.noContent().build();
  }

  private GroupDTO createGroupDto(GroupDetails groupDetails) {
    List<UserDTO> userList = new ArrayList<>();
    for (UUID userId : groupDetails.getUsers()) {
      Identity identity = AuthManager.getInstance().getUserIdentity(userId);
      if (identity != null) {
        userList.add(new UserDTO(identity.getUsername(), identity.getId(), null, null));
      }
    }
    return new GroupDTO(groupDetails.getName(), groupDetails.getGroupId(), userList.toArray(new UserDTO[0]));
  }
}
