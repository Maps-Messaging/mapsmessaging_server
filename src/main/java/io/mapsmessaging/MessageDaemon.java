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

package io.mapsmessaging;

import static io.mapsmessaging.logging.ServerLogMessages.*;

import io.mapsmessaging.admin.MessageDaemonJMX;
import io.mapsmessaging.api.features.Constants;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.config.DeviceManagerConfig;
import io.mapsmessaging.config.MessageDaemonConfig;
import io.mapsmessaging.config.NetworkManagerConfig;
import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.configuration.EnvironmentConfig;
import io.mapsmessaging.configuration.EnvironmentPathLookup;
import io.mapsmessaging.configuration.consul.ConsulManagerFactory;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.SecurityManager;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.engine.system.SystemTopicManager;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.NetworkConnectionManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.network.discovery.DiscoveryManager;
import io.mapsmessaging.network.discovery.ServerConnectionManager;
import io.mapsmessaging.network.monitor.NetworkInterfaceMonitor;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.rest.RestApiServerManager;
import io.mapsmessaging.rest.hawtio.HawtioManager;
import io.mapsmessaging.rest.hawtio.JolokaManager;
import io.mapsmessaging.routing.RoutingManager;
import io.mapsmessaging.security.uuid.UuidGenerator;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.AgentOrder;
import io.mapsmessaging.utilities.SystemProperties;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.SimpleTaskSchedulerJMX;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;

/**
 * This is the MessageDaemon class, which represents a message daemon in the system.
 * It is responsible for managing various agents and starting/stopping them.
 * The MessageDaemon class implements the Agent interface.
 * It has a logger, a map of agents, a unique ID, a MessageDaemonJMX object, and an AtomicBoolean to track if it is started or not.
 * It also has an EnvironmentConfig object and a boolean flag to enable resource statistics.
 * The MessageDaemon class provides methods to start and stop the daemon, get the discovery manager, network manager, destination manager, and session manager.
 * It also has methods to get the MessageDaemonJMX object, check if the daemon is started, and get the unique ID.
 * The class has private methods to load constants, create the agent start/stop list, log service managers, and generate a unique ID.
 * The main method is used to start the daemon.
 */
public class MessageDaemon {

  @Getter
  private static final MessageDaemon instance;

