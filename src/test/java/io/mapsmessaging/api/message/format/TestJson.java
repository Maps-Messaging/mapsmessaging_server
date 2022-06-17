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
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestJson {

  @Test
  public void testJsonFiltering() throws IOException {
    JsonFormat format = new JsonFormat();
    IdentifierResolver resolver = format.getResolver(JSONString.getBytes());
    Assertions.assertEquals(12.0, resolver.get("top"));
    Assertions.assertNull(resolver.get("not there"));

    Assertions.assertEquals("donut", resolver.get("data[0].type"));
    Assertions.assertEquals("Cake", resolver.get("data[0].name"));
    Assertions.assertEquals("Blueberry", resolver.get("data[0].batters.batter[2].type"));

    Assertions.assertEquals("donut", resolver.get("data[1].type"));
    Assertions.assertEquals("Raised", resolver.get("data[1].name"));
    Assertions.assertNull(resolver.get("data[1].batters.batter[2].type"));


    Assertions.assertEquals("donut", resolver.get("data[2].type"));
    Assertions.assertEquals("Old Fashioned", resolver.get("data[2].name"));
    Assertions.assertNull(resolver.get("data[2].batters.batter[2].type"));
  }

  private static final String JSONString = ""
      + "{\"top\": 12.0,\n"
      + "\"data\":\n"
      + "[\n"
      + "\t{\n"
      + "\t\t\"id\": \"0001\",\n"
      + "\t\t\"type\": \"donut\",\n"
      + "\t\t\"name\": \"Cake\",\n"
      + "\t\t\"ppu\": 0.55,\n"
      + "\t\t\"batters\":\n"
      + "\t\t\t{\n"
      + "\t\t\t\t\"batter\":\n"
      + "\t\t\t\t\t[\n"
      + "\t\t\t\t\t\t{ \"id\": \"1001\", \"type\": \"Regular\" },\n"
      + "\t\t\t\t\t\t{ \"id\": \"1002\", \"type\": \"Chocolate\" },\n"
      + "\t\t\t\t\t\t{ \"id\": \"1003\", \"type\": \"Blueberry\" },\n"
      + "\t\t\t\t\t\t{ \"id\": \"1004\", \"type\": \"Devil's Food\" }\n"
      + "\t\t\t\t\t]\n"
      + "\t\t\t},\n"
      + "\t\t\"topping\":\n"
      + "\t\t\t[\n"
      + "\t\t\t\t{ \"id\": \"5001\", \"type\": \"None\" },\n"
      + "\t\t\t\t{ \"id\": \"5002\", \"type\": \"Glazed\" },\n"
      + "\t\t\t\t{ \"id\": \"5005\", \"type\": \"Sugar\" },\n"
      + "\t\t\t\t{ \"id\": \"5007\", \"type\": \"Powdered Sugar\" },\n"
      + "\t\t\t\t{ \"id\": \"5006\", \"type\": \"Chocolate with Sprinkles\" },\n"
      + "\t\t\t\t{ \"id\": \"5003\", \"type\": \"Chocolate\" },\n"
      + "\t\t\t\t{ \"id\": \"5004\", \"type\": \"Maple\" }\n"
      + "\t\t\t]\n"
      + "\t},\n"
      + "\t{\n"
      + "\t\t\"id\": \"0002\",\n"
      + "\t\t\"type\": \"donut\",\n"
      + "\t\t\"name\": \"Raised\",\n"
      + "\t\t\"ppu\": 0.55,\n"
      + "\t\t\"batters\":\n"
      + "\t\t\t{\n"
      + "\t\t\t\t\"batter\":\n"
      + "\t\t\t\t\t[\n"
      + "\t\t\t\t\t\t{ \"id\": \"1001\", \"type\": \"Regular\" }\n"
      + "\t\t\t\t\t]\n"
      + "\t\t\t},\n"
      + "\t\t\"topping\":\n"
      + "\t\t\t[\n"
      + "\t\t\t\t{ \"id\": \"5001\", \"type\": \"None\" },\n"
      + "\t\t\t\t{ \"id\": \"5002\", \"type\": \"Glazed\" },\n"
      + "\t\t\t\t{ \"id\": \"5005\", \"type\": \"Sugar\" },\n"
      + "\t\t\t\t{ \"id\": \"5003\", \"type\": \"Chocolate\" },\n"
      + "\t\t\t\t{ \"id\": \"5004\", \"type\": \"Maple\" }\n"
      + "\t\t\t]\n"
      + "\t},\n"
      + "\t{\n"
      + "\t\t\"id\": \"0003\",\n"
      + "\t\t\"type\": \"donut\",\n"
      + "\t\t\"name\": \"Old Fashioned\",\n"
      + "\t\t\"ppu\": 0.55,\n"
      + "\t\t\"batters\":\n"
      + "\t\t\t{\n"
      + "\t\t\t\t\"batter\":\n"
      + "\t\t\t\t\t[\n"
      + "\t\t\t\t\t\t{ \"id\": \"1001\", \"type\": \"Regular\" },\n"
      + "\t\t\t\t\t\t{ \"id\": \"1002\", \"type\": \"Chocolate\" }\n"
      + "\t\t\t\t\t]\n"
      + "\t\t\t},\n"
      + "\t\t\"topping\":\n"
      + "\t\t\t[\n"
      + "\t\t\t\t{ \"id\": \"5001\", \"type\": \"None\" },\n"
      + "\t\t\t\t{ \"id\": \"5002\", \"type\": \"Glazed\" },\n"
      + "\t\t\t\t{ \"id\": \"5003\", \"type\": \"Chocolate\" },\n"
      + "\t\t\t\t{ \"id\": \"5004\", \"type\": \"Maple\" }\n"
      + "\t\t\t]\n"
      + "\t}\n"
      + "]\n}";
}
