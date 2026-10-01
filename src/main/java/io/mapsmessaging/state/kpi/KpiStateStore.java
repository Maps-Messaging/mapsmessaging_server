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

package io.mapsmessaging.state.kpi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Persists what must survive a MAPS restart: the mission roster and MTI announcements (so a
 * restart doesn't shrink the denominator, clarification Q1/Q2), the assets with fresh MTI opinions
 * (the cache rebuild baseline, Q15) and open fault runs (the RESTART failure type, Q12).
 */
final class KpiStateStore {

  static final class State {
    String savedAt;
    Map<String, String> roster = new TreeMap<>();
    Set<String> mtiAnnounced = new TreeSet<>();
    Set<String> freshMti = new TreeSet<>();
    List<FaultRunTracker.FaultRun> openRuns = new ArrayList<>();
  }

  private final Logger logger = LoggerFactory.getLogger(KpiStateStore.class);
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final Path file;

  KpiStateStore(Path directory) {
    this.file = directory.resolve("kpi-state.json");
  }

  /** @return the saved state, or {@code null} if there is none or it cannot be read. */
  State load() {
    if (!Files.isRegularFile(file)) {
      return null;
    }
    try {
      return gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
    } catch (IOException | JsonParseException e) {
      logger.warn("KPI state {} could not be read, starting with an empty roster", file, e);
      return null;
    }
  }

  void save(State state) {
    try {
      Files.createDirectories(file.getParent());
      Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temporary, gson.toJson(state), StandardCharsets.UTF_8);
      Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      logger.warn("KPI state could not be saved to {}", file, e);
    }
  }
}
