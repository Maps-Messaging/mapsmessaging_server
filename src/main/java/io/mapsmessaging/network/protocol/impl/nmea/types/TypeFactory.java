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

package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.Iterator;

public class TypeFactory {

  public static Type create(String parameterName, String typeName, String param, Iterator<String> iterator) {
    switch (typeName) {
      case "Position":
        return new PositionType(iterator.next(), iterator.next());

      case "UTCTime":
        return new UTCTimeType(iterator.next());

      case "Height":
        return new HeightType(iterator.next(), iterator.next());

      case "String":
      case "char":
        return new StringType(iterator.next());

      case "long":
      case "int":
        return new LongType(iterator.next());

      case "double":
      case "float":
        return new DoubleType(iterator.next());

      case "boolean":
        return new BooleanType(iterator.next(), param);

      case "Date":
        return new DateType(iterator.next());

      case "Enum":
        return EnumTypeFactory.getInstance().getEnum(parameterName, iterator.next());

      default:
        return null;
    }
  }

  private TypeFactory() {
  }
}
