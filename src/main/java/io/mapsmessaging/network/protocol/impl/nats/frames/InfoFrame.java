package io.mapsmessaging.network.protocol.impl.nats.frames;

import com.google.gson.Gson;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.io.Packet;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@ToString
public class InfoFrame extends NatsFrame {

  private static final AtomicLong counter = new AtomicLong();
  private static final Gson gson = new Gson();

  private InfoData infoData;

  public InfoFrame(int maxPayloadLength) {
    super();
    this.infoData = new InfoData();
    this.infoData.setClientId(counter.incrementAndGet());
    this.infoData.setMaxPayloadLength(maxPayloadLength);
    infoData.setVersion("2.11.3");
    infoData.setProto(1);
    infoData.setHost("localhost");
    infoData.setPort(4222);
    infoData.setHeaders(true);
    infoData.setClientIp("127.0.0.1");
    infoData.setServerId(MessageDaemon.getInstance().getUuid().toString());
    infoData.setServerName(MessageDaemon.getInstance().getId());
    infoData.setJava(System.getProperty("java.version"));
  }

  @Override
  public byte[] getCommand() {
    return "INFO".getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  protected void parseLine(String json) {
    this.infoData = gson.fromJson(json, InfoData.class);
  }

  @Override
  public int packFrame(Packet packet) {
    int start = packet.position();
    packet.put(getCommand());
    packet.put((byte) ' ');
    String json = gson.toJson(infoData);
    packet.put(json.getBytes(StandardCharsets.US_ASCII));
    packet.put("\r\n".getBytes(StandardCharsets.US_ASCII));
    return packet.position() - start;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public NatsFrame instance() {
    return new InfoFrame(infoData.getMaxPayloadLength());
  }
}

