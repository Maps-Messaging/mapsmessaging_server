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
