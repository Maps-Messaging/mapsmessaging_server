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

package io.mapsmessaging.api.transformers;

public final class TransformationTestVectors {

  private TransformationTestVectors() {
  }

  public static final String VALID_JSON_OBJECT = "{\"a\":1,\"b\":\"x\",\"nested\":{\"c\":true}}";
  public static final String VALID_JSON_OBJECT_MINIMAL = "{\"a\":1}";
  public static final String VALID_JSON_ARRAY = "[1,2,3]";
  public static final String VALID_JSON_STRING = "\"hello\"";

  public static final String INVALID_JSON = "{a:1";
  public static final String NON_JSON_TEXT = "not json at all";

  public static final String VALID_XML_SIMPLE =
      "<root><a>1</a><b>x</b><nested><c>true</c></nested></root>";

  public static final String INVALID_XML =
      "<root><a>1</root>";

  public static final byte[] EMPTY_BYTES = new byte[0];
}
