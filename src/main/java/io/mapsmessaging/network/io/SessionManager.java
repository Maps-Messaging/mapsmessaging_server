package io.mapsmessaging.network.io;

import io.mapsmessaging.network.protocol.ProtocolImpl;
import java.net.SocketAddress;

public interface SessionManager {

  void openSession(ProtocolImpl protocol, SocketAddress socketAddress);

  void closeSession(SocketAddress socketAddress);
}
