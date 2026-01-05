/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.api.transformers;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;


import java.util.UUID;

public final class TestProtobufSchemas {

  private TestProtobufSchemas() {
  }

  public static SchemaConfig protobufSchemaConfig(UUID schemaId) {
    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    config.setUniqueId(schemaId);

    ProtoBufSchemaConfig.ProtobufConfig pbConfig = new ProtoBufSchemaConfig.ProtobufConfig();
    pbConfig.setDescriptorValue(buildFileDescriptorSetBytes());
    pbConfig.setMessageName("TestEvent");
    config.setProtobufConfig(pbConfig);
    return config;
  }

  public static byte[] encodeProtobufPayload() {
    try {
      Descriptors.Descriptor descriptor = buildMessageDescriptor();

      DynamicMessage message =
          DynamicMessage.newBuilder(descriptor)
              .setField(descriptor.findFieldByName("id"), "abc")
              .setField(descriptor.findFieldByName("count"), 123)
              .setField(descriptor.findFieldByName("active"), true)
              .build();

      return message.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static byte[] buildFileDescriptorSetBytes() {
    DescriptorProtos.FileDescriptorSet set =
        DescriptorProtos.FileDescriptorSet.newBuilder()
            .addFile(buildFileDescriptorProto())
            .build();
    return set.toByteArray();
  }

  private static Descriptors.Descriptor buildMessageDescriptor() throws Descriptors.DescriptorValidationException {
    DescriptorProtos.FileDescriptorProto fileProto = buildFileDescriptorProto();
    Descriptors.FileDescriptor file =
        Descriptors.FileDescriptor.buildFrom(fileProto, new Descriptors.FileDescriptor[0]);
    return file.findMessageTypeByName("TestEvent");
  }

  private static DescriptorProtos.FileDescriptorProto buildFileDescriptorProto() {
    DescriptorProtos.DescriptorProto msg =
        DescriptorProtos.DescriptorProto.newBuilder()
            .setName("TestEvent")
            .addField(field("id", 1, FieldDescriptorProto.Type.TYPE_STRING))
            .addField(field("count", 2, FieldDescriptorProto.Type.TYPE_INT32))
            .addField(field("active", 3, FieldDescriptorProto.Type.TYPE_BOOL))
            .build();

    return DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("TestEvent")
        .setSyntax("proto3")
        .setPackage("io_mapsmessaging_test")
        .addMessageType(msg)
        .build();
  }

  private static FieldDescriptorProto field(String name, int number, FieldDescriptorProto.Type type) {
    return FieldDescriptorProto.newBuilder()
        .setName(name)
        .setNumber(number)
        .setType(type)
        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
        .build();
  }
}
