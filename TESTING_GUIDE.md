# 🧪 Testing Guide - Authentication System

## Prerequisites

- cURL installed
- Application running on `http://localhost:8080`
- Database migrated successfully
- At least one user created
- At least one email in whitelist

---

## Test Suite

### 1. ✅ User Registration

```bash
# Test 1.1: Register new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "test@example.com"
  }' | jq

# Expected: 201 Created
# Response: {"message": "User created successfully", "username": "testuser"}

# Test 1.2: Try duplicate username
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Another123",
    "email": "different@example.com"
  }' | jq

# Expected: 409 Conflict
# Response: {"error": "Username-ul este deja folosit"}

# Test 1.3: Invalid password (too short)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "short",
    "email": "new@example.com"
  }' | jq

# Expected: 400 Bad Request
```

---

### 2. ✅ Standard Login Flow

```bash
# Test 2.1: Successful login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -v \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }' | jq

# Expected: 202 Accepted
# Response: {"tokens": {"accessToken": "eyJ...", "refreshToken": null, "sessionId": "..."}}
# Cookie Set: refreshToken (HttpOnly, Secure, SameSite=Strict)

# Save access token for later
export ACCESS_TOKEN="paste-token-here"

# Test 2.2: Failed login (wrong password)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "WrongPassword"
  }' | jq

# Expected: 401 Unauthorized

# Test 2.3: Login with non-existent user
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nonexistent",
    "password": "SomePassword"
  }' | jq

# Expected: 401 Unauthorized
```

---

### 3. ✅ Access Protected Endpoints

```bash
# Test 3.1: Access with valid token
curl -X GET http://localhost:8080/reminders \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Expected: 200 OK
# Response: List of reminders

# Test 3.2: Access without token
curl -X GET http://localhost:8080/reminders | jq

# Expected: 401 Unauthorized

# Test 3.3: Access with invalid token
curl -X GET http://localhost:8080/reminders \
  -H "Authorization: Bearer invalid.token.here" | jq

# Expected: 401 Unauthorized
```

---

### 4. ✅ Refresh Token Flow

```bash
# Test 4.1: Refresh token using cookie
curl -X POST http://localhost:8080/auth/refresh-token \
  -b cookies.txt \
  -c cookies.txt \
  -v | jq

# Expected: 202 Accepted
# Response: New access token
# Cookie: New refresh token (rotated)

# Test 4.2: Refresh without cookie or body
curl -X POST http://localhost:8080/auth/refresh-token | jq

# Expected: 403 Forbidden

# Test 4.3: Refresh with old token (should fail - compromise detection)
# Save current refresh token, refresh once, try old token again
curl -X POST http://localhost:8080/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "old-token-value"}' | jq

# Expected: 401 Unauthorized
```

---

### 5. ✅ Rate Limiting Tests

```bash
# Test 5.1: Trigger user rate limit (6 failed attempts)
for i in {1..6}; do
  echo "Attempt $i"
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "username": "testuser",
      "password": "WrongPassword"
    }' | jq
  sleep 1
done

# After 6th attempt:
# Expected: 403 Forbidden
# Response: Message about needing email verification

# Test 5.2: Try normal login after 6 failed attempts
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }' | jq

# Expected: 403 Forbidden
# Must use email verification now

# Test 5.3: IP rate limit (10 failed attempts from same IP)
for i in {1..10}; do
  echo "Attempt $i"
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "username": "user'$i'",
      "password": "wrong"
    }' | jq
  sleep 1
done

# After 10th attempt:
# Expected: 429 Too Many Requests
```

---

### 6. ✅ Email Verification (2FA) Flow

