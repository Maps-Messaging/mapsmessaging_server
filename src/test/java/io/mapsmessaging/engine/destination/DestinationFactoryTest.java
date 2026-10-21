package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.auth.DestinationAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class DestinationFactoryTest {

  @Test
  void defaultNamespaceCalculationsPreserveDestinationName() {
    DestinationFactory factory = new StubFactory();

    assertEquals("/a/b", factory.calculateNamespace("/a/b"));
    assertEquals("/a/b", factory.calculateOriginalNamespace("/a/b"));
    assertNull(factory.calculateNamespace(null));
    assertNull(factory.calculateOriginalNamespace(null));
  }

  private static final class StubFactory implements DestinationFactory {
    @Override
    public CompletableFuture<DestinationImpl> find(String destinationName) {
      return null;
    }

    @Override
    public CompletableFuture<DestinationImpl> findOrCreate(
        String destinationName,
        DestinationAuthorisationCheck authCheck) throws IOException {
      return null;
    }

    @Override
    public CompletableFuture<DestinationImpl> findOrCreate(
        String destinationName,
        DestinationType destinationType,
        DestinationAuthorisationCheck authCheck) throws IOException {
      return null;
    }

    @Override
    public CompletableFuture<DestinationImpl> create(
        String destinationName,
        DestinationType destinationType,
        DestinationAuthorisationCheck authCheck) throws IOException {
      return null;
    }

    @Override
    public CompletableFuture<DestinationImpl> delete(DestinationImpl destinationImpl) {
      return null;
    }

    @Override
    public Map<String, DestinationImpl> get(DestinationFilter filter) {
      return Map.of();
    }

    @Override
    public void addListener(DestinationManagerListener subscriptionController) {
    }

    @Override
    public boolean removeListener(DestinationManagerListener subscriptionController) {
      return false;
    }
  }
}