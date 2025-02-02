package io.mapsmessaging.network.protocol.impl.plugin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Getter
public abstract class Plugin {

  private PluginProtocol pluginProtocol;

  protected Plugin() {
  }

  public void close() throws IOException {

  }

  public abstract void initialise();

  public String getSessionId(){
    return pluginProtocol.getSessionId();
  }

  public abstract @NonNull @NotNull String getName();

  public abstract @NotNull String getVersion();

  public abstract boolean supportsRemoteFiltering();

  public abstract void outbound(@NonNull @NotNull String destinationName, @NonNull @NotNull byte[] payload, @Nullable Map<String, Object> map);

  protected void inbound(@NonNull @NotNull String destinationName, @NonNull @NotNull byte[] payload, @Nullable Map<String, Object> map) throws IOException, ExecutionException, InterruptedException, TimeoutException {
    pluginProtocol.saveMessage(destinationName, payload, map);
  }

  public abstract void registerRemoteLink(@NonNull @NotNull String destination, @Nullable String filter) throws IOException;

  public abstract void registerLocalLink(@NonNull @NotNull String destination) throws IOException;

  protected void setPluginProtocol(PluginProtocol protocol) {
    pluginProtocol = protocol;
  }

}
