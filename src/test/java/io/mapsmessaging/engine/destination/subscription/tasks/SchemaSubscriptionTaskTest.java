package io.mapsmessaging.engine.destination.subscription.tasks;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscription;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.schema.Schema;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.config.SchemaConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchemaSubscriptionTaskTest {

  @Test
  void existingSchemaSubscriptionReceivesContextAndSchemaMessage() throws Exception {
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    Schema schema = mock(Schema.class);
    SchemaConfig config = mock(SchemaConfig.class);
    Subscription subscription = mock(Subscription.class);
    SchemaManager manager = mock(SchemaManager.class);
    AtomicLong counter = new AtomicLong(1);

    when(destination.getSchema()).thenReturn(schema);
    when(schema.getUniqueId()).thenReturn("schema-1");
    when(manager.getSchema("schema-1")).thenReturn(config);
    when(config.packAsBytes()).thenReturn(new byte[]{1, 2, 3});
    when(controller.getSchema(destination)).thenReturn(subscription);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(manager);

      assertNotNull(
          new SchemaSubscriptionTask(controller, context, destination, counter)
              .taskCall()
      );
    }

    verify(subscription).addContext(context);
    verify(subscription).sendMessage(any());
    verify(controller, never()).createSchemaSubscription(any(), any());
    assertEquals(0, counter.get());
  }

  @Test
  void missingExistingSubscriptionCreatesSchemaSubscription() throws Exception {
    SubscriptionController controller = mock(SubscriptionController.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    Schema schema = mock(Schema.class);
    SchemaConfig config = mock(SchemaConfig.class);
    Subscription subscription = mock(Subscription.class);
    SchemaManager manager = mock(SchemaManager.class);
    AtomicLong counter = new AtomicLong(1);

    when(destination.getSchema()).thenReturn(schema);
    when(schema.getUniqueId()).thenReturn("schema-2");
    when(manager.getSchema("schema-2")).thenReturn(config);
    when(config.packAsBytes()).thenReturn(new byte[]{9});
    when(controller.getSchema(destination)).thenReturn(null);
    when(controller.createSchemaSubscription(context, destination))
        .thenReturn(subscription);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(manager);

      new SchemaSubscriptionTask(controller, context, destination, counter)
          .taskCall();
    }

    verify(controller).createSchemaSubscription(context, destination);
    verify(subscription).sendMessage(any());
    assertEquals(0, counter.get());
  }
}
