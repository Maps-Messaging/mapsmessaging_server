/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging;

import static io.mapsmessaging.logging.ServerLogMessages.MESSAGE_DAEMON_STARTUP_BOOTSTRAP;

import io.mapsmessaging.admin.MessageDaemonJMX;
import io.mapsmessaging.api.features.Constants;
import io.mapsmessaging.consul.ConsulManagerFactory;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.session.SecurityManager;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.engine.system.SystemTopicManager;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.NetworkConnectionManager;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.network.discovery.DiscoveryManager;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.rest.ServerManager;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.SimpleTaskSchedulerJMX;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public class MessageDaemon implements WrapperListener {

  private static final String PID_FILE = "pid";

  private static MessageDaemon instance;

  private final Logger logger = LoggerFactory.getLogger(MessageDaemon.class);
  private final NetworkManager networkManager;
  private final NetworkConnectionManager networkConnectionManager;
  private final DestinationManager destinationManager;
  private final SessionManager sessionManager;
  private final HawtioManager hawtioManager;
  private final JolokaManager jolokaManager;
  private final DiscoveryManager discoveryManager;
  private final SecurityManager securityManager;
  private final SystemTopicManager systemTopicManager;
  private final String uniqueId;
  private final MessageDaemonJMX mBean;
  private final String homeDirectory;
  private final AtomicBoolean isStarted;

  public MessageDaemon() throws IOException {
    instance = this;
    isStarted = new AtomicBoolean(false);
    String tmpHome = System.getProperty("MAPS_HOME", ".");
    File testHome = new File(tmpHome);
    if (!testHome.exists()) {
      logger.log(ServerLogMessages.MESSAGE_DAEMON_NO_HOME_DIRECTORY, testHome);
      tmpHome = ".";
    }
    if (tmpHome.endsWith(File.separator)) {
      tmpHome = tmpHome.substring(0, tmpHome.length() - 1);
    }
    homeDirectory = tmpHome;
    File data = new File(homeDirectory + "/data");
    if (!data.exists()) {
      Files.createDirectories(data.toPath());
    }
    String path = homeDirectory + "/data/";

    InstanceConfig instanceConfig = new InstanceConfig(path);
    instanceConfig.loadState();
    String serverId = instanceConfig.getServerName();
    if (serverId != null) {
      uniqueId = serverId;
    } else {
      serverId = System.getProperty("SERVER_ID");
      if (serverId == null) {
        uniqueId = generateUniqueId();
      } else {
        uniqueId = serverId;
      }
      instanceConfig.setServerName(uniqueId);
      instanceConfig.saveState();
      logger.log(MESSAGE_DAEMON_STARTUP_BOOTSTRAP, uniqueId);
    }
    // </editor-fold>


    //<editor-fold desc="Now see if we can start the Consul Manager">
    // May block till a consul connection is made, depending on config
    ConsulManagerFactory.getInstance().start(uniqueId);
    //</editor-fold>
    ConfigurationManager.getInstance().initialise(uniqueId + "_");
    ConfigurationProperties properties = ConfigurationManager.getInstance().getProperties("MessageDaemon");
    int delayTimer = properties.getIntProperty("DelayedPublishInterval", 1000);
    int pipeLineSize = properties.getIntProperty("SessionPipeLines", 10);
    int transactionExpiry = properties.getIntProperty("TransactionExpiry", 3600000);
    int transactionScan = properties.getIntProperty("TransactionScan", 1000);
    TransactionManager.setTimeOutInterval(transactionScan);
    TransactionManager.setExpiryTime(transactionExpiry);

    mBean = new MessageDaemonJMX(this);
    if(properties.getBooleanProperty("EnableJMX", true)){
      JMXManager.setEnableJMX(true);
      new SimpleTaskSchedulerJMX(mBean.getTypePath());
      JMXManager.setEnableJMXStatistics(properties.getBooleanProperty("EnableJMXStatistics", true));
    }
    else{
      JMXManager.setEnableJMX(false);
      JMXManager.setEnableJMXStatistics(false);
    }

    SystemTopicManager.setEnableStatistics(properties.getBooleanProperty("EnableSystemTopicAverages", true));

    Constants.getInstance().setMessageCompression(properties.getProperty("CompressionName", "None"));
    Constants.getInstance().setMinimumMessageSize(properties.getIntProperty("CompressMessageMinSize", 1024));

    // Start the Schema manager to it has the defaults and has loaded the required classes
    SchemaManager.getInstance().start();
    discoveryManager = new DiscoveryManager(uniqueId);

    networkManager = new NetworkManager(mBean.getTypePath());
    networkConnectionManager = new NetworkConnectionManager(mBean.getTypePath());
    securityManager = new SecurityManager();
    destinationManager = new DestinationManager(delayTimer);
    systemTopicManager = new SystemTopicManager(destinationManager);
    sessionManager = new SessionManager(securityManager, destinationManager, path, pipeLineSize);
    jolokaManager = new JolokaManager();
    hawtioManager = new HawtioManager();
    logServiceManagers();
    TransactionManager.getInstance().start();
  }

  private void logServiceManagers() {

    logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, "Network Manager");
    logServices(networkManager.getServices());

    logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, "System Topics Manager");
    logServices(systemTopicManager.getServices());

    logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, "Transformation Manager");
    logServices(TransformationManager.getInstance().getServices());

    logServices(networkConnectionManager.getServices());

    logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE_LOADED, "Protocol Manager");
    ServiceLoader<ProtocolImplFactory> protocolServiceLoader = ServiceLoader.load(ProtocolImplFactory.class);
    List<Service> service = new ArrayList<>();
    for (ProtocolImplFactory parser : protocolServiceLoader) {
      service.add(parser);
    }
    logServices(service.listIterator());

    logServices(TransformerManager.getInstance().getServices());
  }

  private void logServices(Iterator<Service> services) {
    while (services.hasNext()) {
      Service service = services.next();
      logger.log(ServerLogMessages.MESSAGE_DAEMON_SERVICE, service.getName(), service.getDescription());
    }
  }

  public static MessageDaemon getInstance() {
    return instance;
  }

  // Start the application.  If the JVM was launched from the native
  //  Wrapper then the application will wait for the native Wrapper to
  //  call the application's start method.  Otherwise, the start method
  //  will be called immediately.
  public static void main(String[] args) throws IOException {
    File pidFile = new File(PID_FILE);

    if (pidFile.exists()) {
      try {
        java.nio.file.Files.delete(Paths.get(PID_FILE));
      } catch (IOException e) {
        LockSupport.parkNanos(10000000);
      }
    }
    try {
      if (pidFile.createNewFile()) {
        pidFile.deleteOnExit();
      }
    } catch (IOException e) {
      // can ignore this exception
    }
    new ExitRunner(pidFile);
    WrapperManager.start(new MessageDaemon(), args);
  }

  public DiscoveryManager getDiscoveryManager(){
    return discoveryManager;
  }

  public NetworkManager getNetworkManager() {
    return networkManager;
  }

  public DestinationManager getDestinationManager() {
    return destinationManager;
  }

  public SessionManager getSessionManager() {
    return sessionManager;
  }

  public MessageDaemonJMX getMBean() {
    return mBean;
  }

  @Override
  public Integer start(String[] strings) {
    logger.log(ServerLogMessages.MESSAGE_DAEMON_STARTUP, BuildInfo.getInstance().getBuildVersion(), BuildInfo.getInstance().getBuildDate());
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
      ConsulManagerFactory.getInstance().getManager().register(meta);
    }
    jolokaManager.start();
    destinationManager.initialise();
    sessionManager.start();
    destinationManager.start();
    networkManager.initialise();
    networkManager.startAll();
    hawtioManager.start();
    networkConnectionManager.initialise();
    networkConnectionManager.start();
    isStarted.set(true);

    ConfigurationProperties map = ConfigurationManager.getInstance().getProperties("RestApi");
    ServerManager restServerManager = new ServerManager();
    restServerManager.initRestApi(map);
    return null;
  }

  @Override
  public int stop(int i) {
    Thread t = new Thread(discoveryManager::deregisterAll);
    t.start();
    isStarted.set(false);
    networkConnectionManager.stop();
    jolokaManager.stop();
    networkManager.stopAll();
    sessionManager.stop();
    systemTopicManager.stop();
    destinationManager.stop();
    mBean.close();
    try {
      t.join(10000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return i;
  }

  @Override
  public void controlEvent(int event) {
    if (!((event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT) && (WrapperManager.isLaunchedAsService()))) {
      WrapperManager.stop(0);
    }
  }

  public boolean isStarted() {
    return isStarted.get();
  }

  public String getId() {
    return uniqueId;
  }

  private String generateUniqueId() {
    String env = System.getenv("SERVER_ID");
    if (env != null) {
      return env;
    }

    boolean useUUID = Boolean.parseBoolean(System.getProperty("USE_UUID", "TRUE"));
    if (useUUID) {
      return UUID.randomUUID().toString();
    }

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return InetAddress.getLoopbackAddress().getHostName();
    }
  }

  public static class ExitRunner extends Thread {

    File pidFile;

    ExitRunner(File pidFile) {
      this.pidFile = pidFile;
      super.start();
    }

    @Override
    public void run() {
      while (pidFile.exists()) {
        LockSupport.parkNanos(1000000);
      }
      WrapperManager.stop(1);
    }
  }
}
