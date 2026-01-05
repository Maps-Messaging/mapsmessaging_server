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

import com.google.gson.JsonObject;
import io.mapsmessaging.config.protocol.impl.MavlinkConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.MavlinkCodec;
import io.mapsmessaging.mavlink.MavlinkFrameCodec;
import io.mapsmessaging.mavlink.MavlinkFrameEnvelope;
import io.mapsmessaging.mavlink.MavlinkMessageFormatLoader;
import io.mapsmessaging.mavlink.message.MavlinkCompiledMessage;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.io.impl.udp.UDPFacadeEndPoint;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MavlinkInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final MavLinkSessionManager<MavlinkProtocol> currentSessions;
  private final ProtocolMessageTransformation transformation;
  private final MavlinkFrameCodec mavlinkFrameCodec;
  private final MavlinkCodec mavlinkMessageCodec;

  private final MavlinkConfig mavlinkConfig;
  private final List<InetSocketAddress> forwardList;



  public MavlinkInterfaceManager(InterfaceInformation info, EndPoint endPoint) throws IOException {
    logger = LoggerFactory.getLogger("Mavlink Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    mavlinkConfig = (MavlinkConfig) endPoint.getConfig().getProtocolConfig("mavlink");
    long timeout = mavlinkConfig.getIdleSessionTimeout();
    mavlinkMessageCodec  = MavlinkMessageFormatLoader.getInstance().getDialect("common").get();
    mavlinkFrameCodec = new MavlinkFrameCodec(mavlinkMessageCodec);
    currentSessions = new MavLinkSessionManager<>(timeout);

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "mavlink",
        "<registered>"
    );
    forwardList = new ArrayList<>();
    String urlList = mavlinkConfig.getForwardUrls();
    if(urlList != null || !urlList.isBlank()){
      String[] urls = urlList.split(",");
      for(String remote:urls){
        try {
          URI uri = URI.create(remote);
          forwardList.add( new InetSocketAddress(uri.getHost(), uri.getPort()));
        } catch (Exception e) {
          e.printStackTrace();
          // log
        }
      }
    }
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
    byte[] mavlink = raw;
    packet.position(pos);
    Optional<MavlinkFrameEnvelope>  envelope = mavlinkFrameCodec.tryUnpackHeaderAndPayload(packet.getRawBuffer());
    if(envelope.isPresent()){
      MavlinkFrameEnvelope env = envelope.get();
      MavlinkDeviceKey key = buildKey(packet, env);
      UDPSessionState<MavlinkProtocol> state = findOrCreate(key);
      if(fromForward(packet)){
        state.getContext().processPacket(packet);
      }
      else if (state.getContext() != null) {
        MavlinkProtocol protocol = state.getContext();
        if(mavlinkConfig.isParseToJson()){
          Map<String, Object> parsed = mavlinkMessageCodec.parsePayload(env.getMessageId(), env.getPayload());
          JsonObject complete  = MavlinkJsonEnvelopeBuilder.toJson(env, parsed);
          raw = complete.toString().getBytes();
        }
        MavlinkCompiledMessage message = mavlinkMessageCodec.getRegistry().getCompiledMessagesById().get(env.getMessageId());
        String messageName = "";
        if(message != null){
          messageName = message.getName();
        }
        protocol.processPacket(env, messageName, raw);
        forwardPacket(mavlink);
      }
      else{
        System.err.println("Has NO State");
      }
    }
    selectorTask.register(SelectionKey.OP_READ);
    return true;
  }

  private MavlinkDeviceKey buildKey(Packet packet, MavlinkFrameEnvelope envelope){
    return new MavlinkDeviceKey(0, (InetSocketAddress) packet.getFromAddress(),  envelope.getSystemId());
  }

  private synchronized UDPSessionState<MavlinkProtocol> findOrCreate(MavlinkDeviceKey key){
    UDPSessionState<MavlinkProtocol> state = currentSessions.getState(key);
    if(state == null){
      UDPFacadeEndPoint facade = new UDPFacadeEndPoint(endPoint, key.getRemoteAddress(),endPoint.getServer());
      try {
        MavlinkProtocol protocol = new MavlinkProtocol(this, key, facade, this.mavlinkConfig);
        state = new UDPSessionState<>(protocol);
        currentSessions.addState(key, state);
      }
      catch(IOException e){
        e.printStackTrace();
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
      } catch (IOException e) {
        e.printStackTrace();
        // To Do log
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
