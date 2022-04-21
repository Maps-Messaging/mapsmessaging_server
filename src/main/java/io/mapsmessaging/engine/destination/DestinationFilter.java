package io.mapsmessaging.engine.destination;

public interface DestinationFilter {

  boolean matches(String name);
}
