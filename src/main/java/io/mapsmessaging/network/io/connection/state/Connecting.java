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

import java.io.IOException;

public class Connecting extends State {

  public Connecting(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    if(endPointConnection.getUrl().getProtocol().equalsIgnoreCase("udp")||
       endPointConnection.getUrl().getProtocol().equalsIgnoreCase("serial")){
      // This is a UDP connection, we are connected by default
      try {
        endPointConnection.handleNewEndPoint(endPointConnection.getConnection().getEndPoint());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    // Need to wait for the protocol to be established before we can move on
  }

  @Override
  public String getName() {
    return "Connecting";
  }
}
