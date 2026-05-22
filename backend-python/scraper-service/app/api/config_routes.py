from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel

from app.services.config_service import ConfigService, get_config_service

router = APIRouter()


class CreateConfigRequest(BaseModel):
    domain: str
    name: str
    selectors: dict[str, str]
    type: str = "static"
    pagination: dict | None = None


@router.get("/")
async def list_configs(config_service: ConfigService = Depends(get_config_service)):
    return await config_service.list_all()


@router.post("/", status_code=201)
async def create_config(request: CreateConfigRequest, config_service: ConfigService = Depends(get_config_service)):
    return await config_service.create(request)


@router.get("/suggestions")
async def list_ai_suggestions(config_service: ConfigService = Depends(get_config_service)):
    """List AI-generated configs awaiting admin review."""
    return await config_service.list_suggestions()


@router.post("/{config_id}/approve")
async def approve_config(config_id: str, config_service: ConfigService = Depends(get_config_service)):
    """Promote AI-generated config to Tier 2 (ACTIVE)."""
    config = await config_service.approve(config_id)
    if not config:
        raise HTTPException(status_code=404, detail="Config not found")
    return config


@router.put("/{config_id}")
async def update_config(config_id: str, request: CreateConfigRequest, config_service: ConfigService = Depends(get_config_service)):
    return await config_service.update(config_id, request)
