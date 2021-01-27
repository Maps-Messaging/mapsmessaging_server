package org.maps.messaging.consul;

import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;

public class ConsulManagerFactory {

  private static final ConsulManagerFactory instance = new ConsulManagerFactory();

  public static ConsulManagerFactory getInstance(){
    return instance;
  }

  private final Logger logger = LoggerFactory.getLogger(ConsulManagerFactory.class);
  private final boolean forceWait;
  private ConsulManager manager;

  public synchronized void start(UUID id) {
    stop(); // just to be sure
    logger.log(LogMessages.CONSUL_MANAGER_START, id.toString());
    boolean retry = true;
    int counter = 0;
    while(retry && counter < Constants.RETRY_COUNT) {
      try {
        manager = new ConsulManager(id);
        retry = false;
      } catch (Exception e) {
        LockSupport.parkNanos(1000000000L);
        counter++;
        if(!forceWait){
          // Log the fact that Consul is not available;
          logger.log(LogMessages.CONSUL_MANAGER_START_ABORTED, id.toString(), e);
          return;
        }
        logger.log(LogMessages.CONSUL_MANAGER_START_DELAYED, id.toString(), e);
      }
    }
  }

  public synchronized void stop(){
    if(manager != null){
      logger.log(LogMessages.CONSUL_MANAGER_STOP);
      manager.stop();
    }
  }

  public synchronized ConsulManager getManager() {
    return manager;
  }

  public synchronized boolean isStarted(){
    return manager != null;
  }

  private ConsulManagerFactory(){
    boolean config;
    try {
      config = Boolean.parseBoolean(System.getProperty("ForceConsul", "FALSE"));
    } catch (Exception e) {
      config = false;
    }
    forceWait = config;
    manager = null;
  }
}
