package org.maps.messaging.consul;

import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

public class ConsulManagerFactory {

  private static final ConsulManagerFactory instance = new ConsulManagerFactory();

  public static ConsulManagerFactory getInstance(){
    return instance;
  }

  private final boolean forceWait;
  private ConsulManager manager;

  public synchronized void start(UUID id) {
    stop(); // just to be sure
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
          return;
        }
      }
    }
  }

  public synchronized void stop(){
    if(manager != null){
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
