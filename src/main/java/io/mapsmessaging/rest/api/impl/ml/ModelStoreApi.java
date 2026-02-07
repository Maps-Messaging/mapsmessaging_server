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

package io.mapsmessaging.rest.api.impl.ml;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.model.ModelStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import java.io.InputStream;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "ML Model Store")
@Path(URI_PATH + "/server/models")
public class ModelStoreApi extends BaseRestApi {

  private static final String RESOURCE = "models";

  @POST
  @Path("/{modelName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "ML Model upload",
      description = "Uploads a model.",
      requestBody = @RequestBody(
          required = true,
          content = @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA,
              schema = @Schema(
                  type = "object",
                  description = "Multipart form containing the model file.",
                  requiredProperties = {"file"}
              )
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Upload succeeded",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "406",
              description = "ML not supported",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response uploadModel(
      @PathParam("modelName") String modelName,
      @FormDataParam("file") InputStream fileStream) throws IOException {

    hasAccess(RESOURCE);

    if (modelName == null || modelName.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Model name must not be blank"))
          .build();
    }

    if (fileStream == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("File must be provided"))
          .build();
    }

    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if (modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failure, ML not supported"))
          .build();
    }

    byte[] bytes = fileStream.readAllBytes();
    modelStore.saveModel(modelName, bytes);

    return Response.ok()
        .type(MediaType.APPLICATION_JSON)
        .entity(new StatusResponse("Success"))
        .build();
  }

  @GET
  @Path("/{modelName}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(
      summary = "Download model",
      description = "Downloads a model by name.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Model content",
              content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Model not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "406",
              description = "ML not supported",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getModel(@PathParam("modelName") String modelName) throws IOException {

    hasAccess(RESOURCE);

    if (modelName == null || modelName.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Model name must not be blank"))
          .build();
    }

    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if (modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failure, ML not supported"))
          .build();
    }

    if (!modelStore.modelExists(modelName)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return Response.status(Response.Status.NOT_FOUND)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Model not found"))
          .build();
    }

    byte[] data = modelStore.loadModel(modelName);
    return Response.ok(data)
        .type(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"" + modelName + "\"")
        .build();
  }

  @HEAD
  @Path("/{modelName}")
  @Operation(
      summary = "Check if model exists",
      description = "Checks if a model with the given name exists.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Model exists"),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "Model not found"),
          @ApiResponse(responseCode = "406", description = "ML not supported")
      }
  )
  public Response modelExists(@PathParam("modelName") String modelName) throws IOException {

    hasAccess(RESOURCE);

    if (modelName == null || modelName.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Model name must not be blank"))
          .build();
    }

    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if (modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE).build();
    }

    if (modelStore.modelExists(modelName)) {
      return Response.ok().build();
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }

  @DELETE
  @Path("/{modelName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete model",
      description = "Deletes the model by name.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Model deleted successfully",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad request",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Model not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "406",
              description = "ML not supported",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response deleteModel(@PathParam("modelName") String modelName) throws IOException {

    hasAccess(RESOURCE);

    if (modelName == null || modelName.trim().isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Model name must not be blank"))
          .build();
    }

    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if (modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failure, ML not supported"))
          .build();
    }

    if (modelStore.modelExists(modelName)) {
      modelStore.deleteModel(modelName);
      return Response.ok()
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Success"))
          .build();
    }

    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return Response.status(Response.Status.NOT_FOUND)
        .type(MediaType.APPLICATION_JSON)
        .entity(new StatusResponse("Model not found"))
        .build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List all models",
      description = "Returns a list of all available model names.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "List of model names",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = String[].class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "406",
              description = "ML not supported",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response listModels() throws IOException {

    hasAccess(RESOURCE);

    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if (modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE)
          .type(MediaType.APPLICATION_JSON)
          .entity(new StatusResponse("Failure, ML not supported"))
          .build();
    }

    return Response.ok()
        .type(MediaType.APPLICATION_JSON)
        .entity(modelStore.listModels())
        .build();
  }
}
