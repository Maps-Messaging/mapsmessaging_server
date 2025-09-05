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

package io.mapsmessaging.rest.translation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GsonMessageBodyWriter implements MessageBodyWriter<Object> {

  private final Gson gson = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeSerialiser())
      .registerTypeAdapter(LocalDateTime.class, new GsonDateTimeDeserialiser())
      .registerTypeAdapter(LocalDate.class, new GsonDateTimeSerialiser())
      .registerTypeAdapter(LocalDate.class, new GsonDateTimeDeserialiser())
      .create();

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType) {
    return mediaType.equals(MediaType.APPLICATION_JSON_TYPE) || MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
  }

  @Override
  public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType,
                      jakarta.ws.rs.core.MultivaluedMap<String, Object> httpHeaders, java.io.OutputStream entityStream) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
    gson.toJson(object, writer);
    writer.flush();
  }
}