  static {
    MessageDaemon tmp;
    try {
      tmp = new MessageDaemon();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    instance = tmp;
  }

  private final String uniqueId;
  @Getter
  private final UUID uuid;
  @Getter
  private final long startTime = System.currentTimeMillis();
  @Getter
  private boolean tagMetaData;
  @Getter
  private boolean enableSystemTopics;

  private final Logger logger = LoggerFactory.getLogger(MessageDaemon.class);
  private final Map<String, AgentOrder> agentMap;
  private MessageDaemonJMX mBean;
  private final AtomicBoolean isStarted;
  private boolean enableDeviceIntegration;

  @Getter
  private MessageDaemonConfig messageDaemonConfig;

  @Getter
  private DeviceManager deviceManager;

  @Getter
  private boolean enableResourceStatistics;

  @Getter
  private final String hostname;

  private static final String MAPS_HOME = "MAPS_HOME";
  private static final String MAPS_DATA = "MAPS_DATA";

  /**
   * The constructor of the MessageDaemon class.
   * It initializes the instance by setting up various configurations and properties.
   * It also registers the paths for the environment variables MAPS_HOME and MAPS_DATA.
   * It loads the instance configuration from the specified path and retrieves the server ID.
   * If the server ID is not found, it generates a unique ID using the SystemProperties class.
   * It then initializes the Consul Manager and the ConfigurationManager with the server ID.
   */
  public MessageDaemon() throws IOException {
    agentMap = new LinkedHashMap<>();
    isStarted = new AtomicBoolean(false);
    EnvironmentConfig.getInstance().registerPath(new EnvironmentPathLookup(MAPS_HOME, ".", false));
    EnvironmentConfig.getInstance().registerPath(new EnvironmentPathLookup(MAPS_DATA, "{{MAPS_HOME}}/data", true));
    InstanceConfig instanceConfig = new InstanceConfig(EnvironmentConfig.getInstance().getPathLookups().get(MAPS_DATA));
    instanceConfig.loadState();
    String serverId = instanceConfig.getServerName();
    if (serverId != null) {
      uniqueId = serverId;
    } else {
      uniqueId = SystemProperties.getInstance().getProperty("SERVER_ID", generateUniqueId());
      instanceConfig.setServerName(uniqueId);
      instanceConfig.saveState();
      logger.log(MESSAGE_DAEMON_STARTUP_BOOTSTRAP, uniqueId);
    }
    uuid = instanceConfig.getUuid();
    hostname = InetAddress.getLocalHost().getHostName();
    // </editor-fold>

    //<editor-fold desc="Now see if we can start the Consul Manager">
    // May block till a consul connection is made, depending on config
    ConsulManagerFactory.getInstance().start(uniqueId);

    //</editor-fold>
    ConfigurationManager.getInstance().initialise(uniqueId);
  }

  /**
   * Loads the constants for the MessageDaemon.
   * This method retrieves the configuration properties for the MessageDaemon and sets the corresponding constants.
   * It also initializes the JMXManager and sets the enablement of JMX and JMX statistics.
   * Additionally, it sets the enablement of system topics and advanced system topics.
   * It sets the message compression and minimum message size for Constants.
   * Finally, it retrieves the configuration properties for the DeviceManager and sets the enablement of device integration.
   */
  private void loadConstants() {
    messageDaemonConfig = MessageDaemonConfig.getInstance();
    if(messageDaemonConfig.getLongitude() != 0 && messageDaemonConfig.getLatitude() != 0) {
      LocationManager.getInstance().setPosition(messageDaemonConfig.getLatitude() , messageDaemonConfig.getLongitude());
    }
    tagMetaData = messageDaemonConfig.isTagMetaData();
    long transactionExpiry = messageDaemonConfig.getTransactionExpiry();
    long transactionScan = messageDaemonConfig.getTransactionScan();
    TransactionManager.setTimeOutInterval(transactionScan);
    TransactionManager.setExpiryTime(transactionExpiry);
    enableSystemTopics = messageDaemonConfig.isEnableSystemTopics();
    boolean enableAdvancedSystemTopics = messageDaemonConfig.isEnableSystemStatusTopics();
    if (messageDaemonConfig.isEnableJMX()) {
      JMXManager.setEnableJMX(true);
      mBean = new MessageDaemonJMX(this);
      new SimpleTaskSchedulerJMX(mBean.getTypePath());
      JMXManager.setEnableJMXStatistics(messageDaemonConfig.isEnableJMXStatistics());
    } else {
      mBean = null;
      JMXManager.setEnableJMX(false);
      JMXManager.setEnableJMXStatistics(false);
    }
    enableResourceStatistics = messageDaemonConfig.isEnableResourceStatistics();

    SystemTopicManager.setEnableStatistics(enableSystemTopics);
    SystemTopicManager.setEnableAdvancedStats(enableAdvancedSystemTopics);
    Constants.getInstance().setMessageCompression(messageDaemonConfig.getCompressionName());
    Constants.getInstance().setMinimumMessageSize(messageDaemonConfig.getCompressMessageMinSize());


    enableDeviceIntegration = DeviceManagerConfig.getInstance().isEnabled();
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
   * - HawtioManager
   *
   * The method also adds optional modules to the agentMap based on the values of enableSystemTopics and enableDeviceIntegration flags.
   * If enableSystemTopics is true, a SystemTopicManager is added to the agentMap.
   * If enableDeviceIntegration is true, a DeviceManager is created and added to the agentMap.
   *
   * @throws IOException if an I/O error occurs
   */
  private void createAgentStartStopList() throws IOException {
    // Start the Schema manager to it has the defaults and has loaded the required classes
    SecurityManager securityManager = new SecurityManager();
    DestinationManager destinationManager = new DestinationManager();
    TransformationManager.getInstance();

    addToMap(10, 2000, AuthManager.getInstance());
    addToMap(50, 1100, SchemaManager.getInstance());
    addToMap(80, 20, NetworkInterfaceMonitor.getInstance());
    addToMap(100, 900, TransactionManager.getInstance());
    addToMap(300, 11, new DiscoveryManager(uniqueId));
    addToMap(400, 1200, securityManager);
    addToMap(500, 950, destinationManager);
    addToMap(600, 300, new SessionManager(securityManager, destinationManager, EnvironmentConfig.getInstance().getPathLookups().get(MAPS_DATA), messageDaemonConfig.getSessionPipeLines()));
    addToMap(700, 150, new NetworkManager());
    addToMap(900, 200, new NetworkConnectionManager());
    addToMap(1200, 400, new RestApiServerManager());
    addToMap(2000, 30, new ServerConnectionManager());
    addToMap(2100, 10, new RoutingManager());
    addToMap(1000, 250, new JolokaManager());
    addToMap(1100, 300, new HawtioManager());

    // Optional modules that if not enabled do not load
    if (enableSystemTopics) {
      addToMap(800, 50, new SystemTopicManager(destinationManager));
    }
    if (enableDeviceIntegration) {
      deviceManager = new DeviceManager();
      addToMap(2200, 70, deviceManager);
    } else {
      deviceManager = null;
    }
  }

  public boolean hasDeviceManager() {
    return deviceManager != null && deviceManager.isEnabled();
  }

  private void addToMap(int start, int stop, Agent agent) {
    agentMap.put(agent.getName(), new AgentOrder(start, stop, agent));
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
    for (Entry<String, AgentOrder> agentEntry : agentMap.entrySet()) {
      if (agentEntry.getValue().getAgent() instanceof ServiceManager) {
        logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, agentEntry.getKey());
        logServices(((ServiceManager) agentEntry.getValue().getAgent()).getServices());
      }
    }

    logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, "Protocol Manager");
    ServiceLoader<ProtocolImplFactory> protocolServiceLoader = ServiceLoader.load(ProtocolImplFactory.class);
    List<Service> service = new ArrayList<>();
    for (ProtocolImplFactory parser : protocolServiceLoader) {
      service.add(parser);
    }
    logServices(service.listIterator());
    logServices(TransformationManager.getInstance().getServices());
    logServices(io.mapsmessaging.engine.transformers.TransformerManager.getInstance().getServices());
  }

