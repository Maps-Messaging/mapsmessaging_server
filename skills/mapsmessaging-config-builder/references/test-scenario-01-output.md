Requirement Mapping
- "Expose MQTT over TCP on 1883" -> `NetworkManager.yaml` fields: `endPointServerConfigList[].url`, `endPointConfig.type=tcp`, `protocolConfigs[].type=mqtt`
- "Default JSON payload" -> `NetworkManager.yaml` field: `protocolConfigs[].messageDefaults.contentType=application/json`
- "Map /telemetry namespace" -> `DestinationManager.yaml` fields: `namespace`, `namespaceMapping`
- "Add route server edge-a" -> `routing.yaml` fields: `routing.predefinedServers[]`, `routing.enabled=true`

Assumptions
- `authenticationRealm` defaults to `anon`.
- `proxyProtocol` remains `false`.
- No QoS override requested.

Deployable Config Entity
```diff
*** NetworkManager.yaml
+    -
+      authenticationRealm: anon
+      name: mqtt-tcp-ingress-1883
+      endPointConfig:
+        type: tcp
+      protocolConfigs:
+        -
+          type: mqtt
+          messageDefaults:
+            contentType: application/json
+      url: tcp://0.0.0.0:1883/

*** DestinationManager.yaml
+    -
+      type: Memory
+      namespace: /telemetry/
+      namespaceMapping: /telemetry/
+
*** routing.yaml
+  enabled: true
+  predefinedServers:
+    -
+      name: edge-a
+      url: https://edge-a:8080/
```

Apply Steps
```bash
# apply edits in working tree
# then verify keys exist
rg -n "type: mqtt|contentType: application/json|tcp://0.0.0.0:1883/" NetworkManager.yaml
rg -n "namespace: /telemetry/|namespaceMapping: /telemetry/" DestinationManager.yaml
rg -n "enabled: true|name: edge-a|url: https://edge-a:8080/" routing.yaml
```

Verification
```bash
# producer
mosquitto_pub -h localhost -p 1883 -t /telemetry/test -m '{"ok":true}'
# consumer
timeout 5 mosquitto_sub -h localhost -p 1883 -t /telemetry/# -C 1
```
