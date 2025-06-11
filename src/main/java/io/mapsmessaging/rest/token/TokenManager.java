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

package io.mapsmessaging.rest.token;

import io.mapsmessaging.security.uuid.RandomVersions;
import io.mapsmessaging.security.uuid.UuidGenerator;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class TokenManager {


  private static class Holder {
    static final TokenManager INSTANCE = new TokenManager();
  }

  public static TokenManager getInstance() {
    return Holder.INSTANCE;
  }


  private TokenManager(){
  }

  public String generateToken(HttpSession session) {
    return generateToken(session, null);
  }

  public String generateToken(HttpSession session, String resource){
    if (session == null) return null;
    String token = UuidGenerator.getInstance().generate(RandomVersions.RANDOM).toString();
    TokenDetails tokenDetails = new TokenDetails(session.getId(), resource);

    Map<String, TokenDetails > tokens = (Map) session.getAttribute("token");
    if(tokens == null) {
      tokens = new ConcurrentHashMap<>();
      session.setAttribute("token", tokens);
    }
    if(tokens.size() > 2){
      tokens.clear();
    }
    tokens.put(token, tokenDetails);
    return token;
  }

  public boolean useToken(HttpSession session, String token){
    return useToken(session, token, null);
  }

  public boolean useToken(HttpSession session, String token, String resource) {
    Map<String, TokenDetails > tokens = (Map) session.getAttribute("token");
    if(tokens == null) {
      return false;
    }
    TokenDetails details = tokens.remove(token);
    return details != null && (resource == null || resource.equals(details.getResource()));
  }

}
