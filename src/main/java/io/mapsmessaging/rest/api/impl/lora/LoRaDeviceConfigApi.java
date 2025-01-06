/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.rest.api.impl.lora;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.config.lora.LoRaDeviceConfig;
import io.mapsmessaging.dto.rest.lora.LoRaDeviceConfigInfoDTO;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDeviceManager;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.LoRaConfigListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

@Tag(name = "LoRa Device Management")
@Path(URI_PATH)
public class LoRaDeviceConfigApi extends LoraBaseRestApi {

  @GET
  @Path("/device/lora/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all LoRa device configs",
      description = "Retrieves all known LoRa device configurations. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "LoRa device configs returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
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
      summary = "Get device config by name",
      description = "Retrieve the device configuration for a specified LoRa device. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "LoRa device config returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Device not found")
  @ApiResponse(responseCode = "500", description = "Server error")
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
      summary = "Add new LoRa device config",
      description = "Adds a new LoRa device configuration. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "LoRa device config added")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public BaseResponse addLoRaDeviceConfig(LoRaDeviceConfigInfoDTO newDevice) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    return new BaseResponse();
    // Logic for adding device config can be implemented here
    // If any error occurs, set response status accordingly
  }

  @DELETE
  @Path("/device/lora/{deviceId}/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Delete device config",
      description = "Deletes a LoRa device configuration by device ID. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "LoRa device config removed")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Device not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public BaseResponse deleteLoRaDeviceConfig(@PathParam("deviceId") String deviceId) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    return new BaseResponse();
    // Logic for deleting device config can be implemented here
    // If any error occurs, set response status accordingly
  }

  private LoRaDeviceConfigInfoDTO createInfo(LoRaDevice device) {
    LoRaDeviceConfigInfoDTO deviceInfo = new LoRaDeviceConfigInfoDTO();
    LoRaDeviceConfig loRaDeviceConfig = device.getConfig();
    deviceInfo.setName(device.getName());
    deviceInfo.setCs(loRaDeviceConfig.getCs());
    deviceInfo.setIrq(loRaDeviceConfig.getIrq());
    deviceInfo.setCadTimeout(loRaDeviceConfig.getCadTimeout());
    deviceInfo.setPower(loRaDeviceConfig.getPower());
    deviceInfo.setFrequency(loRaDeviceConfig.getFrequency());
    deviceInfo.setRst(loRaDeviceConfig.getRst());
    deviceInfo.setRadio(loRaDeviceConfig.getRadio());
    return deviceInfo;
  }
}
