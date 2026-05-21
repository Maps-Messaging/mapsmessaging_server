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

package io.mapsmessaging.api.transformers.jsonmutate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class JsonPath {

  private JsonPath() {
  }

  public static JsonElement get(JsonObject root, String path) {
    if (root == null || path == null || path.isBlank()) {
      return null;
    }

    List<PathToken> tokens = parse(path);
    JsonElement current = root;

    for (PathToken token : tokens) {
      if (current == null || current.isJsonNull()) {
        return null;
      }

      if (token.index == null) {
        if (!current.isJsonObject()) {
          return null;
        }
        JsonObject object = current.getAsJsonObject();
        current = object.get(token.key);
      } else {
        if (!current.isJsonObject()) {
          return null;
        }
        JsonObject object = current.getAsJsonObject();
        JsonElement arrayElement = object.get(token.key);
        if (arrayElement == null || !arrayElement.isJsonArray()) {
          return null;
        }
        JsonArray array = arrayElement.getAsJsonArray();
        int idx = token.index;
        if (idx < 0 || idx >= array.size()) {
          return null;
        }
        current = array.get(idx);
      }
    }

    return current;
  }

  public static void set(JsonObject root, String path, JsonElement value) {
    if (root == null || path == null || path.isBlank()) {
      return;
    }

    List<PathToken> tokens = parse(path);
    if (tokens.isEmpty()) {
      return;
    }

    JsonElement current = root;

    for (int i = 0; i < tokens.size() - 1; i++) {
      PathToken token = tokens.get(i);

      if (!current.isJsonObject()) {
        return;
      }

      JsonObject object = current.getAsJsonObject();
      JsonElement next;

      if (token.index == null) {
        next = object.get(token.key);
        if (next == null || next.isJsonNull()) {
          JsonObject created = new JsonObject();
          object.add(token.key, created);
          next = created;
        }
      } else {
        next = object.get(token.key);
        if (next == null || next.isJsonNull()) {
          JsonArray created = new JsonArray();
          object.add(token.key, created);
          next = created;
        }
        if (!next.isJsonArray()) {
          return;
        }

        JsonArray array = next.getAsJsonArray();
        int idx = token.index;
        ensureArraySize(array, idx + 1);

        JsonElement elementAt = array.get(idx);
        if (elementAt == null || elementAt.isJsonNull()) {
          JsonObject created = new JsonObject();
          array.set(idx, created);
          next = created;
        } else {
          next = elementAt;
        }
      }

      current = next;
    }

    PathToken last = tokens.get(tokens.size() - 1);
    if (!current.isJsonObject()) {
      return;
    }
    JsonObject parent = current.getAsJsonObject();

    if (last.index == null) {
      parent.add(last.key, value);
      return;
    }

    JsonElement arrayElement = parent.get(last.key);
    if (arrayElement == null || arrayElement.isJsonNull()) {
      JsonArray created = new JsonArray();
      parent.add(last.key, created);
      arrayElement = created;
    }
    if (!arrayElement.isJsonArray()) {
      return;
    }

    JsonArray array = arrayElement.getAsJsonArray();
    ensureArraySize(array, last.index + 1);
    array.set(last.index, value);
  }

  public static void remove(JsonObject root, String path) {
    if (root == null || path == null || path.isBlank()) {
      return;
    }

    List<PathToken> tokens = parse(path);
    if (tokens.isEmpty()) {
      return;
    }

    JsonElement current = root;

    for (int i = 0; i < tokens.size() - 1; i++) {
      PathToken token = tokens.get(i);

      if (!current.isJsonObject()) {
        return;
      }
      JsonObject object = current.getAsJsonObject();

      if (token.index == null) {
        current = object.get(token.key);
      } else {
        JsonElement arrayElement = object.get(token.key);
        if (arrayElement == null || !arrayElement.isJsonArray()) {
          return;
        }
        JsonArray array = arrayElement.getAsJsonArray();
        int idx = token.index;
        if (idx < 0 || idx >= array.size()) {
          return;
        }
        current = array.get(idx);
      }

      if (current == null || current.isJsonNull()) {
        return;
      }
    }

    PathToken last = tokens.get(tokens.size() - 1);
    if (!current.isJsonObject()) {
      return;
    }
    JsonObject parent = current.getAsJsonObject();

    if (last.index == null) {
      parent.remove(last.key);
      return;
    }

    JsonElement arrayElement = parent.get(last.key);
    if (arrayElement == null || !arrayElement.isJsonArray()) {
      return;
    }
    JsonArray array = arrayElement.getAsJsonArray();
    int idx = last.index;
    if (idx < 0 || idx >= array.size()) {
      return;
    }
    array.set(idx, JsonNull.INSTANCE);
  }

  private static void ensureArraySize(JsonArray array, int size) {
    while (array.size() < size) {
      array.add(JsonNull.INSTANCE);
    }
  }

  private static List<PathToken> parse(String path) {
    String trimmed = path.trim();
    String[] parts = trimmed.split("\\.");
    List<PathToken> tokens = new ArrayList<>();

    for (String part : parts) {
      if (part.isBlank()) {
        continue;
      }

      int open = part.indexOf('[');
      if (open < 0) {
        tokens.add(new PathToken(part, null));
        continue;
      }

      int close = part.indexOf(']', open);
      if (close < 0) {
        tokens.add(new PathToken(part, null));
        continue;
      }

      String key = part.substring(0, open);
      String indexText = part.substring(open + 1, close).trim();
      Integer index = null;
      try {
        index = Integer.parseInt(indexText);
      } catch (NumberFormatException ignore) {
        // treated as plain key
      }

      if (key.isBlank() || index == null) {
        tokens.add(new PathToken(part, null));
      } else {
        tokens.add(new PathToken(key, index));
      }
    }

    return tokens;
  }

  private static final class PathToken {
    private final String key;
    private final Integer index;

    private PathToken(String key, Integer index) {
      this.key = key;
      this.index = index;
    }
  }
}
