/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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
import io.mapsmessaging.network.io.impl.lora.LoRaEndPoint;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDeviceManager;
import io.mapsmessaging.network.io.impl.lora.stats.LoRaClientStats;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.lora.LoRaDeviceInfo;
import io.mapsmessaging.rest.data.lora.LoRaEndPointConnectionInfo;
import io.mapsmessaging.rest.data.lora.LoRaEndPointInfo;
import io.mapsmessaging.rest.data.lora.LoRaListResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

@Tag(name = "LoRa Device Management")
@Path(URI_PATH)
public class LoRaDeviceApi extends BaseRestApi {

  @GET
  @Path("/device/lora")
  @Produces({MediaType.APPLICATION_JSON})
  public LoRaListResponse getAllLoRaDevices() {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    List<LoRaDeviceInfo> deviceInfos = new ArrayList<>();
    for(LoRaDevice device : deviceManager.getDevices()) {
      deviceInfos.add(createInfo(device));
    }
    return new LoRaListResponse(request, deviceInfos);
  }

  @GET
  @Path("/device/lora/{deviceName}")
  @Produces({MediaType.APPLICATION_JSON})
  public LoRaDeviceInfo getLoRaDevice(@PathParam("deviceName") String deviceName) {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    LoRaDeviceInfo deviceInfo = new LoRaDeviceInfo();
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup = deviceManager.getDevices().stream()
          .filter(device -> deviceName.equals(device.getName()))
          .collect(Collectors.toList());
      if(!lookup.isEmpty()) {
        return createInfo(lookup.get(0));
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return deviceInfo;
  }


  @GET
  @Path("/device/lora/{deviceName}/{nodeId}")
  @Produces({MediaType.APPLICATION_JSON})
  public List<LoRaEndPointConnectionInfo> getLoRaEndPointConnections(
      @PathParam("deviceName") String deviceName,
      @PathParam("nodeId") String nodeId
  ) {
    checkAuthentication();
    LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
    int parsedInt = Integer.parseInt(nodeId);
    if (deviceName != null && !deviceName.isEmpty()) {
      List<LoRaDevice> lookup = deviceManager.getDevices().stream()
          .filter(device -> deviceName.equals(device.getName()))
          .collect(Collectors.toList());
      if(!lookup.isEmpty()) {
        LoRaDevice device = lookup.get(0);
        List<LoRaEndPointConnectionInfo> infoList = new ArrayList<>();
        LoRaEndPoint loRaEndPoint = device.getEndPoint(parsedInt);
        for(LoRaClientStats clientStats : loRaEndPoint.getStats()) {
          infoList.add(createConnectionInfo(clientStats));
        }
        return infoList;
      }
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return new ArrayList<>();
  }

  private LoRaDeviceInfo createInfo(LoRaDevice device) {
    LoRaDeviceInfo deviceInfo = new LoRaDeviceInfo();
    LoRaDeviceConfig loRaDeviceConfig = device.getConfig();
    deviceInfo.setName(device.getName());
    deviceInfo.setRadio(loRaDeviceConfig.getRadio());
    deviceInfo.setBytesReceived(device.getBytesReceived().sum());
    deviceInfo.setBytesSent(device.getBytesSent().sum());
    deviceInfo.setPacketsReceived(device.getPacketsReceived().sum());
    deviceInfo.setPacketsSent(device.getPacketsSent().sum());
    List<LoRaEndPointInfo> endPointInfoList = new ArrayList<>();
    for(LoRaEndPoint endPoint: device.getEndPoints()){
      endPointInfoList.add(createEndPointInfo(endPoint));
    }
    deviceInfo.setEndPointInfoList(endPointInfoList);
    return deviceInfo;
  }

  private LoRaEndPointInfo createEndPointInfo(LoRaEndPoint endPoint) {
    LoRaEndPointInfo endPointInfo = new LoRaEndPointInfo();
    endPointInfo.setLastRSSI(endPoint.getRSSI());
    endPointInfo.setNodeId(endPoint.getNodeId());
    endPointInfo.setIncomingQueueSize(endPoint.getIncomingQueueSize());
    endPointInfo.setConnectionSize(endPoint.getConnectionSize());
    return endPointInfo;
  }

  private LoRaEndPointConnectionInfo createConnectionInfo(LoRaClientStats clientStats) {
    LoRaEndPointConnectionInfo connectionInfo = new LoRaEndPointConnectionInfo();
    connectionInfo.setLastWriteTime(clientStats.getLastWriteTime());
    connectionInfo.setLastReadTime(clientStats.getLastReadTime());
    connectionInfo.setLastPacketId(clientStats.getLastPacketId());
    connectionInfo.setRSSI(clientStats.getRssi());
    connectionInfo.setMissedPackets(clientStats.getMissed());
    connectionInfo.setReceivedPackets(clientStats.getReceived());
    connectionInfo.setRemoteNodeId((int)clientStats.getNodeId());
    return connectionInfo;
  }
}
