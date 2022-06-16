/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package io.mapsmessaging.api.message.format;

import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import org.json.JSONObject;

public class JsonFormat implements Format{

  @Override
  public String getName() {
    return "JSON";
  }

  @Override
  public String getDescription() {
    return "Processes JSON formatted payloads";
  }

  private Object fromByteArray(byte[] payload) throws IOException {
    try {
      return new JSONObject(new String(payload));
    } catch (Throwable e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean isValid(byte[] payload) {
    try{
      fromByteArray(payload);
      return true;
    }
    catch(IOException ex){
      // ignore
    }
    return false;
  }

  @Override
  public Format getInstance(ConfigurationProperties properties) {
    return this;
  }

  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return new JsonIdentifierResolver((JSONObject)fromByteArray(payload));
  }

  public static final class JsonIdentifierResolver implements IdentifierResolver{

    private final JSONObject jsonObject;

    public JsonIdentifierResolver(JSONObject jsonObject){
      this.jsonObject = jsonObject;
    }

    @Override
    public Object get(String s) {
      if(jsonObject.has(s)) {
        return jsonObject.get(s);
      }
      return null;
    }

    @Override
    public byte[] getOpaqueData() {
      return IdentifierResolver.super.getOpaqueData();
    }
  }
}
