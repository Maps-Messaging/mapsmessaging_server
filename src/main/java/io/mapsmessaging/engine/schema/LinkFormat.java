/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.schema;

import lombok.Getter;

public class LinkFormat {

  @Getter
  private final String path;

  @Getter
  private final String interfaceDescription;

  @Getter
  private final String resourceType;

  public LinkFormat(String path, String interfaceDescription, String resourceType) {
    this.path = path;
    this.interfaceDescription = interfaceDescription;
    this.resourceType = resourceType;
  }

  public String pack() {
    return "<" + path + ">"
        + "if=\"" + interfaceDescription + "\";"
        + "rt=\"" + resourceType + "\"";
  }
}
