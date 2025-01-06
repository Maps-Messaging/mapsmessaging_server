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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.configuration.ConfigurationProperties;
import org.json.JSONObject;
import org.json.XML;

@SuppressWarnings("java:S2129") // We convert a Byte[] into a String for json to parse
public class JSONToXML implements Transformer {

  @Override
  public void transform(MessageBuilder messageBuilder) {
    JSONObject jsonObject = new JSONObject(new String(messageBuilder.getOpaqueData()));
    messageBuilder.setOpaqueData(XML.toString(jsonObject).getBytes());
  }

  @Override
  public Transformer build(ConfigurationProperties properties) {
    return this;
  }

  @Override
  public String getName() {
    return "JSONToXML";
  }

  @Override
  public String getDescription() {
    return "Converts JSON to XML";
  }
}
