package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantAdapter extends TypeAdapter<Instant> {
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnX")
          .withZone(java.time.ZoneOffset.UTC);

  @Override
  public void write(JsonWriter out, Instant value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(formatter.format(value));
    }
  }

  @Override
  public Instant read(JsonReader in) throws IOException {
    return Instant.parse(in.nextString());
  }
}
