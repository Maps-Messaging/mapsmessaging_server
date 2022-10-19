package io.mapsmessaging.rest.transformation;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeTransformation implements JsonSerializer<LocalDateTime> {
  public JsonElement serialize(LocalDateTime date, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE)); // "yyyy-mm-dd"
  }
}