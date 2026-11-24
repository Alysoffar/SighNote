"""Contract tests verifying /protocol/fixtures against JSON Schemas and Pydantic models."""

import json
import glob
import os
import pytest
from referencing import Registry, Resource
from jsonschema import Draft7Validator

from backend.protocol.models import Envelope


PROTOCOL_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "..", "protocol")
)


@pytest.fixture(scope="session")
def schema_validator():
    """Builds a Draft7Validator with all local schema resources registered."""
    registry = Registry()
    for root, _, files in os.walk(PROTOCOL_DIR):
        for f in files:
            if f.endswith(".schema.json"):
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, PROTOCOL_DIR).replace("\\", "/")
                with open(full_path, "r", encoding="utf-8") as sf:
                    schema_doc = json.load(sf)
                resource = Resource.from_contents(schema_doc)
                registry = registry.with_resource(uri=rel_path, resource=resource)
                registry = registry.with_resource(uri=f, resource=resource)

    root_path = os.path.join(PROTOCOL_DIR, "message.schema.json")
    with open(root_path, "r", encoding="utf-8") as f:
        root_schema = json.load(f)

    return Draft7Validator(root_schema, registry=registry)


def get_fixtures(subfolder: str):
    folder = os.path.join(PROTOCOL_DIR, "fixtures", subfolder)
    return sorted(glob.glob(os.path.join(folder, "*.json")))


@pytest.mark.parametrize("filepath", get_fixtures("valid"))
def test_valid_fixtures_pass_json_schema(schema_validator, filepath):
    """Every fixture in protocol/fixtures/valid must pass schema validation."""
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    errors = list(schema_validator.iter_errors(data))
    assert not errors, f"Fixture {os.path.basename(filepath)} failed schema validation: {[e.message for e in errors]}"


@pytest.mark.parametrize("filepath", get_fixtures("valid"))
def test_valid_fixtures_parse_pydantic_envelope(filepath):
    """Every fixture in protocol/fixtures/valid must be parsed by Pydantic Envelope."""
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    envelope = Envelope.model_validate(data)
    assert envelope.v == 1
    assert envelope.type in (
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
    )


@pytest.mark.parametrize("filepath", get_fixtures("invalid"))
def test_invalid_fixtures_rejected_by_json_schema(schema_validator, filepath):
    """Every fixture in protocol/fixtures/invalid must be rejected by schema validation."""
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    errors = list(schema_validator.iter_errors(data))
    assert len(errors) > 0, f"Invalid fixture {os.path.basename(filepath)} was unexpectedly accepted by schema!"