  private void logServices(Iterator<Service> services) {
    while (services.hasNext()) {
      Service service = services.next();
      logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE, service.getName(), service.getDescription());
    }
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

  public List<String> getTypePath() {
    if (mBean != null) {
      return mBean.getTypePath();
    }
    return new ArrayList<>();
  }

  /**
   * Starts the MessageDaemon.
   *
   * This method sets the 'isStarted' flag to true and performs the necessary initialization steps to start the daemon.
   * It calls the 'loadConstants' method to load the configuration properties, 'createAgentStartStopList' method to create
   * the list of agents to start and stop, and registers the daemon with Consul if it is already started.
   *
   * The method then sorts the agentMap based on the start order and iterates over the sorted list to start each agent.
   * For each agent, it logs a message indicating that the agent is starting, calls the 'start' method of the agent,
   * and logs a message indicating that the agent has started along with the time taken for the start operation.
   *
   * After starting all the agents, the method calls the 'logServiceManagers' method to log the loaded service managers.
   *
   * @param strings an array of strings (not used in the method)
   * @return null
   * @throws IOException if an I/O error occurs during the initialization steps
   */
  public Integer start(String[] strings) throws IOException {
    isStarted.set(true);
    loadConstants();
    createAgentStartStopList();

    logger.log(ServerLogMessages.MESSAGE_DAEMON_STARTUP, BuildInfo.getBuildVersion(), BuildInfo.getBuildDate());
    if (ConsulManagerFactory.getInstance().isStarted()) {
      NetworkManagerConfig networkManagerConfig = NetworkManagerConfig.getInstance();
      Map<String, String> meta = new LinkedHashMap<>();
      for(EndPointServerConfig serverConfig: networkManagerConfig.getEndPointServerConfigList()){
        String protocols = serverConfig.getProtocols();
        String url = serverConfig.getUrl();
        while (protocols.contains(",")) {
          protocols = protocols.replace(",", "-");
        }
        while (protocols.contains(" ")) {
          protocols = protocols.replace(" ", "-");
        }
        meta.put(protocols, url);
      }
      //look for override
      ConsulManagerFactory.getInstance().getManager().register(meta);
    }
    List<AgentOrder> startList = new ArrayList<>(agentMap.values());
    startList.sort(Comparator.comparingInt(AgentOrder::getStartOrder));
    for (AgentOrder agent : startList) {
      long start = System.currentTimeMillis();
      logger.log(MESSAGE_DAEMON_AGENT_STARTING, agent.getAgent().getName());
      agent.getAgent().start();
      logger.log(MESSAGE_DAEMON_AGENT_STARTED, agent.getAgent().getName(), (System.currentTimeMillis() - start));
    }
    logServiceManagers();
    return null;
  }

  /**
   * Stops the MessageDaemon by setting the 'isStarted' flag to false and stopping all agents in the 'agentMap'.
   *
   * @param i The integer value to be returned.
   * @return The integer value passed as parameter.
   */
  public int stop(int i) {
    isStarted.set(false);
    ConsulManagerFactory.getInstance().stop();
    List<AgentOrder> startList = new ArrayList<>(agentMap.values());
    startList.sort(Comparator.comparingInt(AgentOrder::getStopOrder));
    for (AgentOrder agent : startList) {
      long start = System.currentTimeMillis();
      logger.log(MESSAGE_DAEMON_AGENT_STOPPING, agent.getAgent().getName());
      agent.getAgent().stop();
      logger.log(MESSAGE_DAEMON_AGENT_STOPPED, agent.getAgent().getName(), (System.currentTimeMillis() - start));
    }
    if (mBean != null) mBean.close();
    return i;
  }

  public boolean isStarted() {
    return isStarted.get();
  }

  public String getId() {
    return uniqueId;
  }

  /**
   * Generates a unique identifier for the MessageDaemon instance.
   *
   * The unique identifier is generated based on the following rules:
   * 1. If the environment variable "SERVER_ID" is set, the value of the variable is returned as the unique identifier.
   * 2. If the boolean property "USE_UUID" is set to true (default), a UUID is generated and returned as the unique identifier.
   * 3. If the above conditions are not met, the hostname of the local machine is returned as the unique identifier.
   *
   * @return The unique identifier for the MessageDaemon instance.
   * @throws UnknownHostException If the hostname of the local machine cannot be determined.
   */
  private String generateUniqueId() {
    String env = SystemProperties.getInstance().getEnvProperty("SERVER_ID");
    if (env != null) {
      return env;
    }

    boolean useUUID = SystemProperties.getInstance().getBooleanProperty("USE_UUID", true);
    if (useUUID) {
      return UuidGenerator.getInstance().generate().toString();
    }

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return InetAddress.getLoopbackAddress().getHostName();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    ServerRunner.main(args);
  }
}
