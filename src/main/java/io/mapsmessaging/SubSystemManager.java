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

package io.mapsmessaging;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.configuration.EnvironmentConfig;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.SecurityManager;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.engine.system.SystemTopicManager;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.NetworkConnectionManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.network.discovery.DiscoveryManager;
import io.mapsmessaging.network.discovery.ServerConnectionManager;
import io.mapsmessaging.network.monitor.NetworkInterfaceMonitor;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.rest.RestApiServerManager;
import io.mapsmessaging.rest.jolokia.JolokaManager;
import io.mapsmessaging.routing.RoutingManager;
import io.mapsmessaging.selector.model.ModelStore;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.AgentOrder;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class SubSystemManager {
  private final Logger logger = LoggerFactory.getLogger(SubSystemManager.class);

  private final Map<String, AgentOrder> agentMap;
  private final boolean enableSystemTopics;
  private final boolean enableDeviceIntegration;
  private final String uniqueId;
  private final int sessionPipeLines;
  private final FeatureManager featureManager;
  @Getter
  @Setter
  private ModelStore modelStore;

  public SubSystemManager(String uniqueId, boolean enableSystemTopics, boolean enableDeviceIntegration, int sessionPipeLines, FeatureManager featureManager) {
    agentMap = new LinkedHashMap<>();
    this.uniqueId = uniqueId;
    this.enableDeviceIntegration = enableDeviceIntegration;
    this.enableSystemTopics = enableSystemTopics;
    this.sessionPipeLines = sessionPipeLines;
    this.featureManager = featureManager;
  }

  public void start() throws IOException{
    loadProtocolImplementations();
    createAgentStartStopList();
    List<AgentOrder> startList = new ArrayList<>(agentMap.values());
    startList.sort(Comparator.comparingInt(AgentOrder::getStartOrder));
    for (AgentOrder agent : startList) {
      long start = System.currentTimeMillis();
      logger.log(MESSAGE_DAEMON_AGENT_STARTING, agent.getAgent().getName());
      agent.getAgent().start();
      logger.log(MESSAGE_DAEMON_AGENT_STARTED, agent.getAgent().getName(), (System.currentTimeMillis() - start));
    }
    logServiceManagers();
  }

  private void loadProtocolImplementations() {
    logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, "Protocol Manager");
    ServiceLoader<ProtocolImplFactory> protocolServiceLoader = ServiceLoader.load(ProtocolImplFactory.class);
    List<ProtocolImplFactory> service = new CopyOnWriteArrayList<>();
    for (Iterator<ProtocolImplFactory> iterator = protocolServiceLoader.iterator(); iterator.hasNext(); ) {
      try {
        ProtocolImplFactory parser = iterator.next();
        String name = parser.getName().toLowerCase();
        name = name.replace("-", "_");
        name = name.replace(" ", "_");
        if(name.equals("loop") || featureManager.isEnabled("protocols."+name) ) {
          service.add(parser);
        }
      } catch (ServiceConfigurationError e) {
        logger.log(ServerLogMessages.MESSAGE_DAEMON_PROTOCOL_NOT_AVAILABLE, e);
      }
    }
    ProtocolFactory.setProtocolServiceList(service);
  }

  public void stop(){
    List<AgentOrder> startList = new ArrayList<>(agentMap.values());
    startList.sort(Comparator.comparingInt(AgentOrder::getStopOrder));
    for (AgentOrder agent : startList) {
      long start = System.currentTimeMillis();
      logger.log(MESSAGE_DAEMON_AGENT_STOPPING, agent.getAgent().getName());
      agent.getAgent().stop();
      logger.log(MESSAGE_DAEMON_AGENT_STOPPED, agent.getAgent().getName(), (System.currentTimeMillis() - start));
    }
  }

  private void addToMap(int start, int stop, Agent agent) {
    agentMap.put(agent.getName(), new AgentOrder(start, stop, agent));
  }


  public DiscoveryManager getDiscoveryManager(){
    return (DiscoveryManager) agentMap.get("Discovery Manager").getAgent();
  }

  public ServerConnectionManager getServerConnectionManager() {
    return (ServerConnectionManager) agentMap.get("Server Connection Manager").getAgent();
  }

  public NetworkManager getNetworkManager() {
    return (NetworkManager) agentMap.get("Network Manager").getAgent();
  }

  public NetworkConnectionManager getNetworkConnectionManager(){
    return (NetworkConnectionManager) agentMap.get("Network Connection Manager").getAgent();
  }
  public DestinationManager getDestinationManager() {
    return (DestinationManager) agentMap.get("Destination Manager").getAgent();
  }

  public SessionManager getSessionManager() {
    return (SessionManager) agentMap.get("Session Manager").getAgent();
  }

  public DeviceManager getDeviceManager() {
    AgentOrder order = agentMap.get("Device Manager");
    if(order != null) {
      return (DeviceManager) order.getAgent();
    }
    return null;
  }
  public RestApiServerManager getRestApiServerManager() {
    AgentOrder order = agentMap.get("Rest API Manager");
    if(order != null) {
      return (RestApiServerManager) order.getAgent();
    }
    return null;
  }


  /**
   * Creates a list of agents to start and stop in the MessageDaemon.
   * The method initializes and adds various agents to the agentMap, which is used to manage the start and stop order of the agents.
   * The agents added to the agentMap include:
   * - AuthManager
   * - SchemaManager
   * - NetworkInterfaceMonitor
   * - TransactionManager
   * - DiscoveryManager
   * - SecurityManager
   * - DestinationManager
   * - SessionManager
   * - NetworkManager
   * - NetworkConnectionManager
   * - RestApiServerManager
   * - ServerConnectionManager
   * - RoutingManager
   * - JolokaManager
   *
   * The method also adds optional modules to the agentMap based on the values of enableSystemTopics and enableDeviceIntegration flags.
   * If enableSystemTopics is true, a SystemTopicManager is added to the agentMap.
   * If enableDeviceIntegration is true, a DeviceManager is created and added to the agentMap.
   *
   * @throws IOException if an I/O error occurs
   */
  private void createAgentStartStopList() throws IOException {
    // Start the Schema manager to it has the defaults and has loaded the required classes
    io.mapsmessaging.engine.session.SecurityManager securityManager = new SecurityManager();
    DestinationManager destinationManager = new DestinationManager(featureManager);
    TransformationManager.getInstance();

    addToMap(10, 2000, AuthManager.getInstance());
    addToMap(50, 1100, SchemaManager.getInstance());
    addToMap(80, 20, NetworkInterfaceMonitor.getInstance());
    addToMap(100, 900, TransactionManager.getInstance());
    addToMap(300, 11, new DiscoveryManager(uniqueId));
    addToMap(400, 1200, securityManager);
    addToMap(500, 950, destinationManager);
    addToMap(600, 300, new SessionManager(securityManager, destinationManager, EnvironmentConfig.getInstance().getPathLookups().get("MAPS_DATA"),sessionPipeLines));
    addToMap(700, 150, new NetworkManager(featureManager));
    addToMap(900, 200, new NetworkConnectionManager());
    if(featureManager.isEnabled("management.restApi")) {
      addToMap(1200, 400, new RestApiServerManager());
    }
    if(featureManager.isEnabled("interConnections.pushSupport") ||
        featureManager.isEnabled("interConnections.pullSupport")) {
      addToMap(2000, 30, new ServerConnectionManager());
    }
    addToMap(2100, 10, new RoutingManager());

    if(featureManager.isEnabled("management.jolokia")) {
      addToMap(1000, 250, new JolokaManager());
    }

    // Optional modules that if not enabled do not load
    if (enableSystemTopics && featureManager.isEnabled("management.sysTopics")) {
      addToMap(800, 50, new SystemTopicManager(destinationManager));
    }

    boolean licensed = featureManager.isEnabled("hardware.i2c") ||
        featureManager.isEnabled("hardware.spi") ||
        featureManager.isEnabled("hardware.oneWire");
    if (enableDeviceIntegration && licensed) {
      addToMap(2200, 70, new DeviceManager(featureManager));
    }
    addOptionalML();
  }

  private void addOptionalML(){

    if(featureManager.isEnabled("ml")) {
      try {
        Class<?> clazz = Class.forName("io.mapsmessaging.ml.MLModelManager");
        Object mlManager = clazz.getConstructor().newInstance();
        if(mlManager instanceof Agent mlAgent) {
          addToMap(900, 30, mlAgent);
        }
      } catch (Exception e) {
        // ignore, we do not support ML
      }
    }
  }



  /**
   * Logs the service managers and their services.
   *
   * This method iterates over the agentMap and logs the service managers and their services.
   * It first checks if the agent is an instance of ServiceManager. If it is, it logs the agent's name using the logger.
   * Then, it calls the logServices method to log the services of the ServiceManager.
   *
   * After logging the service managers, it logs the "Protocol Manager" and its services.
   * It uses a ServiceLoader to load instances of ProtocolImplFactory and adds them to a list.
   * Then, it calls the logServices method to log the services of the ProtocolImplFactory instances.
   *
   * Finally, it logs the services of the TransformationManager and the TransformerManager.
   */
  private void logServiceManagers() {
    for (Map.Entry<String, AgentOrder> agentEntry : agentMap.entrySet()) {
      if (agentEntry.getValue().getAgent() instanceof ServiceManager serviceManager) {
        logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, agentEntry.getKey());
        logServices(serviceManager.getServices());
      }
    }
    logServices(ProtocolFactory.getProtocolServiceList().stream().map(p -> (Service) p).iterator());
    logServices(TransformationManager.getInstance().getServices());
    logServices(io.mapsmessaging.engine.transformers.TransformerManager.getInstance().getServices());

  }

  private void logServices(Iterator<Service> services) {
    while (services.hasNext()) {
      Service service = services.next();
      logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE, service.getName(), service.getDescription());
    }
  }

  public List<SubSystemStatusDTO> getSubSystemStatus() {
    List<SubSystemStatusDTO> list = new ArrayList<>();
    for(AgentOrder agent:agentMap.values()){
      list.add(agent.getAgent().getStatus());
    }
    return list;
  }

}
