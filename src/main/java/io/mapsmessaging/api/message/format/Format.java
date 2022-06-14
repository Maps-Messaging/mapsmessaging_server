package io.mapsmessaging.api.message.format;

import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.service.Service;
import java.io.IOException;

public interface Format extends Service {

  byte[] toByteArray(Object obj) throws IOException;

  Object fromByteArray(byte[] payload) throws IOException;

  IdentifierResolver getResolver(byte[] payload) throws IOException;
}
