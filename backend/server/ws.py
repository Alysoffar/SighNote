"""WebSocket connection handler and message router for Math Notebook AI Tutor."""

import json
import logging
import uuid
from typing import Any, Dict
from fastapi import WebSocket, WebSocketDisconnect
from pydantic import BaseModel, ValidationError

from backend.protocol.models import (
    Envelope,
    RegionSubmitPayload,
    PingPayload,
    StatusPayload,
    ActionUnderlinePayload,
    PongPayload,
    ErrorPayload,
)

logger = logging.getLogger(__name__)


def make_envelope(
    msg_type: str,
    session_id: str,
    page_rev: int,
    payload: BaseModel | Dict[str, Any],
) -> dict:
    """Helper to build an outgoing envelope conforming to protocol v1."""
    if hasattr(payload, "model_dump"):
        payload_dict = payload.model_dump(mode="json")
    else:
        payload_dict = payload
    return {
        "v": 1,
        "type": msg_type,
        "id": str(uuid.uuid4()),
        "session_id": session_id,
        "page_rev": page_rev,
        "payload": payload_dict,
    }


async def handle_websocket(websocket: WebSocket) -> None:
    """Handles an active WebSocket connection lifecycle."""
    await websocket.accept()
    session_id = str(uuid.uuid4())
    logger.info("WebSocket connected. Assigned session_id=%s", session_id)

    try:
        while True:
            raw_text = await websocket.receive_text()
            try:
                raw_json = json.loads(raw_text)
            except json.JSONDecodeError as e:
                err_env = make_envelope(
                    msg_type="error",
                    session_id=session_id,
                    page_rev=0,
                    payload=ErrorPayload(
                        code="INVALID_JSON",
                        message=f"Could not parse message as JSON: {e}",
                    ),
                )
                await websocket.send_text(json.dumps(err_env))
                continue

            # Validate envelope
            try:
                envelope = Envelope.model_validate(raw_json)
            except ValidationError as e:
                err_env = make_envelope(
                    msg_type="error",
                    session_id=raw_json.get("session_id", session_id) if isinstance(raw_json, dict) else session_id,
                    page_rev=raw_json.get("page_rev", 0) if isinstance(raw_json, dict) else 0,
                    payload=ErrorPayload(
                        code="INVALID_ENVELOPE",
                        message="Envelope schema validation failed",
                        details=str(e),
                    ),
                )
                await websocket.send_text(json.dumps(err_env))
                continue

            session_id = envelope.session_id
            page_rev = envelope.page_rev

            # Route message types
            if envelope.type == "region.submit":
                try:
                    payload = RegionSubmitPayload.model_validate(envelope.payload)
                except ValidationError as e:
                    err_env = make_envelope(
                        msg_type="error",
                        session_id=session_id,
                        page_rev=page_rev,
                        payload=ErrorPayload(
                            code="INVALID_PAYLOAD",
                            message="Invalid region.submit payload",
                            details=str(e),
                        ),
                    )
                    await websocket.send_text(json.dumps(err_env))
                    continue

                # 1. Send status message
                status_env = make_envelope(
                    msg_type="status",
                    session_id=session_id,
                    page_rev=page_rev,
                    payload=StatusPayload(
                        code="analyzing",
                        message="AI tutor is analyzing the handwritten mathematical region...",
                    ),
                )
                await websocket.send_text(json.dumps(status_env))

                # 2. Send one hard-coded action.underline anchored to first line_id in request
                first_line = payload.lines[0] if payload.lines else None
                first_line_id = first_line.id if first_line else uuid.uuid4()
                fallback_bbox = first_line.bounding_box if first_line else payload.bounding_box

                underline_env = make_envelope(
                    msg_type="action.underline",
                    session_id=session_id,
                    page_rev=page_rev,
                    payload=ActionUnderlinePayload(
                        action_id=uuid.uuid4(),
                        page_rev=page_rev,
                        line_id=first_line_id,
                        fallback_bbox=fallback_bbox,
                        color="#E53935",
                        style="wavy",
                        explanation="Check algebraic manipulation here: possible sign change error.",
                    ),
                )
                await websocket.send_text(json.dumps(underline_env))

            elif envelope.type == "ping":
                try:
                    payload = PingPayload.model_validate(envelope.payload)
                except ValidationError as e:
                    err_env = make_envelope(
                        msg_type="error",
                        session_id=session_id,
                        page_rev=page_rev,
                        payload=ErrorPayload(
                            code="INVALID_PAYLOAD",
                            message="Invalid ping payload",
                            details=str(e),
                        ),
                    )
                    await websocket.send_text(json.dumps(err_env))
                    continue

                pong_env = make_envelope(
                    msg_type="pong",
                    session_id=session_id,
                    page_rev=page_rev,
                    payload=PongPayload(timestamp=payload.timestamp),
                )
                await websocket.send_text(json.dumps(pong_env))

            elif envelope.type in ("lasso.submit", "cancel"):
                # Acknowledge with status for now
                ack_env = make_envelope(
                    msg_type="status",
                    session_id=session_id,
                    page_rev=page_rev,
                    payload=StatusPayload(
                        code="idle",
                        message=f"Received {envelope.type} (stub processed)",
                    ),
                )
                await websocket.send_text(json.dumps(ack_env))

            else:
                err_env = make_envelope(
                    msg_type="error",
                    session_id=session_id,
                    page_rev=page_rev,
                    payload=ErrorPayload(
                        code="UNKNOWN_TYPE",
                        message=f"Unknown or unsupported message type: '{envelope.type}'",
                    ),
                )
                await websocket.send_text(json.dumps(err_env))

    except WebSocketDisconnect:
        logger.info("WebSocket disconnected. session_id=%s", session_id)
