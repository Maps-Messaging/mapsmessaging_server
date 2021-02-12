/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.api.message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.features.Priority;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.engine.resources.DBResource;

public class MessageStreamTest {

  private static final Map<String, Object> dataMapValues;
  static{
    dataMapValues = new LinkedHashMap<>();
    dataMapValues.put("low_long",Long.MIN_VALUE);
    dataMapValues.put("high_long",Long.MAX_VALUE);

    dataMapValues.put("low_float",Float.MIN_VALUE);
    dataMapValues.put("high_float",Float.MAX_VALUE);

    dataMapValues.put("low_int",Integer.MIN_VALUE);
    dataMapValues.put("high_int",Integer.MAX_VALUE);

    dataMapValues.put("low_double",Double.MIN_VALUE);
    dataMapValues.put("high_double",Double.MAX_VALUE);

    dataMapValues.put("boolean_t",true);
    dataMapValues.put("boolean_f",false);

    dataMapValues.put("low_byte",Byte.MIN_VALUE);
    dataMapValues.put("byte",(byte)0xff);
    dataMapValues.put("high_byte",Byte.MAX_VALUE);

    dataMapValues.put("bytes",createOpaqueData(10));

    dataMapValues.put("low_short",Short.MIN_VALUE);
    dataMapValues.put("short", (short) 0xFFFF);
    dataMapValues.put("high_short",Short.MAX_VALUE);

    dataMapValues.put("string","ABC");

    dataMapValues.put("low_char",Character.MIN_VALUE);
    dataMapValues.put("char", 'c');
    dataMapValues.put("japanese", '大');
    dataMapValues.put("high_char",Character.MAX_VALUE);
  }


  @Test
  void messageBuilder(){
    for(int x=0;x<1000;x++) {
      Message message = createMessageBuilder(x).build();
      validateMessage(message, x);
    }
  }

  @Test
  void messageStream() throws IOException {
    File file = new File("./target/data/messageStream");
    Files.createDirectories(file.toPath());
    DBResource dbResource = new DBResource("./target/data/messageStream", "messageStream");

    // Remove any before we start
    if(!dbResource.isEmpty()){
      Iterator<Long> entries = dbResource.getIterator();
      while(entries.hasNext()){
        entries.remove();
      }
    }

    for(int x=0;x<10;x++) {
      Message message = createMessageBuilder(x).build();
      validateMessage(message, x);
      dbResource.add(message);
      System.err.println("Saved::"+message.getIdentifier());
    }
    Assertions.assertEquals(10, dbResource.size());
    dbResource.flush();

    for(int x=1;x<11;x++) {
      Message message = dbResource.get(x);
      validateMessage(message, x);
      dbResource.remove(x);
      System.err.println(message.toString());
    }
    Assertions.assertTrue(dbResource.isEmpty());
    dbResource.delete();
  }

  private MessageBuilder createMessageBuilder(long id){
    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setCreation(System.currentTimeMillis());
    messageBuilder.setContentType("application/octet-stream");
    messageBuilder.setOpaqueData(createOpaqueData(1024));
    messageBuilder.setQoS(QualityOfService.AT_LEAST_ONCE);
    messageBuilder.setCorrelationData("correlationData".getBytes());
    messageBuilder.setPriority(Priority.HIGHEST);
    messageBuilder.setMessageExpiryInterval(System.currentTimeMillis()+60000, TimeUnit.MILLISECONDS);
    messageBuilder.setId(id);
    messageBuilder.setDelayed(30000);
    messageBuilder.setRetain(true);
    messageBuilder.setMeta(createMetaData());
    messageBuilder.setDataMap(createDataMap());
    return messageBuilder;
  }

  private void validateMessage(Message message, long expectedId){
    Assertions.assertNotNull(message);
    // Do the simple equivalence tests
    Assertions.assertEquals("application/octet-stream", message.getContentType(), "Content type validation");
    Assertions.assertEquals(QualityOfService.AT_LEAST_ONCE, message.getQualityOfService(), "Quality of service validation");
    Assertions.assertEquals("correlationData", new String(message.getCorrelationData()), "Correlation Data validation");
    Assertions.assertEquals(Priority.HIGHEST, message.getPriority());
    Assertions.assertEquals(expectedId, message.getIdentifier(), "Identifier validation");
    Assertions.assertTrue(message.isRetain(),"is a retained message validation");

    // Validate the payload
    validateOpaqueData(message.getOpaqueData(), 1024);

    // OK so the message headers seem ok, now lets check the maps
    validateMetaData(message.getMeta());

    // OK now lets check the user supplied data map and its different classes
    validateDataMap(message.getDataMap());



  }

  private static byte[] createOpaqueData(int size){
    byte[] data = new byte[size];
    for(int x=0;x<size;x++){
      data[x] = (byte)((x%26)+65);
    }
    return data;
  }

  private void validateOpaqueData(byte[] data, int expectedSize){
    Assertions.assertNotNull(data);
    Assertions.assertEquals(expectedSize, data.length);
    int x=0;
    for(byte val:data){
      Assertions.assertEquals(val, ((x%26)+65), "Should be an uppercase letter");
      x++;
    }
  }

  private void validateMetaData(Map<String, String> meta){
    Assertions.assertNotNull(meta, "Should not be null since we supplied it");
    Assertions.assertTrue(meta.containsKey("time"), "should contain the time key");
    Assertions.assertEquals(meta.get("test"), "MetaDataValue", "map entry validation");
  }

  private Map<String, String> createMetaData(){
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("time", ""+System.currentTimeMillis());
    meta.put("test", "MetaDataValue");
    return meta;
  }

  private void validateDataMap( Map<String, TypedData> dataMap){
    Assertions.assertNotNull(dataMap, "This should not be a null value");
    for(Map.Entry<String, Object> entry: dataMapValues.entrySet()) {
      validateEntry(dataMap, entry.getKey(), entry.getValue());
    }
  }

  private void validateEntry(Map<String, TypedData> dataMap, String key, Object value){
    TypedData entry = dataMap.get(key);
    Assertions.assertNotNull(entry, "Entry test for "+key+" should not be empty");
    if(value instanceof byte[]){
      validateOpaqueData((byte[])entry.getData(), ((byte[])value).length);
    }
    else {
      Assertions.assertEquals(value, entry.getData());
    }
  }


  private Map<String, TypedData> createDataMap(){
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    for(Map.Entry<String, Object> entry: dataMapValues.entrySet()){
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }
    return dataMap;
  }

}
