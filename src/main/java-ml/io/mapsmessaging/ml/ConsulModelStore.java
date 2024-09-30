/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.ml;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import io.mapsmessaging.selector.operators.functions.ml.ModelStore;
import java.io.IOException;
import java.util.Base64;

public class ConsulModelStore implements ModelStore {

  private final ConsulClient consulClient;
  private final String rootKey;

  public ConsulModelStore(ConsulClient consulClient, String rootKey) {
    this.consulClient = consulClient;
    this.rootKey = rootKey;
  }

  @Override
  public void saveModel(String s, byte[] bytes) {
    String key = rootKey + "/" + s;
    String value = Base64.getEncoder().encodeToString(bytes);
    consulClient.setKVValue(key, value, new PutParams());
  }

  @Override
  public byte[] loadModel(String s) throws IOException {
    String key = rootKey + "/" + s;
    GetValue getValue = consulClient.getKVValue(key).getValue();
    if (getValue == null) {
      throw new IOException("Model not found");
    }
    return Base64.getDecoder().decode(getValue.getValue());
  }

  @Override
  public boolean modelExists(String s)  {
    String key = rootKey + "/" + s;
    return consulClient.getKVValue(key).getValue() != null;
  }

  @Override
  public boolean deleteModel(String s) throws IOException {
    consulClient.deleteKVValue(rootKey + "/" + s);
    return true;
  }
}
