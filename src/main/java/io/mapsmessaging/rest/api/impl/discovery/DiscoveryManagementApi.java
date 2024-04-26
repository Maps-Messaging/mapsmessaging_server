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

package io.mapsmessaging.rest.api.impl.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.discovery.DiscoveredServers;
import io.mapsmessaging.rest.data.discovery.ServiceData;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import javax.jmdns.ServiceInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@Tag(name = "Discovery Management")
@Path(URI_PATH)
public class DiscoveryManagementApi extends BaseRestApi {

  @GET
  @Path("/server/discovery/start")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public void startDiscovery() {
    if (!hasAccess("discovery")) {
      response.setStatus(403);
      return;
    }
    MessageDaemon.getInstance().getDiscoveryManager().start();
  }

  @GET
  @Path("/server/discovery/stop")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public void stopDiscovery() {
    if (!hasAccess("discovery")) {
      response.setStatus(403);
      return;
    }
    MessageDaemon.getInstance().getDiscoveryManager().stop();
  }

  @GET
  @Path("/server/discovery")
  @Produces({MediaType.APPLICATION_JSON})
  //@ApiOperation(value = "Get the specific destination details")
  public List<DiscoveredServers> getAllDiscoveredServers() {
    if (!hasAccess("discovery")) {
      response.setStatus(403);
      return new ArrayList<>();
    }

    List<DiscoveredServers> discoveredServers = new ArrayList<>();
    Map<String, List<ServiceInfo>> discovered = MessageDaemon.getInstance().getServerConnectionManager().getServiceInfoMap();
    for (Map.Entry<String, List<ServiceInfo>> entry : discovered.entrySet()) {
      List<ServiceData> discoveredServiceData = new ArrayList<>();
      for (ServiceInfo serviceInfo : entry.getValue()) {
        discoveredServiceData.add(new ServiceData(serviceInfo));
      }
      discoveredServers.add(new DiscoveredServers(entry.getKey(), discoveredServiceData));
    }
    return discoveredServers;
  }
}
