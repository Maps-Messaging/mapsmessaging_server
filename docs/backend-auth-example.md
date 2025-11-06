# Backend Authentication Implementation Example

This document provides example Java implementations for the three required authentication endpoints.

## Dependencies

Add to `pom.xml`:

```xml
<!-- JWT support -->
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.3</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>

<!-- For encoding/decoding -->
<dependency>
  <groupId>javax.crypto</groupId>
  <artifactId>jcrypto</artifactId>
  <version>1.0</version>
</dependency>
```

## 1. JWT Token Provider Service

```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtTokenProvider {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
        "your-secret-key-at-least-256-bits-long-should-be-in-config".getBytes()
    );
    
    private static final long ACCESS_TOKEN_EXPIRY = 3600 * 1000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRY = 30L * 24 * 3600 * 1000; // 30 days

    /**
     * Generate JWT access token
     */
    public String generateAccessToken(String username, Map<String, Object> claims) {
        claims.put("type", "access");
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .signWith(KEY, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
            .signWith(KEY, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
```

## 2. Authentication Request/Response DTOs

```java
// Request DTOs
public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

public class TokenRefreshRequest {
    private String refreshToken;

    public TokenRefreshRequest() {}

    public TokenRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

// Response DTOs
public class LoginResponse {
    private String token;
    private String refreshToken;
    private long expiresIn;
    private UserResponse user;

    public LoginResponse() {}

    public LoginResponse(String token, String refreshToken, long expiresIn, UserResponse user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}

public class TokenRefreshResponse {
    private String token;
    private String refreshToken;
    private long expiresIn;

    public TokenRefreshResponse() {}

    public TokenRefreshResponse(String token, String refreshToken, long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}

public class UserResponse {
    private String username;
    private String email;
    private List<String> roles;

    public UserResponse() {}

    public UserResponse(String username, String email, List<String> roles) {
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}

public class ErrorResponse {
    private String error;
    private String message;
    private long timestamp;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
```

