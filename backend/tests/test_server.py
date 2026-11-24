"""Unit and integration tests for FastAPI WebSocket endpoint and stub behaviors."""

import json
import uuid
import pytest
from fastapi.testclient import TestClient

from backend.server.app import app


@pytest.fixture
def client():
    return TestClient(app)


def test_healthcheck(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "version": "0.1.0"}


def test_ws_ping_pong(client):
    """Sending a ping must return a pong."""
    with client.websocket_connect("/ws") as websocket:
        ping_msg = {
            "v": 1,
            "type": "ping",
            "id": str(uuid.uuid4()),
            "session_id": "test-session",
            "page_rev": 2,
            "payload": {"timestamp": 123456789},
        }
        websocket.send_text(json.dumps(ping_msg))
        raw_response = websocket.receive_text()
        response = json.loads(raw_response)

        assert response["v"] == 1
        assert response["type"] == "pong"
        assert response["session_id"] == "test-session"
        assert response["page_rev"] == 2
        assert response["payload"]["timestamp"] == 123456789


def test_ws_region_submit_stub(client):
    """
    On region.submit:
    1. Server replies with status (analyzing)
    2. Server replies with action.underline anchored to the first line_id
    """
    with client.websocket_connect("/ws") as websocket:
        target_line_id = str(uuid.uuid4())
        region_msg = {
            "v": 1,
            "type": "region.submit",
            "id": str(uuid.uuid4()),
            "session_id": "session-abc",
            "page_rev": 5,
            "payload": {
                "lines": [
                    {
                        "id": target_line_id,
                        "stroke_ids": [str(uuid.uuid4())],
                        "bounding_box": {
                            "min_x": 10.0,
                            "min_y": 20.0,
                            "max_x": 150.0,
                            "max_y": 60.0,
                        },
                        "recognition": {
                            "latex": "3x + 2 = 11",
                            "confidence": 0.95,
                        },
                    }
                ],
                "bounding_box": {
                    "min_x": 10.0,
                    "min_y": 20.0,
                    "max_x": 150.0,
                    "max_y": 60.0,
                },
                "trigger": "manual",
            },
        }
        websocket.send_text(json.dumps(region_msg))

        # 1. First message: status
        msg1 = json.loads(websocket.receive_text())
        assert msg1["v"] == 1
        assert msg1["type"] == "status"
        assert msg1["page_rev"] == 5
        assert msg1["payload"]["code"] == "analyzing"

        # 2. Second message: action.underline
        msg2 = json.loads(websocket.receive_text())
        assert msg2["v"] == 1
        assert msg2["type"] == "action.underline"
        assert msg2["page_rev"] == 5
        payload = msg2["payload"]
        assert payload["line_id"] == target_line_id
        assert payload["fallback_bbox"]["min_x"] == 10.0
        assert payload["color"] == "#E53935"
        assert payload["style"] == "wavy"


def test_ws_unknown_type(client):
    """Unknown message type should return an error message."""
    with client.websocket_connect("/ws") as websocket:
        bad_msg = {
            "v": 1,
            "type": "custom.unknown.type",
            "id": str(uuid.uuid4()),
            "session_id": "session-1",
            "page_rev": 0,
            "payload": {},
        }
        websocket.send_text(json.dumps(bad_msg))
        resp = json.loads(websocket.receive_text())
        assert resp["type"] == "error"
        assert resp["payload"]["code"] in ("INVALID_ENVELOPE", "UNKNOWN_TYPE")


def test_ws_invalid_payload(client):
    """region.submit without required lines must return INVALID_PAYLOAD error."""
    with client.websocket_connect("/ws") as websocket:
        bad_msg = {
            "v": 1,
            "type": "region.submit",
            "id": str(uuid.uuid4()),
            "session_id": "session-1",
            "page_rev": 1,
            "payload": {
                "bounding_box": {"min_x": 0, "min_y": 0, "max_x": 10, "max_y": 10}
                # Missing 'lines'
            },
        }
        websocket.send_text(json.dumps(bad_msg))
        resp = json.loads(websocket.receive_text())
        assert resp["type"] == "error"
        assert resp["payload"]["code"] == "INVALID_PAYLOAD"
