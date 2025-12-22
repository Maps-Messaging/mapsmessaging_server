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

package io.mapsmessaging.rest.api.impl.auth;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.dto.rest.config.auth.AuthorisationConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.PermissionDetailsDTO;
import io.mapsmessaging.dto.rest.config.auth.ResourceTypeDetailsDTO;
import io.mapsmessaging.rest.api.impl.auth.dto.*;
import io.mapsmessaging.rest.api.impl.auth.service.AuthorisationRestHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Path(URI_PATH+"/auth")
public class AuthorisationResource extends BaseAuthRestApi {

  private final AuthorisationRestHelper managementService = new  AuthorisationRestHelper();

  @GET
  @Path("/permissions")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get the authorisation permission list",
      description = "Retrieves the read only permissions used by the servers Authorisation",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get permissions was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthorisationConfigDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),

      }
  )
  public AuthorisationConfigDTO getAuthorisationStaticInfo() {
    hasAccess(RESOURCE);
    List<PermissionDetailsDTO> details = new ArrayList<>();
    for(ServerPermissions permission : ServerPermissions.values()){
      details.add(new PermissionDetailsDTO(permission));
    }
    List<ResourceTypeDetailsDTO> resourceTypes = new ArrayList<>();
    resourceTypes.add(new ResourceTypeDetailsDTO("Server", true));
    for(DestinationType type : DestinationType.values()){
      resourceTypes.add(new ResourceTypeDetailsDTO(type.getName(), false));
    }

    return new AuthorisationConfigDTO(details,resourceTypes);
  }

  @GET
  @Path("/resources/acl")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get the ACL for a specific resource",
      description = "Retrieves explicit ACL entries for the given resource",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "ACL retrieval was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AclResourceViewDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public AclResourceViewDTO getResourceAcl(
      @Parameter(description = "Resource type", required = true, example = "TOPIC")
      @QueryParam("resourceType") String resourceType,
      @Parameter(description = "Resource key or identifier", required = true, example = "/sensors/room1/temp")
      @QueryParam("resourceKey") String resourceKey) {

    hasAccess(RESOURCE);
    return managementService.getResourceAcl(resourceType, resourceKey);
  }

  @PUT
  @Path("/resources/acl")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Replace the ACL for a specific resource",
      description = "Replaces the explicit ACL entries for the given resource with the provided set",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "ACL update was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AclResourceViewDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public AclResourceViewDTO updateResourceAcl(
      AclResourceUpdateRequestDTO request,
      @DefaultValue("5000") @QueryParam("batchTimeoutMillis") long batchTimeoutMillis) {

    hasAccess(RESOURCE);
    return managementService.updateResourceAcl(request, batchTimeoutMillis);
  }

  @GET
  @Path("/identities/{identityId}/acl")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get explicit ACL entries for an identity",
      description = "Retrieves explicit ACL entries for the specified identity, grouped by resource",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Identity ACL retrieval was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IdentityAclViewDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public IdentityAclViewDTO getIdentityAcl(@PathParam("identityId") String identityId) {
    hasAccess(RESOURCE);
    return managementService.getIdentityAcl(identityId);
  }

  @GET
  @Path("/groups/{groupId}/acl")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get explicit ACL entries for a group",
      description = "Retrieves explicit ACL entries for the specified group, grouped by resource",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Group ACL retrieval was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = IdentityAclViewDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public IdentityAclViewDTO getGroupAcl(@PathParam("groupId") String groupId) {
    hasAccess(RESOURCE);
    return managementService.getGroupAcl(groupId);
  }

  @POST
  @Path("/acl/check")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Check access for an identity to a resource",
      description = "Checks whether the identity has the specified permission on the given resource",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "ACL check was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AclCheckResponseDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource")
      }
  )
  public AclCheckResponseDTO checkAccess(AclCheckRequestDTO request) {
    hasAccess(RESOURCE);
    return managementService.checkAccess(request);
  }
}