## 3. Authentication Controller

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(
    origins = {"http://localhost:3000", "https://admin.yourdomain.com"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowedHeaders = {"Content-Type", "Authorization"},
    exposedHeaders = {"Content-Type", "Authorization"},
    allowCredentials = "true",
    maxAge = 86400
)
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * POST /api/v1/login
     * Authenticate user and issue tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Validate input
            if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_REQUEST", "Username and password are required"));
            }

            // Verify credentials
            User user = userService.findByUsername(loginRequest.getUsername());
            if (user == null || !userService.verifyPassword(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid username or password"));
            }

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRoles());

            String accessToken = tokenProvider.generateAccessToken(user.getUsername(), claims);
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            // Store refresh token (in DB or cache)
            userService.storeRefreshToken(user.getUsername(), refreshToken);

            // Build response
            UserResponse userResponse = new UserResponse(
                user.getUsername(),
                user.getEmail(),
                user.getRoles()
            );

            LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                3600, // expiresIn: 1 hour in seconds
                userResponse
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_ERROR", "Authentication failed"));
        }
    }

    /**
     * POST /api/v1/refreshToken
     * Issue new access token using refresh token
     */
    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest refreshRequest) {
        try {
            // Validate input
            if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_REQUEST", "Refresh token is required"));
            }

            String refreshToken = refreshRequest.getRefreshToken();

            // Validate token
            if (!tokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_TOKEN", "Refresh token is invalid"));
            }

            if (tokenProvider.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("TOKEN_EXPIRED", "Refresh token has expired"));
            }

            // Get username from token
            String username = tokenProvider.getUsernameFromToken(refreshToken);

            // Verify token is stored (check against DB/cache)
            if (!userService.verifyRefreshToken(username, refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_TOKEN", "Refresh token is invalid or revoked"));
            }

            // Get user info for claims
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User no longer exists"));
            }

            // Generate new tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRoles());

            String newAccessToken = tokenProvider.generateAccessToken(username, claims);
            String newRefreshToken = tokenProvider.generateRefreshToken(username); // Optional: rotate

            // Store new refresh token
            userService.storeRefreshToken(username, newRefreshToken);

            TokenRefreshResponse response = new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken,
                3600 // 1 hour in seconds
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_ERROR", "Token refresh failed"));
        }
    }

    /**
     * POST /api/v1/logout
     * Invalidate tokens and end session
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        try {
            // Extract token from Authorization header
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token is missing or invalid"));
            }

            // Validate token
            if (!tokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Token is missing or invalid"));
            }

            // Get username from token
            String username = tokenProvider.getUsernameFromToken(token);

            // Add token to blacklist (revocation list)
            userService.blacklistToken(token, username);

            // Optional: Invalidate all refresh tokens for this user
            // userService.revokeAllRefreshTokens(username);

            return ResponseEntity.ok(Collections.singletonMap("success", true));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_ERROR", "Logout failed"));
        }
    }
}
```

## 4. User Service (Example)

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> refreshTokens = new ConcurrentHashMap<>();
    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    public UserService() {
        // Initialize with test users (replace with DB queries in production)
        initializeTestUsers();
    }

    private void initializeTestUsers() {
        User admin = new User(
            "admin",
            passwordEncoder.encode("admin123"),
            "admin@example.com",
            Arrays.asList("admin", "user")
        );
        users.put(admin.getUsername(), admin);

        User regularUser = new User(
            "user",
            passwordEncoder.encode("user123"),
            "user@example.com",
            Collections.singletonList("user")
        );
        users.put(regularUser.getUsername(), regularUser);
    }

    public User findByUsername(String username) {
        return users.get(username);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void storeRefreshToken(String username, String refreshToken) {
        refreshTokens.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet())
            .add(refreshToken);
    }

    public boolean verifyRefreshToken(String username, String refreshToken) {
        Set<String> userTokens = refreshTokens.get(username);
        return userTokens != null && userTokens.contains(refreshToken);
    }

    public void revokeAllRefreshTokens(String username) {
        refreshTokens.remove(username);
    }

    public void blacklistToken(String token, String username) {
        tokenBlacklist.add(token);
        // In production: store in Redis with expiry
        // redisTemplate.opsForSet().add("token_blacklist", token);
        // redisTemplate.expire("token_blacklist", tokenExpiry, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
        // In production: check Redis
        // return redisTemplate.opsForSet().isMember("token_blacklist", token);
    }
}
```

## 5. User Entity (Example)

```java
import java.util.List;

public class User {
    private String username;
    private String password;
    private String email;
    private List<String> roles;
    private boolean enabled;
    private long createdAt;

    public User(String username, String password, String email, List<String> roles) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
        this.enabled = true;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
```

## 6. CORS Configuration (Alternative)

If using Spring Framework:

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "https://admin.yourdomain.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Authorization")
            .exposedHeaders("Content-Type", "Authorization")
            .allowCredentials(true)
            .maxAge(86400);
    }
}
```

## Testing the Endpoints

### Using curl

```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Response:
# {
#   "token": "eyJhbGc...",
#   "refreshToken": "eyJhbGc...",
#   "expiresIn": 3600,
#   "user": {
#     "username": "admin",
#     "email": "admin@example.com",
#     "roles": ["admin", "user"]
#   }
# }

# 2. Use token for API request
curl -X GET http://localhost:8080/api/v1/dashboard \
  -H "Authorization: Bearer <token_from_login>"

# 3. Refresh token
curl -X POST http://localhost:8080/api/v1/refreshToken \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh_token_from_login>"
  }'

# 4. Logout
curl -X POST http://localhost:8080/api/v1/logout \
  -H "Authorization: Bearer <token>"
```

### Using Postman

1. Create collection: "Maps Messaging Auth"
2. Create requests:
   - **POST /api/v1/login**: Body (raw JSON)
   - **GET /api/v1/dashboard**: Add Authorization header (Bearer token)
   - **POST /api/v1/refreshToken**: Body (raw JSON)
   - **POST /api/v1/logout**: Add Authorization header (Bearer token)

## Security Best Practices

1. **Store secrets in environment variables** or config files (not in code)
2. **Use HTTPS in production** (enforce redirect from HTTP)
3. **Implement rate limiting** on login endpoint
4. **Log authentication events** for audit trail
5. **Implement token rotation** on refresh
6. **Add token blacklist/revocation** mechanism
7. **Set appropriate token expiry** times
8. **Validate tokens on every protected endpoint**
9. **Use secure headers** (CSP, X-Frame-Options, etc.)
10. **Implement CORS carefully** (don't use * in production)

## References

- JJWT: https://github.com/jwtk/jjwt
- JWT: https://jwt.io
- Spring Security: https://spring.io/projects/spring-security
