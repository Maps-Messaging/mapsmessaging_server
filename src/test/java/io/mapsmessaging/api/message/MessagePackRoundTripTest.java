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
package io.mapsmessaging.api.message;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.features.QualityOfService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

class MessagePackRoundTripTest {

  @Test
  void packAndUnpack_preservesCoreFields() throws IOException {
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("m1", "v1");
    meta.put("m2", "v2");

    Map<String, TypedData> data = new LinkedHashMap<>();
    data.put("d1", new TypedData("x"));
    data.put("d2", new TypedData(99L));

    Message original = new MessageBuilder()
        .setCorrelationData("corr")
        .setOpaqueData(new byte[]{9, 8, 7})
        .setContentType("application/test")
        .setResponseTopic("reply/topic")
        .setPriority(Priority.ONE_BELOW_HIGHEST)
        .setQoS(QualityOfService.EXACTLY_ONCE)
        .setMeta(meta)
        .setDataMap(data)
        .setPayloadIndicator(true)
        .setRetain(true)
        .setSchemaId("schema-9")
        .build();

    ByteBuffer[] packed = original.pack();
    Message unpacked = new Message(packed);

    Assertions.assertEquals(original.getPriority(), unpacked.getPriority());
    Assertions.assertEquals(original.getQualityOfService(), unpacked.getQualityOfService());
    Assertions.assertEquals(original.getResponseTopic(), unpacked.getResponseTopic());
    Assertions.assertEquals(original.getContentType(), unpacked.getContentType());
    Assertions.assertEquals(original.isRetain(), unpacked.isRetain());
    Assertions.assertEquals(original.isUTF8(), unpacked.isUTF8());
    Assertions.assertEquals(original.getSchemaId(), unpacked.getSchemaId());

    Assertions.assertArrayEquals(original.getOpaqueData(), unpacked.getOpaqueData());

    // meta in Message returns a copy if null; here it should exist
    Assertions.assertEquals("v1", unpacked.getMeta().get("m1"));
    Assertions.assertEquals("v2", unpacked.getMeta().get("m2"));

    Assertions.assertEquals("x", unpacked.getDataMap().get("d1").getData());
    Assertions.assertEquals(99L, unpacked.getDataMap().get("d2").getData());
  }
}
