package io.mapsmessaging.network.protocol.impl.rest;

import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.extension.Extension;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class RestProtocol extends Extension {

  private final Logger logger;
  private final EndPointURL url;


  public RestProtocol(@NonNull @NotNull EndPoint endPoint, ExtensionConfigDTO protocolConfigDTO) {
    url = new EndPointURL(endPoint.getConfig().getUrl());
    logger = LoggerFactory.getLogger(RestProtocol.class);
    logger.log(RestProtocolLogMessages.INITIALISE_REST_ENDPOINT, url.toString());
  }

  @Override
  public void close() throws IOException {
    logger.log(RestProtocolLogMessages.CLOSE_REST_ENDPOINT, url.toString());
    super.close();
  }

  /**
   * Called when the plugin has connected locally to the messaging engine and is ready to process requests
   */
  @Override
  public void initialise() {

  }

  @Override
  public @NonNull String getName() {
    return "RestProtocol";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public boolean supportsRemoteFiltering() {
    return false;
  }

  /**
   * This is called when the configuration requires events to be pulled from the pulsar server.
   *
   * @param destination The destination to subscribe to
   * @param filter      If filtering is supported then the filter will contain a JMS style selector
   * @throws IOException
   */
  @Override
  public void registerRemoteLink(@NotNull @NotNull String destination, @Nullable String filter) throws IOException {

  }


  /**
   * Before any events are passed from the MAPS server this will be called to indicate that the configuration has
   * a local destination(s) to be sent to a remote Pulsar server. So whatever needs to happen to facilitate that happens here
   *
   * @param destination The name of the destination ( it could be a MQTT wild card subscription )
   * @throws IOException
   */
  @Override
  public void registerLocalLink(@NonNull @NotNull String destination) throws IOException {

  }


  /**
   * Handle message coming from the MAPS server destined to the remote name
   *
   * @param destinationName Fully Qualified remote name
   * @param message         io.mapsmessaging.api.message.Message containing the data to send
   */
  @Override
  public void outbound(@NonNull @NotNull String destinationName, @NonNull @NotNull io.mapsmessaging.api.message.Message message) {

  }

}
