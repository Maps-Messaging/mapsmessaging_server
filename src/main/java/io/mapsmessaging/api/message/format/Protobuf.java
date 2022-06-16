package io.mapsmessaging.api.message.format;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Protobuf implements Format{

  private final FileDescriptor descriptor;
  private final ConfigurationProperties properties;

  public Protobuf(){
    descriptor = null;
    properties = new ConfigurationProperties();
  }

  private Protobuf(ConfigurationProperties props) throws IOException{
    properties = props;
    try {
      descriptor = loadDescFile(props.getProperty("schema"));
    } catch (IOException|DescriptorValidationException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Format getInstance(ConfigurationProperties properties)  {
    try {
      return new Protobuf(properties);
    } catch (IOException e) {
      // ToDo. log the fact we are unable to load the descriptor file
      return new RawFormat();
    }
  }

  private Object fromByteArray(byte[] payload) throws IOException {
    return buildMessage(properties.getProperty("messageName"), descriptor, payload);
  }

  @Override
  public boolean isValid(byte[] payload) {
    try {
      return fromByteArray(payload) != null;
    } catch (IOException e) {
      // todo, log this
    }
    return false;
  }

  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return null;
  }

  @Override
  public String getName() {
    return "protobuf";
  }

  @Override
  public String getDescription() {
    return "Processes Protobuf formatted payloads";
  }

  private FileDescriptor loadDescFile(String filename) throws IOException, DescriptorValidationException {
    FileInputStream fin = new FileInputStream(filename);
    DescriptorProtos.FileDescriptorSet set;
    List<FileDescriptor> dependencyFileDescriptorList;
    try {
      set = DescriptorProtos.FileDescriptorSet.parseFrom(fin);
      dependencyFileDescriptorList = new ArrayList<>();
      for(int i=0; i<set.getFileCount()-1;i++) {
        dependencyFileDescriptorList.add(FileDescriptor.buildFrom(set.getFile(i), dependencyFileDescriptorList.toArray(new FileDescriptor[i])));
      }
    } finally {
      fin.close();
    }
    return Descriptors.FileDescriptor.buildFrom(set.getFile(set.getFileCount()-1), dependencyFileDescriptorList.toArray(new FileDescriptor[0]));
  }

  private DynamicMessage buildMessage(String msgName, FileDescriptor descriptor, byte[] packet) throws InvalidProtocolBufferException {
    return DynamicMessage.parseFrom(descriptor.findMessageTypeByName(msgName), packet);
  }
}
