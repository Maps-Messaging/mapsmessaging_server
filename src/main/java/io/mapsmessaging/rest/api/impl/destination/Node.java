package io.mapsmessaging.rest.api.impl.destination;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Node<T> {
  volatile T value;
  final Map<String, Node<T>> children = new ConcurrentHashMap<>();
}

