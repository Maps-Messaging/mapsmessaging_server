package io.mapsmessaging.network.protocol.impl.z_wave;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.AckPacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.DataPacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.NakPacket;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.requests.GetInitialData;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.requests.SoftResetPacket;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class ZWaveProtocol extends ProtocolImpl {

  private final Session session;
  private final SelectorTask selectorTask;
  private final String destinationName;
  private final PacketFactory packetFactory;
  private boolean sentInit;

  protected ZWaveProtocol(@NonNull @NotNull EndPoint endPoint, Packet packet) throws IOException, LoginException {
    super(endPoint);
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("ZWave" + endPoint.getId(), this);
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive(10);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), true);
    packetFactory = new PacketFactory();
    setTransformation(TransformationManager.getInstance().getTransformation(getName(), null));
    destinationName = "$ZWave/"+endPoint.getName();
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    // Initialise the link
    writeFrame(new NakPacket());
    writeFrame(new SoftResetPacket());
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    sentInit = false;
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }

  public void sendKeepAlive(){
    if(!sentInit){
      sentInit = true;
      writeFrame(new GetInitialData());
    }
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    System.err.println(packet);
    if (packet.available() > 0){
      BasePacket frame = packetFactory.parse(packet);
      if(frame instanceof DataPacket){
        DataPacket dataPacket = (DataPacket) frame;
        if(dataPacket.isValid()){
          writeFrame( new AckPacket());
        }
        else {
          writeFrame(new NakPacket());
        }
      }
      sendKeepAlive();
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }

  public void writeFrame(@NonNull @NotNull BasePacket frame)  {
    sentMessageAverages.increment();
    System.err.println("Sending Packet:"+frame);
    selectorTask.push(frame);
    sentMessage();
  }

  @Override
  public String getName() {
    return "ZWave";
  }

  @Override
  public String getSessionId() {
    return session.getName();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }
}
