# Authorization and IdentityLookup API

The IdentityLookup API is the foundation of authorization within the library, enabling integration with various user management systems. This document outlines how to implement and configure each supported system.

## IdentityLookup Interface

The IdentityLookup interface offers essential methods for managing users and groups, including creation, deletion, and group manipulation.

```java
public interface IdentityLookup {
  String getName();
  String getDomain();
  char[] getPasswordHash(String username) throws IOException, GeneralSecurityException;
  IdentityEntry findEntry(String username);
  List<IdentityEntry> getEntries();
  GroupEntry findGroup(String groupName);
  default List<GroupEntry> getGroups() { return new ArrayList<>(); }
  IdentityLookup create(ConfigurationProperties config);
// Additional methods for group and user management with default implementations throwing NotImplementedException
}
```

This interface is implemented by specific user managers for Apache, Auth0, Cognito, local encrypted store, JWT, LDAP, and Unix, each configured via a `Map<String, Object>` with their unique requirements.

## Configuration Details

Each user manager requires specific configuration parameters provided through a configuration map. Here are the configuration requirements for each:

### Apache
- `passwordFile` and optional `groupFile` or `configDirectory` (where the code locates the password file and group file within the configDirectory)

### Auth0
- `domain`: auth0Domain name for URL usage
- `clientId`: used for Auth0 authentication
- `clientSecret`: used for Auth0 authentication
- `cacheTime`: Time in milliseconds to cache previous results

### Cognito
- `userPoolId`: Specifies which user pool to use
- `appClientId`: Client ID for AWS connection
- `appClientSecret`: Application secret for AWS authentication
- `region`: AWS region
- `accessKeyId`: Access ID for AWS
- `secretAccessKey`: AWS secret tied to the access ID
- `cacheTime`: Time in milliseconds to cache previous results

### Encrypted Auth
- Similar to Apache, with additional fields for encryption:
    - `alias`: Alias of the private key for data encryption
    - `privateKey.passphrase`: Passphrase for the private key
    - `privateKey.name`: Name of the private key

### LDAP
- `passwordKeyName`: Name for retrieving the password from LDAP
- `searchBase`: LDAP search base
- `groupSearchBase`: Search base for group lookups
- Additional map parameters are passed directly into the LDAP API

### Unix
- Follows the Apache requirements for `passwordFile` and optionally `groupFile` or `configDirectory`

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

### Example config

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
                                                     certificateStore.privateKey.passphrase: "privateKeyPassword"


};
```
## Auth0JwtLoginModule

Provides support for JWT authentication via Auth0, simplifying integration with Auth0's managed user identities.

## AwsCognitoLoginModule

Enables authentication with AWS Cognito, allowing the use of AWS-managed user pools for authentication purposes.

## SSLCertificateLoginModule

Facilitates authentication based on SSL/TLS certificates, offering a secure method of user identification and management.

```Note: For each module, include specific configurations and usage examples relevant to your setup. Detail common configuration patterns, such as caching strategies or error handling, for consistency and ease of use.```
