package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

import com.google.gson.annotations.SerializedName;

public enum DeliverPolicy {
  @SerializedName("all")
  ALL,
  @SerializedName("last")
  LAST,
  @SerializedName("new")
  NEW,
  @SerializedName("by_start_sequence")
  BY_START_SEQUENCE,
  @SerializedName("by_start_time")
  BY_START_TIME
}