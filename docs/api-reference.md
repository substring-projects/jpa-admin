# REST API reference

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

Base path: **`/admin/api`**. Entity names come from the metadata response and are looked up case-insensitively.

### Authentication endpoints

| Method | Path            | Behavior                                                                                                                                |
| ------ | --------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/auth/session` | Public session status, username when authenticated, configured title, CSRF token, and CSRF header name.                                 |
| `POST` | `/auth/login`   | Form-encoded `username` and `password`, with CSRF header and session cookie. Returns `204` on success or `401` for invalid credentials. |
| `POST` | `/auth/logout`  | Requires CSRF verification; invalidates the session and returns `204`.                                                                  |

For a non-browser client, retain cookies from the first session request, send the returned token under the returned `csrfHeader`, and fetch a fresh token after login. The token is tied to the session. Do not send JSON to the login endpoint.

### Entity endpoints

All require an authenticated admin session. Modifying requests also require CSRF verification.

| Method   | Path                           | Request / response                                                  |
| -------- | ------------------------------ | ------------------------------------------------------------------- |
| `GET`    | `/metadata`                    | Entity schemas, fields, relationships, and actions.                 |
| `GET`    | `/{entity}`                    | Paginated records with search, sort, and scalar filters.            |
| `GET`    | `/{entity}/{id}`               | One record, including supported inline data.                        |
| `POST`   | `/{entity}`                    | JSON field map; returns the created record with `201`.              |
| `PUT`    | `/{entity}/{id}`               | JSON field map; updates supplied fields and returns the record.     |
| `DELETE` | `/{entity}/{id}`               | Deletes a record; returns `204`.                                    |
| `POST`   | `/{entity}/batch-delete`       | JSON ID array, such as `[1, 2]`; returns `{"deleted": 2}`.          |
| `GET`    | `/{entity}/lookup`             | `q` and `limit`; returns an array of `{ "id": ..., "label": ... }`. |
| `POST`   | `/{entity}/actions/{actionId}` | JSON ID array; returns `success` and `result`.                      |

There are no implemented `PATCH`, CSV export, or XLSX export endpoints.

### Listing example

```http
GET /admin/api/Product?page=0&size=25&sort=name,asc&q=coffee&status=PUBLISHED
```

| Parameter         | Meaning                                                                                                                       |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `page`            | Zero-based page index; default `0`.                                                                                           |
| `size`            | Page size; controller default `25`. The UI sends its selected page size.                                                      |
| `sort`            | One property and direction, for example `name,asc` or `id,desc`.                                                              |
| `q`               | Case-insensitive substring search across designated text fields. Falls back to eligible text fields when none are designated. |
| `search`          | Alias used when `q` is absent or blank.                                                                                       |
| Other field names | Exact-value filters on recognized scalar properties. Range and multi-value filtering are not implemented.                     |

Example response shape:

```json
{
  "content": [{ "id": 1, "name": "Coffee", "status": "PUBLISHED" }],
  "totalElements": 1,
  "pageNumber": 0,
  "pageSize": 25,
  "totalPages": 1
}
```

Actual record fields depend on your entity metadata.

### Write payloads

Example product payload:

```json
{
  "name": "Coffee",
  "price": 12.5,
  "stockQuantity": 10,
  "status": "DRAFT",
  "category": { "id": 1 }
}
```

Use JSON with `Content-Type: application/json`. A single-value relation accepts an ID or an object containing `id`. Send `null` to clear an optional field. `PUT` binds supplied fields; omit sensitive fields to preserve them. Do not echo masked password values back to the API.

The frontend excludes generated IDs, computed/read-only fields, and unsupported nested fields from its payloads. Direct API clients must not assume these UI restrictions are all enforced by the binder.

### Errors

| Status | Common meaning                                                                                  |
| ------ | ----------------------------------------------------------------------------------------------- |
| `400`  | Binding, validation, unsupported-operation, or action failure handled by the entity controller. |
| `401`  | Missing/expired login or invalid credentials.                                                   |
| `403`  | Missing/invalid CSRF token or insufficient authority.                                           |
| `404`  | Entity or record not found where the service raises a not-found exception.                      |

Entity-controller errors generally use `{"error": "message"}`. Security errors may use a different response shape. See [AdminApiController](../jpa-admin-core/src/main/java/io/github/jpaadmin/web/AdminApiController.java) for the exact mappings.
