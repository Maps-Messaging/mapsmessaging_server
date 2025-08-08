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

package io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.io;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.OrbCommOgwsDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.CommonMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.TerminalInfo;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.io.IncomingMessageHandler;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.io.OrbcommOgwsEndPoint;

import javax.security.auth.login.LoginException;
import java.io.IOException;

import static io.mapsmessaging.logging.ServerLogMessages.OGWS_NO_CONFIGURATION_FOUND;

public class InmarsatEndPointServer extends EndPointServer implements IncomingMessageHandler {

  private final ProtocolConfigDTO protocolConfigDTO;



  protected InmarsatEndPointServer(AcceptHandler accept, EndPointURL url, EndPointServerConfigDTO config) throws IOException {
    super(accept, url, config);
    protocolConfigDTO = config.getProtocolConfig("inmarsat");
    if(!(protocolConfigDTO instanceof OrbCommOgwsDTO orbCommOgwsDTO)) {
      logger.log(OGWS_NO_CONFIGURATION_FOUND);
      throw new IOException("no configuration found");
    }
  }


  @Override
  public void register() throws IOException {
    // These are no ops
  }

  @Override
  public void deregister() throws IOException {
    // These are no ops
  }

  @Override
  public void start() throws IOException {

  }

  @Override
  protected Logger createLogger(String url) {
    return logger;
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    //
  }

  private OrbcommOgwsEndPoint locateOrCreateEndPoint(TerminalInfo terminalInfo) throws IOException, LoginException {

    return null;
  }

  @Override
  public void handleIncomingMessage() {
  }

  public void sendClientMessage(String primeId, CommonMessage commonMessage) {

  }
}
