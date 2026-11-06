package io.mapsmessaging.network.auth;

import io.mapsmessaging.utilities.service.Service;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenGeneratorManagerTest {

  @Test
  void singletonIdentityIsStable() {
    assertSame(
        TokenGeneratorManager.getInstance(),
        TokenGeneratorManager.getInstance()
    );
  }

  @Test
  void serviceLoaderRegistersAuth0GeneratorByName() {
    TokenGeneratorManager manager = TokenGeneratorManager.getInstance();

    TokenGenerator generator = manager.get("auth0");

    assertNotNull(generator);
    assertInstanceOf(Auth0TokenGenerator.class, generator);
    assertEquals("auth0", generator.getName());
    assertNull(manager.get("missing"));
  }

  @Test
  void servicesIteratorIncludesRegisteredTokenGenerators() {
    Iterator<Service> iterator = TokenGeneratorManager.getInstance().getServices();
    List<String> names = new ArrayList<>();
    iterator.forEachRemaining(service -> names.add(service.getName()));

    assertTrue(names.contains("auth0"));
  }
}
