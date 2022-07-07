/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.ProtocolJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Timeoutable;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ProtocolImpl implements SelectorCallback, MessageListener, Timeoutable {

  private static final LongAdder totalReceived = new LongAdder();
  private static final LongAdder totalSent = new LongAdder();

  public static long getTotalReceived() {
    return totalReceived.sum();
  }

  public static long getTotalSent() {
    return totalSent.sum();
  }

  protected final EndPoint endPoint;

  protected final LinkedMovingAverages sentMessageAverages;
  protected final LinkedMovingAverages receivedMessageAverages;

  protected ProtocolMessageTransformation transformation;
  protected final Map<String, Transformer> destinationTransformerMap;

  private final ProtocolJMX mbean;
  protected long keepAlive;
  private boolean connected;
  private boolean completed;

  protected ProtocolImpl(@NonNull @NotNull EndPoint endPoint) {
    this.endPoint = endPoint;
    sentMessageAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Sent Packets", 1, 5, 4, TimeUnit.MINUTES, "Messages");
    receivedMessageAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Received Packets", 1, 5, 4, TimeUnit.MINUTES, "Messages");
    mbean = new ProtocolJMX(endPoint.getJMXTypePath(), this);
    connected = false;
    completed = false;
    destinationTransformerMap = new ConcurrentHashMap<>();
  }

  public void completedConnection() {
    if (!completed) {
      completed = true;
      endPoint.completedConnection();
    }
  }

  public ProtocolMessageTransformation getTransformation() {
    return transformation;
  }

  public void setTransformation(ProtocolMessageTransformation transformation) {
    this.transformation = transformation;
  }

  @Override
  public void close() throws IOException {
    if (mbean != null) {
      mbean.close();
    }
    endPoint.close();
  }

  public void connect(String sessionId, String username, String password) throws IOException {
  }

  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable Transformer transformer) throws IOException {
  }

  public void subscribeLocal(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable String selector, @Nullable Transformer transformer)
      throws IOException {
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public LinkedMovingAverages getSentMessages() {
    return sentMessageAverages;
  }

  public LinkedMovingAverages getReceivedMessages() {
    return receivedMessageAverages;
  }

  public void receivedMessage() {
    receivedMessageAverages.increment();
    totalReceived.increment();
  }

  public void sentMessage() {
    sentMessageAverages.increment();
    totalSent.increment();
  }

  public void sendKeepAlive() {
    // by default we don't do anything. A protocol that needs to do something can override this function
  }

  @Override
  public long getTimeOut() {
    return keepAlive;
  }

  public void setKeepAlive(long keepAliveMilliseconds) {
    keepAlive = keepAliveMilliseconds;
  }

  public void setConnected(boolean connected) {
    if (this.connected != connected) {
      this.connected = connected;
      try {
        if (connected) {
          endPoint.getServer().handleNewEndPoint(endPoint);
        } else {
          endPoint.getServer().handleCloseEndPoint(endPoint);
        }
      } catch (IOException ioException) {
        endPoint.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_FAILED, ioException);
        try {
          endPoint.close();
        } catch (IOException e) {
          // we are closing due to an exception, we know we are in an exception state but we just need to tidy up
        }
      }
    }
  }

  public boolean isConnected() {
    return connected;
  }

  protected Message processTransformer(String normalisedName, Message message) {
    Transformer transformer = destinationTransformerMap.get(normalisedName);
    if (transformer != null) {
      MessageBuilder mb = new MessageBuilder(message);
      mb.setDestinationTransformer(transformer);
      message = mb.build();
    }
    return message;
  }

  public Transformer destinationTransformationLookup(String name) {
    return destinationTransformerMap.get(name);
  }

  protected SubscriptionContextBuilder createSubscriptionContextBuilder(String resource, String selector, QualityOfService qos, int receiveMax) {
    ClientAcknowledgement ackManger = qos.getClientAcknowledgement();
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(resource, ackManger);
    builder.setAlias(resource);
    builder.setQos(qos);
    builder.setAllowOverlap(true);
    builder.setReceiveMaximum(receiveMax);
    if (selector != null && selector.length() > 0) {
      builder.setSelector(selector);
    }
    return builder;
  }
}
