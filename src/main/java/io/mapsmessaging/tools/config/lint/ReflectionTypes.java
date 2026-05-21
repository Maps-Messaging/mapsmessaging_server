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

package io.mapsmessaging.tools.config.lint;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ReflectionTypes {

  private ReflectionTypes() {
  }

  public static Class<?> toClass(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    }

    if (type instanceof ParameterizedType) {
      Type raw = ((ParameterizedType) type).getRawType();
      if (raw instanceof Class<?>) {
        return (Class<?>) raw;
      }
    }

    return null;
  }

  public static boolean isNumeric(Class<?> type) {
    if (type == null) {
      return false;
    }

    if (type.isPrimitive()) {
      return type == int.class
          || type == long.class
          || type == short.class
          || type == byte.class
          || type == double.class
          || type == float.class;
    }

    return type == Integer.class
        || type == Long.class
        || type == Short.class
        || type == Byte.class
        || type == Double.class
        || type == Float.class;
  }
}
