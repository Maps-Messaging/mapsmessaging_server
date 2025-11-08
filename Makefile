# MAPS Messaging Server - Authentication Setup Makefile

.PHONY: help start stop restart logs test setup clean status

# Default target
help:
	@echo "MAPS Messaging Server - Authentication Setup"
	@echo ""
	@echo "Available targets:"
	@echo "  start     - Start the MAPS server with authentication"
	@echo "  stop      - Stop the MAPS server"
	@echo "  restart   - Restart the MAPS server"
	@echo "  logs      - Show server logs"
	@echo "  status    - Show container status"
	@echo "  setup     - Setup default admin user"
	@echo "  test      - Test authentication configuration"
	@echo "  clean     - Remove containers and volumes"
	@echo "  help      - Show this help message"
	@echo ""
	@echo "Quick start: make start && make setup && make test"

# Start the server
start:
	@echo "Starting MAPS Messaging Server..."
	docker-compose up -d
	@echo "Server starting... (wait 30-60 seconds for full startup)"
	@echo "Check status with: make status"
	@echo "View logs with: make logs"

# Stop the server
stop:
	@echo "Stopping MAPS Messaging Server..."
	docker-compose down

# Restart the server
restart: stop start

# Show logs
logs:
	docker-compose logs -f maps-messaging

# Show container status
status:
	@echo "Container Status:"
	docker-compose ps
	@echo ""
	@echo "Server Health Check:"
	@curl -s http://localhost:8080/api/v1/ping 2>/dev/null && echo "✅ Server is responding" || echo "❌ Server is not responding"

# Setup admin user
setup:
	@echo "Setting up default admin user..."
	./docker-config/setup-admin.sh

# Test authentication
test:
	@echo "Testing authentication configuration..."
	./docker-config/test-auth.sh

# Clean up containers and volumes
clean:
	@echo "Removing containers and volumes..."
	docker-compose down -v
	docker system prune -f

# Full setup and test
all: start setup test
	@echo ""
	@echo "🎉 MAPS Server setup complete!"
	@echo "Access the admin UI at: http://localhost:8080"
	@echo "Username: admin"
	@echo "Password: admin123"
	@echo ""
	@echo "⚠️  Remember to change default passwords in production!"