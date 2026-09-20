package io.mapsmessaging.api;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.session.security.SecurityContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaTest {

  @Test
  void invalidSchemaPayloadIsRejectedWithoutUpdatingDestination() {
    DestinationImpl destination = destination();
    Schema schema = new Schema(destination, mock(SecurityContext.class));
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(new byte[]{1, 2, 3});

    assertThrows(IOException.class, () -> schema.storeMessage(message));
    verify(destination, never()).updateSchema(any(), any());
  }

  @Test
  void schemaDestinationAdvertisesSchemaNamespaceAndSingleStoredValue() throws Exception {
    DestinationImpl destination = destination();
    Schema schema = new Schema(destination, mock(SecurityContext.class));

    assertEquals(1L, schema.getStoredMessages());
    assertEquals(
        DestinationMode.SCHEMA.getNamespace() + "/target",
        schema.getFullyQualifiedNamespace()
    );
  }

  private static DestinationImpl destination() {
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getResourceType()).thenReturn(DestinationType.TOPIC);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/target");
    return destination;
  }
}
