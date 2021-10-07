package io.mapsmessaging.engine.serializer;

import io.mapsmessaging.storage.impl.streams.ObjectWriter;
import java.io.IOException;

public interface MapSerializable {

  void write(ObjectWriter outputStream) throws IOException;

}
