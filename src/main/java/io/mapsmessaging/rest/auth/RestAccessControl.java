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

package io.mapsmessaging.rest.auth;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.security.access.AccessControlFactory;
import io.mapsmessaging.security.access.AccessControlList;
import io.mapsmessaging.security.access.mapping.GroupIdMap;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RestAccessControl {

  private static final String ACL_TYPE = "Permission";

  private final Map<String,AccessControlList> aclMapping;

  public RestAccessControl() {
    aclMapping = new LinkedHashMap<>();
    GroupIdMap adminId = AuthManager.getInstance().getGroupIdentity("admin");
    GroupIdMap everyoneId = AuthManager.getInstance().getGroupIdentity("everyone");

    List<String> adminAndEveryone = new ArrayList<>();
    adminAndEveryone.add(adminId.getAuthId()+" = Create|Read|Update|Delete");
    adminAndEveryone.add(everyoneId.getAuthId()+" = Read");

    List<String> adminOnly = new ArrayList<>();
    adminOnly.add(adminId.getAuthId()+" = Create|Read|Update|Delete");
    addToMap("/auth", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminOnly ));
    addToMap("/server/connections", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminOnly ));
    addToMap("/server/destination", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminAndEveryone ));
    addToMap("/server/integration", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly ));
    addToMap("/server/interface", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly ));
    addToMap("/messaging", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminAndEveryone ));
    addToMap("/server/schema", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminOnly ));
    addToMap("/server/details", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminOnly ));

    addToMap("root", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminAndEveryone ));
  }

  public boolean hasAccess(String resource, Subject subject, long access){
    String key = aclMapping.keySet().stream()
        .filter(s -> s.contains(resource))
        .findFirst()
        .orElse("root");
    AccessControlList acl = aclMapping.get(key);
    if (acl != null) {
      return acl.canAccess(subject, access);
    }
    return false;
  }

  private void addToMap(String name, AccessControlList acl) {
    aclMapping.put(name, acl);
  }
}
