# WebSocket Protocol Specification (Version 1)

This document specifies the bidirectional WebSocket communication protocol between the Android tablet client and the math notebook backend server.

---

## 1. Message Envelope

Every WebSocket message exchanged between client and server MUST adhere to this envelope schema:

```json
{
  "v": 1,
  "type": "<string>",
  "id": "<uuid>",
  "session_id": "<string>",
  "page_rev": <int>,
  "payload": { ... }
}
```

### Envelope Fields

| Field | Type | Description |
|---|---|---|
| `v` | Integer | Protocol version. Constant `1`. |
| `type` | String | Message identifier (enum of valid types). |
| `id` | String (UUID) | Unique identifier for this message. |
| `session_id` | String | Active client session token / session ID. |
| `page_rev` | Integer | Page revision counter at the time the message was created (`>= 0`). |
| `payload` | Object | Type-specific data conforming to the respective payload schema. |

---

## 2. Core Protocol Rules

1. **Strict Anchor Rule**:
   Every tutor action (`action.underline`, `action.sticky_note`) **MUST** anchor to a specific `line_id` **PLUS** a `fallback_bbox`. Actions must **never** be defined by raw spatial coordinates alone.
2. **Revision Stamping Rule**:
   Every tutor action **MUST** include the `page_rev` integer against which it was computed. When the client receives an action, it compares the action's `page_rev` with the page's current revision counter:
   - If `current_rev == action.page_rev`: apply action directly.
   - If `current_rev > action.page_rev`: check if anchored `line_id` still exists; if modified or removed, the action is recognized as stale and discarded.
3. **Single Source of Truth**:
   The authoritative schemas reside in `/protocol/*.schema.json`.

---

## 3. Client → Server Messages

### 3.1 `region.submit`
Sent when a user writes on the canvas or triggers verification of a bounding region.

**Payload:**
```json
{
  "lines": [
    {
      "id": "22222222-2222-2222-2222-222222222222",
      "stroke_ids": ["33333333-3333-3333-3333-333333333333"],
      "bounding_box": {
        "min_x": 100.0,
        "min_y": 150.0,
        "max_x": 350.0,
        "max_y": 200.0
      },
      "recognition": {
        "latex": "2x + 5 = 15",
        "confidence": 0.98,
        "symbols": [
          { "symbol": "2", "confidence": 0.99 },
          { "symbol": "x", "confidence": 0.97 }
        ]
      }
    }
  ],
  "bounding_box": {
    "min_x": 90.0,
    "min_y": 140.0,
    "max_x": 360.0,
    "max_y": 210.0
  },
  "trigger": "manual"
}
```

### 3.2 `lasso.submit`
Sent when the user circles one or more lines/strokes using a lasso tool.

**Payload:**
```json
{
  "line_ids": [
    "22222222-2222-2222-2222-222222222222"
  ],
  "bounding_box": {
    "min_x": 80.0,
    "min_y": 130.0,
    "max_x": 370.0,
    "max_y": 220.0
  },
  "polygon_points": [
    { "x": 80.0, "y": 130.0 },
    { "x": 370.0, "y": 130.0 }
  ]
}
```

### 3.3 `cancel`
Cancels an ongoing processing task.

**Payload:**
```json
{
  "target_id": "11111111-1111-1111-1111-111111111111",
  "reason": "User navigated away"
}
```

### 3.4 `ping`
Heartbeat check.

**Payload:**
```json
{
  "timestamp": 1700000000000
}
```

---

## 4. Server → Client Messages

### 4.1 `status`
Informs the client about ongoing verification/tutor agent steps.

**Payload:**
```json
{
  "code": "analyzing",
  "message": "AI tutor is checking the algebraic steps..."
}
```
Valid `code` values: `"idle"`, `"processing"`, `"analyzing"`, `"done"`.

### 4.2 `action.underline`
Directs the client to render an underline marking a mathematical error or observation.

**Payload:**
```json
{
  "action_id": "44444444-4444-4444-4444-444444444444",
  "page_rev": 3,
  "line_id": "22222222-2222-2222-2222-222222222222",
  "fallback_bbox": {
    "min_x": 100.0,
    "min_y": 150.0,
    "max_x": 350.0,
    "max_y": 200.0
  },
  "color": "#E53935",
  "style": "wavy",
  "explanation": "Sign error when moving 5 to right-hand side"
}
```
Valid `style` values: `"solid"`, `"wavy"`, `"dashed"`.

### 4.3 `action.sticky_note`
Places an interactive or expandable hint / pedagogical note near a line.

**Payload:**
```json
{
  "action_id": "55555555-5555-5555-5555-555555555555",
  "page_rev": 3,
  "line_id": "22222222-2222-2222-2222-222222222222",
  "fallback_bbox": {
    "min_x": 370.0,
    "min_y": 140.0,
    "max_x": 520.0,
    "max_y": 240.0
  },
  "content": "Remember to subtract 5 from both sides: 15 - 5 = 10.",
  "color": "#FFF9C4"
}
```

### 4.4 `action.dismiss`
Dismisses an action that is no longer applicable (e.g., student corrected the error).

**Payload:**
```json
{
  "action_id": "44444444-4444-4444-4444-444444444444"
}
```

### 4.5 `error`
Reports an error (e.g. invalid payload, unrecognized type, or internal failure).

**Payload:**
```json
{
  "code": "INVALID_PAYLOAD",
  "message": "Field 'lines' is required in region.submit payload",
  "details": "Schema validation failed at path /payload/lines"
}
```

### 4.6 `pong`
Heartbeat response echoing or acknowledging `ping`.

**Payload:**
```json
{
  "timestamp": 1700000000045
}
```
