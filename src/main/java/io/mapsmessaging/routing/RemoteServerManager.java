/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.routing;

import io.mapsmessaging.routing.manager.SchemaMonitor;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class RemoteServerManager implements Runnable{

  private final String url;
  private final SchemaMonitor schemaManager;
  private ScheduledFuture<?> scheduledFuture;

  public RemoteServerManager(String url, boolean schemaEnabled){
    this.url = url;
    if(schemaEnabled){
      schemaManager = new SchemaMonitor(url);
    }
    else{
      schemaManager = null;
    }
    resume();
  }

  public synchronized void stop(){
    if(scheduledFuture != null) {
      scheduledFuture.cancel(true);
      scheduledFuture = null;
    }
  }

  public void pause(){
    stop();
  }

  public synchronized void resume(){
    if(scheduledFuture == null) {
      scheduledFuture = SimpleTaskScheduler.getInstance().schedule(this, 60, TimeUnit.SECONDS);
    }
  }

  public void run(){
    Request request = new Request.Builder()
        .url(url+"/api/v1/updates")
        .build();
    OkHttpClient client = new OkHttpClient();
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        if(schemaManager != null) schemaManager.scanForUpdates(json.getLong("schemaUpdate"));
      } else {
        throw new RuntimeException("Request failed: " + response.code() + " " + response.message());
      }
    } catch (IOException e) {
      throw new RuntimeException("Request failed due to IOException", e);
    }
    finally {
      scheduledFuture=null;
      resume();
    }
  }
}
