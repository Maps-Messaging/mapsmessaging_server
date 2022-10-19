package io.mapsmessaging.utilities;

import lombok.Getter;

public class AgentOrder {

  @Getter
  private final int startOrder;

  @Getter
  private final int stopOrder;

  @Getter
  private final Agent agent;

  public AgentOrder(int start, int stop, Agent agent) {
    stopOrder = stop;
    startOrder = start;
    this.agent = agent;
  }
}
