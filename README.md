# CalSync - Calendar Synchronization Platform

CalSync is a sophisticated calendar synchronization platform that aggregates ICS calendars from multiple sources, providing intelligent free slot detection and sharing capabilities.

## Features

- ICS calendar synchronization from multiple sources
- Automatic calendar updates with configurable intervals
- Free slot detection between multiple calendars
- Public URL for sharing availability
- Booking system for appointments
- Email notifications for bookings
- Multi-language support
- Modern, clean UI

## Technology Stack

- **Backend:** Kotlin with Ktor framework
- **Database:** PostgreSQL
- **Architecture:** Domain-Driven Design
- **Containerization:** Docker & Docker Compose
- **Calendar Format:** ICS (iCalendar)

## Prerequisites

- Docker and Docker Compose installed
- Git

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/manugarciaes/calsync-kotlin.git
cd calsync-kotlin
```

### Configure Environment Variables

Copy the example environment file and modify it according to your needs:

```bash
cp .env.example .env
```

Open the `.env` file and set your values:

```
# Mail configuration (required for sending booking confirmations)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
MAIL_FROM_NAME=CalSync
```

### Development Setup

For development with hot-reloading:

```bash
make dev
```

Or using Docker Compose directly:

```bash
docker-compose -f docker-compose-dev.yml up --build
```

### Production Setup

For production-like setup:

```bash
make prod
```

Or using Docker Compose directly:

```bash
docker-compose up --build
```

### Initialize Database

To create the database schema:

```bash
make init-db
```

### Stop All Services

```bash
make down
```

### Clean All Data

This will remove all containers and volumes:

```bash
make clean
```

## Accessing the Application

- **Backend API:** http://localhost:8080
- **Database Admin (Adminer):** http://localhost:8081
  - System: PostgreSQL
  - Server: postgres
  - Username: postgres
  - Password: postgres
  - Database: calsync

## Project Structure

The project follows Domain-Driven Design principles and is organized into the following layers:

- **Domain Layer:** Core business logic and entities
  - `domain/model`: Domain entities
  - `domain/repository`: Repository interfaces
- **Application Layer:** Application services and use cases
  - `application/service`: Services that implement business use cases
  - `application/scheduler`: Scheduling logic for calendar synchronization
- **Infrastructure Layer:** Implementation details
  - `infrastructure/database`: Database configuration
  - `infrastructure/repository`: Repository implementations
- **Interface Layer:** REST API endpoints
  - `interface/rest`: REST controllers
  - `interface/dto`: Data transfer objects

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.