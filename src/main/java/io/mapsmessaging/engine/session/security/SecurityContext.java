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

package io.mapsmessaging.engine.session.security;

import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import lombok.Getter;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

@Getter
public abstract class SecurityContext {

  protected boolean isLoggedIn;
  protected final String username;
  protected Subject subject;

  private List<UUID> accessIds;

  protected SecurityContext(String username){
    this.username = username;
  }

  public abstract void login() throws IOException;

  public abstract void logout();

  protected static Subject buildSubject(String user, Principal endPointPrincipal) {
    Set<Principal> principalSet = new HashSet<>();
    Set<String> credentials = new HashSet<>();
    Set<String> privileges = new HashSet<>();
    principalSet.add(new UserPrincipal(user));
    if(endPointPrincipal != null) principalSet.add(endPointPrincipal);
    return new Subject(true, principalSet, credentials, privileges);
  }

  public void buildAccessIds() {
    if (accessIds == null) {
      accessIds = new ArrayList<>();
      accessIds.add(SubjectHelper.getUniqueId(subject));
      List<GroupIdMap> groupIds = SubjectHelper.getGroupId(subject);
      if (groupIds != null) {
        for (GroupIdMap g : groupIds) {
          accessIds.add(g.getAuthId());
        }
      }
    }
  }

  @Override
  public String toString(){
    return "Username:"+username+"/tSubject:"+subject;
  }

}
