# RTSP data products

Drone entries may optionally expose one or more external RTSP data products.

```yaml
droneInfo:
  - name: stickleback-drone-1
    uuid: a1eef2cf-7dc2-5655-9aed-9ed6cee64345
    data_products:
      - identifier: dp-optical-camera-001
        description: Optical camera
        uri: rtsp://drone01/optical
        product_type:
          name: video/rtsp
        conforms_to:
          name: ONVIF Profile S

      - identifier: dp-thermal-camera-001
        description: Thermal camera
        uri: rtsp://drone01/thermal
        product_type:
          name: video/rtsp
        conforms_to:
          name: ONVIF Profile S
```

`data_products` is optional. `rtsp` and `rtsps` URIs are accepted by the STANAG adapter. A non-UUID configured identifier is converted to a deterministic UUID scoped to the drone before publication because the AEP-105 wire identifier is a GUID.

Each configured camera is published as a separate `ValueTypeEnum_DATA_PRODUCT` dynamic update referencing the detected track. The optical and thermal feeds therefore retain separate identifiers, descriptions, URIs, and metadata.

The feeds are published immediately for a new track association and refreshed periodically rather than repeated with every track update.
