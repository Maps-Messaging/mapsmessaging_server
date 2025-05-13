package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;


import com.google.gson.annotations.SerializedName;

public enum ReplayPolicy {
  @SerializedName("instant")
  INSTANT,

  @SerializedName("original")
  ORIGINAL
}
