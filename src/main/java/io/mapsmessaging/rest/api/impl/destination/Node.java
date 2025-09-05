package io.mapsmessaging.rest.api.impl.destination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class Node<T> {
  final AtomicReference<T> value = new AtomicReference<>();
  final Map<String, Node<T>> children = new ConcurrentHashMap<>();
}