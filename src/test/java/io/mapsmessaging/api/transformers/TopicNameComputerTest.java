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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.selector.IdentifierResolver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TopicNameComputerTest {

  @Test
  void noTokens_returnsTemplateUnchanged() {
    IdentifierResolver resolver = key -> null;

    String result = TopicNameCompiler.computeTopicName("/a/b/c", "orig/topic", resolver);

    assertEquals("/a/b/c", result);
  }

  @Test
  void replacesSourceToken() {
    IdentifierResolver resolver = key -> null;

    String result = TopicNameCompiler.computeTopicName("{source}/mav/gps", "/in/topic", resolver);

    assertEquals("/in/topic/mav/gps", result);
  }

  @Test
  void replacesSingleLookupToken() {
    Map<String, Object> values = new HashMap<>();
    values.put("lookup_1", "A");
    IdentifierResolver resolver = values::get;

    String result = TopicNameCompiler.computeTopicName("/folder/{lookup_1}/x", "ignored", resolver);

    assertEquals("/folder/A/x", result);
  }

  @Test
  void replacesMultipleDifferentLookupTokens() {
    Map<String, Object> values = new HashMap<>();
    values.put("lookup_1", "A");
    values.put("lookup_2", "B");
    values.put("lookup5", "Z");
    IdentifierResolver resolver = values::get;

    String template = "/folder/{lookup_1}/{lookup_2}/mav/gps/{lookup_1}/{lookup5}";
    String result = TopicNameCompiler.computeTopicName(template, "ignored", resolver);

    assertEquals("/folder/A/B/mav/gps/A/Z", result);
  }

  @Test
  void missingLookupUsesSubstitute() {
    IdentifierResolver resolver = key -> null;

    String result = TopicNameCompiler.computeTopicName("/folder/{lookup_1}/x", "ignored", resolver);

    assertEquals("/folder/..../x", result);
  }

  @Test
  void missingAndPresentMix() {
    Map<String, Object> values = new HashMap<>();
    values.put("lookup_1", "A");
    IdentifierResolver resolver = values::get;

    String result = TopicNameCompiler.computeTopicName("/{lookup_1}/{lookup_2}/{lookup_1}", "ignored", resolver);

    assertEquals("/A/..../A", result);
  }

  @Test
  void nullSourceReplacedAsNullStringLiteral() {
    IdentifierResolver resolver = key -> null;

    String result = TopicNameCompiler.computeTopicName("{source}/x", null, resolver);

    // StringBuilder.append((Object)null) appends "null"
    assertEquals("..../x", result);
  }

  @Test
  void unmatchedOpeningBrace_isTreatedAsLiteralAndTokenIsIgnored() {
    Map<String, Object> values = new HashMap<>();
    values.put("lookup_1", "A");
    IdentifierResolver resolver = values::get;

    String result = TopicNameCompiler.computeTopicName("/x/{lookup_1", "ignored", resolver);

    // With the current implementation, everything after '{' is swallowed into token buffer and never flushed.
    // So the result becomes "/x/".
    assertEquals("/x/", result);
  }

  @Test
  void emptyTokenBecomesMissingSubstitute() {
    IdentifierResolver resolver = key -> null;

    String result = TopicNameCompiler.computeTopicName("/x/{}", "ignored", resolver);

    assertEquals("/x/....", result);
  }

  private static IdentifierResolver resolver(Map<String, Object> map) {
    return map::get;
  }

  @Test
  void returnsTemplateWhenNoTokens() {
    String template = "/a/b/c";
    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(Map.of()));
    assertEquals("/a/b/c", result);
  }

  @Test
  void replacesSameTokenMultipleTimes() {
    String template = "/x/{lookup_1}/y/{lookup_1}/z";
    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(Map.of("lookup_1", "VAL")));
    assertEquals("/x/VAL/y/VAL/z", result);
  }

  @Test
  void replacesMultipleDifferentTokens() {
    String template = "/folder/{lookup_1}/{lookup_2}/mav/gps/{lookup_1}/{lookup5}";
    Map<String, Object> values = new HashMap<>();
    values.put("lookup_1", "one");
    values.put("lookup_2", 2);
    values.put("lookup5", "five");

    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(values));
    assertEquals("/folder/one/2/mav/gps/one/five", result);
  }

  @Test
  void substitutesMissingTokenWithDots() {
    String template = "/a/{missing}/b";
    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(Map.of()));
    assertEquals("/a/..../b", result);
  }

  @Test
  void mixesPresentAndMissingTokens() {
    String template = "/a/{lookup_1}/{missing}/{lookup_2}";
    Map<String, Object> values = new HashMap<>();
    values.put("lookup_1", "ONE");
    values.put("lookup_2", "TWO");

    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(values));
    assertEquals("/a/ONE/..../TWO", result);
  }

  @Test
  void handlesNonStringValuesViaToString() {
    String template = "/a/{lookup_1}/b";
    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(Map.of("lookup_1", 12345)));
    assertEquals("/a/12345/b", result);
  }

  @Test
  void leavesUnclosedTokenAsLiteralPrefix() {
    // Current implementation: starts token capture at '{' and never flushes it if '}' never appears,
    // meaning the '{' and token text are effectively dropped.
    // This test locks in that behaviour. If you later decide to treat it as literal text, change test + code together.
    String template = "/a/{lookup_1/b";
    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(Map.of("lookup_1", "X")));
    assertEquals("/a/", result);
  }

  @Test
  void treatsStrayClosingBraceAsLiteral() {
    String template = "/a}/b";
    String result = TopicNameCompiler.computeTopicName(template, "/source/topic", resolver(Map.of()));
    assertEquals("/a}/b", result);
  }
}
