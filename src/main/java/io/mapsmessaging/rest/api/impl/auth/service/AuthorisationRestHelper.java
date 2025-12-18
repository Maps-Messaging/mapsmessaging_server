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

package io.mapsmessaging.rest.api.impl.auth.service;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.rest.api.impl.auth.dto.*;
import io.mapsmessaging.security.access.Group;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.authorisation.AccessDecision;
import io.mapsmessaging.security.authorisation.AuthorizationProvider;
import io.mapsmessaging.security.authorisation.Grant;
import io.mapsmessaging.security.authorisation.Grantee;
import io.mapsmessaging.security.authorisation.Permission;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthorisationRestHelper {

  private final AuthorizationProvider provider;


  @Inject
  public AuthorisationRestHelper() {
    provider = AuthManager.getInstance().getAuthorizationProvider();
  }

  public AclResourceViewDTO getResourceAcl(String resourceType, String resourceKey) {

    ProtectedResource protectedResource = new ProtectedResource(resourceType, resourceKey, null);
    Collection<Grant> grants = provider.getGrantsForResource(protectedResource);

    Map<String, Map<AclEffect, List<String>>> grouped =
        grants.stream().collect(Collectors.groupingBy(
            grant -> {
              Grantee grantee = grant.getGrantee();
              return grantee.type().name() + ":" + grantee.id().toString();
            },
            Collectors.groupingBy(
                this::toEffect,
                Collectors.mapping(
                    grant -> grant.getPermission().getName(),
                    Collectors.toList()
                )
            )
        ));

    List<AclEntryDTO> entries = new ArrayList<>();
    for (Map.Entry<String, Map<AclEffect, List<String>>> entry : grouped.entrySet()) {
      String key = entry.getKey();
      int separatorIndex = key.indexOf(':');
      String typeString = key.substring(0, separatorIndex);
      String idString = key.substring(separatorIndex + 1);

      PrincipalType principalType = PrincipalType.valueOf(typeString);
      for (Map.Entry<AclEffect, List<String>> effectEntry : entry.getValue().entrySet()) {
        AclEffect effect = effectEntry.getKey();
        List<String> permissions = effectEntry.getValue();
        AclEntryDTO dto = new AclEntryDTO(principalType, idString, effect, permissions);
        entries.add(dto);
      }
    }
    return new AclResourceViewDTO(resourceType, resourceKey, entries);
  }

  public AclResourceViewDTO updateResourceAcl(AclResourceUpdateRequestDTO request, long batchTimeoutMillis) {
    String resourceType = request.getResourceType();
    String resourceKey = request.getResourceKey();

    ProtectedResource protectedResource = new ProtectedResource(resourceType, resourceKey, null);
    Collection<Grant> existingGrants = provider.getGrantsForResource(protectedResource);

    Map<String, Map<String, AclEffect>> desired =
        request.getEntries().stream().collect(Collectors.toMap(
            entry -> entry.getPrincipalType().name() + ":" + entry.getPrincipalId(),
            entry -> entry.getPermissions().stream().collect(Collectors.toMap(
                permissionName -> permissionName,
                permissionName -> entry.getEffect()
            )),
            (left, right) -> {
              left.putAll(right);
              return left;
            }
        ));

    Map<String, Map<String, AclEffect>> current =
        existingGrants.stream().collect(Collectors.groupingBy(
            grant -> {
              Grantee grantee = grant.getGrantee();
              return grantee.type().name() + ":" + grantee.id().toString();
            },
            Collectors.toMap(
                grant -> grant.getPermission().getName(),
                this::toEffect,
                (left, right) -> right
            )
        ));

    provider.startBatch(batchTimeoutMillis);

    try {
      for (Map.Entry<String, Map<String, AclEffect>> desiredEntry : desired.entrySet()) {
        String principalKey = desiredEntry.getKey();
        Map<String, AclEffect> desiredPermissions = desiredEntry.getValue();
        Map<String, AclEffect> currentPermissions = current.get(principalKey);

        if (currentPermissions == null) {
          currentPermissions = Map.of();
        }

        for (Map.Entry<String, AclEffect> permissionEntry : desiredPermissions.entrySet()) {
          String permissionName = permissionEntry.getKey();
          AclEffect desiredEffect = permissionEntry.getValue();
          AclEffect currentEffect = currentPermissions.get(permissionName);

          if (currentEffect == null || currentEffect != desiredEffect) {
            Grantee grantee = toGrantee(principalKey);
            Permission permission = ServerPermissions.valueOf(permissionName);
            if (desiredEffect == AclEffect.ALLOW) {
              provider.grantAccess(grantee, permission, protectedResource);
            } else {
              provider.denyAccess(grantee, permission, protectedResource);
            }
          }
        }
      }

      for (Map.Entry<String, Map<String, AclEffect>> currentEntry : current.entrySet()) {
        String principalKey = currentEntry.getKey();
        Map<String, AclEffect> currentPermissions = currentEntry.getValue();
        Map<String, AclEffect> desiredPermissions = desired.get(principalKey);

        if (desiredPermissions == null) {
          for (String permissionName : currentPermissions.keySet()) {
            Grantee grantee = toGrantee(principalKey);
            Permission permission = ServerPermissions.valueOf(permissionName);
            provider.revokeAccess(grantee, permission, protectedResource);
          }
        } else {
          for (String permissionName : currentPermissions.keySet()) {
            if (!desiredPermissions.containsKey(permissionName)) {
              Grantee grantee = toGrantee(principalKey);
              Permission permission = ServerPermissions.valueOf(permissionName);
              provider.revokeAccess(grantee, permission, protectedResource);
            }
          }
        }
      }
    } finally {
      provider.stopBatch();
    }

    return getResourceAcl(resourceType, resourceKey);
  }

  public IdentityAclViewDTO getIdentityAcl(String identityId) {
    UUID uuid = UUID.fromString(identityId);
    Identity identity = AuthManager.getInstance().getUserIdentity(uuid);
    Collection<Grant> grants = provider.getGrantsForIdentity(identity);

    Map<ProtectedResource, Map<AclEffect, List<String>>> grouped =
        grants.stream().collect(Collectors.groupingBy(
            Grant::getProtectedResource,
            Collectors.groupingBy(
                this::toEffect,
                Collectors.mapping(
                    grant -> grant.getPermission().getName(),
                    Collectors.toList()
                )
            )
        ));

    List<IdentityAclEntryDTO> entries = new ArrayList<>();
    for (Map.Entry<ProtectedResource, Map<AclEffect, List<String>>> entry : grouped.entrySet()) {
      ProtectedResource protectedResource = entry.getKey();
      String resourceType = protectedResource.getResourceType();
      String resourceKey = protectedResource.getResourceId();

      for (Map.Entry<AclEffect, List<String>> effectEntry : entry.getValue().entrySet()) {
        AclEffect effect = effectEntry.getKey();
        List<String> permissions = effectEntry.getValue();
        IdentityAclEntryDTO dto = new IdentityAclEntryDTO(resourceType, resourceKey, effect, permissions);
        entries.add(dto);
      }
    }

    return new IdentityAclViewDTO(PrincipalType.IDENTITY, identityId, entries);
  }

  public IdentityAclViewDTO getGroupAcl(String groupId) {
    UUID uuid = UUID.fromString(groupId);
    Group group = AuthManager.getInstance().getGroupIdentity(uuid);
    Collection<Grant> grants = provider.getGrantsForGroup(group);

    Map<ProtectedResource, Map<AclEffect, List<String>>> grouped =
        grants.stream().collect(Collectors.groupingBy(
            Grant::getProtectedResource,
            Collectors.groupingBy(
                this::toEffect,
                Collectors.mapping(
                    grant -> grant.getPermission().getName(),
                    Collectors.toList()
                )
            )
        ));

    List<IdentityAclEntryDTO> entries = new ArrayList<>();
    for (Map.Entry<ProtectedResource, Map<AclEffect, List<String>>> entry : grouped.entrySet()) {
      ProtectedResource protectedResource = entry.getKey();
      String resourceType = protectedResource.getResourceType();
      String resourceKey = protectedResource.getResourceId();

      for (Map.Entry<AclEffect, List<String>> effectEntry : entry.getValue().entrySet()) {
        AclEffect effect = effectEntry.getKey();
        List<String> permissions = effectEntry.getValue();
        IdentityAclEntryDTO dto = new IdentityAclEntryDTO(resourceType, resourceKey, effect, permissions);
        entries.add(dto);
      }
    }

    return new IdentityAclViewDTO(PrincipalType.GROUP, groupId, entries);
  }

  public AclCheckResponseDTO checkAccess(AclCheckRequestDTO request) {
    UUID uuid = UUID.fromString(request.getIdentityId());
    Identity identity = AuthManager.getInstance().getUserIdentity(uuid);
    ProtectedResource protectedResource = new ProtectedResource(request.getResourceType(), request.getResourceKey(), null);
    Permission permission = ServerPermissions.valueOf(request.getPermission());


    boolean explain = request.isExplain();
    if (explain) {
      AccessDecision accessDecision = provider.explainAccess(identity, permission, protectedResource);
      AclDecision decision = accessDecision.isAllowed() ? AclDecision.ALLOW : AclDecision.DENY;
      String reason = accessDecision.getDetailMessage();
      List<String> sources = accessDecision.getContributingGrants()
          .stream()
          .map(Object::toString)
          .toList();
      return new AclCheckResponseDTO(decision, request.getPermission(), reason, sources);
    } else {
      boolean allowed = provider.canAccess(identity, permission, protectedResource);
      AclDecision decision = allowed ? AclDecision.ALLOW : AclDecision.DENY;
      return new AclCheckResponseDTO(decision, request.getPermission(), null, null);
    }
  }

  private AclEffect toEffect(Grant grant) {
    return grant.isAllow() ? AclEffect.ALLOW : AclEffect.DENY;
  }


  private Grantee toGrantee(String principalKey) {
    int separatorIndex = principalKey.indexOf(':');
    String typeString = principalKey.substring(0, separatorIndex);
    String idString = principalKey.substring(separatorIndex + 1);
    PrincipalType principalType = PrincipalType.valueOf(typeString);
    UUID uuid = UUID.fromString(idString);

    if (principalType == PrincipalType.IDENTITY) {
      Identity identity = AuthManager.getInstance().getUserIdentity(uuid);
      return Grantee.forIdentity(identity);
    }
    Group group = AuthManager.getInstance().getGroupIdentity(uuid);
    return Grantee.forGroup(group);
  }

}