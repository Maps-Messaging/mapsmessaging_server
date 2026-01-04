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

package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.config.protocol.impl.MavlinkConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.mavlink.MavlinkCodec;
import io.mapsmessaging.mavlink.MavlinkFrameCodec;
import io.mapsmessaging.mavlink.MavlinkFrameEnvelope;
import io.mapsmessaging.mavlink.MavlinkMessageFormatLoader;
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
import java.nio.channels.SelectionKey;
import java.util.Optional;

public class MavlinkInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final MavLinkSessionManager<MavlinkProtocol> currentSessions;
  private final ProtocolMessageTransformation transformation;
  private final MavlinkFrameCodec mavlinkFrameCodec;

  private final MavlinkConfig mavlinkConfig;


  public MavlinkInterfaceManager(InterfaceInformation info, EndPoint endPoint) throws IOException {
    logger = LoggerFactory.getLogger("Mavlink Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    mavlinkConfig = (MavlinkConfig) endPoint.getConfig().getProtocolConfig("mavlink");
    long timeout = mavlinkConfig.getIdleSessionTimeout();
    MavlinkCodec  codec = MavlinkMessageFormatLoader.getInstance().getDialect("common").get();
    mavlinkFrameCodec = new MavlinkFrameCodec(codec);

    currentSessions = new MavLinkSessionManager<>(timeout);

    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "mavlink",
        "<registered>"
    );
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }
    System.err.println("PKT RECV:"+packet.getFromAddress());
    byte[] raw = new byte[packet.available()];
    int pos = packet.position();
    packet.get(raw);
    packet.position(pos);
    Optional<MavlinkFrameEnvelope>  envelope = mavlinkFrameCodec.tryUnpackHeaderAndPayload(packet.getRawBuffer());
    if(envelope.isPresent()){
      MavlinkDeviceKey key = buildKey(packet, envelope.get());
      UDPSessionState<MavlinkProtocol> state = findOrCreate(key);
      if (state.getContext() != null) {
        MavlinkProtocol protocol = state.getContext();
        protocol.processPacket(envelope.get(), raw);
      }
      else{
        System.err.println("Has NO State");
      }
    }
    else {
      System.err.println("Has NO Envelope");
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
        // todo log this
      }
    }
    return state;
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
