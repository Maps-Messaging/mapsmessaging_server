package io.mapsmessaging.hardware.device;

import com.google.gson.JsonObject;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.devices.DeviceType;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.schemas.config.SchemaConfig;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.devices.DeviceController;
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceSessionManagementTest {

  @Test
  void runPublishesDevicePayloadWithDeviceMetadata() throws Exception {
    DeviceController controller = mock(DeviceController.class);
    DeviceHandler device = mock(DeviceHandler.class);
    BusHandler bus = mock(BusHandler.class);
    Destination destination = mock(Destination.class);
    Session session = mock(Session.class);
    UUID schema = UUID.randomUUID();

    when(device.getSchemaId()).thenReturn(schema);
    when(device.getController()).thenReturn(controller);
    when(device.getName()).thenReturn("temperature");
    when(device.getBusName()).thenReturn("i2c");
    when(device.getBusNumber()).thenReturn(1);
    when(device.getVersion()).thenReturn("1.0");
    when(device.getData()).thenReturn("{\"value\":21}".getBytes());
    when(session.getName()).thenReturn("device-session");

    DeviceSessionManagement management =
        new DeviceSessionManagement(device, "/device/[device_name]", DataFilter.ALWAYS_SEND, bus, null);
    management.setSession(session);
    management.setDestination(destination);

    management.run();

    ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
    verify(destination).storeMessage(sent.capture());
    assertEquals("i2c", sent.getValue().getMeta().get("busName"));
    assertEquals("1", sent.getValue().getMeta().get("busNumber"));
    assertEquals("temperature", sent.getValue().getMeta().get("device"));
    assertArrayEquals("{\"value\":21}".getBytes(), sent.getValue().getOpaqueData());
    verify(controller).setRaiseExceptionOnError(true);
  }

  @Test
  void sendMessageAlwaysCompletesEvenWhenDeviceUpdateFails() throws Exception {
    DeviceController controller = mock(DeviceController.class);
    DeviceHandler device = mock(DeviceHandler.class);
    when(device.getSchemaId()).thenReturn(UUID.randomUUID());
    when(device.getController()).thenReturn(controller);
    when(device.updateConfig(any())).thenThrow(new java.io.IOException("bad config"));

    DeviceSessionManagement management =
        new DeviceSessionManagement(
            device, "/device/[device_name]", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);

    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(new byte[]{1});

    assertDoesNotThrow(() -> management.sendMessage(event));
    verify(completion).run();
  }

  @Test
  void startRegistersSchemaForNewDeviceDestination() throws Exception {
    SchemaConfig schema = schema("shared-source");
    Destination destination = mock(Destination.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    DeviceSessionManagement management = configuredSession(schema, destination);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(schemaManager);
      management.start();
    }

    verify(destination).updateSchema(eq(schema), any(Message.class));
  }

  @Test
  void startBindsRegisteredSchemaToAnotherDeviceDestination() throws Exception {
    SchemaConfig schema = schema("shared-source");
    Destination destination = mock(Destination.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    when(schemaManager.getSchema(schema.getUniqueId())).thenReturn(schema);
    DeviceSessionManagement management = configuredSession(schema, destination);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(schemaManager);
      management.start();
    }

    verify(destination).updateSchema(eq(schema), any(Message.class));
  }

  @Test
  void startSkipsAlreadyMatchingDestinationAndDefinition() throws Exception {
    SchemaConfig schema = schema("shared-source");
    Destination destination = mock(Destination.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    when(schemaManager.getSchema(schema.getUniqueId())).thenReturn(schema);
    when(destination.getSchema()).thenReturn(new io.mapsmessaging.engine.schema.Schema(schema));
    DeviceSessionManagement management = configuredSession(schema, destination);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(schemaManager);
      management.start();
    }

    verify(destination, never()).updateSchema(any(), any());
  }

  @Test
  void startUpdatesChangedSchemaWithSameUuidAndSource() throws Exception {
    SchemaConfig schema = schema("shared-source");
    SchemaConfig previous = schema("shared-source");
    previous.setUniqueId(schema.getUniqueId());
    JsonObject oldDefinition = new JsonObject();
    oldDefinition.addProperty("previous", true);
    previous.setSchema(oldDefinition);

    Destination destination = mock(Destination.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    when(schemaManager.getSchema(schema.getUniqueId())).thenReturn(previous);
    when(destination.getSchema()).thenReturn(new io.mapsmessaging.engine.schema.Schema(schema));
    DeviceSessionManagement management = configuredSession(schema, destination);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(schemaManager);
      management.start();
    }

    verify(destination).updateSchema(eq(schema), any(Message.class));
  }

  @Test
  void startUsesDeviceSchemaIdWhenDefinitionHasNoUuid() throws Exception {
    SchemaConfig schema = schema("sensor");
    schema.setLabels(null);
    Destination destination = mock(Destination.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    UUID deviceUuid = UUID.randomUUID();
    DeviceSessionManagement management = configuredSession(schema, destination, deviceUuid);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(schemaManager);
      management.start();
    }

    assertEquals(deviceUuid.toString(), schema.getUniqueId());
    verify(destination).updateSchema(eq(schema), any(Message.class));
  }

  @Test
  void startDoesNotActivateDeviceWhenSchemaRegistrationFails() throws Exception {
    SchemaConfig schema = schema("sensor");
    Destination destination = mock(Destination.class);
    DeviceHandler device = mock(DeviceHandler.class);
    BusHandler bus = mock(BusHandler.class);
    Session session = mock(Session.class);
    Trigger trigger = mock(Trigger.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    SessionManager sessionManager = mock(SessionManager.class);
    when(device.getSchemaId()).thenReturn(UUID.fromString(schema.getUniqueId()));
    when(device.getController()).thenReturn(mock(DeviceController.class));
    when(device.getSchema()).thenReturn(schema);
    when(device.getTopicName("/device/data")).thenReturn("/device/data");
    when(device.getTrigger()).thenReturn(trigger);
    when(device.getType()).thenReturn(DeviceType.SENSOR);
    when(session.findDestination("/device/data", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(destination));
    doThrow(new IOException("cannot register")).when(destination).updateSchema(eq(schema), any(Message.class));
    DeviceSessionManagement management = new DeviceSessionManagement(device, "/device", DataFilter.ALWAYS_SEND, bus, null);
    management.setSession(session);

    try (MockedStatic<SchemaManager> mocked = mockStatic(SchemaManager.class);
         MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class)) {
      mocked.when(SchemaManager::getInstance).thenReturn(schemaManager);
      sessions.when(SessionManager::getInstance).thenReturn(sessionManager);
      assertThrows(ExecutionException.class, management::start);
    }

    verify(bus).closedSession(management);
    verify(sessionManager).close(session, true);
    verify(trigger, never()).addTask(management);
  }

  @Test
  void startPreservesDevicesWithoutSchema() throws Exception {
    Destination destination = mock(Destination.class);
    DeviceSessionManagement management = configuredSession(null, destination);

    management.start();

    verify(destination, never()).updateSchema(any(), any());
    verify(management.getDevice().getTrigger()).addTask(management);
  }

  @Test
  void configIsRetainedAndOnlyRequestTopicIsSubscribed() throws Exception {
    DeviceHandler device = mock(DeviceHandler.class);
    DeviceController controller = mock(DeviceController.class);
    Session session = mock(Session.class);
    Destination data = mock(Destination.class);
    Destination config = mock(Destination.class);
    Destination request = mock(Destination.class);
    Destination response = mock(Destination.class);
    Trigger trigger = mock(Trigger.class);
    when(device.getSchemaId()).thenReturn(UUID.randomUUID());
    when(device.getController()).thenReturn(controller);
    when(device.getName()).thenReturn("SEN6x");
    when(device.getBusName()).thenReturn("i2c");
    when(device.getVersion()).thenReturn("1.0");
    when(device.getType()).thenReturn(DeviceType.SENSOR);
    when(device.getTrigger()).thenReturn(trigger);
    when(device.getSchema()).thenReturn(null);
    when(device.enableConfig()).thenReturn(true);
    when(device.getConfiguration()).thenReturn("{\"commands\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    when(device.getTopicName("/device/data")).thenReturn("/device/data");
    when(device.getTopicName("/device/config")).thenReturn("/device/config");
    when(device.getTopicName("/device/request")).thenReturn("/device/request");
    when(device.getTopicName("/device/response")).thenReturn("/device/response");
    when(request.getFullyQualifiedNamespace()).thenReturn("/device/request");
    when(session.getName()).thenReturn("device-session");
    when(session.findDestination("/device/data", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(data));
    when(session.findDestination("/device/config", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(config));
    when(session.findDestination("/device/request", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(request));
    when(session.findDestination("/device/response", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(response));

    DeviceSessionManagement management =
        new DeviceSessionManagement(device, "/device", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);
    management.setSession(session);
    management.start();

    ArgumentCaptor<Message> retained = ArgumentCaptor.forClass(Message.class);
    verify(config).storeMessage(retained.capture());
    assertTrue(retained.getValue().isRetain());
    assertArrayEquals("{\"commands\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
        retained.getValue().getOpaqueData());
    verify(session).addSubscription(any());
    verify(config, never()).getFullyQualifiedNamespace();
    verify(response, never()).storeMessage(any());
  }

  @Test
  void requestRepliesOnResponseTopicWithoutReplacingRetainedConfig() throws Exception {
    DeviceHandler device = mock(DeviceHandler.class);
    DeviceController controller = mock(DeviceController.class);
    Destination config = mock(Destination.class);
    Destination response = mock(Destination.class);
    Session session = mock(Session.class);
    when(device.getSchemaId()).thenReturn(UUID.randomUUID());
    when(device.getController()).thenReturn(controller);
    when(device.getVersion()).thenReturn("1.0");
    when(device.getName()).thenReturn("SEN6x");
    when(device.getBusName()).thenReturn("i2c");
    when(session.getName()).thenReturn("device-session");
    byte[] payload = "{\"command\":\"RESET\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    when(device.updateConfig(payload)).thenReturn("{\"status\":\"success\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    DeviceSessionManagement management =
        new DeviceSessionManagement(device, "/device", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);
    management.setSession(session);
    management.setConfig(config);
    management.setResponse(response);
    Message message = mock(Message.class);
    when(message.getOpaqueData()).thenReturn(payload);
    when(message.getCorrelationData()).thenReturn(new byte[]{7, 8, 9});
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);

    management.sendMessage(event);

    ArgumentCaptor<Message> reply = ArgumentCaptor.forClass(Message.class);
    verify(response, timeout(1000)).storeMessage(reply.capture());
    assertFalse(reply.getValue().isRetain());
    assertArrayEquals(new byte[]{7, 8, 9}, reply.getValue().getCorrelationData());
    verify(config, never()).storeMessage(any());
    verify(completion).run();
  }

  @Test
  void rejectedRetainedRequestProducesNonRetainedErrorWithoutExecuting() throws Exception {
    DeviceHandler device = mock(DeviceHandler.class);
    when(device.getSchemaId()).thenReturn(UUID.randomUUID());
    when(device.getController()).thenReturn(mock(DeviceController.class));
    DeviceSessionManagement management =
        new DeviceSessionManagement(device, "/device", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);
    management.setSession(mock(Session.class));
    Destination response = mock(Destination.class);
    management.setResponse(response);
    Message incoming = mock(Message.class);
    when(incoming.isRetain()).thenReturn(true);
    when(incoming.getCorrelationData()).thenReturn(new byte[]{1});
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(incoming);
    when(event.getCompletionTask()).thenReturn(completion);

    management.sendMessage(event);

    verify(device, never()).updateConfig(any());
    ArgumentCaptor<Message> captured = ArgumentCaptor.forClass(Message.class);
    verify(response, timeout(1000)).storeMessage(captured.capture());
    assertFalse(captured.getValue().isRetain());
    assertArrayEquals(new byte[]{1}, captured.getValue().getCorrelationData());
    assertTrue(new String(captured.getValue().getOpaqueData()).contains("Retained device requests"));
    verify(completion).run();
  }

  @Test
  void failedRequestProducesNonRetainedCorrelatedError() throws Exception {
    DeviceHandler device = mock(DeviceHandler.class);
    when(device.getSchemaId()).thenReturn(UUID.randomUUID());
    when(device.getController()).thenReturn(mock(DeviceController.class));
    when(device.updateConfig(any())).thenThrow(new IOException("bad command"));
    DeviceSessionManagement management =
        new DeviceSessionManagement(device, "/device", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);
    management.setSession(mock(Session.class));
    Destination response = mock(Destination.class);
    management.setResponse(response);
    Message incoming = mock(Message.class);
    when(incoming.getOpaqueData()).thenReturn(new byte[]{1});
    when(incoming.getCorrelationData()).thenReturn(new byte[]{2});
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(incoming);
    when(event.getCompletionTask()).thenReturn(completion);

    management.sendMessage(event);

    ArgumentCaptor<Message> captured = ArgumentCaptor.forClass(Message.class);
    verify(response, timeout(1000)).storeMessage(captured.capture());
    assertFalse(captured.getValue().isRetain());
    assertArrayEquals(new byte[]{2}, captured.getValue().getCorrelationData());
    assertTrue(new String(captured.getValue().getOpaqueData()).contains("bad command"));
    verify(completion).run();
  }

  private static SchemaConfig schema(String source) {
    SchemaConfig config = new SchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setSource(source);
    config.setSchema(new JsonObject());
    return config;
  }

  private static DeviceSessionManagement configuredSession(SchemaConfig schema, Destination destination) {
    return configuredSession(schema, destination, schema == null ? UUID.randomUUID() : UUID.fromString(schema.getUniqueId()));
  }

  private static DeviceSessionManagement configuredSession(SchemaConfig schema, Destination destination, UUID deviceUuid) {
    DeviceHandler device = mock(DeviceHandler.class);
    Session session = mock(Session.class);
    when(device.getSchemaId()).thenReturn(deviceUuid);
    when(device.getController()).thenReturn(mock(DeviceController.class));
    when(device.getSchema()).thenReturn(schema);
    when(device.getTopicName("/device/data")).thenReturn("/device/data");
    when(device.getTrigger()).thenReturn(mock(Trigger.class));
    when(device.getType()).thenReturn(DeviceType.SENSOR);
    when(session.findDestination("/device/data", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(destination));
    DeviceSessionManagement management = new DeviceSessionManagement(
        device, "/device", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);
    management.setSession(session);
    return management;
  }
}
