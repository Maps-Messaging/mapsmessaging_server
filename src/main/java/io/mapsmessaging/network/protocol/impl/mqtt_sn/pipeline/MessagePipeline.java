package io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Register;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class MessagePipeline {

  private final Queue<MessageEvent> publishContexts;
  private final AtomicInteger outstanding;
  private final StateEngine stateEngine;
  private final PacketIdManager packetIdManager;
  private final MQTT_SNProtocol protocol;
  private final AtomicBoolean paused;
  private final AtomicInteger empty;

  private final int maxInFlightEvents;
  private final long eventTimeout;
  private final boolean dropQoS0;

  private Runnable completion;

  public MessagePipeline(MQTT_SNProtocol protocol, StateEngine stateEngine){
    this.protocol = protocol;
    this.stateEngine = stateEngine;
    this.packetIdManager = protocol.getPacketIdManager();
    publishContexts = new ConcurrentLinkedQueue<>();
    outstanding = new AtomicInteger(0);
    paused = new AtomicBoolean(false);
    empty = new AtomicInteger(0);

    ConfigurationProperties props = protocol.getEndPoint().getConfig().getProperties();
    maxInFlightEvents = props.getIntProperty("maxInFlightEvents", 1);
    dropQoS0 = props.getBooleanProperty("dropQoS0Events", false);

    long t = TimeUnit.SECONDS.toMillis(props.getIntProperty("eventQueueTimeout", 0));
    eventTimeout = t == 0? Long.MAX_VALUE:t;

  }

  public void pause(){
    paused.set(true);
  }

  public void resume(){
    paused.set(false);
    sendNext();
  }

  public void queue(@NotNull @NonNull MessageEvent messageEvent){
    if(paused.get()){
      if(dropQoS0 &&
          messageEvent.getMessage().getQualityOfService().getLevel() == 0 &&
          messageEvent.getSubscription().getDepth() > 1){
        messageEvent.getCompletionTask().run();
        // Dropping a QoS:0 event while paused
        return;
      }
      outstanding.incrementAndGet();
      publishContexts.offer(messageEvent);
    }
    else {
      SubscriptionContext subInfo = messageEvent.getSubscription().getContext();
      QualityOfService qos = subInfo.getQualityOfService();
      long depth = outstanding.incrementAndGet();
      if (depth <= maxInFlightEvents) {
        if (qos.getLevel() > 0) {
          publishContexts.offer(messageEvent);
        } else {
          outstanding.decrementAndGet();
        }
        send(messageEvent);
      } else {
        publishContexts.offer(messageEvent);
      }
    }
  }

  public void completed(int messageId) {
    outstanding.decrementAndGet();
    publishContexts.poll(); // Remove outstanding publish
    if (!paused.get() || empty.get() != 0) {
      sendNext();
    }
  }

  private void sendNext(){
    MessageEvent messageEvent = publishContexts.peek();
    if(messageEvent != null) {
      if(messageEvent.getMessage().getCreation() + eventTimeout > System.currentTimeMillis()) {
        SubscriptionContext subInfo = messageEvent.getSubscription().getContext();
        QualityOfService qos = subInfo.getQualityOfService();
        if (qos.getLevel() == 0) {
          empty.decrementAndGet();
          send(messageEvent);
          completed(0);
        } else {
          send(messageEvent);
        }
      }
      else{
        messageEvent.getCompletionTask().run();
        completed(0);
      }
    }
    if(outstanding.get() == 0 && paused.get()){
      empty.set(0);
      stateEngine.getTopicAliasManager().clear();
      if(completion != null) {
        completion.run();
        completion = null;
      }
    }
  }

  private void send(MessageEvent messageEvent){
    SubscriptionContext subInfo = messageEvent.getSubscription().getContext();
    QualityOfService qos = subInfo.getQualityOfService();
    int messageId = 0;
    if (qos.isSendPacketId()) {
      messageId = packetIdManager.nextPacketIdentifier(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier());
    }
    short alias = stateEngine.getTopicAliasManager().findTopicAlias(messageEvent.getDestinationName());
    //
    // If this event is from a wild card then the client would not have registered it, so lets do that now
    //
    if (alias == -1) {
      //
      // Updating the client with the new topic id for the destination
      //
      alias = stateEngine.getTopicAliasManager().getTopicAlias(messageEvent.getDestinationName());
      Register register = new Register(alias, MQTT_SNPacket.TOPIC_NAME, messageEvent.getDestinationName());
      protocol.writeFrame(register);
    }
    MQTT_SNPacket publish = protocol.buildPublish(alias, messageId,  messageEvent, qos);
    stateEngine.sendPublish(protocol, messageEvent.getDestinationName(), publish);
  }

  public void emptyQueue(int sendSize, Runnable task) {
    this.completion = task;
    if(size() == 0){
      if(completion != null) {
        completion.run();
        completion = null;
      }
    }
    else {
      if (sendSize == 0) {
        empty.set(Integer.MAX_VALUE);
      } else {
        empty.set(sendSize);
      }
      sendNext();
    }
  }

  public int size(){
    int total =0;
    Map<String, Long> counters = new LinkedHashMap<>();
    for(MessageEvent event:publishContexts){
      String destination = event.getDestinationName();
      if(!counters.containsKey(destination)){
        total += event.getSubscription().getDepth();
        counters.put(destination, (long)(event.getSubscription().getDepth()));
      }
    }
    return total;
  }
}
