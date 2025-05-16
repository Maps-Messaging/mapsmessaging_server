/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.*;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GatewayManager {

  private final Map<String, GatewayInfo> gatewayMap;
  private final Session session;
  private final String inbound;
  private final String status;
  private final String outbound;
  private final int maxQueued;
  private final Future<?> scheduledTask;

  public GatewayManager(Session session, String inbound, String status, String outbound, int maxQueued) {
    gatewayMap = new ConcurrentHashMap<>();
    this.session = session;
    this.inbound = inbound;
    this.status = status;
    this.outbound = outbound;
    this.maxQueued = maxQueued;
    scheduledTask = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new TimeoutManager(), 30, 30, TimeUnit.SECONDS);
  }

  public void close() {
    scheduledTask.cancel(true);
  }

  public GatewayInfo getInfo(byte[] gatewayIdentifier) throws IOException {
    GatewayInfo info = gatewayMap.get(dumpIdentifier(gatewayIdentifier));
    if (info == null) {
      info = createInfo(gatewayIdentifier);
    }
    info.setLastAccess(System.currentTimeMillis());
    return info;
  }

  public GatewayInfo getInfo(String gatewayIdentifier) {
    GatewayInfo info = gatewayMap.get(gatewayIdentifier);
    if (info != null) {
      info.setLastAccess(System.currentTimeMillis());
    }
    return info;
  }

  private GatewayInfo createInfo(byte[] gatewayIdentifier) throws IOException {
    String name = dumpIdentifier(gatewayIdentifier);
    String inTopic = inbound.replace("{gatewayId}", name);
    String statusTopic = status.replace("{gatewayId}", name);
    try {
      Destination in = session.findDestination(inTopic, DestinationType.TOPIC).get();
      Destination status =session.findDestination(statusTopic, DestinationType.TOPIC).get();
      String outTopic = outbound.replace("{gatewayId}", name);

      SubscriptionContext subscriptionContext = new SubscriptionContext(outTopic);
      subscriptionContext.setAlias(name);
      subscriptionContext.setCreditHandler(CreditHandler.AUTO);
      subscriptionContext.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
      subscriptionContext.setRetainHandler(RetainHandler.SEND_ALWAYS);
      subscriptionContext.setAcknowledgementController(ClientAcknowledgement.AUTO);
      subscriptionContext.setReceiveMaximum(maxQueued);
      GatewayInfo info = new GatewayInfo(gatewayIdentifier, name, in, status, session.addSubscription(subscriptionContext));
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
      if (t.length() == 1) {
        t = "0" + t;
      }
      sb.append(t);
    }
    return sb.toString();
  }

  private final class TimeoutManager implements Runnable {

    @Override
    public void run() {
      long timeout = System.currentTimeMillis() - 600000;
      List<GatewayInfo> timedOut = new ArrayList<>();
      for (GatewayInfo info : gatewayMap.values()) {
        if (info.getLastAccess() < timeout) {
          timedOut.add(info);
        }
      }
      for (GatewayInfo old : timedOut) {
        old.close(session);
        gatewayMap.remove(old.getName());
      }
    }
  }

}
