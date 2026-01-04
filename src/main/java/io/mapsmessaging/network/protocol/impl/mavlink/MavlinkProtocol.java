package io.mapsmessaging.network.protocol.impl.mavlink;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.mavlink.MavlinkFrameEnvelope;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.SocketAddress;

public class MavlinkProtocol extends Protocol {
  private final MavlinkInterfaceManager factory;
  private final MavlinkDeviceKey key;


  protected MavlinkProtocol(@NonNull @NotNull MavlinkInterfaceManager factory,
                            @NonNull @NotNull MavlinkDeviceKey key,
                            @NonNull @NotNull EndPoint endPoint,
                            @NotNull @NonNull ProtocolConfigDTO protocolConfig) {
    super(endPoint, protocolConfig);
    this.factory = factory;
    this.key = key;
  }

  protected MavlinkProtocol(@NonNull @NotNull MavlinkInterfaceManager factory,
                            @NonNull @NotNull MavlinkDeviceKey key,
                            @NonNull @NotNull EndPoint endPoint,
                            @NonNull @NotNull SocketAddress socketAddress,
                            @NotNull @NonNull ProtocolConfigDTO protocolConfig) {
    super(endPoint, socketAddress, protocolConfig);
    this.factory = factory;
    this.key = key;
  }

  @Override
  public void close() throws IOException {
    super.close();
    factory.close(key);
  }

  @Override
  public Subject getSubject() {
    return null;
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    return null;
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    System.err.println("Packet received for "+key);
    return false;
  }

  public boolean processPacket(@NonNull @NotNull MavlinkFrameEnvelope envelope, byte[] raw) throws IOException {
    System.err.println("Packet received for "+key+" for "+envelope.getSystemId()+" msgId:"+envelope.getMessageId());
    return true;
  }



  @Override
  public String getName() {
    return "mavlink";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }
}
