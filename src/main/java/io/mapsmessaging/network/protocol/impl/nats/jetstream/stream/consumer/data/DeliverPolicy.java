package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

public enum DeliverPolicy {
  ALL, LAST, NEW, BY_START_SEQUENCE, BY_START_TIME
}