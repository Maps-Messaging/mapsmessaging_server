package io.mapsmessaging.network.protocol.impl.amqp.proton;

import io.mapsmessaging.network.protocol.sasl.SaslAuthenticationMechanism;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import org.apache.qpid.proton.engine.Sasl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaslManager {

  private final Sasl sasl;
  private final SaslAuthenticationMechanism saslAuthenticationMechanism;

  public SaslManager(ProtonEngine protonEngine) throws IOException {
    saslAuthenticationMechanism = buildMechansim(protonEngine.getProtocol().getEndPoint().getConfig().getProperties());
    sasl = protonEngine.getTransport().sasl();
    String mechanism = saslAuthenticationMechanism != null ? saslAuthenticationMechanism.getMechanism() : "ANONYMOUS";
    sasl.setMechanisms(mechanism);
    sasl.server();
    if (mechanism.equalsIgnoreCase("ANONYMOUS")) {
      sasl.done(Sasl.PN_SASL_OK);
    }
  }

  public void challenge() throws IOException {
    int pending = Math.max(0, sasl.pending());
    byte[] challenge;
    if (pending > 0) {
      challenge = new byte[pending];
      sasl.recv(challenge, 0, challenge.length);
    } else {
      challenge = new byte[0];
    }
    byte[] response = saslAuthenticationMechanism.challenge(challenge);
    if (response != null) {
      sasl.send(response, 0, response.length);
    }
  }

  private SaslAuthenticationMechanism buildMechansim(ConfigurationProperties config) throws IOException {
    SaslAuthenticationMechanism authenticationContext = null;
    if (config.containsKey("sasl")) {
      ConfigurationProperties saslProps = (ConfigurationProperties) config.get("sasl");
      Map<String, String> props = new HashMap<>();
      props.put(javax.security.sasl.Sasl.QOP, "auth");
      authenticationContext = new SaslAuthenticationMechanism(saslProps.getProperty("mechanism"), "ServerNameHere", "AMQP", props, config);
    }
    return authenticationContext;
  }

  public boolean isDone() {
    if (saslAuthenticationMechanism != null && saslAuthenticationMechanism.complete()) {
      sasl.done(Sasl.PN_SASL_AUTH);
    }

    return saslAuthenticationMechanism == null ||
        sasl.getOutcome().equals(Sasl.SaslOutcome.PN_SASL_OK);
  }

}
