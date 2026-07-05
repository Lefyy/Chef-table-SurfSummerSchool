# Шеф-стол

MVP web application foundation for client booking of cooking classes.

## Stack

- Kotlin + Spring Boot
- Spring MVC, Spring Security, Thymeleaf
- Spring Data JPA + PostgreSQL
- Flyway migrations
- TailwindCSS + Alpine.js
- Docker Compose

## Local development

```bash
docker compose up -d postgres
./gradlew bootRun --args='--spring.profiles.active=local'
```

If a Gradle wrapper is not generated yet, use the system Gradle:

```bash
gradle bootRun --args='--spring.profiles.active=local'
```

## CSS build

```bash
npm install
npm run build:css
```

## Tests

```bash
gradle test
```

## Docker

```bash
docker compose up --build
```
