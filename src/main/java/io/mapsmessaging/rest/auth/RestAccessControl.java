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

package io.mapsmessaging.rest.auth;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.security.access.AccessControlFactory;
import io.mapsmessaging.security.access.AccessControlList;
import io.mapsmessaging.security.access.Group;

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
    Group adminId = AuthManager.getInstance().getGroupIdentity("admin");
    Group everyoneId = AuthManager.getInstance().getGroupIdentity("everyone");

    List<String> adminAndEveryone = new ArrayList<>();
    adminAndEveryone.add(adminId.getId()+" = Create|Read|Update|Delete");
    adminAndEveryone.add(everyoneId.getId()+" = Read");

    List<String> adminOnly = new ArrayList<>();
    adminOnly.add(adminId.getId()+" = Create|Read|Update|Delete");
    addToMap("auth", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("connections", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("destinations", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminAndEveryone));
    addToMap("discovery", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("integrations", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("interfaces", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("messaging", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminAndEveryone));
    addToMap("servers", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("schemas", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("details", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(), adminOnly));
    addToMap("root", AccessControlFactory.getInstance().get(ACL_TYPE, new RestAclMapping(),  adminAndEveryone ));
  }

  public Map<String, String> getAccess(Subject subject){
    Map<String, String> accessMap = new LinkedHashMap<>();
    for(Map.Entry<String,AccessControlList> entry: aclMapping.entrySet()){
      long access = entry.getValue().getSubjectAccess(subject);
      accessMap.put(entry.getKey(), RestAclMapping.getAllAccessControls(access));
    }
    return accessMap;
  }

  public boolean hasAccess(String resource, Subject subject, long access){
    if(!aclMapping.containsKey(resource)){
      //ToDo Log the fact we have no mapping for resource
    }
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
