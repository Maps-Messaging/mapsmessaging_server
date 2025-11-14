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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;

import java.io.IOException;

public class Shutdown extends State {

  public Shutdown(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    if (endPointConnection.getProtocol() != null) {
      try {
        endPointConnection.getProtocol().close();
      } catch (IOException ioException) {
        // We are closing, so not too fussed here
      }
    }
  }

  @Override
  public String getName() {
    return "Shutdown";
  }

  @Override
  public LinkState getLinkState() {
    return LinkState.FAILED;
  }
}
