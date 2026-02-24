Requirement Mapping
- "Expose AMQP over TCP on 5672" -> `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`: `url`, `endPointConfig.type=tcp`, `protocolConfigs[].type=amqp`
- "Set XML default payload" -> `/Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml`: `protocolConfigs[].messageDefaults.contentType=application/xml`
- "Enable server routing to core-hub" -> `/Users/krital/dev/starsense/mapsmessaging_server/routing.yaml`: `routing.enabled=true`, `predefinedServers[]`

Assumptions
- Realm uses `anon`.
- AMQP session defaults remain unchanged except explicit contentType.

Deployable Config Entity
```diff
*** /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
+    -
+      authenticationRealm: anon
+      name: amqp-tcp-ingress-5672
+      endPointConfig:
+        type: tcp
+      protocolConfigs:
+        -
+          type: amqp
+          messageDefaults:
+            contentType: application/xml
+      url: tcp://0.0.0.0:5672/

*** /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
+  enabled: true
+  predefinedServers:
+    -
+      name: core-hub
+      url: https://core-hub:8080/
```

Apply Steps
```bash
rg -n "type: amqp|contentType: application/xml|tcp://0.0.0.0:5672/" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "enabled: true|name: core-hub|url: https://core-hub:8080/" /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
```

Verification
```bash
# AMQP smoke checks are environment-specific; verify listener then route entry
rg -n "amqp-tcp-ingress-5672|type: amqp" /Users/krital/dev/starsense/mapsmessaging_server/NetworkManager.yaml
rg -n "core-hub" /Users/krital/dev/starsense/mapsmessaging_server/routing.yaml
```
