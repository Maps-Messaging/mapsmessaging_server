package io.mapsmessaging.network.protocol.impl.mqtt5;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;

public class ClientCallbackHandler  implements CallbackHandler {

  private final String username;
  private final String password;
  private final String serverName;

  public ClientCallbackHandler(String username, String password, String serverName) {
    this.username = username;
    this.password = password;
    this.serverName = serverName;
  }

  @Override
  public void handle(Callback[] cbs) {
    for (Callback cb : cbs) {
      if (cb instanceof NameCallback) {
        NameCallback nc = (NameCallback) cb;
        nc.setName(username);
      } else if (cb instanceof PasswordCallback) {
        PasswordCallback pc = (PasswordCallback) cb;
        pc.setPassword(password.toCharArray());
      } else if (cb instanceof RealmCallback) {
        RealmCallback rc = (RealmCallback) cb;
        rc.setText(serverName);
      }
    }
  }
}
