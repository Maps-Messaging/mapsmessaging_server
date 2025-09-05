package io.mapsmessaging.rest.api.impl.destination;

import java.util.*;

public class FqnTree<T> {
  private final Node<T> root = new Node<>();
  private final FqnListenerRegistry<T> listenerRegistry = new FqnListenerRegistry<>();

  public void add(String fqn, T value) {
    String[] parts = normalize(fqn);
    Node<T> current = root;
    for (String part : parts) {
      current = current.children.computeIfAbsent(part, k -> new Node<>());
    }
    T previous = current.value.getAndSet(value);
    boolean isNew = (previous == null);
    listenerRegistry.notifyIfRegistered(fqn, value, isNew ? ChangeType.ADD : ChangeType.UPDATE);
  }

  public boolean remove(String fqn) {
    String[] parts = normalize(fqn);
    if (parts.length == 0) return false; // don't remove root

    Deque<Node<T>> stack = new ArrayDeque<>();
    Node<T> current = root;
    stack.push(current);
    for (String part : parts) {
      current = current.children.get(part);
      if (current == null) return false;
      stack.push(current);
    }

    T previous = current.value.getAndSet(null);
    boolean changed = (previous != null) || !current.children.isEmpty();

    for (int i = parts.length; i > 0; i--) {
      Node<T> child = stack.pop();
      Node<T> parent = stack.peek();
      if (parent == null) break; // safety: root popped (shouldn't happen)
      String name = parts[i - 1];
      if (child.value.get() == null && child.children.isEmpty()) {
        parent.children.remove(name, child);
      } else {
        break;
      }
    }

    if (changed) {
      listenerRegistry.notifyIfRegistered(fqn, null, ChangeType.REMOVE);
    }
    return changed;
  }


  public T getValue(String fqn) {
    Node<T> node = getNode(fqn);
    return node != null ? node.value.get() : null;
  }

  public Map<String, T> getChildren(String fqn) {
    Node<T> node = getNode(fqn);
    if (node == null) return Collections.emptyMap();
    Map<String, T> result = new HashMap<>();
    for (Map.Entry<String, Node<T>> entry : node.children.entrySet()) {
      result.put(entry.getKey(), entry.getValue().value.get());
    }
    return result;
  }

  public List<String> match(String pattern) {
    List<String> results = new ArrayList<>();
    matchRecursive(root, normalize(pattern), 0, "", results);
    return results;
  }

  public void subscribe(String fqn, FqnListener<T> listener) {
    listenerRegistry.subscribe(clean(fqn), listener);
  }

  public void unsubscribe(String fqn, FqnListener<T> listener) {
    listenerRegistry.unsubscribe(clean(fqn), listener);
  }

  private Node<T> getNode(String fqn) {
    String[] parts = normalize(fqn);
    Node<T> current = root;
    for (String part : parts) {
      current = current.children.get(part);
      if (current == null) return null;
    }
    return current;
  }

  private void matchRecursive(Node<T> node, String[] pattern, int index, String path, List<String> results) {
    if (index == pattern.length) {
      results.add(path.isEmpty() ? "/" : path);
      return;
    }
    String part = pattern[index];
    if (part.equals("#")) {
      collectAllPaths(node, path, results);
    } else if (part.equals("+")) {
      for (Map.Entry<String, Node<T>> entry : node.children.entrySet()) {
        matchRecursive(entry.getValue(), pattern, index + 1, path + "/" + entry.getKey(), results);
      }
    } else {
      Node<T> child = node.children.get(part);
      if (child != null) {
        matchRecursive(child, pattern, index + 1, path + "/" + part, results);
      }
    }
  }

  private void collectAllPaths(Node<T> node, String path, List<String> results) {
    results.add(path.isEmpty() ? "/" : path);
    for (Map.Entry<String, Node<T>> entry : node.children.entrySet()) {
      collectAllPaths(entry.getValue(), path + "/" + entry.getKey(), results);
    }
  }

  private String[] normalize(String fqn) {
    return Arrays.stream(fqn.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
  }

  private String clean(String fqn) {
    return "/" + String.join("/", normalize(fqn));
  }
}
