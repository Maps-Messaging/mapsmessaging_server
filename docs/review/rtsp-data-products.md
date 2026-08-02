# RTSP data products

Drone entries may optionally expose one or more external RTSP data products.

```yaml
droneInfo:
  - name: stickleback-drone-1
    uuid: a1eef2cf-7dc2-5655-9aed-9ed6cee64345
    data_products:
      - identifier: dp-rtsp-feed-001
        description: Forward camera
        uri: rtsp://drone01/videofeed01
        product_type:
          name: video/rtsp
        conforms_to:
          name: ONVIF Profile S
```

`data_products` is optional. `rtsp` and `rtsps` URIs are accepted by the STANAG adapter. A non-UUID configured identifier is converted to a deterministic UUID scoped to the drone before publication because the AEP-105 wire identifier is a GUID.

The adapter publishes each configured feed as a `ValueTypeEnum_DATA_PRODUCT` dynamic update referencing the detected track. The feed is published immediately for a new track association and refreshed periodically rather than repeated with every track update.
