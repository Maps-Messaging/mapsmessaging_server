package io.mapsmessaging.rest.api.impl.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.discovery.AdapterManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.data.destination.Destination;
import io.mapsmessaging.rest.responses.DestinationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.jmdns.ServiceInfo;

import java.util.*;

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

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class DiscoveredServers {
    private String server;
    private List<ServiceData> serviceInfo;
  }

  @Getter
  @Setter
  public static final class ServiceData {

    private String application;
    private String name;
    private String domain;
    private String[] hostAddresses;
    private int port;
    private Map<String, String> properties;

    public ServiceData(ServiceInfo serviceInfo) {
      hostAddresses = serviceInfo.getHostAddresses();
      port = serviceInfo.getPort();
      name = serviceInfo.getName();
      application = serviceInfo.getApplication();
      domain = serviceInfo.getDomain();
      properties = new LinkedHashMap<>();
      Enumeration<String> propertyNames = serviceInfo.getPropertyNames();
      while (propertyNames.hasMoreElements()) {
        String name = propertyNames.nextElement();
        properties.put(name, serviceInfo.getPropertyString(name));
      }
    }
  }
}
