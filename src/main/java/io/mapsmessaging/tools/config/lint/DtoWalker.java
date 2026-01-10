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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DtoWalker {

  private final LintEngine lintEngine;

  public DtoWalker(LintEngine lintEngine) {
    this.lintEngine = lintEngine;
  }

  public List<LintIssue> lint(String configName, Class<? extends BaseConfigDTO> rootDto) {
    List<LintIssue> issues = new ArrayList<>();

    Deque<WalkNode> stack = new ArrayDeque<>();
    Set<Class<?>> visited = new HashSet<>();

    stack.push(new WalkNode(rootDto, rootDto.getSimpleName()));

    while (!stack.isEmpty()) {
      WalkNode node = stack.pop();

      if (!visited.add(node.getClazz())) {
        continue;
      }

      issues.addAll(lintEngine.lintClass(configName, rootDto.getName(), node.getClazz(), node.getPath()));

      for (Field field : ReflectionFields.getAllInstanceFields(node.getClazz())) {
        String fieldPath = node.getPath().isEmpty()
            ? field.getName()
            : node.getPath() + "." + field.getName();

        issues.addAll(lintEngine.lintField(configName, rootDto.getName(), node.getClazz(), field, fieldPath));

        Class<?> rawType = field.getType();

        if (isCollection(rawType)) {
          Type genericType = field.getGenericType();
          if (!(genericType instanceof ParameterizedType)) {
            // ERROR: raw/erased generics
            issues.add(LintIssue.error(
                configName,
                rootDto.getName(),
                fieldPath,
                "GENERIC_TYPE_ERASED",
                "Collection field uses raw/erased generic type info: " + field.getGenericType().getTypeName()
            ));
            continue;
          }

          ParameterizedType parameterizedType = (ParameterizedType) genericType;
          Type[] typeArguments = parameterizedType.getActualTypeArguments();

          if (List.class.isAssignableFrom(rawType)) {
            if (typeArguments.length != 1) {
              issues.add(LintIssue.error(
                  configName,
                  rootDto.getName(),
                  fieldPath,
                  "GENERIC_ARITY_INVALID",
                  "List must have 1 generic type argument but has " + typeArguments.length
              ));
              continue;
            }

            Class<?> itemClass = ReflectionTypes.toClass(typeArguments[0]);
            if (itemClass != null && BaseConfigDTO.class.isAssignableFrom(itemClass)) {
              stack.push(new WalkNode(itemClass, fieldPath + "[]"));
            }
          }
          else if (Map.class.isAssignableFrom(rawType)) {
            if (typeArguments.length != 2) {
              issues.add(LintIssue.error(
                  configName,
                  rootDto.getName(),
                  fieldPath,
                  "GENERIC_ARITY_INVALID",
                  "Map must have 2 generic type arguments but has " + typeArguments.length
              ));
              continue;
            }

            Class<?> valueClass = ReflectionTypes.toClass(typeArguments[1]);
            if (valueClass != null && BaseConfigDTO.class.isAssignableFrom(valueClass)) {
              stack.push(new WalkNode(valueClass, fieldPath + "{}"));
            }
          }

          continue;
        }

        if (BaseConfigDTO.class.isAssignableFrom(rawType)) {
          stack.push(new WalkNode(rawType, fieldPath));
        }
      }

      issues.addAll(lintPolymorphic(configName, rootDto.getName(), node.getClazz(), node.getPath()));
    }

    return issues;
  }

  private boolean isCollection(Class<?> type) {
    return List.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
  }

  private List<LintIssue> lintPolymorphic(String configName, String rootDtoName, Class<?> clazz, String path) {
    List<LintIssue> issues = new ArrayList<>();

    JsonTypeInfo typeInfo = clazz.getAnnotation(JsonTypeInfo.class);
    JsonSubTypes subTypes = clazz.getAnnotation(JsonSubTypes.class);

    if (typeInfo == null && subTypes == null) {
      return issues;
    }

    if (typeInfo != null && subTypes == null) {
      issues.add(LintIssue.warn(
          configName,
          rootDtoName,
          path,
          "POLYMORPHIC_SUBTYPES_MISSING",
          "Class has @JsonTypeInfo but no @JsonSubTypes: " + clazz.getName()
      ));
      return issues;
    }

    if (subTypes == null) {
      return issues;
    }

    Map<String, Class<?>> discriminatorToClass = new java.util.LinkedHashMap<>();
    Set<Class<?>> subtypeClasses = new HashSet<>();

    for (JsonSubTypes.Type subtype : subTypes.value()) {
      Class<?> subtypeClass = subtype.value();
      subtypeClasses.add(subtypeClass);

      String name = subtype.name();
      String discriminator = (name == null || name.isBlank())
          ? subtypeClass.getSimpleName()
          : name;

      if (discriminatorToClass.containsKey(discriminator)) {
        Class<?> first = discriminatorToClass.get(discriminator);
        issues.add(LintIssue.error(
            configName,
            rootDtoName,
            path,
            "POLYMORPHIC_DISCRIMINATOR_COLLISION",
            "Discriminator '" + discriminator + "' maps to both " + first.getName() + " and " + subtypeClass.getName()
        ));
      }
      else {
        discriminatorToClass.put(discriminator, subtypeClass);
      }
    }

    // Reachability check (best-effort): ensure each subtype is assignable to base
    for (Class<?> subtypeClass : subtypeClasses) {
      if (!clazz.isAssignableFrom(subtypeClass)) {
        issues.add(LintIssue.warn(
            configName,
            rootDtoName,
            path,
            "POLYMORPHIC_SUBTYPE_NOT_ASSIGNABLE",
            "Subtype " + subtypeClass.getName() + " is not assignable to base " + clazz.getName()
        ));
      }
    }

    return issues;
  }

  private static final class WalkNode {

    private final Class<?> clazz;
    private final String path;

    private WalkNode(Class<?> clazz, String path) {
      this.clazz = clazz;
      this.path = path == null ? "" : path;
    }

    public Class<?> getClazz() {
      return clazz;
    }

    public String getPath() {
      return path;
    }
  }
}
