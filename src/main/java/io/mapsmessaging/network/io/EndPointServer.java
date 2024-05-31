/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.config.network.EndPointServerConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.EndPointURL;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

public abstract class EndPointServer extends EndPointServerStatus implements Closeable, Selectable {

  protected AcceptHandler acceptHandler;
  protected final Logger logger;
  protected final Map<Long, EndPoint> activeEndPoints;

  @Getter
  private final EndPointServerConfig config;

  protected EndPointServer(AcceptHandler accept, EndPointURL url, EndPointServerConfig config) {
    super(url);
    this.config = config;
    acceptHandler = accept;
    activeEndPoints = new ConcurrentHashMap<>();
    logger = createLogger(url.toString());
    if(config != null) {
      MessageDaemon.getInstance().getDiscoveryManager().register(this);
    }
  }

  public void close()throws IOException{
    MessageDaemon.getInstance().getDiscoveryManager().deregister(this);
  }

  public long generateID() {
    return Constants.getNextId();
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
    activeEndPoints.put(endPoint.getId(), endPoint);
    acceptHandler.accept(endPoint);
  }

  public List<EndPoint> getActiveEndPoints() {
    return new ArrayList<>(activeEndPoints.values());
  }

  public String getConfigName() {
    String tmp = config.getName();
    if(tmp == null || !tmp.isEmpty()) {
      tmp = getName();
    }
    return tmp;
  }

  public String getName() {
    return url.getProtocol() + "_" + url.getHost() + "_" + url.getPort();
  }

  public abstract void start() throws IOException;

  protected abstract Logger createLogger(String url);

}
