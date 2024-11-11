/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.UserListResponse;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
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
public class UserManagementApi extends BaseRestApi {

  @GET
  @Path("/auth/users")
  @Produces({MediaType.APPLICATION_JSON})
  public UserListResponse getAllUsers(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    List<UserDetails> users = authManager.getUsers();
    ParserExecutor parser = (filter != null && !filter.isEmpty())  ? SelectorParser.compile(filter) : null;
    List<UserDTO> results = users.stream()
        .map(userDetails -> buildUser(userDetails, authManager))
        .filter(user -> parser == null || parser.evaluate(user))
        .collect(Collectors.toList());
    return new UserListResponse(request, results);
  }

  @GET
  @Path("/auth/user/{username}")
  @Produces({MediaType.APPLICATION_JSON})
  public UserDTO getUser(@PathParam("username") String username) {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    List<UserDetails> users = authManager.getUsers();
    for (UserDetails user : users) {
      if(user.getIdentityEntry().getUsername().equals(username)) {
        return buildUser(user, authManager);
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return null;
  }

  @POST
  @Path("/auth/users")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse addUser(NewUserDTO newUser) {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    SessionPrivileges sessionPrivileges = new SessionPrivileges(newUser.getUsername());
    if (authManager.addUser(newUser.getUsername(), newUser.getPassword().toCharArray(), sessionPrivileges, new String[0])) {
      return new BaseResponse(request);
    }
    return new BaseResponse(request);//, 500, "Failed to create user");
  }

  @DELETE
  @Path("/auth/users/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse deleteUser(@PathParam("userUuid") String userUuid) {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    Identity userIdMap = authManager.getUserIdentity(UUID.fromString(userUuid));
    if (userIdMap != null) {
      authManager.delUser(userIdMap.getUsername());
      return new BaseResponse(request);
    }
    response.setStatus(500);
    return new BaseResponse(request);
  }

  private UserDTO buildUser(UserDetails user, AuthManager authManager){
    List<String> groupNames = new ArrayList<>();
    for (UUID groupId : user.getGroups()) {
      groupNames.add(authManager.getGroupIdentity(groupId).getName());
    }
    return new UserDTO(
        user.getIdentityEntry().getUsername(),
        user.getIdentityEntry().getId(),
        groupNames,
        new LinkedHashMap<>(user.getIdentityEntry().getAttributes())
    );
  }
}