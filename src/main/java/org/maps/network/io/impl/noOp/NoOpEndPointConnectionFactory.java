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
import java.util.List;
import org.maps.network.EndPointURL;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectedCallback;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.impl.SelectorLoadManager;

public class NoOpEndPointConnectionFactory implements EndPointConnectionFactory {

  @Override
  public EndPoint connect(EndPointURL url, SelectorLoadManager selector, EndPointConnectedCallback callback, EndPointServerStatus endPointServerStatus, List<String> jmxPath)
      throws IOException {
    EndPoint endPoint = new NoOpEndPoint(generateID(),endPointServerStatus, jmxPath);
    callback.connected(endPoint);
    return endPoint;
  }

  @Override
  public String getName() {
    return "noop";
  }

  @Override
  public String getDescription() {
    return "Provides a EndPoint object for a loop based protocol";
  }
}