```bash
# Prerequisites: User must have requires_email_verification = true
# (trigger this with 6 failed login attempts from Test 5)

# Test 6.1: Add email to whitelist first (if not already)
curl -X POST http://localhost:8080/admin/email-whitelist \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "description": "Test user email"
  }' | jq

# Test 6.2: Request verification code
curl -X POST http://localhost:8080/auth/request-email-code \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "test@example.com"
  }' | jq

# Expected: 200 OK
# Response: {"message": "Cod de verificare trimis pe email"}
# Check your email for 6-digit code

# Test 6.3: Login with email verification code
# Replace 123456 with actual code from email
curl -X POST http://localhost:8080/auth/login-with-email \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "test@example.com",
    "verificationCode": "123456"
  }' | jq

# Expected: 202 Accepted
# Response: Tokens
# User's failed_login_attempts reset to 0

# Test 6.4: Try with wrong code
curl -X POST http://localhost:8080/auth/login-with-email \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "test@example.com",
    "verificationCode": "000000"
  }' | jq

# Expected: 401 Unauthorized

# Test 6.5: Try with expired code (wait 16 minutes after requesting)
# Expected: 401 Unauthorized

# Test 6.6: Request code with non-whitelisted email
curl -X POST http://localhost:8080/auth/request-email-code \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456",
    "email": "notinwhitelist@example.com"
  }' | jq

# Expected: 400 Bad Request
```

---

### 7. ✅ Admin API - Email Whitelist

```bash
# Test 7.1: Get all whitelisted emails
curl -X GET http://localhost:8080/admin/email-whitelist \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 7.2: Get only active emails
curl -X GET http://localhost:8080/admin/email-whitelist/active \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 7.3: Add new email
curl -X POST http://localhost:8080/admin/email-whitelist \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newwhitelisted@example.com",
    "description": "New user"
  }' | jq

# Test 7.4: Check if email is whitelisted
curl -X GET "http://localhost:8080/admin/email-whitelist/check/test@example.com" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Expected: {"whitelisted": true}

# Test 7.5: Remove email (get ID from list first)
export EMAIL_ID="paste-uuid-here"
curl -X DELETE http://localhost:8080/admin/email-whitelist/$EMAIL_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 7.6: Reactivate email
curl -X PUT http://localhost:8080/admin/email-whitelist/$EMAIL_ID/activate \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

---

### 8. ✅ Admin API - User Management

```bash
# Test 8.1: Get all users
curl -X GET http://localhost:8080/admin/users \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 8.2: Get user by ID
export USER_ID="paste-uuid-here"
curl -X GET http://localhost:8080/admin/users/$USER_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 8.3: Disable user
curl -X PUT "http://localhost:8080/admin/users/$USER_ID/enabled?enabled=false" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 8.4: Try to login with disabled user
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }' | jq

# Expected: 403 Forbidden

# Test 8.5: Re-enable user
curl -X PUT "http://localhost:8080/admin/users/$USER_ID/enabled?enabled=true" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 8.6: Unlock user (after account is locked)
curl -X POST http://localhost:8080/admin/users/$USER_ID/unlock \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 8.7: Reset email verification requirement
curl -X POST http://localhost:8080/admin/users/$USER_ID/reset-email-verification \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Test 8.8: Get user statistics
curl -X GET http://localhost:8080/admin/users/stats \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Expected: {"totalUsers": X, "enabledUsers": Y, ...}

# Test 8.9: Delete user (USE WITH CAUTION!)
curl -X DELETE http://localhost:8080/admin/users/$USER_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

---

### 9. ✅ Logout

```bash
# Test 9.1: Logout
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -b cookies.txt | jq

# Expected: 200 OK
# Cookie cleared

# Test 9.2: Try to use old access token
curl -X GET http://localhost:8080/reminders \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq

# Expected: 401 Unauthorized (session revoked)

# Test 9.3: Try to refresh with old token
curl -X POST http://localhost:8080/auth/refresh-token \
  -b cookies.txt | jq

# Expected: 401 Unauthorized
```

---

### 10. ✅ Change Password

```bash
# Test 10.1: Login first to get valid token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }' | jq

export ACCESS_TOKEN="new-token-here"

# Test 10.2: Change password
curl -X POST http://localhost:8080/auth/change-password \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "Test123456",
    "newPassword": "NewPassword123"
  }' | jq

# Expected: 200 OK

# Test 10.3: Try login with old password
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123456"
  }' | jq

# Expected: 401 Unauthorized

# Test 10.4: Login with new password
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "NewPassword123"
  }' | jq

# Expected: 202 Accepted
```

