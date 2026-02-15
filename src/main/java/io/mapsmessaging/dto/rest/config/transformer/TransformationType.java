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
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.dto.rest.config.transformer;

import com.google.gson.annotations.SerializedName;

public enum TransformationType {

  @SerializedName("json-to-xml")
  JSON_TO_XML("json-to-xml"),

  @SerializedName("xml-to-json")
  XML_TO_JSON("xml-to-json"),

  @SerializedName("json-to-value")
  JSON_TO_VALUE("json-to-value"),

  @SerializedName("json-query")
  JSON_QUERY("json-query"),

  @SerializedName("geohash")
  GEOHASH("geohash");

  private final String wireName;

  TransformationType(String wireName) {
    this.wireName = wireName;
  }

  public String getWireName() {
    return wireName;
  }

  public static TransformationType fromWireName(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim().toLowerCase();
    for (TransformationType transformationType : values()) {
      if (transformationType.wireName.equals(normalized)) {
        return transformationType;
      }
    }
    return null;
  }
}
