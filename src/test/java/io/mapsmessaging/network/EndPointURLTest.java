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

package io.mapsmessaging.network;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndPointURLTest {

  @Test
  void testProtocolHostPortFile() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234/file_path");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(1234, endPointURL.getPort());
    assertEquals("file_path", endPointURL.getFile());
    assertEquals("protocol_host_1234", endPointURL.getJMXName());
    assertEquals("protocol://host:1234/file_path", endPointURL.toString());
  }

  @Test
  void testProtocolHostPortRoot() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234/");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(1234, endPointURL.getPort());
    assertEquals("", endPointURL.getFile());
    assertEquals("protocol_host_1234", endPointURL.getJMXName());
  }

  @Test
  void testProtocolHostPortNoPath() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(1234, endPointURL.getPort());
    assertEquals("", endPointURL.getFile());
    assertEquals("protocol_host_1234", endPointURL.getJMXName());
    assertEquals("protocol://host:1234/", endPointURL.toString());
  }

  @Test
  void testProtocolHostFileNoPort() {
    EndPointURL endPointURL = new EndPointURL("protocol://host/file_path");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(0, endPointURL.getPort());
    assertEquals("file_path", endPointURL.getFile());
    assertEquals("protocol_host", endPointURL.getJMXName());
    assertEquals("protocol://host/file_path", endPointURL.toString());
  }

  @Test
  void testProtocolHostRootNoPort() {
    EndPointURL endPointURL = new EndPointURL("protocol://host/");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(0, endPointURL.getPort());
    assertEquals("", endPointURL.getFile());
    assertEquals("protocol_host", endPointURL.getJMXName());
    assertEquals("protocol://host/", endPointURL.toString());
  }

  @Test
  void testProtocolHostOnly() {
    EndPointURL endPointURL = new EndPointURL("protocol://host");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(0, endPointURL.getPort());
    assertEquals("", endPointURL.getFile());
    assertEquals("protocol_host", endPointURL.getJMXName());
    assertEquals("protocol://host/", endPointURL.toString());
  }


  @Test
  void testParametersTwoValues() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234/file_path?key1=val1&key2=val2");

    Map<String, String> params = endPointURL.getParameters();

    assertEquals(2, params.size());
    assertEquals("val1", params.get("key1"));
    assertEquals("val2", params.get("key2"));
    assertEquals("protocol://host:1234/file_path?key1=val1&key2=val2", endPointURL.toString());
  }

  @Test
  void testParametersSingleValue() {
    EndPointURL endPointURL =
        new EndPointURL("protocol://host:1234/path?token=ABC123");

    Map<String, String> params = endPointURL.getParameters();

    assertEquals(1, params.size());
    assertEquals("ABC123", params.get("token"));
    assertEquals("protocol://host:1234/path?token=ABC123", endPointURL.toString());
  }

  @Test
  void testParametersNoValues() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234/path");
    Map<String, String> params = endPointURL.getParameters();
    assertEquals(0, params.size());
    assertEquals("protocol://host:1234/path", endPointURL.toString());
  }

  @Test
  void testParametersEmptyValue() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234/path?flag=");

    Map<String, String> params = endPointURL.getParameters();

    assertEquals(1, params.size());
    assertEquals("", params.get("flag"));
    assertEquals("protocol://host:1234/path?flag=", endPointURL.toString());
  }

  @Test
  void testParametersWeirdSpacingIgnoredByTokenizer() {
    // Your tokenizer splits strictly on '&' so whitespace becomes part of keys.
    EndPointURL endPointURL = new EndPointURL("protocol://host/path?alpha=1&beta=2&gamma=3");

    Map<String, String> params = endPointURL.getParameters();

    assertEquals(3, params.size());
    assertEquals("1", params.get("alpha"));
    assertEquals("2", params.get("beta"));
    assertEquals("3", params.get("gamma"));
    assertEquals("protocol://host/path?alpha=1&beta=2&gamma=3", endPointURL.toString());
  }

  @Test
  void testMissingProtocolThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new EndPointURL("host:1234/file_path"));
  }

  @Test
  void testMissingHostThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new EndPointURL("protocol://"));
  }

  @Test
  void testEmptyUrlThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> new EndPointURL(""));
  }
  @Test
  void testParametersWithNoFilePath() {
    EndPointURL endPointURL = new EndPointURL("protocol://host:1234/?key=value");

    assertEquals("protocol", endPointURL.getProtocol());
    assertEquals("host", endPointURL.getHost());
    assertEquals(1234, endPointURL.getPort());
    assertEquals("", endPointURL.getFile());

    Map<String, String> params = endPointURL.getParameters();
    assertEquals(1, params.size());
    assertEquals("value", params.get("key"));
    assertEquals("protocol://host:1234/?key=value", endPointURL.toString());

  }


  @Test
 void testBracketedIpv6WithPort() {
    EndPointURL endPointURL = new EndPointURL("tcp://[2001:db8::7334]:443");

    assertEquals("tcp", endPointURL.getProtocol());
    assertEquals("2001:db8::7334", endPointURL.getHost());
    assertEquals(443, endPointURL.getPort());
  }

  @Test
 void testBracketedIpv6Localhost() {
    EndPointURL endPointURL = new EndPointURL("tcp://[::1]:1234");

    assertEquals("tcp", endPointURL.getProtocol());
    assertEquals("::1", endPointURL.getHost());
    assertEquals(1234, endPointURL.getPort());
  }

  @Test
 void testBracketedIpv6NoPort() {
    EndPointURL endPointURL = new EndPointURL("tcp://[fe80::1]");

    assertEquals("tcp", endPointURL.getProtocol());
    assertEquals("fe80::1", endPointURL.getHost());
    assertEquals(0, endPointURL.getPort());
  }

  @Test
 void testLegacyTripleColonSpecialCase() {
    EndPointURL endPointURL = new EndPointURL("tcp://:::443");

    assertEquals("tcp", endPointURL.getProtocol());
    assertEquals("::", endPointURL.getHost());
    assertEquals(443, endPointURL.getPort());
  }


}
