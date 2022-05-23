package io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline;

import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_CREATED;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_EVENT_COMPLETED;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_EVENT_DROPPED;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_EVENT_QUEUED;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_EVENT_SENT;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_EVENT_TIMED_OUT;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_PAUSED;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_RESUMED;
import static io.mapsmessaging.logging.ServerLogMessages.MQTT_SN_PIPELINE_WOKEN;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_NAME;
import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_PRE_DEFINED_ID;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
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
  private final Logger logger;
  private final Queue<MessageEvent> publishContexts;
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
    paused = new AtomicBoolean(false);
    empty = new AtomicInteger(0);
    logger = LoggerFactory.getLogger(MessagePipeline.class);
    ConfigurationProperties props = protocol.getEndPoint().getConfig().getProperties();
    maxInFlightEvents = props.getIntProperty("maxInFlightEvents", 1);
    dropQoS0 = props.getBooleanProperty("dropQoS0Events", false);

    long t = TimeUnit.SECONDS.toMillis(props.getIntProperty("eventQueueTimeout", 0));
    eventTimeout = t == 0? (Long.MAX_VALUE>>2):t;
    logger.log(MQTT_SN_PIPELINE_CREATED, protocol.getName(), dropQoS0, maxInFlightEvents, eventTimeout);
  }

  public void pause(){
    paused.set(true);
    logger.log(MQTT_SN_PIPELINE_PAUSED, protocol.getName());

  }

  public void resume(){
    paused.set(false);
    logger.log(MQTT_SN_PIPELINE_RESUMED, protocol.getName());
    sendNext();
  }

  public void queue(@NotNull @NonNull MessageEvent messageEvent){
    QualityOfService qos = messageEvent.getSubscription().getContext().getQualityOfService();
    if(paused.get()){
      if(dropQoS0 &&
          messageEvent.getMessage().getQualityOfService().getLevel() == 0 &&
          messageEvent.getSubscription().getDepth() > 1){
        messageEvent.getCompletionTask().run();
        logger.log(MQTT_SN_PIPELINE_EVENT_DROPPED, protocol.getName(), messageEvent.getDestinationName(), messageEvent.getMessage().getIdentifier(), messageEvent.getMessage().getQualityOfService().getLevel());
        return;
      }
      publishContexts.offer(messageEvent);
      logger.log(MQTT_SN_PIPELINE_EVENT_QUEUED, protocol.getName(), messageEvent.getDestinationName(), messageEvent.getMessage().getIdentifier());

    }
    else {
      if (publishContexts.size()+1 <= maxInFlightEvents) {
        if (qos.getLevel() > 0) {
          logger.log(MQTT_SN_PIPELINE_EVENT_QUEUED, protocol.getName(), messageEvent.getDestinationName(), messageEvent.getMessage().getIdentifier());
          publishContexts.offer(messageEvent);
        }
        send(messageEvent);
      } else {
        logger.log(MQTT_SN_PIPELINE_EVENT_QUEUED, protocol.getName(), messageEvent.getDestinationName(), messageEvent.getMessage().getIdentifier());
        publishContexts.offer(messageEvent);
      }
    }
  }

  public void completed(int messageId) {
    publishContexts.poll(); // Remove outstanding publish
    logger.log(MQTT_SN_PIPELINE_EVENT_COMPLETED, protocol.getName());
    if (!paused.get() || empty.get() != 0) {
      sendNext();
    }
  }

  private void sendNext(){
    MessageEvent messageEvent = publishContexts.peek();
    if(messageEvent != null) {
      QualityOfService qos = messageEvent.getSubscription().getContext().getQualityOfService();
      if(messageEvent.getMessage().getCreation() + eventTimeout > System.currentTimeMillis()) {
        if (qos.getLevel() == 0) {
          empty.decrementAndGet();
          send(messageEvent);
          completed(0);
        } else {
          send(messageEvent);
        }
      }
      else{
        logger.log(MQTT_SN_PIPELINE_EVENT_TIMED_OUT, protocol.getName(), messageEvent.getDestinationName(), messageEvent.getMessage().getIdentifier(), qos.getLevel());
        messageEvent.getCompletionTask().run();
        completed(0);
      }
    }
    if(publishContexts.size() == 0 && paused.get()){
      empty.set(0);
      stateEngine.getTopicAliasManager().clear();
      if(completion != null) {
        completion.run();
        completion = null;
      }
    }
  }

  private void send(MessageEvent messageEvent){
    QualityOfService qos = messageEvent.getSubscription().getContext().getQualityOfService();
    int messageId = 0;
    if (qos.isSendPacketId()) {
      messageId = packetIdManager.nextPacketIdentifier(messageEvent.getSubscription(), messageEvent.getMessage().getIdentifier());
    }
    short topicTypeId = TOPIC_NAME;
    short alias = stateEngine.getTopicAliasManager().findTopicAlias(messageEvent.getDestinationName());
    if(alias == -1){
      alias = (short)stateEngine.getTopicAliasManager().findRegisteredTopicAlias(protocol.getAddressKey(), messageEvent.getDestinationName());
      if(alias != -1){
        topicTypeId = TOPIC_PRE_DEFINED_ID;
      }
    }

    //
    // If this event is from a wild card then the client would not have registered it, so lets do that now
    //
    if (alias == -1) {
      //
      // Updating the client with the new topic id for the destination
      //
      alias = stateEngine.getTopicAliasManager().getTopicAlias(messageEvent.getDestinationName());
      Register register = new Register(alias, TOPIC_NAME, messageEvent.getDestinationName());
      protocol.writeFrame(register);
    }
    MQTT_SNPacket publish = protocol.buildPublish(alias, messageId,  messageEvent, qos, topicTypeId);
    stateEngine.sendPublish(protocol, messageEvent.getDestinationName(), publish);
    logger.log(MQTT_SN_PIPELINE_EVENT_SENT, protocol.getName(), messageEvent.getDestinationName(), messageEvent.getMessage().getIdentifier());
  }

  public void emptyQueue(int sendSize, Runnable task) {
    int size = size();
    logger.log(MQTT_SN_PIPELINE_WOKEN, protocol.getName(), sendSize,size);


    this.completion = task;
    if(size == 0){
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
