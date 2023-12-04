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
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.Group;
import io.mapsmessaging.rest.responses.GroupListResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "User Profile and Authentication")
@Path(URI_PATH)
public class GroupManagementApi extends BaseRestApi {


  @GET
  @Path("/auth/groups")
  @Produces({MediaType.APPLICATION_JSON})
  public GroupListResponse getGroups() {
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


}
