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

package io.mapsmessaging.rest.api.impl.hardware;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.dto.rest.devices.DeviceInfoDTO;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.rest.responses.DeviceList;
import io.mapsmessaging.rest.responses.DeviceScanList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Hardware Management")
@Path(URI_PATH)
public class HardwareManagementApi extends HardwareBaseRestApi {

  @GET
  @Path("/server/hardware/scan")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Scan for new hardware",
      description = "Requests a scan to detect new hardware on I2C bus or configured devices. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Scan for devices was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceScanList.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DeviceScanList scanForDevices() throws InterruptedException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "");
    DeviceScanList cachedResponse = getFromCache(key, DeviceScanList.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    DeviceManager deviceManager = MessageDaemon.getInstance().getSubSystemManager().getDeviceManager();
    if (deviceManager != null) {
      DeviceScanList deviceScanList = new DeviceScanList(deviceManager.scan());
      putToCache(key, deviceScanList);
      return deviceScanList;
    }
    return new DeviceScanList(new ArrayList<>());
  }

  @GET
  @Path("/server/hardware")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get known devices",
      description = "Retreive a list of all detected devices currently online. Requires authentication if enabled in the configuration.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get all discovered devices was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceList.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public DeviceList getAllDiscoveredDevices() throws IOException {
    hasAccess(RESOURCE);
    CacheKey key = new CacheKey(uriInfo.getPath(), "");
    DeviceList cachedResponse = getFromCache(key, DeviceList.class);
    if (cachedResponse != null) {
      return cachedResponse;
    }
    List<DeviceInfoDTO> devices = new ArrayList<>();
    DeviceManager deviceManager = MessageDaemon.getInstance().getSubSystemManager().getDeviceManager();
    if (deviceManager != null){
      List<DeviceController> activeDevices = deviceManager.getActiveDevices();
      for (DeviceController device : activeDevices) {
        DeviceInfoDTO deviceInfo = new DeviceInfoDTO();
        deviceInfo.setName(device.getName());
        deviceInfo.setType(device.getType().name());
        deviceInfo.setDescription(device.getDescription());
        deviceInfo.setState(new String(device.getDeviceState()));
        devices.add(deviceInfo);
      }
    }
    DeviceList dl = new DeviceList(devices);
    putToCache(key, dl);
    return dl;
  }

}
