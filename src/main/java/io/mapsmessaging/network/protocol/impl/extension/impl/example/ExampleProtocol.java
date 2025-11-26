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

package io.mapsmessaging.network.protocol.impl.extension.impl.example;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.extension.Extension;

import java.io.IOException;

public class ExampleProtocol extends Extension {

  private final ExtensionConfigDTO protocolConfig; // protocolConfig.getConfig() contains the extensions config section
  private final EndPoint endPoint;

  public ExampleProtocol(EndPoint endPoint, ExtensionConfigDTO protocolConfigDTO) {
    this.protocolConfig = protocolConfigDTO;
    this.endPoint = endPoint;
  }

  @Override
  public void close() throws IOException {
    // Close all resources and finish operations
  }

  @Override
  public void initialise() throws IOException {
    // This is where you actually initialise / connect and do the startup for the extentsion
  }

  @Override
  public String getName() {
    return "Example";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public boolean supportsRemoteFiltering() {
    return false;
  }


  /*
  this function receives an event and simply sends it back to the server
   */
  @Override
  public void outbound(String destination, Message message) {
    System.err.println("Receive event for "+destination);
    MessageBuilder messageBuilder = new MessageBuilder(message);
    try {
      inbound(destination, messageBuilder.build());
    } catch (IOException e) {
      // Log here or do something.
    }
  }

  @Override
  public void registerRemoteLink(String destination, String filter) throws IOException {
    // This is for information, it tells you what remote destination/filter has been configured to use
  }

  @Override
  public void registerLocalLink(String destination) throws IOException {
    // This is for information, it tells you what local destination/filter has been configured to use
  }
}
