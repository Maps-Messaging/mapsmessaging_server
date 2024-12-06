/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.dto.rest.auth.GroupDTO;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.GroupListResponse;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
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
public class GroupManagementApi extends BaseRestApi {


  @GET
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  public GroupListResponse getAllGroups(@QueryParam("filter") String filter) throws ParseException {
    checkAuthentication();

    // Prepare the parser and cache key
    ParserExecutor parser = (filter != null && !filter.isEmpty()) ? SelectorParser.compile(filter) : null;
    CacheKey key = new CacheKey(uriInfo.getPath(), (filter != null && !filter.isEmpty()) ? ""+filter.hashCode() : "");

    // Check cache
    GroupListResponse cachedResponse = getFromCache(key, GroupListResponse.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    // Fetch groups
    AuthManager authManager = AuthManager.getInstance();
    List<GroupDetails> groups = authManager.getGroups();

    // Transform and filter groups
    List<GroupDTO> results = groups.stream()
        .map(groupDetails -> new GroupDTO(
            groupDetails.getName(),
            groupDetails.getGroupId(),
            groupDetails.getUsers()))
        .filter(group -> filterGroup(parser, group))
        .collect(Collectors.toList());

    // Create response and cache it
    GroupListResponse result = new GroupListResponse(request, results);
    putToCache(key, result);
    return result;
  }


  @GET
  @Path("/auth/groups/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public GroupDTO getGroupsById(@PathParam("groupUuid") String groupUuid) {
    checkAuthentication();

    // Fetch the group by UUID
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups()
        .stream()
        .filter(g -> g.getGroupId().toString().equals(groupUuid))
        .findFirst()
        .orElse(null);

    // Return the GroupDTO if found
    if (groupDetails != null) {
      return new GroupDTO(groupDetails.getName(), groupDetails.getGroupId(), groupDetails.getUsers());
    }

    // Return a 404 if the group is not found
    throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
  }

  @POST
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  public GroupDTO addGroup(String groupName) throws IOException {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    GroupIdMap groupIdMap = authManager.addGroup(groupName);
    return new GroupDTO(groupName, groupIdMap.getAuthId(), new ArrayList<>());
  }

  @POST
  @Path("/auth/groups/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse addUserToGroup(@PathParam("groupUuid") String groupUuid, @PathParam("userUuid") String userUuid) throws IOException {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    UserDetails userDetails = authManager.getUsers().stream().filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid)).findFirst().orElse(null);
    if (groupDetails != null && userDetails != null) {
      authManager.addUserToGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    }
    return new BaseResponse();
  }

  @DELETE
  @Path("/auth/groups/{groupUuid}/{userUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse removeUserFromGroup(@PathParam("groupUuid") String groupUuid, @PathParam("userUuid") String userUuid) throws IOException {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    UserDetails userDetails = authManager.getUsers().stream().filter(u -> u.getIdentityEntry().getId().toString().equals(userUuid)).findFirst().orElse(null);
    if (groupDetails != null && userDetails != null) {
      authManager.removeUserFromGroup(userDetails.getIdentityEntry().getUsername(), groupDetails.getName());
    }
    return new BaseResponse();
  }

  @DELETE
  @Path("/auth/groups/{groupUuid}")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse deleteGroup(@PathParam("groupUuid") String groupUuid) throws IOException {
    checkAuthentication();
    AuthManager authManager = AuthManager.getInstance();
    GroupDetails groupDetails = authManager.getGroups().stream().filter(g -> g.getGroupId().toString().equals(groupUuid)).findFirst().orElse(null);
    if (groupDetails != null) {
      authManager.delGroup(groupDetails.getName());
      return new BaseResponse();
    }
    response.setStatus(500);
    return new BaseResponse();
  }


  // Helper methods
  private boolean filterGroup(ParserExecutor parser, GroupDTO group) {
    return parser == null || parser.evaluate(group);
  }

}
