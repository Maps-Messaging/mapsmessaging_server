/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

import io.mapsmessaging.admin.MessageDaemonJMX;
import io.mapsmessaging.api.features.Constants;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.SecurityManager;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.engine.system.SystemTopicManager;
import io.mapsmessaging.hardware.DeviceManager;
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
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.AgentOrder;
import io.mapsmessaging.utilities.SystemProperties;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.SimpleTaskSchedulerJMX;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import lombok.Getter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.*;

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

  private final Logger logger = LoggerFactory.getLogger(MessageDaemon.class);
  private final Map<String, AgentOrder> agentMap;
  private final String uniqueId;
  private MessageDaemonJMX mBean;
  private final AtomicBoolean isStarted;
  private boolean enableSystemTopics;
  private boolean enableDeviceIntegration;
  private DeviceManager deviceManager;

  @Getter
  private final EnvironmentConfig environmentConfig;

  @Getter
  private boolean enableResourceStatistics;


  public MessageDaemon() throws IOException {
    agentMap = new LinkedHashMap<>();
    isStarted = new AtomicBoolean(false);
    environmentConfig = new EnvironmentConfig();
    InstanceConfig instanceConfig = new InstanceConfig(environmentConfig.getDataDirectory());
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
    // </editor-fold>

    //<editor-fold desc="Now see if we can start the Consul Manager">
    // May block till a consul connection is made, depending on config
    ConsulManagerFactory.getInstance().start(uniqueId);

    //</editor-fold>
    ConfigurationManager.getInstance().initialise(uniqueId);
  }


  private void loadConstants() {
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("MessageDaemon");
    int transactionExpiry = properties.getIntProperty("TransactionExpiry", 3600000);
    int transactionScan = properties.getIntProperty("TransactionScan", 1000);
    TransactionManager.setTimeOutInterval(transactionScan);
    TransactionManager.setExpiryTime(transactionExpiry);
    enableSystemTopics = properties.getBooleanProperty("EnableSystemTopics", false);
    if (properties.getBooleanProperty("EnableJMX", true)) {
      JMXManager.setEnableJMX(true);
      mBean = new MessageDaemonJMX(this);
      new SimpleTaskSchedulerJMX(mBean.getTypePath());
      JMXManager.setEnableJMXStatistics(properties.getBooleanProperty("EnableJMXStatistics", true));
    } else {
      mBean = null;
      JMXManager.setEnableJMX(false);
      JMXManager.setEnableJMXStatistics(false);
    }
    enableResourceStatistics = properties.getBooleanProperty("EnableResourceStatistics", false);

    SystemTopicManager.setEnableStatistics(properties.getBooleanProperty("EnableSystemTopics", true));
    Constants.getInstance().setMessageCompression(properties.getProperty("CompressionName", "None"));
    Constants.getInstance().setMinimumMessageSize(properties.getIntProperty("CompressMessageMinSize", 1024));

    ConfigurationProperties deviceManager = ConfigurationManager.getInstance().getProperties("DeviceManager");
    enableDeviceIntegration = deviceManager.getBooleanProperty("enabled", false);

  }

  private void createAgentStartStopList() throws IOException {
    // Start the Schema manager to it has the defaults and has loaded the required classes
    SecurityManager securityManager = new SecurityManager();
    DestinationManager destinationManager = new DestinationManager();

    addToMap(10, 2000, AuthManager.getInstance());
    addToMap(50, 1100, SchemaManager.getInstance());
    addToMap(80, 20, NetworkInterfaceMonitor.getInstance());
    addToMap(100, 900, TransactionManager.getInstance());
    addToMap(300, 11, new DiscoveryManager(uniqueId));
    addToMap(400, 1200, securityManager);
    addToMap(500, 950, destinationManager);
    addToMap(600, 300, new SessionManager(securityManager, destinationManager, environmentConfig.getDataDirectory()));
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

  public NetworkManager getNetworkManager() {
    return (NetworkManager) agentMap.get("Network Manager").getAgent();
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

  public Integer start(String[] strings) throws IOException {
    isStarted.set(true);
    loadConstants();
    createAgentStartStopList();

    logger.log(ServerLogMessages.MESSAGE_DAEMON_STARTUP, BuildInfo.getBuildVersion(), BuildInfo.getBuildDate());
    if (ConsulManagerFactory.getInstance().isStarted()) {
      ConfigurationProperties map = ConfigurationManager.getInstance().getProperties("NetworkManager");
      List<ConfigurationProperties> list = (List<ConfigurationProperties>) map.get("data");
      Map<String, String> meta = new LinkedHashMap<>();

      for (ConfigurationProperties properties : list) {
        String protocol = properties.getProperty("protocol");
        String url = properties.getProperty("url");
        while (protocol.contains(",")) {
          protocol = protocol.replace(",", "-");
        }
        while (protocol.contains(" ")) {
          protocol = protocol.replace(" ", "-");
        }
        meta.put(protocol, url);
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

  private String generateUniqueId() {
    String env = SystemProperties.getInstance().getEnvProperty("SERVER_ID");
    if (env != null) {
      return env;
    }

    boolean useUUID = SystemProperties.getInstance().getBooleanProperty("USE_UUID", true);
    if (useUUID) {
      return UUID.randomUUID().toString();
    }

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return InetAddress.getLoopbackAddress().getHostName();
    }
  }

  public static void main(String[] args) throws IOException {
    ServerRunner.main(args);
  }
}
