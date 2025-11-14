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

package io.mapsmessaging.network.protocol.transformation.cloudevent;

import io.mapsmessaging.network.protocol.transformation.cloudevent.pack.EnvelopePackHelper;


public class CloudEventEnvelopeTransformation extends CloudEventTransformation {

  public CloudEventEnvelopeTransformation() {
    super();
    packHelper = new EnvelopePackHelper(gson);
  }

  @Override
  public String getName() {
    return "CloudEvent-Envelope";
  }

  @Override
  public int getId() {
    return 7;
  }

  @Override
  public String getDescription() {
    return "Emit a JSON object in data containing payload_base64 (and payload_mime, optional _mapsData) with datacontenttype: application/json and dataschema for the envelope schema.";
  }

}