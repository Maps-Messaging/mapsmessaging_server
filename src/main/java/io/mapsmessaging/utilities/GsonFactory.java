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

package io.mapsmessaging.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

@SuppressWarnings("java:S6548") // yes, it is a singleton
public class GsonFactory {

  private static class Holder {
    static final GsonFactory INSTANCE = new GsonFactory();
  }

  public static GsonFactory getInstance() {
    return GsonFactory.Holder.INSTANCE;
  }

  @Getter
  private final Gson prettyGson;

  private GsonFactory() {
    prettyGson = new GsonBuilder().setPrettyPrinting().create();
  }

  public Gson getSimpleGson() {
    return prettyGson;
  }
}
