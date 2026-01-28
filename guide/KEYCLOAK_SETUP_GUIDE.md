# Keycloak Setup Guide - Trading Platform

## Prerequisites

- Docker and Docker Compose installed
- Keycloak service configured in docker-compose.yml

---

## Step 1: Start Keycloak

### 1.1 Start PostgreSQL (if not running)
```bash
cd /Users/princeangellos/docker
docker-compose up -d postgres
```

Wait for PostgreSQL to be healthy (check with `docker-compose ps`)

### 1.2 Start Keycloak
```bash
docker-compose up -d keycloak
```

### 1.3 Check Keycloak logs
```bash
docker-compose logs -f keycloak
```

Wait until you see:
```
Keycloak 25.0.6 started in XXms
```

**Note:** First startup takes 1-2 minutes as it initializes the database.

---

## Step 2: Access Keycloak Admin Console

### 2.1 Open Admin Console
- URL: `http://localhost:8080`
- Click "Administration Console"

### 2.2 Login
- Username: `admin`
- Password: `admin123`

---

## Step 3: Create a Realm

### 3.1 Create New Realm
1. In the top-left, click the dropdown (currently shows "master")
2. Click "Create Realm"
3. Realm name: `trading-platform`
4. Click "Create"

**Why:** Realms isolate users, clients, and settings. Each realm is like a separate tenant.

---

## Step 4: Create a Client for Trading Service

### 4.1 Navigate to Clients
1. In the left sidebar, click "Clients"
2. Click "Create client"

### 4.2 Basic Client Settings
- **Client type:** `OpenID Connect`
- **Client ID:** `trading-service`
- Click "Next"

### 4.3 Capability Config
- ✅ **Client authentication:** ON (This makes it a confidential client)
- ✅ **Authorization:** OFF (for now)
- ✅ **Standard flow:** ON (Authorization Code Flow)
- ✅ **Direct access grants:** ON (For testing with Postman)
- Click "Next"

### 4.4 Login Settings
- **Root URL:** `http://localhost:8081`
- **Home URL:** `http://localhost:8081`
- **Valid redirect URIs:** 
  - `http://localhost:8081/*`
  - `http://localhost:8081/swagger-ui.html`
- **Web origins:** `http://localhost:8081`
- **Valid post logout redirect URIs:** `http://localhost:8081/*`
- Click "Save"

