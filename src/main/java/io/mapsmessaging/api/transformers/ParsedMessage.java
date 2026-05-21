package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedMessage{
  private String destinationName;
  private Message message;
}