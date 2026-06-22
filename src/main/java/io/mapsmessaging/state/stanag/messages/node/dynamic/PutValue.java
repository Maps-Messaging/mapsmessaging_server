package io.mapsmessaging.state.stanag.messages.node.dynamic;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PutValue {

  @SerializedName("$discriminator")
  private final String discriminator;

  private final Track track;
}