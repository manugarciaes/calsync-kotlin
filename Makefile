.PHONY: dev prod down clean logs

# Start development environment
dev:
	docker-compose -f docker-compose-dev.yml up --build

# Start production environment
prod:
	docker-compose up --build

# Stop all containers
down:
	docker-compose -f docker-compose-dev.yml down
	docker-compose down

# Clean volumes and containers
clean:
	docker-compose -f docker-compose-dev.yml down -v
	docker-compose down -v

# View backend logs
logs:
	docker-compose logs -f backend

# Initialize database
init-db:
	docker-compose exec postgres psql -U postgres -d calsync -f /app/schema.sql