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
 *
 */

package io.mapsmessaging.rest.api.impl.hardware;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.dto.rest.devices.DeviceInfoDTO;
import io.mapsmessaging.hardware.DeviceManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "Hardware Management")
@Path(URI_PATH)
public class HardwareManagementApi extends HardwareBaseRestApi {

  @GET
  @Path("/server/hardware/scan")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Scan for new hardware",
      description = "Requests a scan to detect new hardware on I2C bus or configured devices. Requires authentication if enabled in the configuration."
  )
  public List<String> scanForDevices() throws InterruptedException {
    hasAccess(RESOURCE);
    return MessageDaemon.getInstance().getSubSystemManager().getDeviceManager().scan();
  }

  @GET
  @Path("/server/hardware")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get known devices",
      description = "Retreive a list of all detected devices currently online. Requires authentication if enabled in the configuration."
  )
  public List<DeviceInfoDTO> getAllDiscoveredDevices() throws IOException {
    hasAccess(RESOURCE);
    List<DeviceInfoDTO> devices = new ArrayList<>();
    DeviceManager deviceManager =
        MessageDaemon.getInstance().getSubSystemManager().getDeviceManager();
    List<DeviceController> activeDevices = deviceManager.getActiveDevices();
    for (DeviceController device : activeDevices) {
      DeviceInfoDTO deviceInfo = new DeviceInfoDTO();
      deviceInfo.setName(device.getName());
      deviceInfo.setType(device.getType().name());
      deviceInfo.setDescription(device.getDescription());
      deviceInfo.setState(new String(device.getDeviceState()));
      devices.add(deviceInfo);
    }
    return devices;
  }
}
