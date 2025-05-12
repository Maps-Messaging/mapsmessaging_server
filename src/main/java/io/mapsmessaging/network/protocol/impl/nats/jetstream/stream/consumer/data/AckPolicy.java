package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

import com.google.gson.annotations.SerializedName;

public enum AckPolicy {
  @SerializedName("none")
  NONE,

  @SerializedName("all")
  ALL,

  @SerializedName("explicit")
  EXPLICIT
}
