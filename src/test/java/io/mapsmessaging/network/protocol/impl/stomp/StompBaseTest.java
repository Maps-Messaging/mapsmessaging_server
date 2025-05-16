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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.test.BaseTestConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import org.junit.jupiter.params.provider.Arguments;
import org.projectodd.stilts.stomp.Headers;

public class StompBaseTest extends BaseTestConfig {


  int[] tcp_ports = {8674, 8675};
  int[] ssl_ports = {8694, 8695};


  static Stream<Arguments> testParameters() {
    List<Arguments> argumentsList = new ArrayList<>();
    String[] connectionTypes = {"tcp", "ws"};
    boolean[] authOptions = {false, true};
    for (String connectionType : connectionTypes) {
      for (boolean auth : authOptions) {
        argumentsList.add(Arguments.of(connectionType, auth));
      }
    }
    return argumentsList.stream();
  }

  public Headers getHeaders(boolean auth) throws IOException {
    if (auth) {
      return new AuthHeaders("admin", getPassword("admin"));
    }
    return new AuthHeaders("", "");
  }

  public Client getClient() throws IOException, LoginException {
    return new Client("127.0.0.1", 8675, "admin", getPassword("admin"));
  }

  public Client getClient(String protocol, boolean auth) throws IOException, LoginException {
    if (auth) {
      return new Client("localhost", getPort(protocol, auth), "admin", getPassword("admin"));
    }
    return new Client("localhost", getPort(protocol, auth), "", "");
  }

  private int getPort(String protocol, boolean auth) {
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    int[] ports = isSsl ? ssl_ports : tcp_ports;
    int portIndex = auth ? 1 : 0;
    return ports[portIndex];
  }

  public String getUrl(String protocol, boolean auth) {
    boolean isSsl = protocol.equalsIgnoreCase("ssl") || protocol.equalsIgnoreCase("wss");
    int[] ports = isSsl ? ssl_ports : tcp_ports;
    int portIndex = auth ? 1 : 0;
    if (protocol.equalsIgnoreCase("tcp")) protocol = "";
    return "stomp+" + protocol + "://localhost:" + ports[portIndex];
  }

  private class AuthHeaders implements Headers {

    private final String username;
    private final String password;

    public AuthHeaders(String username, String password) {
      this.username = username;
      this.password = password;
    }

    @Override
    public Set<String> getHeaderNames() {
      return Set.of("login", "passcode");
    }

    @Override
    public String get(String s) {
      switch (s.toLowerCase()) {
        case "login":
          return username;

        case "passcode":
          return password;
      }
      return null;
    }

    @Override
    public String put(String s, String s1) {
      System.err.println("hmm");
      return null;
    }

    @Override
    public void remove(String s) {
      System.err.println("hmm");

    }

    @Override
    public Headers duplicate() {
      return this;
    }
  }

}
