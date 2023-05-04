package io.mapsmessaging.network.protocol.sasl;


import io.mapsmessaging.security.identity.IdentityLookup;
import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

public class ServerCallbackHandler implements CallbackHandler {

  private String username;
  private char[] hashedPassword;
  private final String serverName;

  private final IdentityLookup identityLookup;

  public ServerCallbackHandler(String serverName, IdentityLookup identityLookup){
    this.identityLookup = identityLookup;
    this.serverName = serverName;
  }

  @Override
  public void handle(Callback[] cbs) throws IOException {
    for (Callback cb : cbs) {
      if (cb instanceof AuthorizeCallback) {
        AuthorizeCallback ac = (AuthorizeCallback) cb;
        ac.setAuthorized(true);
      } else if (cb instanceof NameCallback) {
        NameCallback nc = (NameCallback) cb;
        username = nc.getDefaultName();
        hashedPassword = identityLookup.getPasswordHash(username);
        nc.setName(nc.getDefaultName());
      } else if (cb instanceof PasswordCallback) {
        PasswordCallback pc = (PasswordCallback) cb;
        pc.setPassword(hashedPassword);
      } else if (cb instanceof RealmCallback) {
        RealmCallback rc = (RealmCallback) cb;
        rc.setText(serverName);
      }
    }
  }
}