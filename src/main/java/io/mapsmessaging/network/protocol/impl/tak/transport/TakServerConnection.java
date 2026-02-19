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

package io.mapsmessaging.network.protocol.impl.tak.transport;

import io.mapsmessaging.network.EndPointURL;

import javax.net.SocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

public class TakServerConnection {

  private final EndPointURL url;
  private final int timeoutMs;

  private Socket socket;
  private InputStream inputStream;
  private OutputStream outputStream;

  public TakServerConnection(EndPointURL url, Duration timeout) {
    this.url = url;
    this.timeoutMs = (int) Math.max(1000, timeout.toMillis());
  }

  public synchronized void connect() throws IOException {
    close();
    SocketFactory socketFactory = isSecure() ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
    socket = socketFactory.createSocket();
    socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), timeoutMs);
    socket.setKeepAlive(true);
    socket.setTcpNoDelay(true);
    socket.setSoTimeout(timeoutMs);
    if (isSecure() && socket instanceof SSLSocket sslSocket) {
      try {
        sslSocket.startHandshake();
      } catch (SSLException sslException) {
        throw new IOException("TAK TLS handshake failed. Verify trustStore/keyStore and server certificate validity", sslException);
      }
    }
    inputStream = socket.getInputStream();
    outputStream = socket.getOutputStream();
  }

  public synchronized InputStream getInputStream() throws IOException {
    if (!isConnected()) {
      throw new IOException("TAK connection is not connected");
    }
    return inputStream;
  }

  public synchronized void write(byte[] data) throws IOException {
    if (!isConnected()) {
      throw new IOException("TAK connection is not connected");
    }
    outputStream.write(data);
    outputStream.flush();
  }

  public synchronized boolean isConnected() {
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  public synchronized void close() throws IOException {
    IOException deferred = null;
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException ex) {
        deferred = ex;
      }
      inputStream = null;
    }
    if (outputStream != null) {
      try {
        outputStream.close();
      } catch (IOException ex) {
        deferred = ex;
      }
      outputStream = null;
    }
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException ex) {
        deferred = ex;
      }
      socket = null;
    }
    if (deferred != null) {
      throw deferred;
    }
  }

  private boolean isSecure() {
    String protocol = url.getProtocol();
    return "ssl".equalsIgnoreCase(protocol) || "wss".equalsIgnoreCase(protocol);
  }
}
