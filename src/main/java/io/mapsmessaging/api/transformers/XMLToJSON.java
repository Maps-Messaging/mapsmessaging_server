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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.MessageBuilder;
import org.json.JSONObject;
import org.json.XML;

public class XMLToJSON implements Transformer {

  @Override
  public void transform(MessageBuilder messageBuilder) {
    JSONObject xmlJSONObj = XML.toJSONObject(new String(messageBuilder.getOpaqueData()));
    messageBuilder.setOpaqueData(xmlJSONObj.toString(2).getBytes());
  }

  @Override
  public String getName() {
    return "XMLToJSON";
  }

  @Override
  public String getDescription() {
    return "Converts XML to JSON";
  }
}