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

package io.mapsmessaging.rest.api.impl.auth;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.dto.rest.auth.NewUserDTO;
import io.mapsmessaging.dto.rest.auth.UserDTO;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.UserListResponse;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH)
public class UserManagementApi extends BaseAuthRestApi {

  @GET
  @Path("/auth/users")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all users",
      description = "Retrieves all currently known users filtered by the optional filter string, SQL like syntax. Requires authentication if enabled in the configuration."
  )
  public UserListResponse getAllUsers(
      @Parameter(
          description = "Optional filter string ",
          schema = @Schema(type= "String", example = "username = 'bill'")
      )
      @QueryParam("filter") String filter) throws ParseException {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    List<UserDetails> users = authManager.getUsers();
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    List<UserDTO> results =
        users.stream()
            .map(userDetails -> buildUser(userDetails, authManager))
            .filter(user -> parser == null || parser.evaluate(user))
            .collect(Collectors.toList());
    return new UserListResponse(results);
  }

  @GET
  @Path("/auth/user/{username}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get user by username",
      description = "Retrieve the user by username. Requires authentication if enabled in the configuration."
  )
  public UserDTO getUser(@PathParam("username") String username) {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    return authManager.getUsers().stream()
        .filter(user -> user.getIdentityEntry().getUsername().equals(username))
        .findFirst()
        .map(user -> buildUser(user, authManager))
        .orElseGet(() -> {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          return null;
        });
  }


  @POST
  @Path("/auth/users")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add a new user",
      description = "Adds a new user to the system. Requires authentication if enabled in the configuration."
  )
  public StatusResponse addUser(NewUserDTO newUser) {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    SessionPrivileges sessionPrivileges = new SessionPrivileges(newUser.getUsername());
    if (authManager.addUser(newUser.getUsername(), newUser.getPassword().toCharArray(), sessionPrivileges, new String[0])) {
      response.setStatus(HttpServletResponse.SC_CREATED);
      return new StatusResponse("User added successfully");
    }
    response.setStatus(HttpServletResponse.SC_CONFLICT);
    return new StatusResponse("Username already exists");
  }

  @DELETE
  @Path("/auth/users/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete a user",
      description = "Deletes a user from the system. Requires authentication if enabled in the configuration."
  )
  public StatusResponse deleteUser(@PathParam("userUuid") String userUuid) {
    hasAccess(RESOURCE);
    AuthManager authManager = AuthManager.getInstance();
    Identity userIdMap = authManager.getUserIdentity(UUID.fromString(userUuid));
    if (userIdMap != null) {
      authManager.delUser(userIdMap.getUsername());
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new StatusResponse("No such user");
  }

  private UserDTO buildUser(UserDetails user, AuthManager authManager) {
    List<String> groupNames = new ArrayList<>();
    for (UUID groupId : user.getGroups()) {
      groupNames.add(authManager.getGroupIdentity(groupId).getName());
    }
    return new UserDTO(
        user.getIdentityEntry().getUsername(),
        user.getIdentityEntry().getId(),
        groupNames,
        new LinkedHashMap<>(user.getIdentityEntry().getAttributes()));
  }
}
