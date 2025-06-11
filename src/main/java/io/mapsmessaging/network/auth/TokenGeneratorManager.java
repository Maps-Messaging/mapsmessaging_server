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

package io.mapsmessaging.network.auth;

import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class TokenGeneratorManager implements ServiceManager {

  private static class Holder {
    static final TokenGeneratorManager INSTANCE = new TokenGeneratorManager();
  }

  public static TokenGeneratorManager getInstance() {
    return Holder.INSTANCE;
  }


  private final Map<String, Service> tokenGeneratorMap;

  public TokenGenerator get(String tokenGenerator) {
    return (TokenGenerator) tokenGeneratorMap.get(tokenGenerator);
  }

  private TokenGeneratorManager() {
    ServiceLoader<TokenGenerator> transformerServiceLoader = ServiceLoader.load(TokenGenerator.class);
    tokenGeneratorMap = new LinkedHashMap<>();
    for (TokenGenerator tokenGenerator : transformerServiceLoader) {
      tokenGeneratorMap.put(tokenGenerator.getName(), tokenGenerator);
    }
  }

  @Override
  public Iterator<Service> getServices() {
    return tokenGeneratorMap.values().iterator();
  }
}
