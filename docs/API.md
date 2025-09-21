# API Documentation

This document describes the main HTTP APIs exposed by the platform through the gateway. All requests should target the Gateway base URL unless otherwise noted.

Base URL (local):

- http://localhost:18081

---

## Authentication APIs

### POST /auth/login

Authenticates user credentials and returns an access token. Also sets an HttpOnly refresh token cookie.

Request

```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}
```

Response (200 OK)

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "sub": "admin"
}
```

- Sets `Set-Cookie: refresh_token=<jwt>; HttpOnly; Path=/; Max-Age=604800; SameSite=Lax`

Errors

- 401 invalid_credentials

### POST /auth/refresh

Rotates the refresh token and returns a new access token. Requires the HttpOnly `refresh_token` cookie.

Request

```http
POST /auth/refresh
```

Response (200 OK)

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "sub": "admin"
}
```

Errors

- 401 missing_refresh_token
- 401 invalid_refresh_token

### POST /auth/logout

Clears the refresh token cookie.

Request

```http
POST /auth/logout
```

Response (200 OK)

```json
{ "ok": true }
```

---

## Health API

### GET /health

Returns the aggregated actuator health as JSON.

Response (200 OK)

```json
{
  "status": "UP",
  "components": { ... }
}
```

---

## User API (secured)

### GET /me

Returns information about the authenticated user. Requires `Authorization: Bearer <accessToken>`.

Response (200 OK)

```json
{
  "name": "admin",
  "authorities": [
    { "authority": "ROLE_USER" }
  ],
  "details": {}
}
```

Errors

- 401 if no/invalid token
- 403 if roles are insufficient for the endpoint

---

## Chat APIs (secured)

All Chat APIs are behind `/api/*` and require a valid access token.

Base path: `/api/chat`

### POST /api/chat/session

Creates a new chat session.

Request

```http
POST /api/chat/session
Content-Type: application/json
Authorization: Bearer <token>

{
  "userId": "optional-user-id"
}
```

Response (200 OK)

```json
{
  "sessionId": "d290f1ee-6c54-4b01-90e6-d701748f0851"
}
```

### POST /api/chat/message

Appends a message to a session and returns the assistant reply.

Request

```http
POST /api/chat/message
Content-Type: application/json
Authorization: Bearer <token>

{
  "sessionId": "<uuid>",
  "message": "Hello"
}
```

Response (200 OK)

```json
{
  "sessionId": "<uuid>",
  "reply": "Hi!"
}
```

Errors

- 404 if session not found

### GET /api/chat/history/{sessionId}

Returns the message history for a session.

Request

```http
GET /api/chat/history/<uuid>
Authorization: Bearer <token>
```

Response (200 OK)

```json
[
  { "role": "system", "content": "...", "timestamp": "2024-01-01T00:00:00Z" },
  { "role": "user", "content": "Hi", "timestamp": "2024-01-01T00:00:10Z"},
  { "role": "assistant", "content": "Hello!", "timestamp": "2024-01-01T00:00:11Z"}
]
```

Errors

- 404 if session not found

---

## CORS and Credentials

- `GET /health` and `/auth/*` are CORS-permitted.
- UI must send `credentials: include` when calling `/auth/refresh` and `/auth/logout` to include/clear HttpOnly cookie.
- For other APIs, send `Authorization: Bearer <accessToken>`.

---

## Error Envelope Examples

- 401 Unauthorized

```json
{ "error": "invalid_credentials" }
```

- 401 Refresh token missing/invalid

```json
{ "error": "missing_refresh_token" }
{ "error": "invalid_refresh_token" }
```

- 404 Not Found

```json
{ "message": "Session not found: <id>" }
```