### 4.5 Get Client Credentials
1. Go to "Credentials" tab
2. Copy the **Client Secret** (you'll need this for Spring Boot config)
3. Save it somewhere safe

---

## Step 5: Configure JWT Token Settings

### 5.1 Access Token Settings
1. In the client settings, go to "Settings" tab
2. Scroll to "Access token settings"
3. Set **Access token lifespan** to `5 minutes` (or your preference)
4. Set **SSO session idle** to `30 minutes`

### 5.2 Token Mapper (Add User ID to Token)
1. Go to "Client scopes" → `trading-service-dedicated`
2. Click "Mappers" tab
3. Click "Create mapper"
4. Select "By configuration" → "User Attribute"
5. Configure:
   - **Name:** `userId`
   - **User Attribute:** `id` (or create a custom attribute)
   - **Token Claim Name:** `userId`
   - **Claim JSON Type:** `String`
   - ✅ **Add to ID token**
   - ✅ **Add to access token**
   - ✅ **Add to userinfo**
6. Click "Save"

**Alternative:** Use the default `sub` claim which contains the user ID.

---

## Step 6: Create Roles

**Important:** Roles are used for authorization in Spring Boot. The application expects roles like `TRADER`, `ADMIN`, and `VIEWER`.

### 6.1 Create Realm Roles
1. Go to "Realm roles" (in the left sidebar)
2. Click "Create role"
3. Create the following roles (one at a time):
   - **Role name:** `TRADER`
     - Description: "Can create, update, cancel orders, and view their own orders"
     - Click "Save"
   - **Role name:** `ADMIN`
     - Description: "Full access - can do everything including execute orders and view all orders"
     - Click "Save"
   - **Role name:** `VIEWER`
     - Description: "Read-only access - can only view orders"
     - Click "Save"

**Note:** You can also create client roles (under Clients → trading-service → Roles), but realm roles are simpler and work for all clients.

### 6.2 Create Client Roles (Optional)
If you prefer client-specific roles:
1. Go to "Clients" → `trading-service`
2. Go to "Roles" tab
3. Create the same roles: `TRADER`, `ADMIN`, `VIEWER`
4. Client roles will be in `resource_access.trading-service.roles` claim

---

## Step 7: Create Test Users

### 7.1 Create User
1. Go to "Users"
2. Click "Create new user"
3. Fill in:
   - **Username:** `trader1`
   - **Email:** `trader1@example.com`
   - **First name:** `Trader`
   - **Last name:** `One`
   - ✅ **Email verified**
   - ✅ **Enabled**
4. Click "Create"

### 7.2 Set Password
1. Go to "Credentials" tab
2. Set password: `password123`
3. ✅ **Temporary:** OFF (so user doesn't have to change on first login)
4. Click "Set password"
5. Confirm

### 7.3 Assign Roles
1. Go to "Role mapping" tab
2. Click "Assign role"
3. **For realm roles:** Leave "Filter by clients" empty, select "Realm roles" tab
   - Assign `TRADER` role (or `ADMIN`, `VIEWER` as needed)
4. **OR for client roles:** Filter by "Filter by clients" → Select `trading-service`
   - Assign `TRADER` role
5. Click "Assign"

**Recommended:** Use realm roles for simplicity. The Spring Boot application will extract roles from both realm and client roles.

### 7.4 Create More Test Users
Repeat for:
- `admin1` with `ADMIN` role
- `viewer1` with `VIEWER` role

---

## Step 8: Configure Social Login (Optional)

**Is it free?** ✅ Yes! Google OAuth is free for most use cases. There are generous quotas (e.g., 100 requests per 100 seconds per user), which is more than enough for development and small-to-medium applications.

### 8.1 Google OAuth Setup

#### Step 1: Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Sign in with your Google account (or create one if you don't have one)
3. Click the project dropdown at the top (next to "Google Cloud")
4. Click "New Project"
5. Enter project name: `Trading Platform` (or any name)
6. Click "Create"
7. Wait a few seconds, then select your new project from the dropdown

#### Step 2: Enable Google+ API (or Google Identity API)
1. In Google Cloud Console, go to "APIs & Services" → "Library"
2. Search for "Google+ API" or "Google Identity API"
3. Click on it and click "Enable"
4. Wait for it to enable (takes a few seconds)

#### Step 3: Create OAuth 2.0 Credentials
1. Go to "APIs & Services" → "Credentials"
2. Click "+ CREATE CREDENTIALS" at the top
3. Select "OAuth client ID"
4. If prompted, configure the OAuth consent screen first:
   - **User Type:** Choose "External" (unless you have a Google Workspace)
   - Click "Create"
   - **App name:** `Trading Platform` (or any name)
   - **User support email:** Your email
   - **Developer contact information:** Your email
   - Click "Save and Continue"
   - **Scopes:** Click "Save and Continue" (default scopes are fine)
   - **Test users:** Click "Save and Continue" (skip for now)
   - **Summary:** Click "Back to Dashboard"
5. Back at "Create OAuth client ID":
   - **Application type:** Select "Web application"
   - **Name:** `Trading Platform Keycloak` (or any name)
   - **Authorized redirect URIs:** Click "+ ADD URI"
     - Add: `http://localhost:8080/realms/trading-platform/broker/google/endpoint`
     - **Important:** This must match exactly!
   - Click "Create"
6. **Copy your credentials:**
   - A popup will show your **Client ID** and **Client Secret**
   - **Copy both immediately** (you can't see the secret again!)
   - Or click "OK" and find them in the Credentials list

#### Step 4: Configure in Keycloak
1. In Keycloak Admin Console, go to "Identity providers"
2. Click "Add provider" → "Google"
3. Fill in:
   - **Client ID:** (paste from Google Cloud Console)
   - **Client Secret:** (paste from Google Cloud Console)
4. Click "Save"
5. Test by going to the login page and clicking "Sign in with Google"

**Note:** For production, you'll need to:
- Verify your OAuth consent screen
- Add production redirect URIs (e.g., `https://yourdomain.com/realms/trading-platform/broker/google/endpoint`)

### 8.2 GitHub OAuth
1. Go to "Identity providers"
2. Click "Add provider" → "GitHub"
3. You'll need:
   - **Client ID:** (from GitHub Developer Settings)
   - **Client Secret:** (from GitHub)
4. Configure redirect URI in GitHub: `http://localhost:8080/realms/trading-platform/broker/github/endpoint`

**Note:** Social login setup requires creating OAuth apps with the providers first.

---

## Step 9: Get JWT Configuration for Spring Boot

### 9.1 Get Realm Public Key
1. Go to "Realm settings"
2. Go to "Keys" tab
3. Find "RS256" algorithm
4. Click "Public key" - Copy this (you'll need it for JWT validation)

### 9.2 Get Issuer URL
The issuer URL format is:
```
http://localhost:8080/realms/trading-platform
```

### 9.3 Get JWKS URL (JSON Web Key Set)
```
http://localhost:8080/realms/trading-platform/protocol/openid-connect/certs
```

This URL provides the public keys for JWT validation (Spring Boot can use this automatically).

---

## Step 10: Test Keycloak Setup

### 10.1 Get Access Token (Using Direct Access Grant)
```bash
curl -X POST http://localhost:8080/realms/trading-platform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=trading-service" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=trader1" \
  -d "password=password123" \
  -d "grant_type=password"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer",
  "scope": "profile email"
}
```

### 10.2 Decode JWT Token (Optional)
Use https://jwt.io to decode the token and verify it contains:
- `sub` - User ID
- `preferred_username` - Username
- `email` - User email
- `realm_access.roles` - User roles

---

## Step 11: Verify Configuration

### Checklist:
- ✅ Keycloak running on `http://localhost:8080`
- ✅ Realm `trading-platform` created
- ✅ Client `trading-service` created
- ✅ Client secret copied
- ✅ Test users created with roles
- ✅ JWT token can be obtained
- ✅ Token contains user ID and roles

---

## Configuration Summary for Spring Boot

You'll need these values for `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/trading-platform
          # OR use jwk-set-uri:
          # jwk-set-uri: http://localhost:8080/realms/trading-platform/protocol/openid-connect/certs
```

**Client Credentials:**
- Client ID: `trading-service`
- Client Secret: `[from Keycloak]`

---

## Next Steps After Setup

1. ✅ Keycloak is configured
2. ⏭️ Integrate with Spring Boot (update SecurityConfig)
3. ⏭️ Replace X-User-Id with SecurityContext
4. ⏭️ Add role-based authorization
5. ⏭️ Secure WebSocket connections

---

## Troubleshooting

### Keycloak won't start
- Check PostgreSQL is running: `docker-compose ps postgres`
- Check logs: `docker-compose logs keycloak`
- Verify database exists: Check if `keycloak` database was created

### Can't access admin console
- Wait 1-2 minutes for first startup
- Check port 8080 is not in use
- Verify container is running: `docker ps | grep keycloak`

### Token doesn't contain user ID
- Check token mapper is configured correctly
- Verify user has the attribute set
- Use `sub` claim as fallback (it contains user ID)

### "Sign in with Google" button not showing on login page

**Step 1: Verify Provider is Enabled**
1. Go to "Identity providers" → Click "Google"
2. Ensure "Enabled" toggle is ON (blue)
3. Click "Save" if you made changes

**Step 2: Check Display Name**
1. In Google provider settings, scroll to "General settings"
2. Set "Display name" to: `Google` or `Sign in with Google`
3. Click "Save"

**Step 3: Verify "Hide on login page" is OFF**
1. In Google provider settings, go to "Advanced settings"
2. Ensure "Hide on login page" toggle is OFF (gray)
3. If it's ON, turn it OFF and click "Save"

**Step 4: Check Authentication Flow (Most Important!)**
1. Go to "Authentication" in left menu
2. Click "Flows" tab
3. Find "Browser" flow and click the dropdown arrow to expand
4. Look for "Identity Provider Redirector" execution
5. If it's missing:
   - Click "Add execution" (or "Add flow" then "Add execution")
   - Search for "Identity Provider Redirector"
   - Add it
   - Set requirement to "Alternative" or "Required"
   - Click "Save"
6. If it exists but is disabled:
   - Click the dropdown next to it
   - Change requirement to "Alternative" or "Required"
   - Click "Save"

**Step 5: Verify Login Page URL**
- Use: `http://localhost:8080/realms/trading-platform`
- NOT: `http://localhost:8080/realms/master` (that's admin realm)
- Clear browser cache (Ctrl+Shift+R or Cmd+Shift+R)

**Step 6: Test Direct Provider URL**
- Try: `http://localhost:8080/realms/trading-platform/broker/google`
- This should redirect to Google login
- If this works, provider is configured correctly, just not showing on login page

**Step 7: Check Browser Console**
- Open Developer Tools (F12)
- Check Console tab for JavaScript errors
- Check Network tab for failed requests

**Most Common Fix:**
The "Identity Provider Redirector" in the Browser flow is usually the issue. Make sure it's added and set to "Alternative" or "Required".

### Social login not working
- Verify OAuth app is created with provider
- Check redirect URI matches exactly
- Ensure client ID and secret are correct

---

## Quick Reference

**Keycloak URLs:**
- Admin Console: `http://localhost:8080`
- Realm: `trading-platform`
- Token Endpoint: `http://localhost:8080/realms/trading-platform/protocol/openid-connect/token`
- JWKS Endpoint: `http://localhost:8080/realms/trading-platform/protocol/openid-connect/certs`

**Default Credentials:**
- Admin: `admin` / `admin123`
- Test User: `trader1` / `password123`



Google client id - <YOUR_GOOGLE_OAUTH_CLIENT_ID>
Google client secret - <YOUR_GOOGLE_OAUTH_CLIENT_SECRET>


http://localhost:8080/realms/trading-platform/protocol/openid-connect/auth?client_id=trading-service&redirect_uri=http://localhost:8081&response_type=code&scope=openid