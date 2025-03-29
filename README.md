# CalSync - Calendar Synchronization Application

CalSync is a robust calendar synchronization application built with Kotlin and Ktor that aggregates ICS calendars from various sources and provides shareable views of free slots.

## Features

- **Calendar Synchronization**: Import calendars from URLs or uploaded ICS files
- **Free Slot Management**: Create customizable free slots based on your availability
- **Public Booking**: Share your availability with public links for easy booking
- **Email Notifications**: Automatic notifications for bookings and cancellations
- **Multi-language Support**: Internationalization for global users

## Technology Stack

- **Backend**: Kotlin, Ktor
- **Database**: PostgreSQL
- **Architecture**: Domain-Driven Design
- **Concurrency**: Kotlin Coroutines
- **Scheduling**: Background processing for calendar updates
- **Authentication**: Secure user authentication and authorization

## Project Structure

The application follows a clean DDD architecture with the following layers:

- **Domain Layer**: Core business models and repository interfaces
- **Application Layer**: Services implementing business logic
- **Infrastructure Layer**: Repository implementations and external integrations
- **Interface Layer**: API endpoints and controllers

## Setup and Configuration

### Prerequisites

- JDK 11+
- PostgreSQL 12+
- Docker (optional for containerized deployment)

### Environment Variables

The application uses the following environment variables:

```
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/calsync
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
MAIL_FROM_NAME=CalSync

# Calendar Sync Configuration
CALENDAR_SYNC_INTERVAL_MINUTES=15
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.