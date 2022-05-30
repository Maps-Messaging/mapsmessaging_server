package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class GatewayManager {

  private final Map<String, GatewayInfo> gatewayMap;
  private final Session session;
  private final String inbound;
  private final String outbound;
  private final int maxQueued;

  public GatewayManager(Session session, String inbound, String outbound, int maxQueued){
    gatewayMap = new ConcurrentHashMap<>();
    this.session = session;
    this.inbound = inbound;
    this.outbound = outbound;
    this.maxQueued = maxQueued;
  }

  public GatewayInfo getInfo(byte[] gatewayIdentifier) throws IOException {
    GatewayInfo info = gatewayMap.get(dumpIdentifier(gatewayIdentifier));
    if(info == null){
      info = createInfo(gatewayIdentifier);
    }
    return info;
  }

  public GatewayInfo getInfo(String gatewayIdentifier){
    return gatewayMap.get(gatewayIdentifier);
  }


  private GatewayInfo createInfo(byte[] gatewayIdentifier) throws IOException {
    String name = dumpIdentifier(gatewayIdentifier);
    String inTopic = inbound.replace("{gatewayId}", name);
    try {
      Destination in = session.findDestination(inTopic, DestinationType.TOPIC).get();

      String outTopic = outbound.replace("{gatewayId}", name);

      SubscriptionContext subscriptionContext = new SubscriptionContext(outTopic);
      subscriptionContext.setAlias(name);
      subscriptionContext.setCreditHandler(CreditHandler.AUTO);
      subscriptionContext.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
      subscriptionContext.setRetainHandler(RetainHandler.SEND_ALWAYS);
      subscriptionContext.setAcknowledgementController(ClientAcknowledgement.AUTO);
      subscriptionContext.setReceiveMaximum(maxQueued);
      GatewayInfo info = new GatewayInfo(gatewayIdentifier, name, in, session.addSubscription(subscriptionContext));
      gatewayMap.put(name, info);
      return info;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Thread interrupted");
    } catch (ExecutionException e) {
      throw new IOException(e);
    }
  }

  public static String dumpIdentifier(byte[] gatewayIdentifier) {
    StringBuilder sb = new StringBuilder();
    for (byte b : gatewayIdentifier) {
      String t = Integer.toHexString(b & 0xff);
      if(t.length() == 1){
        t = "0"+t;
      }
      sb.append(t);
    }
    return sb.toString();
  }

}
