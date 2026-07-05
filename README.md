# Шеф-стол

«Шеф-стол» — MVP server-rendered веб-приложения для записи клиентов на кулинарные классы. Проект реализован как модульный монолит на Kotlin и Spring Boot: backend хранит доменные данные, применяет бизнес-правила, отдаёт REST API и рендерит клиентский интерфейс через Thymeleaf.

## Основные возможности

- Авторизация клиента по телефону и одноразовому SMS-коду; в local/dev режиме используется mock-код `1234`.
- Просмотр расписания на ближайшие 7 дней и фильтрация по уровню сложности и свободным местам.
- Просмотр карточки класса с шефом, временем, ценой и доступностью мест.
- Создание брони с выбором проката и аллергенов; выбранные аллергии сохраняются в профиле.
- Личный кабинет клиента: профиль, история бронирований, детали брони.
- Отмена активной брони более чем за 12 часов до начала класса.
- Отображение классов, отменённых студией.
- Оценка шефа после завершённого посещённого класса.
- REST API для auth, расписания, профиля, бронирований и оценок.
- Единая обработка ошибок для API и SSR, correlation id, health endpoint и настройки для безопасного логирования без SMS-кодов, токенов, комментариев и аллергенов.

## Проверки

Для проверки работоспособности проекта войдите в аккаунт с номером +79501234567 - в нем можно оценить прошедший класс и записаться на новые классы.

## Технологии

- Kotlin 2, Java 21, Spring Boot 3
- Spring MVC, Spring Security, Bean Validation, Thymeleaf
- Spring Data JPA, PostgreSQL, Flyway
- TailwindCSS, Alpine.js
- Spring Boot Actuator
- Docker Compose

## Структура проекта

```text
src/main/kotlin/ru/cheftable/
  application/        # use cases: auth, booking, profile, cancellation, rating
  config/             # security and application configuration
  domain/             # domain model declarations
  infrastructure/     # security filters, correlation id, operational logging
  persistence/        # JPA entities, repositories, link-table helpers
  web/
    api/              # REST controllers and DTOs
    error/            # API/SSR exception handlers
    page/             # Thymeleaf page controllers and forms
src/main/resources/
  db/migration/       # Flyway schema migrations
  db/local/           # local seed data
  templates/          # Thymeleaf pages
  static/css/         # Tailwind input and generated CSS
analysis/             # requirements, design and OpenAPI analysis artifacts
devepolment/          # implementation plan
```

## Конфигурация

Создайте локальный `.env` по примеру:

```bash
cp .env.example .env
```

Ключевые переменные:

- `SPRING_PROFILES_ACTIVE` — профиль запуска (`local`, `docker`, `prod`).
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — подключение к PostgreSQL.
- `CHEF_TABLE_AUTH_DEV_CODE` — mock SMS-код для локального входа.
- `CHEF_TABLE_AUTH_SECURE_COOKIE` — включает Secure-флаг cookie в production.

## Локальный запуск без Docker-приложения

Поднимите PostgreSQL:

```bash
docker compose up -d postgres
```

Запустите приложение:

```bash
gradle bootRun --args='--spring.profiles.active=local'
```

Откройте приложение: <http://localhost:8080>. Для входа запросите код на любой телефон формата `+79991234567` и введите `1234`.

## Docker-запуск

```bash
docker compose up --build
```

Compose поднимает PostgreSQL и приложение на <http://localhost:8080>. Flyway применяет миграции автоматически при старте.

## CSS

Если меняли Tailwind-классы или input CSS:

```bash
npm install
npm run build:css
```

## Health и эксплуатация

- Health endpoint: `GET /actuator/health`.
- API-ошибки возвращают единый JSON с `code`, `message` и `correlationId`.
- Каждый HTTP-ответ получает заголовок `X-Correlation-Id`; то же значение попадает в MDC логов.


