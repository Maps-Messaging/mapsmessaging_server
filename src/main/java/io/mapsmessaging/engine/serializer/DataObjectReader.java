package io.mapsmessaging.engine.serializer;

import io.mapsmessaging.storage.impl.streams.ObjectReader;
import java.io.IOException;
import org.mapdb.DataInput2;

public class DataObjectReader extends ObjectReader {
    private final DataInput2 inputStream;

    public DataObjectReader(DataInput2 inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public byte readByte() throws IOException {
      return (byte) (0xff & inputStream.readByte());
    }

    @Override
    public char readChar() throws IOException {
      return (char) readShort();
    }

    @Override

    protected long read(int size) throws IOException {
      return fromByteArray(readFromStream(size));
    }

    protected byte[] readFromStream(int length) throws IOException {
      byte[] result = null;
      if (length > -1) {
        result = new byte[length];
        inputStream.readFully(result, 0, length);
      }
      return result;
    }
}
