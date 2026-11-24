"""FastAPI application entrypoint for Math Notebook AI Tutor backend."""

from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from backend.server.ws import handle_websocket

app = FastAPI(
    title="Math Notebook AI Tutor Backend",
    version="0.1.0",
    description="WebSocket server providing canvas action feedback to Android tablet client.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health():
    """Healthcheck endpoint."""
    return {"status": "ok", "version": "0.1.0"}


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """Primary WebSocket endpoint for canvas interaction."""
    await handle_websocket(websocket)
