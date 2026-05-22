#!/bin/bash
set -e

echo "🚀 Starting SmartCart AI infrastructure..."

# Start infrastructure
docker-compose up -d postgres redis rabbitmq

echo "⏳ Waiting for services to be healthy..."
sleep 15

echo "✅ Infrastructure ready!"
echo "  PostgreSQL: localhost:5432"
echo "  Redis:      localhost:6379"
echo "  RabbitMQ:   localhost:5672 (Management: http://localhost:15672)"
echo ""
echo "Credentials: smartcart / smartcart_secret"
