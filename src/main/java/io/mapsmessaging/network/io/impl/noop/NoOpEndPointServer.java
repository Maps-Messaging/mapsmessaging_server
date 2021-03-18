/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.network.io.impl.noop;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import java.io.IOException;

/**
 * This class is simply a place holder where an EndPointServer object is required but we don't have
 * an end point. For example, locally connected protocols that have no network requirements
 */
public class NoOpEndPointServer extends EndPointServer {

  public NoOpEndPointServer(AcceptHandler accept, EndPointURL url, NetworkConfig config) {
    super(accept, url, config);
  }

  @Override
  public void register() throws IOException {
    // There is nothing here to do, its all a No Operation
  }

  @Override
  public void deregister() throws IOException {
    // There is nothing here to do, its all a No Operation
  }

  @Override
  public void start() throws IOException {
    // There is nothing here to do, its all a No Operation
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(NoOpEndPointServer.class);
  }

  @Override
  public void close() throws IOException {
    // There is nothing here to do, its all a No Operation
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // There is nothing here to do, its all a No Operation
  }
}
