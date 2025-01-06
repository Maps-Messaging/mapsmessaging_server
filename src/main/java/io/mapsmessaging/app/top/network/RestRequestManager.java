/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.app.top.network;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestRequestManager implements Runnable {
  private final List<Object> queue;
  private final List<RestApiConnection> requests;
  private AtomicBoolean running = new AtomicBoolean(true);
  private AtomicBoolean connected = new AtomicBoolean(false);

  public RestRequestManager(String url, String username, String password) {
    queue = new LinkedList<>();
    requests = new LinkedList<>();
    requests.add(new ServerDestinationStatusRequest(url, username, password));
    requests.add(new ServerDetailsRequest(url, username, password));
    requests.add(new ServerInfoRequest(url, username, password));
    requests.add(new ServerInterfaceStatusRequest(url, username, password));
    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  public void close(){
    running.set(false);
  }

  public void run(){
    while(running.get()){
      boolean test = true;
      for(RestApiConnection request : requests){
        try {
          Object result = request.getData();
          if(result != null){
            queue.add(result);
          }
        } catch (IOException e) {
          connected.set(false);
          test = false;
          // Ignore
        }
      }
      connected.set(test);
      try {
        Thread.sleep(6000);
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  public Object getUpdate() {
    return queue.remove(0);
  }

  public boolean isQueueEmpty() {
    return queue.isEmpty();
  }

  public boolean isConnected() {
    return connected.get();
  }
}

