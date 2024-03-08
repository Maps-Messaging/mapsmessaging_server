# JAAS Implementations

The library enhances Java Authentication and Authorization Service (JAAS) support through various login modules. These modules facilitate authentication using different providers, such as Auth0 JWT, AWS Cognito, SSL certificates, and a site-wide identity manager.

## IdentityLoginModule

Integrates with the `IdentityLookup` API, offering a flexible approach to user management. It can be configured to utilize a system-wide global Identity Manager, streamlining authentication across multiple components.

### Configuration

- Utilize configurations from the Authorization and IdentityLookup API section.
- Implementations are specified in the JAAS config file.

### SiteWide Configuration

- This module supports a "siteWide" option, which allows the use of a named site-wide identity manager for authentication. If "siteWide" is specified, the module employs the global manager. Otherwise, it creates its own `IdentityLookup` instance.

  ```
  siteWide = "nameOfSiteWideIdentityManager";
  ```

### Example JaasAuth.config

This uses a site wide Identity Module so it must have been constructed and configured elsewhere
```text
UsernamePasswordLoginModule{
  io.mapsmessaging.security.jaas.IdentityLoginModule Required
                                                     debug=false
                                                     siteWide="system";
};
```

else it can be configured here such as

```text
UsernamePasswordLoginModule{
  io.mapsmessaging.security.jaas.IdentityLoginModule Required
                                                     debug=false
                                                     identityProvider="Encrypted-Auth"
                                                     passwordHander="EncryptedPasswordCipher"
                                                     configDirectory: "./.security"
                                                     certificateStore.type= "JKS"
                                                     certificateStore.path: "./.security/authKeystore.jks"
                                                     certificateStore.passphrase: "keyStorePassword"
                                                     certificateStore.alias: "default"
                                                     certificateStore.privateKey.name: "default"
                                                     certificateStore.privateKey.passphrase: "privateKeyPassword";


};
```
## Auth0JwtLoginModule

Provides support for JWT authentication via Auth0, simplifying integration with Auth0's managed user identities.

### Example JaasAuth.config
```text
JWTAuthConfig{
    io.mapsmessaging.security.jaas.AwsJwtLoginModule Required
                                                     domain="auth0_domain"
                                                     debug=false;
};

```

## AwsCognitoLoginModule

Enables authentication with AWS Cognito, allowing the use of AWS-managed user pools for authentication purposes.

### Example JaasAuth.config
```text
JWTAuthConfig{
    io.mapsmessaging.security.jaas.AwsJwtLoginModule Required
                                                     region="aws_region_here"
                                                     poolId="pool_id_here"
                                                     clientId="client_id_here"
                                                     debug=false;
};

```


## SSLCertificateLoginModule

Facilitates authentication based on SSL/TLS certificates, offering a secure method of user identification and management.

### Example JaasAuth.config
```text
SSLAuthConfig{
    io.mapsmessaging.security.jaas.SSLCertificateLoginModule Required debug=true;

};
```
