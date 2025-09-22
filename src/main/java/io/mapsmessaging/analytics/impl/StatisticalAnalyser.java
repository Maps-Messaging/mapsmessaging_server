package io.mapsmessaging.analytics.impl;

import io.mapsmessaging.analytics.Analyser;
import io.mapsmessaging.analytics.AnalyserListener;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import org.jetbrains.annotations.NotNull;

public class StatisticalAnalyser implements Analyser {

  private AnalyserListener analyserListener;
  private MessageFormatter formatter;

  public StatisticalAnalyser() {
    analyserListener = null;
    formatter = null;
  }

  public StatisticalAnalyser(AnalyserListener analyserListener) {
    this.analyserListener = analyserListener;
    formatter = null;
  }

  @Override
  public Analyser create(@NotNull ConfigurationProperties configuration, @NotNull AnalyserListener listener) {
    return new StatisticalAnalyser(listener);
  }

  @Override
  public void ingest(@NotNull MessageEvent event) {
    if (analyserListener != null) {
      String schemaId = event.getMessage().getSchemaId();
      formatter = SchemaManager.getInstance().getMessageFormatter(schemaId);
      if(formatter == null) {

      }
    }

  }

  @Override
  public void flush() {

  }

  @Override
  public void close() {

  }

  @Override
  public String getName() {
    return "stats";
  }

  @Override
  public String getDescription() {
    return "Statistical Event Analyser";
  }
}
