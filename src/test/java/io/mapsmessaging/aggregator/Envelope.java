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

package io.mapsmessaging.aggregator;

import lombok.Getter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Getter
public class Envelope {

  private final Map<String, Object> inputs;

  public Envelope(Map<String, Object> inputs) {
    this.inputs = inputs;
  }

  Object getPayloadField(String inputTopic, String fieldName) {
    Object entryObj = inputs.get(inputTopic);
    assertNotNull(entryObj, "Missing envelope entry for topic " + inputTopic);

    @SuppressWarnings("unchecked")
    Map<String, Object> entry = (Map<String, Object>) entryObj;

    Object payloadObj = entry.get("payload");
    assertNotNull(payloadObj, "Expected 'payload' object for topic " + inputTopic);

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) payloadObj;

    Object val = payload.get(fieldName);
    assertNotNull(val, "Missing payload field '" + fieldName + "' for topic " + inputTopic);
    return val;
  }
}