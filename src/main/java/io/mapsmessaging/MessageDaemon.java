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

package io.mapsmessaging;

import static io.mapsmessaging.logging.ServerLogMessages.*;

import io.mapsmessaging.admin.MessageDaemonJMX;
import io.mapsmessaging.api.features.Constants;
import io.mapsmessaging.config.*;
import io.mapsmessaging.configuration.EnvironmentConfig;
import io.mapsmessaging.configuration.EnvironmentPathLookup;
import io.mapsmessaging.configuration.consul.ConsulManagerFactory;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.system.SystemTopicManager;
import io.mapsmessaging.hardware.DeviceManager;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.license.LicenseController;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.logging.LogMonitor;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.security.uuid.UuidGenerator;
import io.mapsmessaging.stats.StatsReporter;
import io.mapsmessaging.utilities.PidFileManager;
import io.mapsmessaging.utilities.SystemProperties;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.SimpleTaskSchedulerJMX;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
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
  private static String PID_FILE = "pid";
  @Getter
  private static ExitRunner exitRunner;

  @Getter
  private static MessageDaemon instance;

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

  @Getter
  private SubSystemManager subSystemManager;

  private MessageDaemonJMX mBean;
  private final AtomicBoolean isStarted;
  private boolean enableDeviceIntegration;

  @Getter
  private MessageDaemonConfig messageDaemonConfig;

  @Getter
  private boolean enableResourceStatistics;

  @Getter
  private final String hostname;

  @Getter
  private final String licenseHome;
  @Getter
  private FeatureManager featureManager;

  private static final String MAPS_HOME = "MAPS_HOME";
  private static final String MAPS_DATA = "MAPS_DATA";

  @Getter
  private final LogMonitor logMonitor;


  private StatsReporter statsReporter;

  /**
   * The constructor of the MessageDaemon class.
   * It initializes the instance by setting up various configurations and properties.
   * It also registers the paths for the environment variables MAPS_HOME and MAPS_DATA.
   * It loads the instance configuration from the specified path and retrieves the server ID.
   * If the server ID is not found, it generates a unique ID using the SystemProperties class.
   * It then initializes the Consul Manager and the ConfigurationManager with the server ID.
   */
  public MessageDaemon() throws IOException {
    logMonitor = new LogMonitor();
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
    String mapsHome = SystemProperties.getInstance().getProperty(MAPS_HOME, ".");
    licenseHome =mapsHome+File.separator+"licenses";
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

    if (messageDaemonConfig.isEnableJMX() && featureManager.isEnabled("management.jmx")) {
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

  public boolean hasDeviceManager() {
    DeviceManager deviceManager = subSystemManager.getDeviceManager();
    return deviceManager != null && deviceManager.isEnabled();
  }

  public List<String> getTypePath() {
    if (mBean != null) {
      return mBean.getTypePath();
    }
    return new ArrayList<>();
  }

  public DestinationManager getDestinationManager() {
    return subSystemManager.getDestinationManager();
  }

  /**
   * Starts the MessageDaemon.
   * This method sets the 'isStarted' flag to true and performs the necessary initialization steps to start the daemon.
   * It calls the 'loadConstants' method to load the configuration properties, 'createAgentStartStopList' method to create
   * the list of agents to start and stop, and registers the daemon with Consul if it is already started.
   * The method then sorts the agentMap based on the start order and iterates over the sorted list to start each agent.
   * For each agent, it logs a message indicating that the agent is starting, calls the 'start' method of the agent,
   * and logs a message indicating that the agent has started along with the time taken for the start operation.
   * After starting all the agents, the method calls the 'logServiceManagers' method to log the loaded service managers.
   *
   * @return null
   * @throws IOException if an I/O error occurs during the initialization steps
   */
  public Integer start() throws IOException {

    // Load the license
    File licenseDir = new File(licenseHome);
    licenseDir.mkdirs();
    LicenseController licenseController = new LicenseController(licenseHome, uniqueId, uuid);
    featureManager = licenseController.getFeatureManager();

    logMonitor.register();
    isStarted.set(true);
    loadConstants();

    subSystemManager = new SubSystemManager(uniqueId, enableSystemTopics, enableDeviceIntegration, messageDaemonConfig.getSessionPipeLines(), featureManager);
    subSystemManager.start();
    logger.log(ServerLogMessages.MESSAGE_DAEMON_STARTUP, BuildInfo.getBuildVersion(), BuildInfo.getBuildDate());
    if (ConsulManagerFactory.getInstance().isStarted()) {
      ConsulManagerFactory.getInstance().getManager().register(buildMetaData());
    }
    statsReporter = new StatsReporter();
    return null;
  }

  private Map<String, String> buildMetaData(){
    NetworkManagerConfig networkManagerConfig = NetworkManagerConfig.getInstance();
    Map<String, String> meta =new LinkedHashMap<>();
    for(EndPointServerConfigDTO serverConfig: networkManagerConfig.getEndPointServerConfigList()){
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
    return meta;
  }

  /**
   * Stops the MessageDaemon by setting the 'isStarted' flag to false and stopping all agents in the 'agentMap'.
   *
   * @param i The integer value to be returned.
   * @return The integer value passed as parameter.
   */
  public int stop(int i) {
    statsReporter.close();
    isStarted.set(false);
    ConsulManagerFactory.getInstance().stop();
    subSystemManager.stop();
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
    String env = SystemProperties.getInstance().getProperty("SERVER_ID", SystemProperties.getInstance().getEnvProperty("SERVER_ID"));
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

  public List<SubSystemStatusDTO> getSubSystemStatus() {
    return subSystemManager.getSubSystemStatus();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    String directoryPath = SystemProperties.getInstance().locateProperty("MAPS_HOME", ".");
    if (!directoryPath.isEmpty()) {
      PID_FILE = directoryPath + File.separator + PID_FILE;
      PID_FILE = PID_FILE.replace("//", "/");
    }
    PidFileManager pidFileManager = new PidFileManager( new File(PID_FILE));

    if (pidFileManager.exists()) {
      long pid = pidFileManager.readPidFromFile();
      pidFileManager.deletePidFile();
      int count = 0;
      Logger logger = LoggerFactory.getLogger(MessageDaemon.class);

      while(pidFileManager.isProcessRunning(pid) && count < 30){
        logger.log(MESSAGE_DAEMON_WAIT_PREVIOUS_INSTANCE, "Waiting for previous instance to exit");
        count++;
        Thread.sleep(1000);
      }
      if(count == 30){
        logger.log(MESSAGE_DAEMON_WAIT_PREVIOUS_INSTANCE, "Previous process not stopping, unable to start");
        return;
      }
    }
    pidFileManager.writeNewFile();
    exitRunner = new ExitRunner(pidFileManager);
    instance = new MessageDaemon();
    instance.start();
  }
}
