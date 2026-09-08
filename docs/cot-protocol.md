# Cursor on Target protocol

The `cot` protocol reads and writes complete Cursor on Target XML events over an existing stream endpoint. The endpoint owns TCP or TLS; the protocol has no transport-specific security configuration.

## Listener endpoint

Listener-mode endpoints use `inboundTopicName` and `outboundTopicName` directly:

```yaml
- name: tak-listener
  url: ssl://0.0.0.0:8089
  protocol: cot
  inboundTopicName: /tak/cot/inbound
  outboundTopicName: /tak/cot/outbound
  maximumEventSize: 1048576
  qualityOfService: 1
  storeOffline: false
  appendNewLine: true
```

TLS keys, trust and client-certificate policy remain part of the existing SSL endpoint configuration.

## NetworkConnectionManager client

A client connection binds CoT through the standard `push` and `pull` links:

```yaml
NetworkConnectionManager:
  data:
    - name: remote-tak
      url: ssl://tak.example:8089
      protocol: cot
      maximumEventSize: 1048576
      appendNewLine: true

      links:
        - direction: push
          local_namespace: /tak/cot/outbound
          remote_namespace: /cot
          qos: 1

        - direction: pull
          remote_namespace: /cot
          local_namespace: /tak/cot/inbound
          qos: 1
```

For `push`, `local_namespace` is the broker subscription that supplies XML events for the socket. For `pull`, `local_namespace` is where received XML events are published.

CoT/TAK streams do not carry broker topic names and have no protocol-level subscribe request. Consequently, `remote_namespace` is a logical mapping key required by the common link configuration; it is not transmitted to the TAK server. One incoming event is published to every configured pull binding.

Use `tcp://` for an unencrypted connection or `ssl://` for TLS. The CoT protocol code is identical for both.

## Payload behaviour

- Inbound stream data is buffered until complete CoT XML events are available.
- Each complete event is published as `text/xml`.
- Fragmented and adjacent events are supported by the CoT stream decoder.
- Outbound payloads are passed through configured link transformations, encoded as CoT XML and optionally terminated by a newline.
- No CoT-to-Twin mapping is performed by this protocol.
