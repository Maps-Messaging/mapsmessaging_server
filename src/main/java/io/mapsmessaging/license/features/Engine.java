package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Engine {
  private boolean queueSupport;
  private boolean topicSupport;
  private boolean namedSubscriptionSupport;
  private boolean peristSupport;
  private boolean filteringSupport;
  private boolean schemaSupport;
  private int maxTopics;
  private int maxQueues;
}
