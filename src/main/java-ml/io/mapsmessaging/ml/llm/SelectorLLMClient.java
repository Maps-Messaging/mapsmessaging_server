package io.mapsmessaging.ml.llm;

interface SelectorLLMClient {
  String generateSelector(String schemaJson, String contextHint);
}

