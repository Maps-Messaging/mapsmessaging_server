/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.MessageListener;
import org.maps.network.admin.ProtocolJMX;
import org.maps.network.io.EndPoint;
import org.maps.network.io.impl.SelectorCallback;
import org.maps.utilities.stats.LinkedMovingAverages;
import org.maps.utilities.stats.MovingAverageFactory;
import org.maps.utilities.stats.MovingAverageFactory.ACCUMULATOR;

public abstract class ProtocolImpl implements SelectorCallback, MessageListener {

  private static final LongAdder totalReceived = new LongAdder();
  private static final LongAdder totalSent = new LongAdder();

  public static long getTotalReceived(){
    return totalReceived.sum();
  }
  public static long getTotalSent(){
    return totalSent.sum();
  }

  protected final EndPoint endPoint;

  protected final LinkedMovingAverages sentMessageAverages;
  protected final LinkedMovingAverages receivedMessageAverages;

  protected ProtocolMessageTransformation transformation;

  private final ProtocolJMX mbean;
  protected long keepAlive;
  private boolean connected;

  public ProtocolImpl(@NotNull EndPoint endPoint) {
    this.endPoint = endPoint;
    sentMessageAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Sent Packets", 1, 5, 4, TimeUnit.MINUTES, "Messages");
    receivedMessageAverages = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, "Received Packets", 1, 5, 4, TimeUnit.MINUTES, "Messages");
    mbean = new ProtocolJMX(endPoint.getJMXTypePath(), this);
    connected = false;
  }

  public ProtocolMessageTransformation getTransformation() {
    return transformation;
  }

  public void setTransformation(ProtocolMessageTransformation transformation) {
    this.transformation = transformation;
  }

  public void close() throws IOException {
    if (mbean != null) {
      mbean.close();
    }
    endPoint.close();
  }

  public void connect() throws IOException{
  }

  public void subscribeRemote(String resource, String mappedResource) throws IOException{
  }

  public void subscribeLocal(String resource, String mappedResource) throws IOException {
  }

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

  public abstract void sendKeepAlive();

  public long getKeepAlive() {
    return keepAlive;
  }

  public void setKeepAlive(long keepAliveMilliseconds) {
    keepAlive = keepAliveMilliseconds;
  }

  public void setConnected(boolean connected) {
    if(this.connected != connected){
      this.connected = connected;
      try {
        if(connected) {
          endPoint.getServer().handleNewEndPoint(endPoint);
        }
        else{
          endPoint.getServer().handleCloseEndPoint(endPoint);
        }
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  public boolean isConnected(){
    return connected;
  }
}
