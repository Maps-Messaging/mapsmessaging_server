
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


import io.mapsmessaging.api.message.format.proto.AddressBookProtos.Person;
import io.mapsmessaging.api.message.format.proto.AddressBookProtos.Person.Builder;
import io.mapsmessaging.api.message.format.proto.AddressBookProtos.Person.PhoneNumber;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestProtobuf {

  public static byte[] packSample() throws IOException {
    Builder builder = Person.newBuilder();
    builder.addPhones(PhoneNumber.newBuilder().setNumber("+1 555 555-55555").build());
    builder.setEmail("matthew@buckton.org");
    builder.setName("Matthew Buckton");
    Person person = builder.build();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    person.writeTo(baos);
    return baos.toByteArray();
  }


  @Test
  public void testSimpleLoad() throws IOException {
    ConfigurationProperties props = new ConfigurationProperties();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
    byte[] tmp = new byte[10240];
    try (InputStream fis = getClass().getClassLoader().getResourceAsStream("Person.desc")) {
      int len = fis.read(tmp);
      baos.write(tmp, 0, len);
    }
    props.put("descriptor", new String(Base64.getEncoder().encode(baos.toByteArray())));
    props.put("messageName", "Person");
    Format proto = new Protobuf();
    proto = proto.getInstance(props);
    byte[] packed = packSample();

    IdentifierResolver resolver = proto.getResolver(packed);
    Assertions.assertEquals("Matthew Buckton", resolver.get("name"));
    Assertions.assertEquals("+1 555 555-55555", resolver.get("phones[0].number"));
  }

}
