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

package io.mapsmessaging.auth.acl;

import io.mapsmessaging.auth.registry.principal.AccessIdPrincipal;
import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.access.AccessControlList;
import io.mapsmessaging.security.access.AclEntry;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.identity.principals.GroupIdPrincipal;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class BaseAccessControlList implements AccessControlList {

  protected final List<AclEntry> aclEntries;

  protected BaseAccessControlList() {
    aclEntries = new ArrayList<>();
  }

  protected BaseAccessControlList(List<AclEntry> aclEntries) {
    this.aclEntries = new ArrayList<>(aclEntries);
  }


  public long getSubjectAccess(Subject subject) {
    long mask = 0;
    if (subject != null) {
      long time = System.currentTimeMillis();
      Set<AccessIdPrincipal> accessIdPrincipals = subject.getPrincipals(AccessIdPrincipal.class);
      if (!accessIdPrincipals.isEmpty()) { // If we have a prebuilt list of UUIDs then lets scan it rather than look up individually
        mask = parseUUIDList(accessIdPrincipals, time);
      } else {
        // Scan the user
        mask = parseUserUUID(SubjectHelper.getUniqueId(subject), time);
        // Scan the groups for access
        mask = parseGroupUUID(subject.getPrincipals(GroupIdPrincipal.class), mask, time);
      }
    }
    return mask;
  }


  public boolean canAccess(Subject subject, long requestedAccess) {
    if (subject == null || requestedAccess == 0) {
      return false;
    }
    Set<AccessIdPrincipal> accessIdPrincipals = subject.getPrincipals(AccessIdPrincipal.class);
    if (!accessIdPrincipals.isEmpty()) { // If we have a prebuilt list of UUIDs then lets scan it rather than look up individually
      return listHas(accessIdPrincipals, System.currentTimeMillis(), requestedAccess);
    } else {
      UUID authId = SubjectHelper.getUniqueId(subject);
      if (userHas(authId, System.currentTimeMillis(), requestedAccess)) return true;

      // Scan the groups for access
      Set<GroupIdPrincipal> groups = subject.getPrincipals(GroupIdPrincipal.class);
      return groupHas(groups, System.currentTimeMillis(), requestedAccess);
    }
  }

  public boolean add(UUID uuid, long requestedAccess){
    return aclEntries.add(new AclEntry(uuid, requestedAccess));
  }

  public boolean remove(UUID uuid, long requestedAccess){
    return aclEntries.removeIf(aclEntry -> aclEntry.matches(uuid) && aclEntry.getPermissions() == requestedAccess);
  }

  private long parseUserUUID(UUID authId, long time) {
    long mask = 0;
    for (AclEntry aclEntry : aclEntries) {
      if (!aclEntry.getExpiryPolicy().hasExpired(time) &&
          aclEntry.matches(authId)) {
        mask = mask | aclEntry.getPermissions();
      }
    }
    return mask;
  }


  private boolean userHas(UUID authId, long time, long requestedAccess) {
    for (AclEntry aclEntry : aclEntries) {
      if (!aclEntry.getExpiryPolicy().hasExpired(time) &&
          (aclEntry.getPermissions() & requestedAccess) == requestedAccess
          && aclEntry.matches(authId)) {
        return true;
      }
    }
    return false;
  }


  private long parseGroupUUID(Set<GroupIdPrincipal> groups, long mask, long time) {
    for (GroupIdPrincipal group : groups) {
      for (GroupIdMap groupIdMap : group.getGroupIds()) {
        for (AclEntry aclEntry : aclEntries) {
          if (!aclEntry.getExpiryPolicy().hasExpired(time)
              && aclEntry.matches(groupIdMap.getAuthId())) {
            mask = mask | aclEntry.getPermissions();
          }
        }
      }
    }
    return mask;
  }

  private boolean groupHas(Set<GroupIdPrincipal> groups, long time, long requestedAccess) {
    for (GroupIdPrincipal group : groups) {
      for (GroupIdMap groupIdMap : group.getGroupIds()) {
        for (AclEntry aclEntry : aclEntries) {
          if (!aclEntry.getExpiryPolicy().hasExpired(time) &&
              (aclEntry.getPermissions() & requestedAccess) == requestedAccess
              && aclEntry.matches(groupIdMap.getAuthId())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private long parseUUIDList(Set<AccessIdPrincipal> accessIdPrincipals, long time) {
    long mask = 0;
    for (AccessIdPrincipal accessIdPrincipal : accessIdPrincipals) {
      for (UUID uuid : accessIdPrincipal.getAccessIds()) {
        for (AclEntry aclEntry : aclEntries) {
          if (!aclEntry.getExpiryPolicy().hasExpired(time) && aclEntry.matches(uuid))
            mask = mask | aclEntry.getPermissions();
        }
      }
    }
    return mask;
  }


  private boolean listHas(Set<AccessIdPrincipal> accessIdPrincipals, long timeMillis, long requestedAccess) {
    for (AccessIdPrincipal accessIdPrincipal : accessIdPrincipals) {
      for (UUID uuid : accessIdPrincipal.getAccessIds()) {
        for (AclEntry aclEntry : aclEntries) {
          if (!aclEntry.getExpiryPolicy().hasExpired(timeMillis) &&
              (aclEntry.getPermissions() & requestedAccess) == requestedAccess
              && aclEntry.matches(uuid)) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
