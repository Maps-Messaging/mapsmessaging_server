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

package org.maps.network.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.maps.logging.Logger;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;

public abstract class EndPointServer implements Closeable, Selectable {

  protected final Logger logger;
  protected final AcceptHandler acceptHandler;
  protected final LinkedHashMap<Long, EndPoint> activeEndPoints;
  private final NetworkConfig config;
  private final AtomicLong idGenerator;
  private final EndPointURL url;

  private final LongAdder totalPacketsSent;
  private final LongAdder totalPacketsRead;

  private final LongAdder totalBytesSent;
  private final LongAdder totalBytesRead;

  public EndPointServer(AcceptHandler accept, EndPointURL url, NetworkConfig config) {
    this.url = url;
    this.config = config;
    acceptHandler = accept;
    activeEndPoints = new LinkedHashMap<>();
    idGenerator = new AtomicLong(0);
    logger = createLogger(url.toString());
    totalPacketsSent = new LongAdder();
    totalPacketsRead = new LongAdder();
    totalBytesSent = new LongAdder();
    totalBytesRead = new LongAdder();
  }

  public long generateID() {
    return idGenerator.incrementAndGet();
  }

  public abstract void register() throws IOException;

  public abstract void deregister() throws IOException;

  public int size() {
    return activeEndPoints.size();
  }

  public void handleCloseEndPoint(EndPoint endPoint) {
    activeEndPoints.remove(endPoint.getId());
  }

  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    if (activeEndPoints.containsKey(endPoint.getId())) {
      activeEndPoints.remove(endPoint.getId());
    }
    activeEndPoints.put(endPoint.getId(), endPoint);
    acceptHandler.accept(endPoint);
  }

  public EndPointURL getUrl() {
    return url;
  }

  public String getName() {
    return url.getProtocol() + "_" + url.getHost() + "_" + url.getPort();
  }

  public NetworkConfig getConfig() {
    return config;
  }

  public abstract void start() throws IOException;

  protected abstract Logger createLogger(String url);

  public long getTotalPacketsRead(){
    return totalPacketsRead.sum();
  }

  public long getTotalPacketsSent(){
    return totalPacketsSent.sum();
  }

  public long getTotalBytesSent(){
    return totalBytesSent.sum();
  }

  public long getTotalBytesRead(){
    return totalBytesRead.sum();
  }

  public void incrementPacketsSent(){
    totalPacketsSent.increment();
  }

  public void incrementPacketsRead(){
    totalPacketsRead.increment();
  }

  public void updateBytesSent(int count){
    totalBytesSent.add(count);
  }

  public void updateBytesRead(int count){
    totalBytesRead.add(count);
  }
}
