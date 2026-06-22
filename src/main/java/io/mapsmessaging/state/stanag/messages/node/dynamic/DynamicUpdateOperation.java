package io.mapsmessaging.state.stanag.messages.node.dynamic;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DynamicUpdateOperation {

  @SerializedName("$discriminator")
  private final String discriminator;

  @SerializedName("put_value")
  private final PutValue putValue;
}