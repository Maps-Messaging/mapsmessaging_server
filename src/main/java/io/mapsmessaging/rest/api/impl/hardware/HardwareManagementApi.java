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

package io.mapsmessaging.rest.api.impl.hardware;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.dto.rest.devices.DeviceInfoDTO;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Hardware Management")
@Path(URI_PATH + "/server/hardware")
public class HardwareManagementApi extends HardwareBaseRestApi {

  @POST
  @Path("/scan")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Scan for new hardware",
      description = "Requests a scan to detect new hardware on I2C bus or configured devices. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Scan for devices was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = String[].class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response scanForDevices() {
    hasAccess(RESOURCE);

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), "");

    String[] cachedResponse = getFromCache(cacheKey, String[].class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    DeviceManager deviceManager;
    try {
      deviceManager = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getDeviceManager();
    } catch (RuntimeException ex) {
      return internalServerError("Unable to resolve device manager");
    }

    if (deviceManager == null) {
      String[] emptyResponse = new String[0];
      putToCache(cacheKey, emptyResponse);
      return ok(emptyResponse);
    }

    List<String> scanResults;
    try {
      scanResults = deviceManager.scan();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return internalServerError("Device scan interrupted");
    } catch (RuntimeException ex) {
      return internalServerError("Device scan failed");
    }

    String[] response = scanResults == null ? new String[0] : scanResults.toArray(new String[0]);
    putToCache(cacheKey, response);
    return ok(response);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get known devices",
      description = "Retrieve a list of all detected devices currently online. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all discovered devices was successful",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DeviceInfoDTO[].class)
              )
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Invalid credentials or unauthorized access",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "403",
              description = "User is not authorised to access the resource",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = StatusResponse.class)
              )
          )
      }
  )
  public Response getAllDiscoveredDevices() throws IOException {
    hasAccess(RESOURCE);

    CacheKey cacheKey = new CacheKey(uriInfo.getPath(), "");

    DeviceInfoDTO[] cachedResponse = getFromCache(cacheKey, DeviceInfoDTO[].class);
    if (cachedResponse != null) {
      return ok(cachedResponse);
    }

    DeviceManager deviceManager;
    try {
      deviceManager = MessageDaemon.getInstance()
          .getSubSystemManager()
          .getDeviceManager();
    } catch (RuntimeException ex) {
      return internalServerError("Unable to resolve device manager");
    }

    List<DeviceInfoDTO> devices = new ArrayList<>();

    if (deviceManager != null && deviceManager.isEnabled()) {
      List<DeviceController> activeDevices;
      try {
        activeDevices = deviceManager.getActiveDevices();
      } catch (RuntimeException ex) {
        return internalServerError("Failed to resolve active devices");
      }

      if (activeDevices != null) {
        for (DeviceController deviceController : activeDevices) {
          if (deviceController == null) {
            continue;
          }

          DeviceInfoDTO deviceInfo = new DeviceInfoDTO();
          deviceInfo.setName(deviceController.getName());
          deviceInfo.setType(deviceController.getType() == null ? null : deviceController.getType().name());
          deviceInfo.setDescription(deviceController.getDescription());

          byte[] stateBytes = deviceController.getDeviceState();
          if (stateBytes != null) {
            deviceInfo.setState(new String(stateBytes));
          } else {
            deviceInfo.setState(null);
          }

          devices.add(deviceInfo);
        }
      }
    }

    DeviceInfoDTO[] response = devices.toArray(new DeviceInfoDTO[0]);
    putToCache(cacheKey, response);
    return ok(response);
  }

}
