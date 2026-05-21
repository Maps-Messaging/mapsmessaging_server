/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commonsclause.com/
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

import io.mapsmessaging.config.network.impl.LoRaChipDeviceConfig;
import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.dto.rest.config.network.impl.LoRaConfigDTO;
import io.mapsmessaging.dto.rest.lora.LoRaDeviceInfoDTO;
import io.mapsmessaging.dto.rest.lora.LoRaEndPointConnectionInfoDTO;
import io.mapsmessaging.dto.rest.lora.LoRaEndPointInfoDTO;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.impl.lora.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.LoRaDeviceManager;
import io.mapsmessaging.network.io.impl.lora.LoRaEndPoint;
import io.mapsmessaging.network.io.impl.lora.device.LoRaChipDevice;
import io.mapsmessaging.network.io.impl.lora.serial.LoRaSerialDevice;
import io.mapsmessaging.network.io.impl.lora.stats.LoRaClientStats;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "LoRa Device Management")
@Path(URI_PATH + "/device/lora")
public class LoRaDeviceApi extends LoraBaseRestApi {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Retrieve all LoRa devices",
      description = "Fetches a list of all LoRa devices along with their configurations and statistics.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoRaDeviceInfoDTO[].class))
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
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getAllLoRaDevices() {
    hasAccess(RESOURCE);

    try {
      LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
      List<LoRaDeviceInfoDTO> deviceInfos = new ArrayList<>();
      for (LoRaDevice device : deviceManager.getDevices()) {
        deviceInfos.add(createInfo(device));
      }
      LoRaDeviceInfoDTO[] result = deviceInfos.toArray(new LoRaDeviceInfoDTO[0]);
      return Response.ok(result, MediaType.APPLICATION_JSON).build();
    } catch (Exception ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Failed to retrieve LoRa devices"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  @GET
  @Path("/{deviceName}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Retrieve a specific LoRa device",
      description = "Fetches the details of a specific LoRa device identified by its name.",
      parameters = {
          @Parameter(
              name = "deviceName",
              description = "LoRa device name",
              required = true,
              schema = @Schema(type = "string", minLength = 1)
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoRaDeviceInfoDTO.class))
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
              description = "LoRa device not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getLoRaDevice(@PathParam("deviceName") String deviceName) {
    hasAccess(RESOURCE);

    String normalizedDeviceName = normalizeRequiredValue(deviceName);
    if (normalizedDeviceName == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Device name must not be blank"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
      List<LoRaDevice> lookup =
          deviceManager.getDevices().stream()
              .filter(device -> normalizedDeviceName.equals(device.getName()))
              .toList();

      if (lookup.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("LoRa device not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }

      LoRaDeviceInfoDTO deviceInfo = createInfo(lookup.getFirst());
      return Response.ok(deviceInfo, MediaType.APPLICATION_JSON).build();
    } catch (Exception ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Failed to retrieve LoRa device"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  @GET
  @Path("/{deviceName}/{nodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Retrieve endpoint connections for a LoRa device",
      description = "Fetches the connection information for a specific endpoint of a LoRa device, identified by the device name and node ID.",
      parameters = {
          @Parameter(
              name = "deviceName",
              description = "LoRa device name",
              required = true,
              schema = @Schema(type = "string", minLength = 1)
          ),
          @Parameter(
              name = "nodeId",
              description = "LoRa endpoint node id",
              required = true,
              schema = @Schema(type = "integer", minimum = "0", example = "1")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Operation was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoRaEndPointConnectionInfoDTO[].class))
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
              description = "LoRa device or endpoint not found",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          )
      }
  )
  public Response getLoRaEndPointConnections(
      @PathParam("deviceName") String deviceName,
      @PathParam("nodeId") String nodeId
  ) {
    hasAccess(RESOURCE);

    String normalizedDeviceName = normalizeRequiredValue(deviceName);
    if (normalizedDeviceName == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Device name must not be blank"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    Integer parsedNodeId = parseRequiredInteger(nodeId);
    if (parsedNodeId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new StatusResponse("Node id must be a valid integer"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }

    try {
      LoRaDeviceManager deviceManager = LoRaDeviceManager.getInstance();
      List<LoRaDevice> lookup =
          deviceManager.getDevices().stream()
              .filter(device -> normalizedDeviceName.equals(device.getName()))
              .toList();

      if (lookup.isEmpty()) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("LoRa device not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }

      LoRaDevice device = lookup.getFirst();
      List<LoRaEndPointConnectionInfoDTO> infoList = new ArrayList<>();

      if (device instanceof LoRaChipDevice loraChipDevice) {
        LoRaEndPoint loRaEndPoint = loraChipDevice.getEndPoint(parsedNodeId);
        if (loRaEndPoint == null) {
          return Response.status(Response.Status.NOT_FOUND)
              .entity(new StatusResponse("LoRa endpoint not found"))
              .type(MediaType.APPLICATION_JSON)
              .build();
        }

        for (LoRaClientStats clientStats : loRaEndPoint.getStats()) {
          infoList.add(createConnectionInfo(clientStats));
        }
      } else {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new StatusResponse("LoRa endpoint not found"))
            .type(MediaType.APPLICATION_JSON)
            .build();
      }

      LoRaEndPointConnectionInfoDTO[] result = infoList.toArray(new LoRaEndPointConnectionInfoDTO[0]);
      return Response.ok(result, MediaType.APPLICATION_JSON).build();
    } catch (Exception ex) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new StatusResponse("Failed to retrieve LoRa endpoint connections"))
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
  }

  private String normalizeRequiredValue(String value) {
    if (value == null) {
      return null;
    }
    String trimmedValue = value.trim();
    if (trimmedValue.isEmpty()) {
      return null;
    }
    return trimmedValue;
  }

  private Integer parseRequiredInteger(String value) {
    if (value == null) {
      return null;
    }
    String trimmedValue = value.trim();
    if (trimmedValue.isEmpty()) {
      return null;
    }
    try {
      return Integer.parseInt(trimmedValue);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private LoRaDeviceInfoDTO createInfo(LoRaDevice device) {
    LoRaDeviceInfoDTO deviceInfo = new LoRaDeviceInfoDTO();
    LoRaConfigDTO loRaDeviceConfig = device.getConfig();
    List<LoRaEndPointInfoDTO> endPointInfoList = new ArrayList<>();
    deviceInfo.setName(device.getName());

    if (loRaDeviceConfig instanceof LoRaChipDeviceConfig loraChipDevice) {
      deviceInfo.setRadio(loraChipDevice.getRadio());
      deviceInfo.setBytesReceived(device.getBytesReceived().sum());
      deviceInfo.setBytesSent(device.getBytesSent().sum());
      deviceInfo.setPacketsReceived(device.getPacketsReceived().sum());
      deviceInfo.setPacketsSent(device.getPacketsSent().sum());
      for (LoRaEndPoint endPoint : ((LoRaChipDevice) device).getEndPoints()) {
        endPointInfoList.add(createEndPointInfo(endPoint));
      }
    } else if (loRaDeviceConfig instanceof LoRaSerialDeviceConfig) {
      deviceInfo.setRadio("Serial");
      LoRaSerialDevice serialDevice = (LoRaSerialDevice) device;
      LoRaProtocol loRaProtocol = serialDevice.getActiveProtocol();
      if (loRaProtocol != null) {
        EndPointServerStatus endPointStatus = loRaProtocol.getEndPoint().getServer();
        deviceInfo.setBytesReceived(endPointStatus.getTotalBytesRead());
        deviceInfo.setBytesSent(endPointStatus.getTotalBytesSent());
        deviceInfo.setPacketsReceived(endPointStatus.getTotalPacketsRead());
        deviceInfo.setPacketsSent(endPointStatus.getTotalPacketsSent());

        for (LoRaClientStats status : loRaProtocol.getClientStats()) {
          endPointInfoList.add(createStatsInfo(status));
        }
      }
    }

    deviceInfo.setEndPointInfoList(endPointInfoList);
    return deviceInfo;
  }

  private LoRaEndPointInfoDTO createStatsInfo(LoRaClientStats stats) {
    LoRaEndPointInfoDTO endPointInfo = new LoRaEndPointInfoDTO();
    endPointInfo.setLastRSSI((int) stats.getRssi());
    endPointInfo.setNodeId((int) stats.getNodeId());
    endPointInfo.setLastRead(stats.getLastReadTime());
    endPointInfo.setLastWrite(stats.getLastWriteTime());
    return endPointInfo;
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
