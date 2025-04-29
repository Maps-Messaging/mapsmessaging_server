package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class HPubFrame extends HPayloadFrame {

  protected HPubFrame(int maxBufferSize) {
    super(maxBufferSize);
  }


  @Override
  public byte[] getCommand() {
    return "HPUB".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public NatsFrame instance() {
    return new HPubFrame(maxBufferSize);
  }

}
