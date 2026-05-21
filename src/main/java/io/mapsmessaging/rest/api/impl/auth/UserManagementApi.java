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
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.dto.rest.auth.ChangePasswordDTO;
import io.mapsmessaging.dto.rest.auth.GroupInfoDTO;
import io.mapsmessaging.dto.rest.auth.NewUserDTO;
import io.mapsmessaging.dto.rest.auth.UserDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.security.access.Group;
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
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH + "/auth/users")
public class UserManagementApi extends BaseAuthRestApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get all users",
      description = "Retrieves all currently known users filtered by the optional filter string, SQL like syntax. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all users was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO[].class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
      }
  )
  public Response getAllUsers(
      @Parameter(
          description = "Optional filter string",
          schema = @Schema(type = "string", example = "username = 'bill'")
      )
      @QueryParam("filter") String filter) {

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

    AuthManager authManager = AuthManager.getInstance();
    List<UserDetails> users = authManager.getUsers();

    UserDTO[] result = users.stream()
        .map(userDetails -> buildUser(userDetails, authManager))
        .filter(userDto -> parserExecutor == null || parserExecutor.evaluate(userDto))
        .toList()
        .toArray(new UserDTO[0]);

    return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("/{userUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get user by uuid",
      description = "Retrieve the user by uuid. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get user was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(
              responseCode = "404",
              description = "User not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response getUser(
      @Parameter(
          required = true,
          description = "User unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("userUuid") String userUuid) {

    hasAccess(RESOURCE);

    UUID uuid;
    try {
      uuid = UUID.fromString(userUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();

    return authManager.getUsers().stream()
        .filter(user -> user.getIdentityEntry().getId().equals(uuid))
        .findFirst()
        .map(user -> buildUser(user, authManager))
        .map(userDto -> Response.ok(userDto).type(MediaType.APPLICATION_JSON).build())
        .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("User not found"))
            .type(MediaType.APPLICATION_JSON)
            .build());
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Add a new user",
      description = "Adds a new user to the system. Requires authentication if enabled in the configuration.",
      requestBody = @RequestBody(
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewUserDTO.class))
      ),
      responses = {
          @ApiResponse(
              responseCode = "201",
              description = "User created",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(
              responseCode = "409",
              description = "Username already exists",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response addUser(@Valid NewUserDTO newUser) {

    hasAccess(RESOURCE);

    if (newUser == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Request body is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (newUser.getUsername() == null || newUser.getUsername().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Username is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (newUser.getPassword() == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Password is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();
    SessionPrivileges sessionPrivileges = new SessionPrivileges(newUser.getUsername());

    boolean created = authManager.addUser(
        newUser.getUsername(),
        newUser.getPassword().toCharArray(),
        sessionPrivileges,
        new String[0]
    );

    if (!created) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new StatusResponse("Username already exists"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    removeUriFromCache(URI_PATH + "/auth/users");

    return Response.status(Response.Status.CREATED)
        .entity(new StatusResponse("User added successfully"))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  @DELETE
  @Path("/{userUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete a user",
      description = "Deletes a user from the system. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(responseCode = "204", description = "User deleted"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(
              responseCode = "404",
              description = "User not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response deleteUser(
      @Parameter(
          required = true,
          description = "User unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("userUuid") String userUuid) {

    hasAccess(RESOURCE);

    UUID uuid;
    try {
      uuid = UUID.fromString(userUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();
    Identity identity = authManager.getUserIdentity(uuid);

    if (identity == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("User not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    authManager.delUser(identity.getUsername());
    removeUriFromCache(URI_PATH + "/auth/users");
    return Response.noContent().build();
  }

  @PUT
  @Path("/{userUuid}/password")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Change user password",
      description = "Change the password for a user. Admin may reset any user. A user may change their own password; currentPassword may be required depending on policy.",
      requestBody = @RequestBody(
          required = true,
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChangePasswordDTO.class))
      ),
      responses = {
          @ApiResponse(responseCode = "204", description = "Password changed"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(
              responseCode = "404",
              description = "User not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Password update failed",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
      }
  )
  public Response changeUserPassword(
      @Parameter(
          required = true,
          description = "User unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("userUuid") String userUuid,
      @Valid ChangePasswordDTO request
  ) {

    hasAccess(RESOURCE);

    UUID uuid;
    try {
      uuid = UUID.fromString(userUuid);
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    if (request == null || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("New password is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    AuthManager authManager = AuthManager.getInstance();
    Identity identity = authManager.getUserIdentity(uuid);

    if (identity == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new StatusResponse("User not found"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      authManager.updatePassword(identity, request.getNewPassword().toCharArray());
      removeUriFromCache(URI_PATH + "/auth/users");
      return Response.noContent().build();
    } catch (GeneralSecurityException | IOException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Password update failed"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  private UserDTO buildUser(UserDetails user, AuthManager authManager) {
    List<GroupInfoDTO> groupNames = new ArrayList<>();
    for (UUID groupId : user.getGroups()) {
      Group group = authManager.getGroupIdentity(groupId);
      GroupInfoDTO groupDTO = new GroupInfoDTO(group.getName(), groupId);
      groupNames.add(groupDTO);
    }
    return new UserDTO(
        user.getIdentityEntry().getUsername(),
        user.getIdentityEntry().getId(),
        groupNames,
        new LinkedHashMap<>(user.getIdentityEntry().getAttributes())
    );
  }
}
