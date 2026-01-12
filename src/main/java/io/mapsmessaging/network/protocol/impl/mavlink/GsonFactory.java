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

package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public final class GsonFactory {

  private GsonFactory() {
  }

  public static Gson createStrictJsonWithSafeFloats() {
    return new GsonBuilder()
        .registerTypeAdapter(Double.class, new SafeDoubleAdapter())
        .registerTypeAdapter(double.class, new SafeDoubleAdapter())
        .registerTypeAdapter(Float.class, new SafeFloatAdapter())
        .registerTypeAdapter(float.class, new SafeFloatAdapter())
        .create();
  }

  private static final class SafeDoubleAdapter extends TypeAdapter<Double> {

    @Override
    public void write(JsonWriter out, Double value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      if (!Double.isFinite(value)) {
        out.nullValue();
        return;
      }
      out.value(value);
    }

    @Override
    public Double read(JsonReader in) throws IOException {
      JsonToken token = in.peek();
      if (token == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      if (token == JsonToken.STRING) {
        String text = in.nextString();
        if ("NaN".equalsIgnoreCase(text) || "Infinity".equalsIgnoreCase(text) || "-Infinity".equalsIgnoreCase(text)) {
          return null;
        }
        return Double.valueOf(text);
      }
      return in.nextDouble();
    }
  }

  private static final class SafeFloatAdapter extends TypeAdapter<Float> {

    @Override
    public void write(JsonWriter out, Float value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      if (!Float.isFinite(value)) {
        out.nullValue();
        return;
      }
      out.value(value);
    }

    @Override
    public Float read(JsonReader in) throws IOException {
      JsonToken token = in.peek();
      if (token == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      if (token == JsonToken.STRING) {
        String text = in.nextString();
        if ("NaN".equalsIgnoreCase(text) || "Infinity".equalsIgnoreCase(text) || "-Infinity".equalsIgnoreCase(text)) {
          return null;
        }
        return Float.valueOf(text);
      }
      return (float) in.nextDouble();
    }
  }
}
