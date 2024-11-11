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

package io.mapsmessaging.rest.api.impl.hardware;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.dto.rest.devices.DeviceInfoDTO;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
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
public class HardwareManagementApi extends BaseRestApi {

  @GET
  @Path("/server/hardware/scan")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public List<String> scanForDevices() throws InterruptedException {
    if (!hasAccess("hardware")) {
      response.setStatus(403);
      return new ArrayList<>();
    }
    return MessageDaemon.getInstance().getDeviceManager().scan();
  }

  @GET
  @Path("/server/hardware")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public List<DeviceInfoDTO> getAllDiscoveredDevices() throws IOException {
    if (!hasAccess("hardware")) {
      response.setStatus(403);
      return new ArrayList<>();
    }
    List<DeviceInfoDTO> devices = new ArrayList<>();
    DeviceManager deviceManager = MessageDaemon.getInstance().getDeviceManager();
    List<DeviceController> activeDevices = deviceManager.getActiveDevices();
    for(DeviceController device : activeDevices) {
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
