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

package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.*;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class UDPFacadeEndPoint extends EndPoint {

  private static final AtomicInteger counter = new AtomicInteger(0);

  private final EndPoint endPoint;
  private final SocketAddress fromAddress;

  public UDPFacadeEndPoint(EndPoint endPoint, SocketAddress fromAddress, EndPointServerStatus server) {
    super(counter.incrementAndGet(), server);
    this.endPoint = endPoint;
    this.fromAddress = fromAddress;
    List<String> end = new ArrayList<>(endPoint.getJMXTypePath());
    end.remove(end.size() - 1);

    String entry = strip("endPointName=" + endPoint.getName());
    end.add(entry);
    String remote = strip("remoteHost=" + getName());
    end.add(remote);
    jmxParentPath = end;
    EndPointServer endPointServer = (EndPointServer)server;
    endPointServer.registerNewEndPoint(this);
  }

  private String strip(String val) {
    while (val.contains(":")) {
      val = val.replace(":", "_");
    }
    return val;
  }

  @Override
  public String getProtocol() {
    return endPoint.getProtocol();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return endPoint.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return endPoint.readPacket(packet);
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return endPoint.register(selectionKey, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return endPoint.deregister(selectionKey);
  }

  @Override
  public String getAuthenticationConfig() {
    return endPoint.getAuthenticationConfig();
  }

  @Override
  public String getName() {
    return "udp:/" + fromAddress.toString();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(UDPFacadeEndPoint.class);
  }



  @Override
  public void close() throws IOException {
    endPoint.close();
    EndPointServer endPointServer = (EndPointServer)endPoint.getServer();
    endPointServer.handleCloseEndPoint(this);
  }

  @Override
  public boolean isClient(){
    return endPoint.isClient();
  }

  @Override
  public boolean isUDP() {
    return endPoint.isUDP();
  }

  @Override
  public Subject getEndPointSubject() {
    return endPoint.getEndPointSubject();
  }

  @Override
  public Principal getEndPointPrincipal() {
    return endPoint.getEndPointPrincipal();
  }

  @Override
  public boolean isSSL() {
    return endPoint.isSSL();
  }


}
