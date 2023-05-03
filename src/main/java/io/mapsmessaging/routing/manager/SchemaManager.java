/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.routing.manager;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class SchemaManager implements Runnable {

  private final String remoteUrl;

  public SchemaManager(String remoteUrl){
    this.remoteUrl = remoteUrl;
    run();
  }

  public void run(){
    Request request = new Request.Builder()
        .url(remoteUrl+"/api/v1/server/schema/")
        .build();
    OkHttpClient client = new OkHttpClient();
    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        JSONArray jsonArray = json.getJSONArray("data");
        if(jsonArray != null && !jsonArray.isEmpty()){
          for(int x=0;x<jsonArray.length();x++) {
            SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(jsonArray.get(x).toString());
            System.err.println(config);
          }
        }
      } else {
        throw new RuntimeException("Request failed: " + response.code() + " " + response.message());
      }
    } catch (IOException e) {
      throw new RuntimeException("Request failed due to IOException", e);
    }
  }

}
