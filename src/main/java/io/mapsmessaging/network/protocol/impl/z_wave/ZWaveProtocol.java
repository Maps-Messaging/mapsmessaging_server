package io.mapsmessaging.network.protocol.impl.z_wave;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamEndPoint;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.z_wave.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.z_wave.state.NetworkStateManager;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class ZWaveProtocol extends ProtocolImpl {

  private final Session session;
  private final SelectorTask selectorTask;
  private final NetworkStateManager stateManager;

  protected ZWaveProtocol(@NonNull @NotNull EndPoint endPoint, Packet packet) throws IOException, LoginException {
    super(endPoint);
    if (endPoint instanceof StreamEndPoint) {
      ((StreamEndPoint) endPoint).setStreamHandler(new ZWaveStreamHandler());
    }
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("ZWave" + endPoint.getId(), this);
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive((int)keepAlive);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), true);
    setTransformation(TransformationManager.getInstance().getTransformation(getName(), null));
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    stateManager = new NetworkStateManager(this);
  }

  @Override
  public void close() throws IOException {
    SessionManager.getInstance().close(session,true);
    stateManager.close();
    super.close();
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {

  }

  public void sendKeepAlive(){
  }

  @Override
  public boolean processPacket(@NonNull @NotNull Packet packet) throws IOException {
    if (packet.available() > 0){
      stateManager.processPacket(packet);
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }

  public void writeFrame(@NonNull @NotNull BasePacket frame)  {
    sentMessageAverages.increment();
    selectorTask.push(frame);
    sentMessage();
  }

  @Override
  public String getName() {
    return "Z_Wave";
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
