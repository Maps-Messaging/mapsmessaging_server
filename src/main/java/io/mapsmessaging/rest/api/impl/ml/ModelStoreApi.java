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

package io.mapsmessaging.rest.api.impl.ml;


import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.selector.model.ModelStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import java.io.InputStream;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "ML Model Store")
@Path(URI_PATH)
public class ModelStoreApi extends BaseRestApi {

  private static final String RESOURCE = "models";


  @POST
  @Path("/server/model/{modelName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public StatusResponse uploadModel(
      @PathParam("modelName") String modelName,
      @FormDataParam("file") InputStream fileStream) throws IOException {
    hasAccess(RESOURCE);
    byte[] bytes = fileStream.readAllBytes();
    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if(modelStore == null) {
       response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return new StatusResponse("Failure, ML not supported");
    }
    else {
      modelStore.saveModel(modelName, bytes);
    }
    return new StatusResponse("Success");
  }
  @GET
  @Path("/server/model/{modelName}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(
      summary = "Download model",
      description = "Downloads a model by name.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Model content"),
          @ApiResponse(responseCode = "404", description = "Model not found")
      }
  )
  public Response getModel(@PathParam("modelName") String modelName) throws IOException {
    hasAccess(RESOURCE);
    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if(modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE).build();
    }
    if (!modelStore.modelExists(modelName)) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    byte[] data = modelStore.loadModel(modelName);
    return Response.ok(data)
        .header("Content-Disposition", "attachment; filename=\"" + modelName + "\"")
        .build();
  }

  @HEAD
  @Path("/server/model/{modelName}")
  @Operation(
      summary = "Check if model exists",
      description = "Checks if a model with the given name exists.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Model exists"),
          @ApiResponse(responseCode = "404", description = "Model not found")
      }
  )
  public Response modelExists(@PathParam("modelName") String modelName) throws IOException {
    hasAccess(RESOURCE);
    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if(modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE).build();
    }

    if (modelStore.modelExists(modelName)) {
      return Response.ok().build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  @DELETE
  @Path("/server/model/{modelName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Delete model",
      description = "Deletes the model by name.",
      responses = @ApiResponse(
          responseCode = "200",
          description = "Model deleted successfully",
          content = @Content(schema = @Schema(implementation = StatusResponse.class))
      )
  )
  public StatusResponse deleteModel(@PathParam("modelName") String modelName) throws IOException {
    hasAccess(RESOURCE);
    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if(modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return new StatusResponse("Failure, ML not supported");
    }

    if (modelStore.modelExists(modelName)) {
      modelStore.deleteModel(modelName);
      return new StatusResponse("Success");
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new StatusResponse("Failure");
  }

  @GET
  @Path("/server/models")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "List all models",
      description = "Returns a list of all available model names.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "List of model names",
              content = @Content(schema = @Schema(implementation = String[].class))
          ),
          @ApiResponse(responseCode = "406", description = "ML not supported")
      }
  )
  public Response listModels() throws IOException {
    hasAccess(RESOURCE);
    ModelStore modelStore = MessageDaemon.getInstance().getSubSystemManager().getModelStore();
    if (modelStore == null) {
      response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
      return Response.status(HttpServletResponse.SC_NOT_ACCEPTABLE).build();
    }

    return Response.ok(modelStore.listModels()).build();
  }

}
