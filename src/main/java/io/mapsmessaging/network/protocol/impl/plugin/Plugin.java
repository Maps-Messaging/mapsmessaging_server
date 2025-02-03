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
  private boolean initialized = false;

  protected Plugin() {
  }

  public final void initializePlugin() {
    if (!initialized) {
      initialise();
      initialized = true;
    } else {
      throw new IllegalStateException("Plugin is already initialized");
    }
  }

  public void close() throws IOException {
    if (pluginProtocol != null) {
      pluginProtocol.close();
    }
  }

  public String getSessionId() {
    if (pluginProtocol == null) {
      throw new IllegalStateException("PluginProtocol is not set");
    }
    return pluginProtocol.getSessionId();
  }

  public abstract void initialise();

  public abstract @NonNull @NotNull String getName();

  public abstract @NotNull String getVersion();

  public abstract boolean supportsRemoteFiltering();

  public abstract void outbound(@NonNull @NotNull String destinationName, @NonNull @NotNull byte[] payload, @Nullable Map<String, Object> map);

  protected void inbound(@NonNull @NotNull String destinationName, @NonNull @NotNull byte[] payload, @Nullable Map<String, Object> map) throws IOException {
    if (pluginProtocol == null) {
      throw new IllegalStateException("PluginProtocol is not set");
    }
    try {
      pluginProtocol.saveMessage(destinationName, payload, map);
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      throw new IOException("Error processing inbound message", e);
    }
  }

  public abstract void registerRemoteLink(@NonNull @NotNull String destination, @Nullable String filter) throws IOException;

  public abstract void registerLocalLink(@NonNull @NotNull String destination) throws IOException;

  protected final void setPluginProtocol(@NonNull PluginProtocol protocol) {
    if (this.pluginProtocol != null) {
      throw new IllegalStateException("PluginProtocol is already set");
    }
    this.pluginProtocol = protocol;
  }
}
