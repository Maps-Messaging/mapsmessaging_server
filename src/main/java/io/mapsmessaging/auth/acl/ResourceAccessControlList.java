package io.mapsmessaging.auth.acl;

import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.access.AccessControlList;
import io.mapsmessaging.security.access.AccessControlListParser;
import io.mapsmessaging.security.access.AccessControlMapping;
import io.mapsmessaging.security.access.AclEntry;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.identity.principals.GroupIdPrincipal;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ResourceAccessControlList implements AccessControlList {


  private final List<AclEntry> aclEntries;

  public ResourceAccessControlList() {
    aclEntries = new ArrayList<>();
  }

  public ResourceAccessControlList(List<AclEntry> aclEntries) {
    this.aclEntries = new ArrayList<>(aclEntries);
  }

  @Override
  public String getName() {
    return "ResourceAccessControlList";
  }

  @Override
  public AccessControlList create(AccessControlMapping accessControlMapping, List<String> config) {
    AccessControlListParser parser = new AccessControlListParser();
    return new ResourceAccessControlList(parser.createList(accessControlMapping, config));
  }

  public long getSubjectAccess(Subject subject) {
    long mask = 0;
    if (subject != null) {
      long time = System.currentTimeMillis();
      UUID authId = SubjectHelper.getUniqueId(subject);
      for (AclEntry aclEntry : aclEntries) {
        if (!aclEntry.getExpiryPolicy().hasExpired(time) &&
            aclEntry.matches(authId)) {
          mask = mask | aclEntry.getPermissions();
        }
      }

      // Scan the groups for access
      Set<GroupIdPrincipal> groups = subject.getPrincipals(GroupIdPrincipal.class);
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
    }
    return mask;
  }

  public boolean canAccess(Subject subject, long requestedAccess) {
    if (subject == null || requestedAccess == 0) {
      return false;
    }
    UUID authId = SubjectHelper.getUniqueId(subject);

    // Scan for authId for access
    for (AclEntry aclEntry : aclEntries) {
      if ((aclEntry.getPermissions() & requestedAccess) == requestedAccess
          && aclEntry.matches(authId)) {
        return true;
      }
    }

    // Scan the groups for access
    Set<GroupIdPrincipal> groups = subject.getPrincipals(GroupIdPrincipal.class);
    for (GroupIdPrincipal group : groups) {
      for (GroupIdMap groupIdMap : group.getGroupIds()) {
        for (AclEntry aclEntry : aclEntries) {
          if ((aclEntry.getPermissions() & requestedAccess) == requestedAccess
              && aclEntry.matches(groupIdMap.getAuthId())) {
            return true;
          }
        }
      }
    }
    // This means neither user nor group has access
    return false;
  }
}
