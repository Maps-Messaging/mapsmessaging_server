package io.mapsmessaging.network.io.impl.udp.session;

import java.io.Closeable;

public class StateTimeoutMonitor<T extends Closeable> implements Runnable{

  private final UDPSessionManager<T> manager;

  public StateTimeoutMonitor(UDPSessionManager<T> manager){
    this.manager = manager;
  }

  @Override
  public void run() {
    manager.scanForTimeouts();
  }
}