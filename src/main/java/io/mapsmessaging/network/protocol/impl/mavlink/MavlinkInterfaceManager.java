/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mavlink;


import io.mapsmessaging.config.protocol.impl.MavlinkConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.MavlinkEventFactory;
import io.mapsmessaging.mavlink.ProcessedFrame;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPFacadeEndPoint;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class MavlinkInterfaceManager implements SelectorCallback, MavlinkConnectionManager  {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final MavLinkSessionManager<MavlinkProtocol> currentSessions;
  private final MavlinkEventFactory mavlinkEventFactory;


  private final MavlinkConfig mavlinkConfig;
  private final List<InetSocketAddress> forwardList;

  public MavlinkInterfaceManager(EndPoint endPoint) throws IOException {
    logger = LoggerFactory.getLogger(MavlinkInterfaceManager.class);
    this.endPoint = endPoint;
    mavlinkConfig = (MavlinkConfig) endPoint.getConfig().getProtocolConfig("mavlink");
    long timeout = mavlinkConfig.getIdleSessionTimeout();
    String path = mavlinkConfig.getFullyQualifiedPathToDialectXml();
    Path dialectPath = null;
    if(path != null && !path.isBlank()){
      dialectPath = Path.of(path);
      if(!dialectPath.toFile().exists()){
        logger.log(MAVLINK_DIALECT_FAILED_TO_LOAD, path);
        dialectPath = null;
      }
    }
    mavlinkEventFactory  = loadDialect(dialectPath);
    currentSessions = new MavLinkSessionManager<>(timeout);

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);

    forwardList = new ArrayList<>();
    String urlList = mavlinkConfig.getForwardUrls();
    if(urlList != null && !urlList.isBlank()){
      String[] urls = urlList.split(",");
      for(String remote:urls){
        if(!remote.isBlank()) {
          try {
            URI uri = URI.create(remote);
            forwardList.add(new InetSocketAddress(uri.getHost(), uri.getPort()));
          } catch (Exception e) {
            logger.log(MAVLINK_FAILED_PARSING_FORWARD_LIST, remote, e);
          }
        }
      }
    }
  }

  private MavlinkEventFactory loadDialect(Path dialectPath) throws IOException {

    if(dialectPath != null){
      try {
        return new MavlinkEventFactory(dialectPath);
      } catch (ParserConfigurationException |SAXException e) {
        logger.log(MAVLINK_DIALECT_FORMAT_FAILURES, dialectPath.toString(), e);
      }
    }
    return new MavlinkEventFactory();
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }

    byte[] raw = new byte[packet.available()];
    int pos = packet.position();
    packet.get(raw);
    packet.position(pos);
    Optional<ProcessedFrame> potentialFrame = mavlinkEventFactory.unpack(endPoint.getName(), packet.getRawBuffer());
    if(potentialFrame.isPresent()){
      ProcessedFrame env = potentialFrame.get();
      logger.log(MAVLINK_DETECTED_PACKET, endPoint.getName(), env.getMessageName());
      MavlinkDeviceKey key = buildKey(packet, env.getFrame().getSystemId());
      boolean allowed = mavlinkConfig.getKnownSources() == null ||
          mavlinkConfig.getKnownSources().isEmpty() ||
          mavlinkConfig.getKnownSources().stream().anyMatch(knownSource -> knownSource.getSystemId() == key.getSystemId());

      if(allowed) {
        UDPSessionState<MavlinkProtocol> state = findOrCreate(key);
        if (fromForward(packet)) {
          state.getContext().processPacket(packet);
        } else if (state.getContext() != null) {
          MavlinkProtocol protocol = state.getContext();
          protocol.processRawFrame(env, raw);
          forwardPacket(raw);
        }
      }
      else{
        System.out.println("Ignoring packet from unknown source "+key.getSystemId());
        forwardPacket(raw);
      }
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private MavlinkDeviceKey buildKey(Packet packet, int systemId){
    return new MavlinkDeviceKey(0, (InetSocketAddress) packet.getFromAddress(),  systemId);
  }

  private synchronized UDPSessionState<MavlinkProtocol> findOrCreate(MavlinkDeviceKey key){
    UDPSessionState<MavlinkProtocol> state = currentSessions.getState(key);
    if(state == null){
      UDPFacadeEndPoint facade = new UDPFacadeEndPoint(endPoint, key.getRemoteAddress(),endPoint.getServer());
      try {
        MavlinkProtocol protocol = new MavlinkProtocol(this, key, facade, this.mavlinkConfig);
        state = new UDPSessionState<>(protocol);
        currentSessions.addState(key, state);
        logger.log(MAVLINK_SESSION_CREATED, key.toString());
      }
      catch(IOException e){
        logger.log(MAVLINK_FAILED_SETTING_UP_SESSION, key.toString(), e);
      }
    }
    return state;
  }

  private boolean fromForward(Packet packet){
    for(SocketAddress socketAddress:forwardList){
      if(socketAddress.equals(packet.getFromAddress())){
        return true;
      }
    }
    return false;
  }

  private void forwardPacket(byte[] raw){
    for(SocketAddress socketAddress:forwardList){
      ByteBuffer byteBuffer = ByteBuffer.wrap(raw);
      Packet forward = new Packet(byteBuffer);
      forward.setFromAddress(socketAddress);
      try {
        endPoint.sendPacket(forward);
        logger.log(MAVLINK_SUCCESSFUL_FORWARD_PACKET, socketAddress.toString());
      } catch (IOException e) {
        logger.log(MAVLINK_FAILED_FORWARD_PACKET, socketAddress.toString(), e);
      }
    }
  }

  @Override
  public void close() {
    currentSessions.close();
  }

  @Override
  public String getName() {
    return "Mavlink";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void close(MavlinkDeviceKey remoteClient) {
    currentSessions.deleteState(remoteClient);
  }

}
