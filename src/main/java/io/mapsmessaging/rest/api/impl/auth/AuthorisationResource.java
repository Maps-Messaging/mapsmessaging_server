/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.auth.ResourceTypes;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.dto.rest.auth.*;
import io.mapsmessaging.dto.rest.config.auth.AuthorisationConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.PermissionDetailsDTO;
import io.mapsmessaging.dto.rest.config.auth.ResourceTypeDetailsDTO;
import io.mapsmessaging.rest.api.impl.auth.service.AuthorisationRestHelper;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Authentication and Authorisation Management")
@Path(URI_PATH + "/auth")
public class AuthorisationResource extends BaseAuthRestApi {

  private final AuthorisationRestHelper managementService = new AuthorisationRestHelper();

  @GET
  @Path("/permissions")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get the authorisation permission list",
      description = "Retrieves the read only permissions used by the servers Authorisation",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get permissions was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthorisationConfigDTO.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
      }
  )
  public Response getAuthorisationStaticInfo() {
    hasAccess(RESOURCE);

    try {
      List<PermissionDetailsDTO> details = new ArrayList<>();
      for (ServerPermissions permission : ServerPermissions.values()) {
        details.add(new PermissionDetailsDTO(permission));
      }

      List<ResourceTypeDetailsDTO> resourceTypes = new ArrayList<>();
      resourceTypes.add(new ResourceTypeDetailsDTO("Server", true));
      for (DestinationType type : DestinationType.values()) {
        resourceTypes.add(new ResourceTypeDetailsDTO(type.getName(), false));
      }

      return Response.ok(new AuthorisationConfigDTO(details, resourceTypes))
          .type(MediaType.APPLICATION_JSON)
          .build();
    } catch (RuntimeException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Internal server error"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Resource not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getResourceAcl(
      @Parameter(
          description = "Resource type",
          required = true,
          schema = @Schema(type = "string", example = "TOPIC")
      )
      @QueryParam("resourceType") String resourceType,
      @Parameter(
          description = "Resource key or identifier",
          required = true,
          schema = @Schema(type = "string", example = "/sensors/room1/temp")
      )
      @QueryParam("resourceKey") String resourceKey
  ) {
    hasAccess(RESOURCE);

    if (resourceType == null || resourceType.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("resourceType is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (resourceKey == null || resourceKey.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("resourceKey is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    if(!ResourceTypes.getInstance().getResources().contains(resourceType)){
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("resourceType is not known"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      AclResourceViewDTO result = managementService.getResourceAcl(resourceType, resourceKey);
      if (result == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("Resource not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
      return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
    } catch (RuntimeException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Internal server error"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Resource not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response updateResourceAcl(
      @Valid AclResourceUpdateRequestDTO request,
      @Parameter(
          required = false,
          description = "Maximum time to wait for batch propagation",
          schema = @Schema(type = "integer", format = "int64", defaultValue = "5000", minimum = "1")
      )
      @DefaultValue("5000")
      @QueryParam("batchTimeoutMillis") long batchTimeoutMillis
  ) {
    hasAccess(RESOURCE);

    if (request == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("request body is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    if (batchTimeoutMillis <= 0L) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("batchTimeoutMillis must be > 0"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      AclResourceViewDTO result = managementService.updateResourceAcl(request, batchTimeoutMillis);
      if (result == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("Resource not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
      return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
    } catch (RuntimeException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Internal server error"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  @GET
  @Path("/identities/{userUuid}/acl")
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Identity not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getIdentityAcl(
      @Parameter(
          required = true,
          description = "User unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("userUuid") String userUuid
  ) {
    hasAccess(RESOURCE);

    UUID uuid;
    try {
      uuid = UUID.fromString(userUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      IdentityAclViewDTO result = managementService.getIdentityAcl(uuid.toString());
      if (result == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("Identity not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
      return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
    } catch (RuntimeException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Internal server error"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  @GET
  @Path("/groups/{groupUuid}/acl")
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Group not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response getGroupAcl(
      @Parameter(
          required = true,
          description = "Group unique identifier",
          schema = @Schema(type = "string", format = "uuid")
      )
      @PathParam("groupUuid") String groupUuid
  ) {
    hasAccess(RESOURCE);

    UUID uuid;
    try {
      uuid = UUID.fromString(groupUuid);
    } catch (IllegalArgumentException ex) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Invalid UUID"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      IdentityAclViewDTO result = managementService.getGroupAcl(uuid.toString());
      if (result == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("Group not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }
      return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
    } catch (RuntimeException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Internal server error"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
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
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
      }
  )
  public Response checkAccess(@Valid AclCheckRequestDTO request) {
    hasAccess(RESOURCE);

    if (request == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("request body is required"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      AclCheckResponseDTO result = managementService.checkAccess(request);
      return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
    } catch (RuntimeException ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Internal server error"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }
}
