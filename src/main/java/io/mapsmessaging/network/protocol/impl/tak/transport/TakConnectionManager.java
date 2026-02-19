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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;

public class TakConnectionManager {

  private final TakServerConnection connection;
  private final ReentrantLock writeLock;

  public TakConnectionManager(TakServerConnection connection) {
    this.connection = connection;
    this.writeLock = new ReentrantLock();
  }

  public void connect() throws IOException {
    connection.connect();
  }

  public void reconnect() throws IOException {
    connection.close();
    connection.connect();
  }

  public boolean isConnected() {
    return connection.isConnected();
  }

  public InputStream input() throws IOException {
    return connection.getInputStream();
  }

  public void write(byte[] data) throws IOException {
    writeLock.lock();
    try {
      connection.write(data);
    } finally {
      writeLock.unlock();
    }
  }

  public void close() throws IOException {
    connection.close();
  }
}
