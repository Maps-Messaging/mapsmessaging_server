package io.mapsmessaging.engine.session.persistence;


import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.MessageFactory;
import io.mapsmessaging.engine.session.will.WillDetails;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class WillData{

  @Getter
  @Setter
  private long delay;

  @Getter
  @Setter
  private String destination;

  @Getter
  @Setter
  private String msg;

  public WillData(){}

  public WillData(WillDetails details) throws IOException {
    delay = details.getDelay();
    destination = details.getDestination();
    Message message = details.getMsg();
    if(message != null){
      msg = packMessage(message);
    }
  }

  private String packMessage(Message message) throws IOException {
    ByteBuffer[] packed = MessageFactory.getInstance().pack(message);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    for(ByteBuffer buffer:packed){
      byte[] buf = buffer.array();
      writeInt(byteArrayOutputStream, buf.length);
      byteArrayOutputStream.write(buf);
    }
    return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
  }

  public Message getMessage() throws IOException {
    byte[] raw = Base64.getDecoder().decode(msg);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(raw);
    List<ByteBuffer> buffers = new ArrayList<>();
    while(byteArrayInputStream.available() > 0){
      long size = readInt(byteArrayInputStream);
      byte[] tmp = new byte[(int)size];
      byteArrayInputStream.read(tmp);
      buffers.add(ByteBuffer.wrap(tmp));
    }
    ByteBuffer[] array = new ByteBuffer[buffers.size()];
    buffers.toArray(array);
    return MessageFactory.getInstance().unpack(array);
  }

  public long readInt(InputStream is) throws IOException {
    long tmp = (is.read() & 0xff);
    tmp = tmp << 8;
    tmp = tmp + (is.read() & 0xff);
    tmp = tmp << 8;
    tmp = tmp + (is.read() & 0xff);
    tmp = tmp << 8;
    tmp = tmp + (is.read() & 0xff);
    return tmp;
  }

  public void writeInt(OutputStream os, long value) throws IOException {
    os.write((byte) ((value >> 24) & 0xff));
    os.write((byte) ((value >> 16) & 0xff));
    os.write((byte) ((value >> 8) & 0xff));
    os.write((byte) ((value) & 0xff));
  }

}