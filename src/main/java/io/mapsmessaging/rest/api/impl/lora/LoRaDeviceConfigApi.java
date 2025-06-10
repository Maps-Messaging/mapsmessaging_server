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

package io.mapsmessaging.rest.api.impl.lora;

import io.mapsmessaging.dto.rest.config.network.impl.LoRaConfigDTO;
import io.mapsmessaging.dto.rest.lora.LoRaDeviceConfigInfoDTO;
import io.mapsmessaging.network.io.impl.lora.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.LoRaDeviceManager;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.LoRaConfigListResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "LoRa Device Management")
@Path(URI_PATH)
public class LoRaDeviceConfigApi extends LoraBaseRestApi {

  @GET
  @Path("/device/lora/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve all LoRa device configurations",
      description = "Fetches a list of all configured LoRa devices and their settings.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoRaConfigListResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public LoRaConfigListResponse getAllLoRaDeviceConfigs() {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    List<LoRaDeviceConfigInfoDTO> deviceInfos = new ArrayList<>();
    for (LoRaDevice device : deviceManager.getDevices()) {
      deviceInfos.add(createInfo(device));
    }
    return new LoRaConfigListResponse(deviceInfos);
  }

  @GET
  @Path("/device/lora/{deviceName}/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve a specific LoRa device configuration",
      description = "Fetches the configuration for a specific LoRa device identified by its name.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoRaDeviceConfigInfoDTO.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Device not found"),
      }
  )
  public LoRaDeviceConfigInfoDTO getLoRaDeviceConfig(@PathParam("deviceName") String deviceName) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    LoRaDeviceConfigInfoDTO deviceInfo = null;
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup =
          deviceManager.getDevices().stream()
              .filter(device -> deviceName.equals(device.getName()))
              .collect(Collectors.toList());
      if (!lookup.isEmpty()) {
        deviceInfo = createInfo(lookup.get(0));
      }
    }

    if (deviceInfo == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return deviceInfo;
  }

  @POST
  @Path("/device/lora/config")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Add a new LoRa device configuration",
      description = "Creates a new LoRa device configuration and adds it to the system.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Device not found"),
      }
  )
  public BaseResponse addLoRaDeviceConfig(LoRaDeviceConfigInfoDTO newDevice) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    return new BaseResponse();
  }

  @DELETE
  @Path("/device/lora/{deviceName}/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete a specific LoRa device configuration",
      description = "Removes a LoRa device configuration identified by its unique ID.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
          @ApiResponse(responseCode = "404", description = "Device not found"),
      }
  )
  public StatusResponse deleteLoRaDeviceConfig(@PathParam("deviceName") String deviceName) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    LoRaDeviceConfigInfoDTO deviceInfo = null;
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup =
          deviceManager.getDevices().stream()
              .filter(device -> deviceName.equals(device.getName()))
              .collect(Collectors.toList());
      if (!lookup.isEmpty()) {
        deviceInfo = createInfo(lookup.get(0));
      }
    }

    if (deviceInfo == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return new StatusResponse("Failed");
    }
    return new StatusResponse("Success");
  }

  private LoRaDeviceConfigInfoDTO createInfo(LoRaDevice device) {
    LoRaDeviceConfigInfoDTO deviceInfo = new LoRaDeviceConfigInfoDTO();
    LoRaConfigDTO loRaDeviceConfig = device.getConfig();
    deviceInfo.setName(device.getName());
    deviceInfo.setPower(loRaDeviceConfig.getPower());
    deviceInfo.setFrequency(loRaDeviceConfig.getFrequency());
    return deviceInfo;
  }
}

