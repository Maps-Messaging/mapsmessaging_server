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

import java.util.Objects;

public class TopicNameCompiler {

  public static String computeTopicName(
      String template,
      String source,
      IdentifierResolver identifierResolver
  ) {
    Objects.requireNonNull(template, "template");
    Objects.requireNonNull(identifierResolver, "identifierResolver");

    StringBuilder output = new StringBuilder(template.length());
    StringBuilder token = null;

    int length = template.length();

    for (int i = 0; i < length; i++) {
      char character = template.charAt(i);

      if (character == '{') {
        token = new StringBuilder(16);
        continue;
      }

      if (character == '}' && token != null) {
        String key = token.toString();
        token = null;

        Object resolvedValue;
        if ("source".equals(key)) {
          resolvedValue = source;
        }
        else {
          resolvedValue = identifierResolver.get(key);
        }

        if (resolvedValue == null) {
          appendValidated(output, "....");
        }
        else {
          appendValidated(output, resolvedValue.toString());
        }
        continue;
      }

      if (token != null) {
        token.append(character);
      }
      else {
        appendValidated(output, character);
      }
    }

    return output.toString();
  }

  private static void appendValidated(StringBuilder output, String value) {
    int length = value.length();
    for (int i = 0; i < length; i++) {
      appendValidated(output, value.charAt(i));
    }
  }

  private static void appendValidated(StringBuilder output, char character) {
    if (character == '*' || character == '+' || character == '#') {
      throw new IllegalArgumentException("Illegal character in topic: '" + character + "'");
    }

    if (character == '/') {
      int outLength = output.length();
      if (outLength > 0 && output.charAt(outLength - 1) == '/') {
        return; // prevent "//"
      }
    }

    output.append(character);
  }

  private TopicNameCompiler(){}
}
