package io.mapsmessaging.rest.api.impl.destination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Node<T> {
  volatile T value;
  final Map<String, Node<T>> children = new ConcurrentHashMap<>();
}

