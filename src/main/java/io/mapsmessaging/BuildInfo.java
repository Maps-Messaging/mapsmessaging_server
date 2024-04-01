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

package io.mapsmessaging;

import lombok.Getter;
import lombok.ToString;

/**
 * This class wraps two public static final String variables: buildDate and buildVersion.
 * The buildDate variable represents the date and time of the build in the format "dd-mmm-yyyy HH:MM:SS".
 * The buildVersion variable represents the version of the build in the format "00.00.00-SNAPSHOT".
 * The class is annotated with @ToString, which generates a toString() method for the class.
 * This allows the class to be printed in a human-readable format.
 */
@ToString
public class BuildInfo {

  @Getter
  public static final String buildDate = "dd-mmm-yyyy HH:MM:SS";
  @Getter
  public static final String buildVersion = "00.00.00-SNAPSHOT";

}