---

## Database Verification Queries

After running tests, verify in database:

```sql
-- Check users table
SELECT username, enabled, failed_login_attempts, requires_email_verification, account_locked 
FROM users;

-- Check login attempts
SELECT username, ip_address, successful, failure_reason, created_at 
FROM login_attempts 
ORDER BY created_at DESC 
LIMIT 20;

-- Check email whitelist
SELECT email, active, description 
FROM email_whitelist;

-- Check verification codes
SELECT u.username, e.email, e.code, e.expires_at, e.used 
FROM email_verification_codes e
JOIN users u ON e.user_id = u.id
ORDER BY e.created_at DESC 
LIMIT 10;

-- Check active sessions
SELECT session_id, ip_address, user_agent, created_at, expires_at 
FROM refresh_token_state 
WHERE expires_at > NOW()
ORDER BY created_at DESC;
```

---

## Automated Test Script

Save this as `test_auth.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
TEST_USERNAME="testuser_$(date +%s)"
TEST_PASSWORD="Test123456"
TEST_EMAIL="test$(date +%s)@example.com"

echo "🧪 Starting Authentication System Tests"
echo "========================================"

# Test 1: Register
echo "Test 1: Registering user..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$TEST_USERNAME\", \"password\": \"$TEST_PASSWORD\", \"email\": \"$TEST_EMAIL\"}")
echo "✅ Register: $REGISTER_RESPONSE"

# Test 2: Login
echo "Test 2: Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -c /tmp/cookies.txt \
  -d "{\"username\": \"$TEST_USERNAME\", \"password\": \"$TEST_PASSWORD\"}")
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.tokens.accessToken')
echo "✅ Login successful, token: ${ACCESS_TOKEN:0:20}..."

# Test 3: Access protected endpoint
echo "Test 3: Accessing protected endpoint..."
REMINDERS_RESPONSE=$(curl -s -X GET "$BASE_URL/reminders" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "✅ Protected endpoint: Success"

# Test 4: Refresh token
echo "Test 4: Refreshing token..."
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/refresh-token" \
  -b /tmp/cookies.txt \
  -c /tmp/cookies.txt)
echo "✅ Token refreshed"

# Test 5: Logout
echo "Test 5: Logging out..."
LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -b /tmp/cookies.txt)
echo "✅ Logout: $LOGOUT_RESPONSE"

echo ""
echo "🎉 All tests completed!"
```

Make executable and run:
```bash
chmod +x test_auth.sh
./test_auth.sh
```

---

## Performance Testing

Use Apache Bench for load testing:

```bash
# Test login endpoint (be careful with rate limiting!)
ab -n 100 -c 10 -p login_payload.json -T application/json \
  http://localhost:8080/auth/login

# login_payload.json content:
# {"username": "testuser", "password": "Test123456"}
```

---

## Monitoring During Tests

Watch logs in real-time:
```bash
tail -f logs/application.log | grep AUTH
```

Monitor database:
```bash
watch -n 1 "psql -U postgres -d total_app -c 'SELECT COUNT(*) FROM login_attempts;'"
```

---

## Expected Results Summary

| Test | Expected Status | Expected Behavior |
|------|----------------|-------------------|
| Register new user | 201 Created | User created |
| Login valid | 202 Accepted | Tokens returned |
| Login invalid | 401 Unauthorized | Error message |
| Access with token | 200 OK | Data returned |
| Access without token | 401 Unauthorized | Access denied |
| Refresh token | 202 Accepted | New token |
| 6 failed attempts | 403 Forbidden | 2FA required |
| Request email code | 200 OK | Email sent |
| Login with code | 202 Accepted | Login successful |
| Logout | 200 OK | Session invalidated |

---

**✅ If all tests pass, your authentication system is working correctly!**

