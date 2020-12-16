/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.engine.security;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

public class SaslManager {

  private static final SaslManager instance = new SaslManager();
  private static SaslManager getInstance(){
    return instance;
  }


  SaslServer getServer(String mechanism, String protocol, String serverName, Map<String,?> props, CallbackHandler serverHandler) throws SaslException {
    return Sasl.createSaslServer(mechanism, protocol, serverName, props, serverHandler);
  }

  private SaslManager(){}

}
