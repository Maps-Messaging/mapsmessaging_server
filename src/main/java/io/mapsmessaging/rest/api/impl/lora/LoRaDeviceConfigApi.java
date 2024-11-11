/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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
 *
 */

package io.mapsmessaging.rest.api.impl.lora;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.config.lora.LoRaDeviceConfig;
import io.mapsmessaging.dto.rest.lora.LoRaDeviceConfigInfoDTO;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDeviceManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.BaseResponse;
import io.mapsmessaging.rest.responses.LoRaConfigListResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

@Tag(name = "LoRa Device Management")
@Path(URI_PATH)
public class LoRaDeviceConfigApi extends BaseRestApi {

  @GET
  @Path("/device/lora/config")
  @Produces({MediaType.APPLICATION_JSON})
  public LoRaConfigListResponse getAllLoRaDeviceConfigs() {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    List<LoRaDeviceConfigInfoDTO> deviceInfos = new ArrayList<>();
    for (LoRaDevice device : deviceManager.getDevices()) {
      deviceInfos.add(createInfo(device));
    }
    return new LoRaConfigListResponse(request, deviceInfos);
  }

  @GET
  @Path("/device/lora/{deviceName}/config")
  @Produces({MediaType.APPLICATION_JSON})
  public LoRaDeviceConfigInfoDTO getLoRaDeviceConfig(@PathParam("deviceName") String deviceName) {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    LoRaDeviceConfigInfoDTO deviceConfig = null;
    LoRaDeviceConfigInfoDTO deviceInfo = new LoRaDeviceConfigInfoDTO();
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup = deviceManager.getDevices().stream()
          .filter(device -> deviceName.equals(device.getName()))
          .collect(Collectors.toList());
      if (!lookup.isEmpty()) {
        deviceInfo = createInfo(lookup.get(0));
      }
    }

    if (deviceConfig == null) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    return deviceInfo;
  }

  @POST
  @Path("/device/lora/config")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse addLoRaDeviceConfig(LoRaDeviceConfigInfoDTO newDevice) {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    return new BaseResponse(request);
//    response.setStatus(500);
    //  return new BaseResponse(request);
  }

  @DELETE
  @Path("/device/lora/{deviceId}/config")
  @Produces({MediaType.APPLICATION_JSON})
  public BaseResponse deleteLoRaDeviceConfig(@PathParam("deviceId") String deviceId) {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
//    if (deviceManager.deleteDevice(UUID.fromString(deviceId)))
    return new BaseResponse(request);
//    }
//    response.setStatus(500);
//    return new BaseResponse(request);
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
