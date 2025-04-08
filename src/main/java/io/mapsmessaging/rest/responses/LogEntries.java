package io.mapsmessaging.rest.responses;

import io.mapsmessaging.logging.LogEntry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntries {
  private List<LogEntry> logEntries;
}
