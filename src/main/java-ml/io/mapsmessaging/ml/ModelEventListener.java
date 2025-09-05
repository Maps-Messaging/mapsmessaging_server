package io.mapsmessaging.ml;

public interface ModelEventListener {

  void modelCreated(String modelId);

  void modelLoaded(String modelId);

  void modelDeleted(String modelId);

}
