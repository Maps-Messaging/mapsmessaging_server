package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.network.io.Timeoutable;

public class StateTimeoutMonitor<T extends Timeoutable> implements Runnable{

  private final UDPSessionManager<T> manager;

  public StateTimeoutMonitor(UDPSessionManager<T> manager){
    this.manager = manager;
  }

  @Override
  public void run() {
    manager.scanForTimeouts();
  }
}