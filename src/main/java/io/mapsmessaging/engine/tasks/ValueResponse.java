package io.mapsmessaging.engine.tasks;

public class ValueResponse<T> implements Response {
  private final T response;

  public ValueResponse(T response) {
    this.response = response;
  }

  public T getResponse() {
    return response;
  }
}
