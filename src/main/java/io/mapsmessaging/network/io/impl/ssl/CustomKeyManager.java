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

package io.mapsmessaging.network.io.impl.ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

class CustomKeyManager extends X509ExtendedKeyManager {

  private X509ExtendedKeyManager originalKeyManager;
  private String alias;

  public CustomKeyManager(X509ExtendedKeyManager keyManager, String alias) {
    this.originalKeyManager = keyManager;
    this.alias = alias;
  }

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers) {
    return new String[0];
  }

  @Override
  public String chooseClientAlias(String[] keyType, java.security.Principal[] issuers, java.net.Socket socket) {
    return alias;
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers) {
    return new String[]{alias};
  }

  @Override
  public String chooseServerAlias(String keyType, java.security.Principal[] issuers, java.net.Socket socket) {
    return alias;
  }

  @Override
  public X509Certificate[] getCertificateChain(String alias) {
    return originalKeyManager.getCertificateChain(alias);
  }

  @Override
  public PrivateKey getPrivateKey(String alias) {
    return originalKeyManager.getPrivateKey(alias);
  }

  @Override
  public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
    return alias;
  }

  @Override
  public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
    return alias;
  }
}