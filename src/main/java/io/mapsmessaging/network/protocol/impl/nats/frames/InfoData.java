package io.mapsmessaging.network.protocol.impl.nats.frames;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class InfoData {

  @SerializedName("client_id")
  private long clientId;

  @SerializedName("client_ip")
  private String clientIp;

  @SerializedName("server_id")
  private String serverId;

  private String serverName;

  private String version;
  private String host;
  private int port;

  @SerializedName("max_payload")
  private int maxPayloadLength;

  @SerializedName("tls_required")
  private boolean tlsRequired = false;

  @SerializedName("auth_required")
  private boolean authRequired = false;

  private boolean headers = true;

  @SerializedName("jetstream")
  private boolean jetStream = true;

  private int proto = 1;

  private String java;
}
