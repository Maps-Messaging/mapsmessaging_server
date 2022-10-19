package io.mapsmessaging.network.discovery;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class ServerConnectionManager implements ServiceListener, Agent {

  private final Logger logger = LoggerFactory.getLogger(ServerConnectionManager.class);

  public ServerConnectionManager(){
  }

  @Override
  public void serviceAdded(ServiceEvent serviceEvent) {
  }

  @Override
  public void serviceRemoved(ServiceEvent serviceEvent) {

  }

  @Override
  public void serviceResolved(ServiceEvent serviceEvent) {
    if(!serviceEvent.getName().equals(MessageDaemon.getInstance().getId())){ // Ignore local
      for(String host:serviceEvent.getInfo().getHostAddresses()){
        logger.log(ServerLogMessages.DISCOVERY_RESOLVED_REMOTE_SERVER, serviceEvent.getName(), host+":"+serviceEvent.getInfo().getPort(), serviceEvent.getInfo().getApplication());
      }
    }
  }

  @Override
  public String getName() {
    return "Server Connection Manager";
  }

  @Override
  public String getDescription() {
    return "Listens for new maps servers via mDNS";
  }

  @Override
  public void start() {
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_maps._tcp.local.", this);
    MessageDaemon.getInstance().getDiscoveryManager().registerListener("_mqtt._tcp.local.", this);
  }

  @Override
  public void stop() {

  }
}
