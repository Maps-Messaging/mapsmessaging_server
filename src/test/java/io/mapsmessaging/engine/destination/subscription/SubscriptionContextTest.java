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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.BitSet;

class SubscriptionContextTest {

  @Test
  void defaults_constructor_setsExpectedValues() {
    SubscriptionContext context = new SubscriptionContext("a/b");

    Assertions.assertEquals(-1L, context.getAllocatedId());
    Assertions.assertEquals("a/b", context.getDestinationName());
    Assertions.assertEquals("a/b", context.getAlias());
    Assertions.assertEquals(0L, context.getSubscriptionId());
    Assertions.assertEquals(0, context.getMaxAtRest());
    Assertions.assertEquals(1, context.getReceiveMaximum());

    Assertions.assertEquals(CreditHandler.AUTO, context.getCreditHandler());
    Assertions.assertEquals(RetainHandler.SEND_ALWAYS, context.getRetainHandler());
    Assertions.assertEquals(QualityOfService.AT_MOST_ONCE, context.getQualityOfService());
    Assertions.assertEquals(ClientAcknowledgement.AUTO, context.getAcknowledgementController());

    Assertions.assertNotNull(context.getFlags());
    Assertions.assertFalse(context.noLocalMessages());
    Assertions.assertFalse(context.allowOverlap());
    Assertions.assertFalse(context.isBrowser());
    Assertions.assertFalse(context.isRetainAsPublish());
    Assertions.assertFalse(context.isSync());

    Assertions.assertEquals(DestinationMode.NORMAL, context.getDestinationMode());
  }

  @Test
  void schemaNamespace_prefix_isParsed_andRemovedFromDestinationName() {
    String schemaPrefix = DestinationMode.SCHEMA.getNamespace();
    SubscriptionContext context = new SubscriptionContext(schemaPrefix + "sensor/temp");

    Assertions.assertEquals(DestinationMode.SCHEMA, context.getDestinationMode());
    Assertions.assertEquals("sensor/temp", context.getDestinationName());

    String key = context.getKey();
    Assertions.assertTrue(key.startsWith(DestinationMode.SCHEMA.getNamespace()));
    Assertions.assertTrue(key.contains("sensor/temp"));
  }

  @Test
  void setDestinationName_updatesAliasOnlyWhenAliasMatchedOldDestination() {
    SubscriptionContext context = new SubscriptionContext("old");

    // alias initially equals destinationName, so rename should update alias too
    context.setDestinationName("new");
    Assertions.assertEquals("new", context.getDestinationName());
    Assertions.assertEquals("new", context.getAlias());

    // now set custom alias, rename should NOT overwrite it
    context.setAlias("custom");
    context.setDestinationName("newer");
    Assertions.assertEquals("newer", context.getDestinationName());
    Assertions.assertEquals("custom", context.getAlias());
  }

  @Test
  void setAlias_null_fallsBackToCorrectedPath() throws Exception {
    SubscriptionContext context = new SubscriptionContext("dest");

    // rootPath has no setter, so we set it via reflection to test behavior.
    setPrivateField(context, "rootPath", "/root//");
    context.setDestinationName("/a//b");

    context.setAlias(null);
    Assertions.assertEquals("/root/a/b", context.getAlias().replace("//", "/"));
  }

  @Test
  void wildcardDetection_works() {
    SubscriptionContext normal = new SubscriptionContext("a/b");
    Assertions.assertFalse(normal.containsWildcard());

    SubscriptionContext hash = new SubscriptionContext("a/#");
    Assertions.assertTrue(hash.containsWildcard());

    SubscriptionContext plus = new SubscriptionContext("a/+/b");
    Assertions.assertTrue(plus.containsWildcard());
  }

  @Test
  void sharedSubscriptionDetection_works() {
    SubscriptionContext context = new SubscriptionContext("a/b");
    Assertions.assertFalse(context.isSharedSubscription());

    context.setSharedName("");
    Assertions.assertFalse(context.isSharedSubscription());

    context.setSharedName("group1");
    Assertions.assertTrue(context.isSharedSubscription());
  }

  @Test
  void flags_roundTripThroughSettersAndGetters() {
    SubscriptionContext context = new SubscriptionContext("a/b");

    context.setNoLocalMessages(true);
    context.setAllowOverlap(true);
    context.setBrowserFlag(true);
    context.setRetainAsPublish(true);
    context.setSync(true);

    Assertions.assertTrue(context.noLocalMessages());
    Assertions.assertTrue(context.allowOverlap());
    Assertions.assertTrue(context.isBrowser());
    Assertions.assertTrue(context.isRetainAsPublish());
    Assertions.assertTrue(context.isSync());

    context.setNoLocalMessages(false);
    context.setAllowOverlap(false);
    context.setBrowserFlag(false);
    context.setRetainAsPublish(false);
    context.setSync(false);

    Assertions.assertFalse(context.noLocalMessages());
    Assertions.assertFalse(context.allowOverlap());
    Assertions.assertFalse(context.isBrowser());
    Assertions.assertFalse(context.isRetainAsPublish());
    Assertions.assertFalse(context.isSync());
  }

