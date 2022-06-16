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

import io.mapsmessaging.api.message.format.walker.MapResolver;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import org.json.JSONException;
import org.json.XML;

public class XmlFormat implements Format{

  @Override
  public String getName() {
    return "XML";
  }

  @Override
  public String getDescription() {
    return "Processes XML formatted payloads";
  }


  @Override
  public boolean isValid(byte[] payload) {
    try{
      XML.toJSONObject(new String(payload));
      return true;
    }
    catch(JSONException ex){
      // ignore
    }
    return false;
  }

  @Override
  public Format getInstance(ConfigurationProperties properties) {
    return this;
  }

  @Override
  public IdentifierResolver getResolver(byte[] payload) {
    return new GeneralIdentifierResolver(new MapResolver((XML.toJSONObject(new String(payload))).toMap()));
  }
}
