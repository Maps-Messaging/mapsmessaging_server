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

import io.mapsmessaging.config.DeviceManagerConfig;
import io.mapsmessaging.dto.rest.config.DeviceManagerConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Hardware Management")
@Path(URI_PATH)
public class HardwareConfigurationApi extends HardwareBaseRestApi {

  @GET
  @Path("/server/hardware/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Get hardware configuration",
      description = "Retrieve the configuration for the hardware sub-system. Requires authentication if enabled in the configuration."
  )
  public DeviceManagerConfigDTO getConfig() {
    hasAccess(RESOURCE);
    return DeviceManagerConfig.getInstance();
  }

  @POST
  @Path("/server/hardware/config")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Update hardware configuration",
      description = "Update the configuration for the hardware sub-system. Requires authentication if enabled in the configuration."
  )
  public boolean updateConfig(DeviceManagerConfigDTO update) throws IOException {
    hasAccess(RESOURCE);
    if (DeviceManagerConfig.getInstance().update(update)) {
      DeviceManagerConfig.getInstance().save();
      return true;
    }
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    return false;
  }
}
