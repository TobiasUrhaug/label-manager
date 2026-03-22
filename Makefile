.PHONY: build test test-js test-e2e start

# Build backend
build:
	cd backend && ./gradlew build

# Run all backend tests
test:
	cd backend && ./gradlew test

# Run backend checkstyle
lint:
	cd backend && ./gradlew checkstyleMain checkstyleTest

# Run JS unit tests (Thymeleaf static JS — temporary, removed when migration is complete)
test-js:
	cd backend && npm run test

# Run frontend dev server
start-frontend:
	cd frontend && npm run dev

# Run backend dev server
start-backend:
	cd backend && ./gradlew bootRun

# Run both dev servers concurrently (requires two terminals, or use & for background)
start:
	cd backend && ./gradlew bootRun &
	cd frontend && npm run dev

# Run e2e tests (starts backend automatically if targeting localhost)
test-e2e:
	cd e2e && npm run test

# Install all npm dependencies
install:
	cd backend && npm install
	cd frontend && npm install
	cd e2e && npm install
