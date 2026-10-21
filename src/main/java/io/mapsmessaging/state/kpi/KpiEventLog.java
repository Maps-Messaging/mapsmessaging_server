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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Timestamped raw KPI events as JSON lines, one file per UTC day
 * ({@code kpi-events-YYYY-MM-DD.jsonl}), for the post-exercise analysis and export (IC26 KPI
 * clarification Q20). Every line has {@code ts} (ISO-8601 UTC) and {@code type}. Flushed per
 * event, so a crash or restart loses nothing already logged.
 */
final class KpiEventLog implements AutoCloseable {

  private final Logger logger = LoggerFactory.getLogger(KpiEventLog.class);
  private final Gson gson = new GsonBuilder().serializeNulls().create();
  private final Path directory;
  private BufferedWriter writer;
  private LocalDate writerDate;
  private boolean failureLogged;

  KpiEventLog(Path directory) {
    this.directory = directory;
  }

  synchronized void log(Instant at, String type, Map<String, ?> fields) {
    Map<String, Object> line = new LinkedHashMap<>();
    line.put("ts", at.toString());
    line.put("type", type);
    if (fields != null) {
      line.putAll(fields);
    }
    try {
      BufferedWriter current = writerFor(LocalDate.ofInstant(at, ZoneOffset.UTC));
      current.write(gson.toJson(line));
      current.newLine();
      current.flush();
      failureLogged = false;
    } catch (IOException e) {
      if (!failureLogged) {
        failureLogged = true;
        logger.warn("KPI event log could not be written to {}", directory, e);
      }
    }
  }

  private BufferedWriter writerFor(LocalDate date) throws IOException {
    if (writer == null || !date.equals(writerDate)) {
      closeWriter();
      Files.createDirectories(directory);
      writer = Files.newBufferedWriter(directory.resolve("kpi-events-" + date + ".jsonl"), StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      writerDate = date;
    }
    return writer;
  }

  private void closeWriter() {
    if (writer != null) {
      try {
        writer.close();
      } catch (IOException e) {
        logger.warn("KPI event log could not be closed", e);
      }
      writer = null;
    }
  }

  @Override
  public synchronized void close() {
    closeWriter();
  }
}
