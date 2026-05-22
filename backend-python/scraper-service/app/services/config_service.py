import uuid
import json
import logging
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from fastapi import Depends

from app.config.database import get_db
from app.models import ScraperConfigModel
from shared.models import ScraperConfig

logger = logging.getLogger(__name__)


class ConfigService:
    def __init__(self, db: AsyncSession):
        self._db = db

    async def get_active_config(self, domain: str) -> ScraperConfig | None:
        result = await self._db.execute(
            select(ScraperConfigModel).where(
                ScraperConfigModel.domain == domain,
                ScraperConfigModel.is_active == True,
                ScraperConfigModel.status == "ACTIVE",
            )
        )
        model = result.scalar_one_or_none()
        if not model:
            return None
        cfg = model.config
        return ScraperConfig(
            id=str(model.id),
            domain=model.domain,
            name=model.name,
            selectors=cfg.get("selectors", {}),
            type=cfg.get("type", "static"),
            pagination=cfg.get("pagination"),
        )

    async def list_all(self) -> list[dict]:
        result = await self._db.execute(select(ScraperConfigModel).order_by(ScraperConfigModel.domain))
        return [self._to_dict(m) for m in result.scalars().all()]

    async def list_suggestions(self) -> list[dict]:
        result = await self._db.execute(
            select(ScraperConfigModel).where(ScraperConfigModel.status == "AI_GENERATED")
        )
        return [self._to_dict(m) for m in result.scalars().all()]

    async def create(self, request) -> dict:
        model = ScraperConfigModel(
            id=uuid.uuid4(),
            domain=request.domain,
            name=request.name,
            config={"selectors": request.selectors, "type": request.type, "pagination": request.pagination},
            status="ACTIVE",
            created_by="ADMIN",
        )
        self._db.add(model)
        await self._db.commit()
        await self._db.refresh(model)
        return self._to_dict(model)

    async def update(self, config_id: str, request) -> dict | None:
        model = await self._db.get(ScraperConfigModel, uuid.UUID(config_id))
        if not model:
            return None
        model.domain = request.domain
        model.name = request.name
        model.config = {"selectors": request.selectors, "type": request.type, "pagination": request.pagination}
        await self._db.commit()
        return self._to_dict(model)

    async def approve(self, config_id: str) -> dict | None:
        model = await self._db.get(ScraperConfigModel, uuid.UUID(config_id))
        if not model:
            return None
        model.status = "ACTIVE"
        model.is_active = True
        await self._db.commit()
        logger.info(f"Approved AI-generated config for {model.domain}")
        return self._to_dict(model)

    async def save_ai_suggestion(self, domain: str, name: str, selectors: dict):
        """Save auto-generated config from Tier 3 as AI_GENERATED for admin review."""
        existing = await self._db.execute(
            select(ScraperConfigModel).where(ScraperConfigModel.domain == domain)
        )
        if existing.scalar_one_or_none():
            return  # Already have a config for this domain

        model = ScraperConfigModel(
            id=uuid.uuid4(),
            domain=domain,
            name=name,
            config={"selectors": selectors, "type": "static"},
            status="AI_GENERATED",
            is_active=False,
            created_by="AI",
        )
        self._db.add(model)
        await self._db.commit()

    def _to_dict(self, model: ScraperConfigModel) -> dict:
        return {
            "id": str(model.id),
            "domain": model.domain,
            "name": model.name,
            "config": model.config,
            "status": model.status,
            "is_active": model.is_active,
            "success_count": model.success_count,
            "fail_count": model.fail_count,
            "created_by": model.created_by,
            "last_used_at": model.last_used_at.isoformat() if model.last_used_at else None,
            "created_at": model.created_at.isoformat() if model.created_at else None,
        }


async def get_config_service(db: AsyncSession = Depends(get_db)) -> ConfigService:
    return ConfigService(db)
