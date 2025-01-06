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
import io.mapsmessaging.dto.rest.lora.LoRaDeviceInfoDTO;
import io.mapsmessaging.dto.rest.lora.LoRaEndPointConnectionInfoDTO;
import io.mapsmessaging.dto.rest.lora.LoRaEndPointInfoDTO;
import io.mapsmessaging.network.io.impl.lora.LoRaEndPoint;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDeviceManager;
import io.mapsmessaging.network.io.impl.lora.stats.LoRaClientStats;
import io.mapsmessaging.rest.responses.LoRaListResponse;
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
public class LoRaDeviceApi extends LoraBaseRestApi {

  @GET
  @Path("/device/lora")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get all LoRa devices",
      description = "Returns a list of all known LoRa devices. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "LoRa devices returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "500", description = "Server error")
  public LoRaListResponse getAllLoRaDevices() {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    List<LoRaDeviceInfoDTO> deviceInfos = new ArrayList<>();
    for (LoRaDevice device : deviceManager.getDevices()) {
      deviceInfos.add(createInfo(device));
    }
    return new LoRaListResponse(deviceInfos);
  }

  @GET
  @Path("/device/lora/{deviceName}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get LoRa device by name",
      description = "Returns information about the specified LoRa device. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "LoRa device returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Device not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public LoRaDeviceInfoDTO getLoRaDevice(@PathParam("deviceName") String deviceName) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    LoRaDeviceInfoDTO deviceInfo = new LoRaDeviceInfoDTO();
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup =
          deviceManager.getDevices().stream()
              .filter(device -> deviceName.equals(device.getName()))
              .collect(Collectors.toList());
      if (!lookup.isEmpty()) {
        return createInfo(lookup.get(0));
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return deviceInfo;
  }

  @GET
  @Path("/device/lora/{deviceName}/{nodeId}")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get LoRa endpoint connections",
      description = "Returns connection info for the specified node on the specified LoRa device. Requires authentication if enabled."
  )
  @ApiResponse(responseCode = "200", description = "Endpoint connections returned")
  @ApiResponse(responseCode = "401", description = "Unauthorized")
  @ApiResponse(responseCode = "404", description = "Device or node not found")
  @ApiResponse(responseCode = "500", description = "Server error")
  public List<LoRaEndPointConnectionInfoDTO> getLoRaEndPointConnections(
      @PathParam("deviceName") String deviceName,
      @PathParam("nodeId") String nodeId
  ) {
    hasAccess(RESOURCE);
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    int parsedInt = Integer.parseInt(nodeId);
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup =
          deviceManager.getDevices().stream()
              .filter(device -> deviceName.equals(device.getName()))
              .collect(Collectors.toList());
      if (!lookup.isEmpty()) {
        LoRaDevice device = lookup.get(0);
        List<LoRaEndPointConnectionInfoDTO> infoList = new ArrayList<>();
        LoRaEndPoint loRaEndPoint = device.getEndPoint(parsedInt);
        for (LoRaClientStats clientStats : loRaEndPoint.getStats()) {
          infoList.add(createConnectionInfo(clientStats));
        }
        return infoList;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new ArrayList<>();
  }

  private LoRaDeviceInfoDTO createInfo(LoRaDevice device) {
    LoRaDeviceInfoDTO deviceInfo = new LoRaDeviceInfoDTO();
    LoRaDeviceConfig loRaDeviceConfig = device.getConfig();
    deviceInfo.setName(device.getName());
    deviceInfo.setRadio(loRaDeviceConfig.getRadio());
    deviceInfo.setBytesReceived(device.getBytesReceived().sum());
    deviceInfo.setBytesSent(device.getBytesSent().sum());
    deviceInfo.setPacketsReceived(device.getPacketsReceived().sum());
    deviceInfo.setPacketsSent(device.getPacketsSent().sum());
    List<LoRaEndPointInfoDTO> endPointInfoList = new ArrayList<>();
    for (LoRaEndPoint endPoint : device.getEndPoints()) {
      endPointInfoList.add(createEndPointInfo(endPoint));
    }
    deviceInfo.setEndPointInfoList(endPointInfoList);
    return deviceInfo;
  }

  private LoRaEndPointInfoDTO createEndPointInfo(LoRaEndPoint endPoint) {
    LoRaEndPointInfoDTO endPointInfo = new LoRaEndPointInfoDTO();
    endPointInfo.setLastRSSI(endPoint.getRSSI());
    endPointInfo.setNodeId(endPoint.getNodeId());
    endPointInfo.setIncomingQueueSize(endPoint.getIncomingQueueSize());
    endPointInfo.setConnectionSize(endPoint.getConnectionSize());
    return endPointInfo;
  }

  private LoRaEndPointConnectionInfoDTO createConnectionInfo(LoRaClientStats clientStats) {
    LoRaEndPointConnectionInfoDTO connectionInfo = new LoRaEndPointConnectionInfoDTO();
    connectionInfo.setLastWriteTime(clientStats.getLastWriteTime());
    connectionInfo.setLastReadTime(clientStats.getLastReadTime());
    connectionInfo.setLastPacketId(clientStats.getLastPacketId());
    connectionInfo.setRssi(clientStats.getRssi());
    connectionInfo.setMissedPackets(clientStats.getMissed());
    connectionInfo.setReceivedPackets(clientStats.getReceived());
    connectionInfo.setRemoteNodeId((int) clientStats.getNodeId());
    return connectionInfo;
  }
}
