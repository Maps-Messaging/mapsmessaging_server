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

package org.maps.network.io.impl.noOp;

import java.io.IOException;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.io.AcceptHandler;
import org.maps.network.io.EndPointServer;
import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;

public class NoOpEndPointServer extends EndPointServer {

  public NoOpEndPointServer(AcceptHandler accept, EndPointURL url, NetworkConfig config) {
    super(accept, url, config);
  }

  @Override
  public void register() throws IOException {

  }

  @Override
  public void deregister() throws IOException {

  }

  @Override
  public void start() throws IOException {

  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(NoOpEndPointServer.class);
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {

  }
}
