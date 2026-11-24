"""Pydantic models matching the /protocol JSON schemas and domain data models."""

from __future__ import annotations
from typing import Any, List, Literal, Optional, Union
from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field


# ---------------------------------------------------------------------------
# Domain Data Models (Page-space)
# ---------------------------------------------------------------------------

class BoundingBox(BaseModel):
    model_config = ConfigDict(extra="forbid")
    min_x: float
    min_y: float
    max_x: float
    max_y: float


class StrokePoint(BaseModel):
    model_config = ConfigDict(extra="forbid")
    x: float
    y: float
    t: int = Field(ge=0, description="Timestamp in milliseconds")
    pressure: float = Field(ge=0.0, le=1.0)
    tilt: float = Field(default=0.0, description="Tilt in degrees")


class Stroke(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: UUID
    brush_id: str = Field(min_length=1)
    points: List[StrokePoint]
    bounds: BoundingBox


class SymbolConfidence(BaseModel):
    model_config = ConfigDict(extra="forbid")
    symbol: str
    confidence: float = Field(ge=0.0, le=1.0)


class RecognitionResult(BaseModel):
    model_config = ConfigDict(extra="forbid")
    latex: str
    confidence: float = Field(ge=0.0, le=1.0)
    symbols: Optional[List[SymbolConfidence]] = None


class Line(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: UUID
    stroke_ids: List[UUID]
    bounding_box: BoundingBox
    recognition: Optional[RecognitionResult] = None


class Problem(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: UUID
    line_ids: List[UUID]


class Page(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: UUID
    notebook_id: UUID
    revision_counter: int = Field(ge=0)
    strokes: List[Stroke] = Field(default_factory=list)
    lines: List[Line] = Field(default_factory=list)
    problems: List[Problem] = Field(default_factory=list)


class Notebook(BaseModel):
    model_config = ConfigDict(extra="forbid")
    id: UUID
    title: str
    pages: List[Page] = Field(default_factory=list)


# ---------------------------------------------------------------------------
# WebSocket Payloads
# ---------------------------------------------------------------------------

class RegionSubmitPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    lines: List[Line]
    bounding_box: BoundingBox
    trigger: Optional[Literal["manual", "auto", "selection"]] = "manual"


class Point2D(BaseModel):
    model_config = ConfigDict(extra="forbid")
    x: float
    y: float


class LassoSubmitPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    line_ids: List[UUID]
    bounding_box: BoundingBox
    polygon_points: Optional[List[Point2D]] = None


class CancelPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    target_id: UUID
    reason: Optional[str] = None


class PingPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    timestamp: Optional[int] = Field(default=None, ge=0)


class StatusPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    code: Literal["idle", "processing", "analyzing", "done"]
    message: str


class ActionUnderlinePayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    action_id: UUID
    page_rev: int = Field(ge=0)
    line_id: UUID
    fallback_bbox: BoundingBox
    color: str = Field(pattern=r"^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
    style: Literal["solid", "wavy", "dashed"]
    explanation: Optional[str] = None


class ActionStickyNotePayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    action_id: UUID
    page_rev: int = Field(ge=0)
    line_id: UUID
    fallback_bbox: BoundingBox
    content: str
    color: Optional[str] = Field(default=None, pattern=r"^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")


class ActionDismissPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    action_id: UUID


class ErrorPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    code: str
    message: str
    details: Optional[Union[str, dict]] = None


class PongPayload(BaseModel):
    model_config = ConfigDict(extra="forbid")
    timestamp: Optional[int] = Field(default=None, ge=0)


# ---------------------------------------------------------------------------
# Envelope
# ---------------------------------------------------------------------------

MessageType = Literal[
    "region.submit",
    "lasso.submit",
    "cancel",
    "ping",
    "status",
    "action.underline",
    "action.sticky_note",
    "action.dismiss",
    "error",
    "pong",
]


class Envelope(BaseModel):
    model_config = ConfigDict(extra="forbid")
    v: Literal[1] = 1
    type: MessageType
    id: UUID
    session_id: str = Field(min_length=1)
    page_rev: int = Field(ge=0)
    payload: dict[str, Any]
