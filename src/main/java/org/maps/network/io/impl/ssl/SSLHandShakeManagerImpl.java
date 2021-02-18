/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.io.impl.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.network.io.EndPointConnectedCallback;
import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;

public class SSLHandShakeManagerImpl implements SSLHandshakeManager {

  private final SSLEndPoint sslEndPointImpl;
  private final ByteBuffer handshakeBufferIn;
  private final ByteBuffer handshakeBufferOut;
  private final EndPointConnectedCallback callback;

  SSLHandShakeManagerImpl(SSLEndPoint sslEndPointImpl, EndPointConnectedCallback callback) {
    this.sslEndPointImpl = sslEndPointImpl;
    this.callback = callback;
    handshakeBufferIn = ByteBuffer.allocate(sslEndPointImpl.sslEngine.getSession().getApplicationBufferSize());
    handshakeBufferOut = ByteBuffer.allocate(sslEndPointImpl.sslEngine.getSession().getApplicationBufferSize());
  }

  public boolean handleSSLHandshakeStatus() throws IOException {
    HandshakeStatus handshakeStatus = sslEndPointImpl.sslEngine.getHandshakeStatus();
    Logger logger = sslEndPointImpl.getLogger();
    while (handshakeStatus != HandshakeStatus.FINISHED
        && handshakeStatus != HandshakeStatus.NOT_HANDSHAKING) {
      if (handshakeStatus == HandshakeStatus.NEED_TASK) {
        runDelegatedTasks();
      } else if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
        logger.log(LogMessages.SSL_HANDSHAKE_NEED_UNWRAP);
        handshakeBufferIn.clear();
        if (sslEndPointImpl.readBuffer(handshakeBufferIn) == 0) {
          return true; // Wait for more data
        }
      } else if (handshakeStatus == HandshakeStatus.NEED_WRAP) {
        logger.log(LogMessages.SSL_HANDSHAKE_NEED_WRAP);
        sslEndPointImpl.sendBuffer(handshakeBufferOut);
      }
      handshakeStatus = sslEndPointImpl.sslEngine.getHandshakeStatus();
    }
    logger.log(LogMessages.SSL_HANDSHAKE_FINISHED);
    logger.log(LogMessages.SSL_HANDSHAKE_ENCRYPTED, handshakeBufferIn.position(), handshakeBufferIn.limit());
    sslEndPointImpl.handshakeManager = new SSLHandshakeManagerFinished(); // All done, no longer required
    if(callback != null){
      callback.connected(sslEndPointImpl);
    }
    return false;
  }

  protected void runDelegatedTasks() {
    Logger logger = sslEndPointImpl.getLogger();
    logger.log(LogMessages.SSL_HANDSHAKE_NEED_TASK);
    Runnable runnable;
    while ((runnable = sslEndPointImpl.sslEngine.getDelegatedTask()) != null) {
      runnable.run();
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    try {
      handleSSLHandshakeStatus();
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }
}
