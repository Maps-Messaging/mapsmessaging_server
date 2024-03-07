/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.auth.NewUser;
import io.mapsmessaging.rest.data.auth.User;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.UserListResponse;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH)
public class UserManagementApi extends BaseRestApi {

  @GET
  @Path("/auth/users")
  @Produces({MediaType.APPLICATION_JSON})
  public UserListResponse getUsers() {
    AuthManager authManager = AuthManager.getInstance();
    List<UserDetails> users = authManager.getUsers();
    List<User> results = new ArrayList<>();
    for (UserDetails user : users) {
      String username = user.getUserIdMap().getUsername();
      UUID userId = user.getUserIdMap().getAuthId();

      results.add(new User(username, userId, user.getGroups()));
    }
    return new UserListResponse(request, results);
  }

  @POST
  @Path("/auth/users")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse addUser(NewUser newUser) {
    AuthManager authManager = AuthManager.getInstance();
    SessionPrivileges sessionPrivileges = new SessionPrivileges(newUser.getUsername());
    if (authManager.addUser(newUser.getUsername(), newUser.getPassword(), sessionPrivileges, new String[0])) {
      return new BaseResponse(request);
    }
    return new BaseResponse(request);//, 500, "Failed to create user");
  }

  @DELETE
  @Path("/auth/users/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse deleteUser(@PathParam("userUuid") String userUuid) {
    AuthManager authManager = AuthManager.getInstance();
    UserIdMap userIdMap = authManager.getUserIdentity(UUID.fromString(userUuid));
    if (userIdMap != null) {
      authManager.delUser(userIdMap.getUsername());
      return new BaseResponse(request);
    }
    response.setStatus(500);
    return new BaseResponse(request);
  }
}