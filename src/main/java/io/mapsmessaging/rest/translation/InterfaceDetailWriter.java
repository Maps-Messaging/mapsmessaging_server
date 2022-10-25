package io.mapsmessaging.rest.translation;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Produces({
    MediaType.APPLICATION_JSON
})
public class InterfaceDetailWriter implements MessageBodyWriter<Object> {

  @Override
  public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    // Called before isWriteable by Jersey. Return -1 if you don't the size yet.
    return -1;
  }

  @Override
  public boolean isWriteable(Class<?> clazz, Type genericType, Annotation[] annotations, MediaType mediaType) {
    // Check that the passed class by Jersey can be handled by our message body writer
    return true;
  }

  @Override
  public void writeTo(Object t, Class<?> clazz, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws IOException, WebApplicationException {

    // Call your favorite JSON library to generate the JSON code and remove the unwanted fields...
    Gson gson = new Gson();
    out.write(gson.toJson(t).getBytes(StandardCharsets.UTF_8));
  }
}
