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

package io.mapsmessaging.network.protocol.impl.echo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class EchoClient {

  protected final Socket connection;
  private final OutputStream outputStream;
  private final InputStream inputStream;
  private final byte[] workingBuffer;

  public EchoClient(String host, int port) throws IOException {
    workingBuffer = new byte[10240];
    connection = createSocket(host, port);
    outputStream = connection.getOutputStream();
    inputStream = connection.getInputStream();

    //
    // Indicate that we want an echo protocol
    //
    outputStream.write("ECHO        ".getBytes());
    outputStream.flush();

    int len = inputStream.read(workingBuffer);
    if(len < 4){
      throw new IOException("Invalid response from server, protocol not supported");
    }
    String result = new String(workingBuffer, 0, len);
    if(!result.equals("ECHO        ")){
      throw new IOException("Invalid response from server, protocol not supported");
    }
  }

  protected Socket createSocket(String host, int port) throws IOException {
    return  new Socket(host, port);
  }

  public void close() throws IOException {
    connection.close();
  }

  public void send(String msg) throws IOException {
    outputStream.write(msg.getBytes());
    outputStream.flush();
  }

  public String read() throws IOException{
    int len = inputStream.read(workingBuffer);
    if(len < 0){
      throw new IOException("Socket has been closed");
    }
    return new String(workingBuffer, 0, len);
  }
}
