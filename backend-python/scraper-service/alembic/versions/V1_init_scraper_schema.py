"""init scraper schema

Revision ID: 001
Revises:
Create Date: 2025-01-01 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = '001'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'scraper_configs',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('domain', sa.String(255), nullable=False, unique=True),
        sa.Column('name', sa.String(255), nullable=False),
        sa.Column('config', postgresql.JSONB, nullable=False),
        sa.Column('status', sa.String(20), default='ACTIVE'),
        sa.Column('is_active', sa.Boolean, default=True),
        sa.Column('success_count', sa.Integer, default=0),
        sa.Column('fail_count', sa.Integer, default=0),
        sa.Column('created_by', sa.String(20), default='ADMIN'),
        sa.Column('last_used_at', sa.DateTime, nullable=True),
        sa.Column('created_at', sa.DateTime, server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime, server_default=sa.func.now()),
    )
    op.create_table(
        'scrape_jobs',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('url', sa.String(1000), nullable=False),
        sa.Column('domain', sa.String(255), nullable=False),
        sa.Column('scraper_tier', sa.String(20), nullable=True),
        sa.Column('status', sa.String(20), default='PENDING'),
        sa.Column('discover_sellers', sa.Boolean, default=False),
        sa.Column('sellers_found', sa.Integer, default=0),
        sa.Column('retry_count', sa.Integer, default=0),
        sa.Column('max_retries', sa.Integer, default=3),
        sa.Column('error_message', sa.Text, nullable=True),
        sa.Column('raw_data', postgresql.JSONB, nullable=True),
        sa.Column('started_at', sa.DateTime, nullable=True),
        sa.Column('completed_at', sa.DateTime, nullable=True),
        sa.Column('created_at', sa.DateTime, server_default=sa.func.now()),
    )
    op.create_index('idx_jobs_status', 'scrape_jobs', ['status'])

    # Seed Tier 2 configs
    op.execute("""
    INSERT INTO scraper_configs (id, domain, name, config, status) VALUES
    (gen_random_uuid(), 'thegioididong.com', 'Thế Giới Di Động', '{"selectors":{"product_name":"h1.product-name","price":".box-price .product-price","original_price":".box-price .old-price","specs":".parameter-list li","images":".gallery-image img"},"type":"static"}', 'ACTIVE'),
    (gen_random_uuid(), 'fptshop.com.vn', 'FPT Shop', '{"selectors":{"product_name":"h1.product-name","price":".product-price","specs":".specifi li","images":".owl-item img"},"type":"static"}', 'ACTIVE'),
    (gen_random_uuid(), 'cellphones.com.vn', 'CellphoneS', '{"selectors":{"product_name":"h1.product-name","price":".tpt-price","specs":".tech-content li","images":".product-gallery img"},"type":"dynamic"}', 'ACTIVE'),
    (gen_random_uuid(), 'phongvu.vn', 'Phong Vũ', '{"selectors":{"product_name":"h1","price":".product-price","specs":".specification tr","images":".product-images img"},"type":"static"}', 'ACTIVE'),
    (gen_random_uuid(), 'gearvn.com', 'GearVN', '{"selectors":{"product_name":"h1.product_title","price":".woocommerce-Price-amount","specs":".woocommerce-product-attributes tr","images":".product-thumbnail img"},"type":"static"}', 'ACTIVE')
    """)


def downgrade() -> None:
    op.drop_table('scrape_jobs')
    op.drop_table('scraper_configs')
