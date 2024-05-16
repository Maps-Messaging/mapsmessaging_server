/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.auth.Group;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.GroupListResponse;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH)
public class GroupManagementApi extends BaseRestApi {


  @GET
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  public GroupListResponse getAllGroups() {
    AuthManager authManager = AuthManager.getInstance();
    List<GroupDetails> groups = authManager.getGroups();
    List<Group> results = new ArrayList<>();
    for (GroupDetails group : groups) {
      String groupName = group.getName();
      UUID groupId = group.getGroupId();

      results.add(new Group(groupName, groupId, group.getUsers()));
    }
    return new GroupListResponse(request, results);
  }

  @GET
  @Path("/auth/groups/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public Group getGroupsById(@PathParam("groupUuid") String groupUuid) {
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    if (groupDetails != null) {
      String groupName = groupDetails.getName();
      UUID groupId = groupDetails.getGroupId();
      return new Group(groupName, groupId, groupDetails.getUsers());
    }
    response.setStatus(500);
    return null;
  }

  @POST
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  public Group addGroup(String groupName) throws IOException {
    AuthManager authManager = AuthManager.getInstance();
    GroupIdMap groupIdMap = authManager.addGroup(groupName);
    return new Group(groupName, groupIdMap.getAuthId(), new ArrayList<>());
  }

  @POST
  @Path("/auth/groups/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse addUserToGroup(@PathParam("groupUuid") String groupUuid, @PathParam("userUuid") String userUuid) throws IOException {
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    UserDetails userDetails = authManager.getUsers().stream().filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid)).findFirst().orElse(null);
    if (groupDetails != null && userDetails != null) {
      authManager.addUserToGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    }
    return new BaseResponse(request);
  }

  @DELETE
  @Path("/auth/groups/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse removeUserFromGroup(@PathParam("groupUuid") String groupUuid, @PathParam("userUuid") String userUuid) throws IOException {
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    UserDetails userDetails = authManager.getUsers().stream().filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid)).findFirst().orElse(null);
    if (groupDetails != null && userDetails != null) {
      authManager.removeUserFromGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    }
    return new BaseResponse(request);
  }

  @DELETE
  @Path("/auth/groups/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse deleteGroup(@PathParam("groupUuid") String groupUuid) throws IOException {
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    if (groupDetails != null) {
      authManager.delGroup(groupDetails.getName());
      return new BaseResponse(request);
    }
    response.setStatus(500);
    return new BaseResponse(request);
  }
}