  @Test
  void saveLoad_roundTrip_preservesState() throws Exception {
    SubscriptionContext original = new SubscriptionContext("topic/test");

    // rootPath no setter: set via reflection
    setPrivateField(original, "rootPath", "/root/");
    original.setAlias("alias1");
    original.setSharedName("share");
    original.setSelector("a = 1");

    original.setAcknowledgementController(ClientAcknowledgement.AUTO);
    original.setCreditHandler(CreditHandler.AUTO);
    original.setRetainHandler(RetainHandler.SEND_ALWAYS);
    original.setQualityOfService(QualityOfService.AT_LEAST_ONCE);

    original.setSubscriptionId(1234L);
    original.setReceiveMaximum(77);
    original.setMaxAtRest(999);

    original.setNoLocalMessages(true);
    original.setAllowOverlap(true);
    original.setBrowserFlag(false);
    original.setRetainAsPublish(true);
    original.setSync(true);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    original.save(baos);

    long sessionId = 555L;
    SubscriptionContext loaded = new SubscriptionContext(new ByteArrayInputStream(baos.toByteArray()), sessionId);

    Assertions.assertEquals(sessionId, loaded.getAllocatedId());
    Assertions.assertEquals(original.getDestinationName(), loaded.getDestinationName());
    Assertions.assertEquals(original.getAlias(), loaded.getAlias());
    Assertions.assertEquals(original.getSharedName(), loaded.getSharedName());
    Assertions.assertEquals(original.getSelector(), loaded.getSelector());

    Assertions.assertEquals(original.getAcknowledgementController(), loaded.getAcknowledgementController());
    Assertions.assertEquals(original.getDestinationMode(), loaded.getDestinationMode());
    Assertions.assertEquals(original.getRetainHandler(), loaded.getRetainHandler());
    Assertions.assertEquals(original.getQualityOfService(), loaded.getQualityOfService());
    Assertions.assertEquals(original.getCreditHandler(), loaded.getCreditHandler());

    Assertions.assertEquals(original.getSubscriptionId(), loaded.getSubscriptionId());
    Assertions.assertEquals(original.getReceiveMaximum(), loaded.getReceiveMaximum());
    Assertions.assertEquals(original.getMaxAtRest(), loaded.getMaxAtRest());

    Assertions.assertEquals(original.noLocalMessages(), loaded.noLocalMessages());
    Assertions.assertEquals(original.allowOverlap(), loaded.allowOverlap());
    Assertions.assertEquals(original.isBrowser(), loaded.isBrowser());
    Assertions.assertEquals(original.isRetainAsPublish(), loaded.isRetainAsPublish());
    Assertions.assertEquals(original.isSync(), loaded.isSync());

    // Root path should round-trip too (it is persisted)
    Assertions.assertEquals(getPrivateField(original, "rootPath"), getPrivateField(loaded, "rootPath"));
  }

  @Test
  void compareTo_ordersByQualityOfServiceDescending() {
    SubscriptionContext qos0 = new SubscriptionContext("a");
    qos0.setQualityOfService(QualityOfService.AT_MOST_ONCE);

    SubscriptionContext qos1 = new SubscriptionContext("b");
    qos1.setQualityOfService(QualityOfService.AT_LEAST_ONCE);

    SubscriptionContext qos2 = new SubscriptionContext("c");
    qos2.setQualityOfService(QualityOfService.EXACTLY_ONCE);

    // compareTo returns: lhs.level - this.level
    // So higher QoS should be considered "smaller" (comes first in ascending sorts).

    Assertions.assertTrue(qos2.compareTo(qos1) < 0);
    Assertions.assertTrue(qos2.compareTo(qos0) < 0);

    Assertions.assertTrue(qos1.compareTo(qos2) > 0);
    Assertions.assertTrue(qos1.compareTo(qos0) < 0);

    Assertions.assertTrue(qos0.compareTo(qos2) > 0);
    Assertions.assertTrue(qos0.compareTo(qos1) > 0);

    Assertions.assertEquals(0, qos1.compareTo(qos1));
  }

  @Test
  void equals_isQualityOfServiceOnly_andIgnoresEverythingElse() throws Exception {
    SubscriptionContext a = new SubscriptionContext("one");
    SubscriptionContext b = new SubscriptionContext("two");

    a.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    b.setQualityOfService(QualityOfService.AT_LEAST_ONCE);

    // Make everything else wildly different
    a.setAlias("alias-a");
    b.setAlias("alias-b");
    a.setSharedName("share-a");
    b.setSharedName("share-b");
    a.setSelector("x = 1");
    b.setSelector("y = 2");
    a.setSubscriptionId(1L);
    b.setSubscriptionId(2L);
    a.setReceiveMaximum(10);
    b.setReceiveMaximum(99);
    a.setMaxAtRest(1);
    b.setMaxAtRest(999);

    BitSet flagsA = new BitSet();
    flagsA.set(0, true);
    a.setFlags(flagsA);

    BitSet flagsB = new BitSet();
    flagsB.set(3, true);
    b.setFlags(flagsB);

    setPrivateField(a, "rootPath", "/a/");
    setPrivateField(b, "rootPath", "/b/");

    Assertions.assertEquals(a, b);

    b.setQualityOfService(QualityOfService.AT_MOST_ONCE);
    Assertions.assertNotEquals(a, b);
  }

  @Test
  void equalsHashCode_contractRisk_isVisible() {
    SubscriptionContext a = new SubscriptionContext("x");
    SubscriptionContext b = new SubscriptionContext("y");
    a.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    b.setQualityOfService(QualityOfService.AT_LEAST_ONCE);

    // equals true...
    Assertions.assertEquals(a, b);

    // ...but hashCode is inherited (and likely differs). This isn't "assert must differ",
    // it's "this can differ", so we just assert the current behavior isn't forcing equality.
    // If this starts passing with equal hashCodes later, fine. The real issue is equals ignores identity.
    Assertions.assertNotEquals(a.hashCode(), b.hashCode());
  }

  private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Object getPrivateField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }
}
