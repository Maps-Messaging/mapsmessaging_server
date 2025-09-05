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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WildcardTest {

  @Test
  void wildcardTest(){
    Assertions.assertTrue(mqttWildcard("test/foo/bar", "test/foo/bar")); // []
    Assertions.assertTrue(mqttWildcard("test/foo/bar", "test/+/bar")); // ["foo"]
    Assertions.assertTrue(mqttWildcard("test/foo/bar", "test/#")); // ["foo/bar"]
    Assertions.assertTrue(mqttWildcard("test/foo/bar/baz", "test/+/#")); // ["foo", "bar/baz"]
    Assertions.assertTrue(mqttWildcard("test/foo/bar/baz", "test/+/+/baz")); // ["foo", "bar"]

    Assertions.assertTrue(mqttWildcard("test", "test/#")); // []
    Assertions.assertTrue(mqttWildcard("test/", "test/#")); // [""]
    Assertions.assertTrue(mqttWildcard("test/temp", "test/#")); // [""]
    Assertions.assertTrue(mqttWildcard("test/temp", "test/+")); // [""]

    Assertions.assertFalse(mqttWildcard("testA", "+/+"));
    Assertions.assertFalse(mqttWildcard("test/foo/bar", "test/+")); // null
    Assertions.assertFalse(mqttWildcard("test/foo/bar", "test/nope/bar")); // null
  }


  private boolean mqttWildcard(String name, String subscription){
    return DestinationSet.matches(subscription, name);
  }
}
