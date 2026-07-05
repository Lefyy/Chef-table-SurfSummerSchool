# План разработки MVP веб-приложения «Шеф-стол»

## 1. Краткое понимание проекта

### 1.1. Цель MVP

Необходимо разработать **полноценное backend + server-rendered web приложение с нуля** на стеке **Spring Boot + Kotlin + Thymeleaf + Alpine.js + TailwindCSS + Docker** для клиентской записи на кулинарные классы.

В отличие от варианта с интеграцией к уже существующему backend API, в этом плане backend является частью разрабатываемой системы и отвечает за:

1. хранение доменных данных;
2. реализацию REST/API-контракта из `analysis/api/openapi.yaml`;
3. бизнес-правила бронирования, отмены, проката, аллергий, оценок и авторизации;
4. SSR-страницы Thymeleaf для клиентского интерфейса;
5. безопасность, сессии, валидацию, миграции, Docker-сборку и локальный запуск.

Основные пользовательские сценарии MVP:

1. Авторизация клиента по телефону и одноразовому SMS-коду.
2. Просмотр расписания классов на ближайшие 7 дней.
3. Фильтрация расписания по описанным параметрам.
4. Просмотр деталей класса.
5. Создание брони: прокат → аллергии → сводка → подтверждение.
6. Сохранение выбранных аллергий в профиле клиента.
7. Просмотр профиля, истории броней и деталей брони.
8. Отмена активной брони клиентом более чем за 12 часов до начала класса.
9. Отображение классов/броней, отмененных студией.
10. Оценка шефа после завершенного посещенного класса.
11. Выход из системы.

### 1.2. Границы MVP

Входит в MVP:

- backend на Spring Boot + Kotlin;
- server-rendered frontend на Thymeleaf;
- минимальная интерактивность через Alpine.js;
- TailwindCSS для адаптивной верстки;
- PostgreSQL как основная БД;
- миграции через Flyway или Liquibase;
- реализация REST endpoints по OpenAPI из `analysis/api`;
- веб-страницы по экранам из `analysis/screens`;
- Dockerfile и docker-compose для локального запуска;
- тесты backend, web controllers, БД и ключевых сценариев.

Не входит в MVP, если этого нет в требованиях и OpenAPI:

- полноценная админ-панель;
- онлайн-оплата;
- промокоды;
- waitlist;
- переносы броней;
- push/email-уведомления;
- публичные отзывы;
- редактирование профиля клиента;
- свободный ввод аллергий вне справочника;
- сложный SPA frontend.

### 1.3. Главные архитектурные выводы

1. Приложение должно быть **единым модульным монолитом**, а не набором микросервисов.
2. Backend — источник истины для расписания, броней, клиентов, аллергий, проката, оценок и статусов.
3. OpenAPI нужно воспринимать как внешний контракт приложения: REST endpoints должны соответствовать спецификации.
4. Thymeleaf web UI может использовать внутренние application services напрямую, но бизнес-логика должна быть общей с REST API, чтобы не дублировать правила.
5. Alpine.js использовать только для локального поведения UI: bottom sheets, таймеры, disabled states, выбор звезд, раскрытие фильтров.
6. Все критические проверки должны выполняться на сервере, даже если дублируются в UI.
7. Данные для расписания, программ, шефов, проката и аллергенов должны появляться через миграции/seed data или подготовленный bootstrap-механизм, потому что админ-интерфейс вне MVP.

### 1.4. Допущения, которые нужно явно зафиксировать

1. SMS gateway в MVP может быть заменен dev/mock-провайдером, если реальный провайдер не предоставлен.
2. Интерфейса администратора нет, поэтому начальные программы, шефы, слоты, прокат и аллергены загружаются seed-миграциями или тестовыми fixtures.
3. Отмена класса студией должна быть представлена в доменной модели и API, но способ ее выполнения без админки для MVP может быть реализован только через seed/test data, SQL/script или минимальный internal-only maintenance mechanism, если команда согласует это отдельно.
4. Факт посещения класса, необходимый для оценки шефа, без админки должен задаваться тестовыми данными/maintenance-механизмом. Пользовательский интерфейс клиента не должен сам отмечать посещение.
5. Реальная отправка SMS, юридические тексты согласия на обработку персональных данных и production-настройки хранения ПД требуют отдельного согласования до production-релиза.

---

## 2. Предлагаемая архитектура

## 2.1. Архитектурный стиль

Рекомендуемая архитектура: **modular monolith with layered architecture**.

Слои:

1. **Web SSR layer**
   - Spring MVC controllers для Thymeleaf страниц;
   - формы;
   - view models;
   - fragments/components;
   - обработка пользовательских ошибок.

2. **REST API layer**
   - controllers, реализующие OpenAPI endpoints;
   - request/response DTO;
   - HTTP status mapping;
   - API validation.

3. **Application layer**
   - use cases: auth, schedule, booking, profile, cancellation, rating;
   - transaction boundaries;
   - orchestration;
   - вызов domain services и repositories.

4. **Domain layer**
   - entities;
   - value objects;
   - domain services;
   - business rules;
   - domain exceptions.

5. **Persistence layer**
   - Spring Data repositories;
   - JPA entities или Exposed/jOOQ, если команда выберет их;
   - DB mappings;
   - migrations.

6. **Infrastructure layer**
   - SMS provider;
   - token/session infrastructure;
   - clock/timezone;
   - logging/correlation id;
   - configuration;
   - Docker/runtime.

### 2.2. Почему не SPA и не микросервисы

1. Требования явно указывают Thymeleaf и Alpine.js, поэтому SSR должен быть основным способом рендеринга.
2. MVP небольшой по доменным границам: auth, schedule, booking, profile, rating находятся в одном bounded context клиентской записи.
3. Микросервисы усложнят транзакционность бронирования, миграции, локальный запуск и тестирование без пользы для MVP.
4. Alpine.js достаточно для интерактивных bottom sheets, таймеров и small UI state.

### 2.3. Рекомендуемая структура пакетов

```text
src/main/kotlin/<base_package>/
  ChefTableApplication.kt

  config/
    WebConfig.kt
    SecurityConfig.kt
    PersistenceConfig.kt
    JacksonConfig.kt
    LocaleFormatConfig.kt
    OpenApiConfig.kt
    SmsConfig.kt

  web/
    page/
      auth/
        AuthPageController.kt
        AuthForms.kt
        AuthPageModels.kt
      schedule/
        SchedulePageController.kt
        ScheduleForms.kt
        SchedulePageModels.kt
      slot/
        SlotPageController.kt
        SlotPageModels.kt
      booking/
        BookingFlowPageController.kt
        BookingDetailsPageController.kt
        BookingForms.kt
        BookingPageModels.kt
      profile/
        ProfilePageController.kt
        ProfilePageModels.kt
      rating/
        RatingPageController.kt
        RatingForms.kt
        RatingPageModels.kt
      error/
        PageExceptionHandler.kt
        ErrorPageModels.kt

    api/
      auth/
        AuthApiController.kt
        AuthApiDto.kt
      schedule/
        ScheduleApiController.kt
        ScheduleApiDto.kt
      booking/
        BookingApiController.kt
        BookingApiDto.kt
      profile/
        ProfileApiController.kt
        ProfileApiDto.kt
      rating/
        RatingApiController.kt
        RatingApiDto.kt
      error/
        ApiExceptionHandler.kt
        ErrorApiDto.kt

  application/
    auth/
      AuthService.kt
      SmsCodeService.kt
      SessionService.kt
    schedule/
      ScheduleQueryService.kt
      SlotQueryService.kt
      ScheduleFilter.kt
    booking/
      BookingCreationService.kt
      BookingCancellationService.kt
      BookingQueryService.kt
      BookingFlowDraftService.kt
    profile/
      ProfileService.kt
      AllergyProfileService.kt
    rating/
      RatingService.kt
    seed/
      ReferenceDataService.kt

  domain/
    client/
      Client.kt
      PhoneNumber.kt
    auth/
      SmsChallenge.kt
      AuthToken.kt
    schedule/
      Slot.kt
      Program.kt
      Chef.kt
      DifficultyLevel.kt
    booking/
      Booking.kt
      BookingStatus.kt
      PaymentStatus.kt
      BookingPolicy.kt
    rental/
      RentalItem.kt
    allergy/
      Allergen.kt
    rating/
      Rating.kt
    common/
      Money.kt
      BusinessClock.kt
      DomainException.kt

  persistence/
    entity/
      ClientEntity.kt
      SmsChallengeEntity.kt
      AuthSessionEntity.kt
      ProgramEntity.kt
      ChefEntity.kt
      SlotEntity.kt
      BookingEntity.kt
      BookingRentalItemEntity.kt
      AllergenEntity.kt
      ClientAllergenEntity.kt
      BookingAllergenEntity.kt
      RentalItemEntity.kt
      RatingEntity.kt
    repository/
      ClientRepository.kt
      SmsChallengeRepository.kt
      AuthSessionRepository.kt
      SlotRepository.kt
      ProgramRepository.kt
      ChefRepository.kt
      BookingRepository.kt
      RentalItemRepository.kt
      AllergenRepository.kt
      RatingRepository.kt
    mapper/
      PersistenceMapper.kt

  infrastructure/
    sms/
      SmsProvider.kt
      MockSmsProvider.kt
      ExternalSmsProvider.kt
    security/
      CurrentUser.kt
      AuthenticationFilter.kt
      TokenService.kt
      PasswordlessAuthenticationProvider.kt
    logging/
      CorrelationIdFilter.kt
      LogSanitizer.kt
    time/
      SystemBusinessClock.kt

src/main/resources/
  db/migration/
  templates/
    layout/
    auth/
    schedule/
    slot/
    booking/
    profile/
    rating/
    error/
    fragments/
  static/
    css/
    js/
```

### 2.4. Основные доменные сущности

#### Client

Назначение: клиент, авторизуемый по телефону.

Поля:

- `id`;
- `phone`;
- `createdAt`;
- `lastActivityAt`;
- `savedAllergies` через M:N связь.

Правила:

- телефон должен быть уникальным;
- клиент создается при первой успешной авторизации, если еще не существует;
- сохраненные аллергии обновляются через успешное бронирование.

#### SmsChallenge

Назначение: одноразовый SMS-код для passwordless login.

Поля:

- `id`;
- `phone`;
- `codeHash`;
- `expiresAt`;
- `attemptsUsed`;
- `resendCount`;
- `lastSentAt`;
- `status`.

Правила:

- код действует 5 минут;
- максимум 3 попытки ввода;
- повторная отправка не чаще 1 раза в минуту;
- максимум 5 повторных отправок;
- код не хранить в открытом виде.

#### AuthSession

Назначение: серверная авторизационная сессия/API token.

Поля:

- `id`;
- `clientId`;
- `tokenHash`;
- `createdAt`;
- `expiresAt`;
- `revokedAt`.

Правила:

- защищенные страницы и API требуют активной сессии;
- logout отзывает сессию;
- токен не логируется.

#### Program

Назначение: программа/меню класса.

Поля:

- `id`;
- `title`;
- `description`;
- `level`;
- `menuItems`;
- `needsOven`.

#### Chef

Назначение: шеф, ведущий классы.

Поля:

- `id`;
- `name`;
- `specialization`;
- `avgRating`.

Правила:

- `avgRating` пересчитывается backend после сохранения оценки.

#### Slot

Назначение: конкретный класс в расписании.

Поля:

- `id`;
- `programId`;
- `chefId`;
- `startsAt`;
- `endsAt`;
- `maxSeats`;
- `availableSeats` или вычисляемое значение;
- `address`;
- `status`;
- `studioCancellationReason`.

Правила:

- max seats: 12 или 8 при `needsOven = true`, если это правило подтверждено данными;
- слот можно бронировать только если он не отменен студией и есть свободные места;
- повторная запись на отмененный студией слот запрещена.

#### Booking

Назначение: бронь клиента на одно место в слоте.

Поля:

- `id`;
- `slotId`;
- `clientId`;
- `status`;
- `createdAt`;
- `paymentStatus`;
- `attended`;
- `rentalItems`;
- `allergies`.

Правила:

- одна бронь резервирует ровно 1 место;
- создание брони атомарно;
- создать бронь можно только при наличии свободных мест;
- отменить клиентом можно только активную бронь более чем за 12 часов до начала класса;
- статус не меняется локально без успешной транзакции;
- причина отмены клиентом не требуется.

#### RentalItem

Назначение: позиция проката.

Поля:

- `id`;
- `name`;
- `price`;
- `stockTotal`;
- `stockAvailable` или вычисляемое значение с учетом активных броней.

Правила:

- позицию с `stockAvailable = 0` выбрать нельзя;
- списание доступности происходит атомарно при создании брони;
- цена определяется backend.

#### Allergen

Назначение: справочник аллергенов.

Поля:

- `id`;
- `name`.

Правила:

- свободный ввод запрещен;
- выбор необязателен;
- выбранные аллергии сохраняются в бронь и профиль клиента.

#### Rating

Назначение: оценка шефа после посещения.

Поля:

- `id`;
- `bookingId`;
- `chefId`;
- `stars`;
- `comment`;
- `createdAt`.

Правила:

- `stars` обязателен и находится в диапазоне 1..5;
- `comment` необязателен;
- одна оценка на одну бронь;
- оценка доступна только после завершенного посещенного класса;
- повторная оценка и изменение оценки запрещены.

---

## 3. Модель БД и миграции

### 3.1. Подход к БД

Основная БД MVP — PostgreSQL. Все доменные данные, необходимые для клиентского приложения, хранятся в БД приложения.

Рекомендуемый инструмент миграций: Flyway.

Миграции должны быть атомарными и воспроизводимыми:

1. schema migrations;
2. reference data migrations;
3. test/dev seed migrations отдельно от production seed, если требуется.

### 3.2. Минимальные таблицы

1. `clients`
   - `id` PK;
   - `phone` unique;
   - `created_at`;
   - `last_activity_at`.

2. `sms_challenges`
   - `id` PK;
   - `phone`;
   - `code_hash`;
   - `expires_at`;
   - `attempts_used`;
   - `resend_count`;
   - `last_sent_at`;
   - `status`;
   - `created_at`.

3. `auth_sessions`
   - `id` PK;
   - `client_id` FK;
   - `token_hash` unique;
   - `created_at`;
   - `expires_at`;
   - `revoked_at`.

4. `programs`
   - `id` PK;
   - `title`;
   - `description`;
   - `level`;
   - `needs_oven`.

5. `program_menu_items`
   - `id` PK;
   - `program_id` FK;
   - `name`;
   - `position`.

6. `chefs`
   - `id` PK;
   - `name`;
   - `avg_rating`.

7. `chef_specializations`
   - `id` PK;
   - `chef_id` FK;
   - `name`.

8. `slots`
   - `id` PK;
   - `program_id` FK;
   - `chef_id` FK;
   - `starts_at`;
   - `ends_at`;
   - `max_seats`;
   - `address`;
   - `status`;
   - `studio_cancellation_reason`;
   - indexes by `starts_at`, `status`, `program_id`, `chef_id`.

9. `rental_items`
   - `id` PK;
   - `name`;
   - `price_amount`;
   - `stock_total`;
   - `active`.

10. `allergens`
    - `id` PK;
    - `name` unique;
    - `active`.

11. `bookings`
    - `id` PK;
    - `slot_id` FK;
    - `client_id` FK;
    - `status`;
    - `payment_status`;
    - `attended`;
    - `created_at`;
    - `cancelled_at`;
    - indexes by `client_id`, `slot_id`, `status`.

12. `booking_rental_items`
    - `booking_id` FK;
    - `rental_item_id` FK;
    - `price_amount_snapshot`;
    - composite PK.

13. `booking_allergens`
    - `booking_id` FK;
    - `allergen_id` FK;
    - composite PK.

14. `client_allergens`
    - `client_id` FK;
    - `allergen_id` FK;
    - composite PK.

15. `ratings`
    - `id` PK;
    - `booking_id` unique FK;
    - `chef_id` FK;
    - `stars`;
    - `comment`;
    - `created_at`.

16. `idempotency_keys`, если будет реализована серверная защита от повторных POST:
    - `key` PK;
    - `client_id`;
    - `operation`;
    - `request_hash`;
    - `response_snapshot`;
    - `created_at`;
    - `expires_at`.

### 3.3. Транзакционность и блокировки

Критические операции должны выполняться в транзакциях:

1. создание брони;
2. отмена брони;
3. отправка оценки и пересчет рейтинга;
4. успешная авторизация и создание клиента/сессии;
5. обновление сохраненных аллергий клиента.

Для создания брони нужно предотвратить overbooking:

- использовать pessimistic lock на `slot` при расчете свободных мест;
- или optimistic locking/version на `slot`;
- или SQL constraint/transaction check через count active bookings;
- выбранный подход должен быть покрыт тестом конкурентного бронирования.

Для проката нужно предотвратить oversell:

- учитывать только активные брони;
- проверять доступный stock в той же транзакции, где создается бронь;
- при недостатке stock возвращать бизнес-ошибку.

### 3.4. Seed data

Так как админки нет, для MVP нужны seed-данные:

1. программы/меню;
2. шефы и специализации;
3. слоты расписания;
4. позиции проката: например, набор ножей и фартук, если они описаны в требованиях;
5. справочник аллергенов;
6. тестовые брони для истории, отмены студией и оценки.

Seed-данные должны быть разделены:

- обязательный справочник аллергенов — production-safe;
- demo/test slots/bookings — только local/test profile.

---

## 4. REST API

### 4.1. Цель

Реализовать API согласно OpenAPI-спецификации из `analysis/api`, чтобы веб-интерфейс и внешние клиенты могли использовать один контракт.

### 4.2. Endpoint-группы

1. Auth:
   - `POST /auth/sms/request`;
   - `POST /auth/sms/verify`;
   - `POST /auth/logout`.

2. Schedule:
   - `GET /schedule/slots`;
   - `GET /schedule/slots/{slotId}`;
   - `GET /schedule/slots/{slotId}/booking-options`.

3. Bookings:
   - `GET /bookings`;
   - `POST /bookings`;
   - `GET /bookings/{bookingId}`;
   - `POST /bookings/{bookingId}/cancel`.

4. Profile:
   - `GET /profile`;
   - `GET /profile/allergies`.

5. Ratings:
   - `POST /ratings`.

### 4.3. Правила реализации API

1. API controllers не должны содержать бизнес-логику.
2. Все request DTO валидируются через Bean Validation и application services.
3. Ошибки возвращаются в едином формате, совместимом с OpenAPI.
4. 401 возвращается для отсутствующей/недействительной сессии.
5. 409 используется для конфликтов: нет мест, поздняя отмена, повторная оценка, отмененный слот.
6. 422/400 используется для ошибок формата и валидации.
7. 5xx не должен раскрывать stack trace.
8. Каждый API response должен быть покрыт contract/integration tests.

### 4.4. API и SSR UI

SSR controllers могут не ходить в собственный REST API по HTTP, чтобы не создавать лишний overhead. Но они должны использовать те же application services, что и API controllers.

Принцип:

```text
Thymeleaf Page Controller ┐
                          ├── Application Service ── Domain/Persistence
REST API Controller      ┘
```

Так UI и API будут иметь одинаковые бизнес-правила без дублирования.

---

## 5. Серверные страницы и UI-компоненты

### 5.1. Страницы

1. `/auth/phone` — ввод телефона.
2. `/auth/code` — ввод SMS-кода.
3. `/schedule` — расписание классов.
4. `/slots/{slotId}` — детали класса.
5. `/booking/{slotId}/equipment` — выбор проката.
6. `/booking/{slotId}/allergies` — выбор аллергий.
7. `/booking/{slotId}/summary` — итоговая сводка бронирования.
8. `/booking/success/{bookingId}` — бронь подтверждена.
9. `/profile` — профиль клиента.
10. `/profile/bookings` — история бронирований.
11. `/profile/bookings/{bookingId}` — детали брони.
12. `/profile/allergies` — мои аллергии.
13. `/ratings/{bookingId}` — оценка шефа.
14. `/session-ended` — сессия завершена.
15. `/error/server` — универсальная ошибка сервера.

### 5.2. Bottom sheets

1. Фильтры расписания.
2. Подтверждение отмены брони.
3. Уведомление о поздней отмене.
4. Уведомление об отсутствии мест.
5. Подтверждение выхода.

### 5.3. UI fragments

1. layout;
2. navigation;
3. slot card;
4. booking status badge;
5. payment status badge;
6. rental item card;
7. allergen checkbox list;
8. form errors;
9. bottom sheet wrapper;
10. server error block;
11. empty state block;
12. rating stars input.

### 5.4. Alpine.js usage

Разрешено:

- открыть/закрыть bottom sheet;
- timer для повторной отправки SMS;
- disabled/loading state кнопок;
- локальное выделение выбранных звезд;
- раскрытие/скрытие фильтров;
- косметические счетчики выбранных элементов.

Не использовать Alpine.js для:

- хранения авторизационного состояния;
- расчета бизнес-доступности бронирования;
- финальной проверки отмены;
- финальной проверки доступности проката;
- создания SPA-router.

---

## 6. Этапы разработки

## Этап 0. Подготовка проекта

### Цель

Создать базовый Spring Boot Kotlin проект и технический каркас для backend, SSR UI, БД, миграций и Docker.

### Задачи

1. Создать/настроить Gradle Kotlin DSL проект.
2. Подключить зависимости:
   - Spring Web MVC;
   - Spring Security;
   - Thymeleaf;
   - Validation;
   - Spring Data JPA или выбранный persistence stack;
   - PostgreSQL driver;
   - Flyway/Liquibase;
   - Jackson Kotlin;
   - Actuator;
   - Testcontainers;
   - JUnit 5;
   - MockMvc;
   - TailwindCSS;
   - Alpine.js.
3. Создать структуру пакетов.
4. Настроить profiles: `local`, `test`, `docker`, `prod`.
5. Настроить Tailwind build.
6. Создать базовый layout Thymeleaf.
7. Настроить Dockerfile и первичный docker-compose с PostgreSQL.
8. Добавить README с базовым запуском.

### Зависимости

Нет.

### Критерии готовности

- Приложение стартует локально.
- PostgreSQL поднимается через docker-compose.
- Миграции запускаются при старте.
- TailwindCSS собирается.
- Есть базовая страница или redirect на login.

### Риски и проверки

- Риск: выбрать слишком сложный persistence stack.
  - Проверить: команда уверенно поддерживает выбранный подход.
- Риск: frontend pipeline усложнит SSR.
  - Проверить: Tailwind генерирует static CSS, UI остается Thymeleaf-based.

---

## Этап 1. База данных, миграции и seed-данные

### Цель

Создать физическую модель данных для MVP и наполнить ее минимальными справочниками/демо-данными.

### Задачи

1. Создать миграции для таблиц:
   - clients;
   - sms_challenges;
   - auth_sessions;
   - programs;
   - program_menu_items;
   - chefs;
   - chef_specializations;
   - slots;
   - rental_items;
   - allergens;
   - bookings;
   - booking_rental_items;
   - booking_allergens;
   - client_allergens;
   - ratings;
   - idempotency_keys, если используются.
2. Добавить constraints:
   - unique phone;
   - unique rating by booking;
   - FK constraints;
   - check `stars between 1 and 5`;
   - check non-negative stock/price;
   - allowed statuses, если используются DB checks.
3. Добавить indexes для schedule/history queries.
4. Добавить seed для allergen dictionary.
5. Добавить local/test seed для программ, шефов, слотов и проката.
6. Добавить тест миграций через Testcontainers.

### Зависимости

- Этап 0.

### Критерии готовности

- Миграции применяются на чистую БД.
- Миграции повторяемы и проходят в test environment.
- Справочник аллергенов доступен.
- Есть local/test расписание для ручной проверки UI.

### Риски и проверки

- Риск: seed demo data попадает в production.
  - Проверить: разделить production-safe и local/test seed.
- Риск: отсутствуют constraints, и бизнес-правила держатся только в коде.
  - Проверить: уникальность телефона и оценки, FK и базовые check constraints есть в БД.

---

## Этап 2. Persistence и domain model

### Цель

Реализовать JPA entities/repositories и доменную модель, не смешивая persistence details с бизнес-правилами.

### Задачи

1. Создать persistence entities и repositories.
2. Создать domain entities/value objects.
3. Реализовать mappers persistence ↔ domain/read models.
4. Реализовать базовые repositories queries:
   - поиск клиента по телефону;
   - поиск активной сессии;
   - расписание по диапазону дат;
   - фильтры расписания;
   - slot by id;
   - booking by id/client;
   - booking history by client;
   - available rental items;
   - allergen dictionary;
   - rating by booking.
5. Добавить unit/integration tests на repositories.

### Зависимости

- Этап 1.

### Критерии готовности

- Все основные сущности читаются/пишутся через repositories.
- Расписание можно получить из БД за период.
- Историю клиента можно получить из БД.
- Domain model не зависит напрямую от web layer.

### Риски и проверки

- Риск: lazy loading ломает Thymeleaf views.
  - Проверить: application services формируют read models в транзакции.
- Риск: N+1 queries на расписании/истории.
  - Проверить: fetch joins/entity graphs/projections для ключевых экранов.

---

## Этап 3. Авторизация, SMS-коды и сессии

### Цель

Реализовать passwordless авторизацию по телефону и SMS-коду с серверными лимитами.

### Задачи

1. Реализовать `SmsProvider` interface.
2. Реализовать `MockSmsProvider` для local/test.
3. Реализовать `SmsCodeService`:
   - генерация 4-значного кода;
   - hash кода;
   - срок жизни 5 минут;
   - максимум 3 попытки;
   - resend не чаще 1 раза в минуту;
   - максимум 5 повторов.
4. Реализовать `AuthService`:
   - request SMS;
   - verify SMS;
   - создание клиента при первой успешной авторизации;
   - обновление `lastActivityAt`;
   - создание auth session/token.
5. Реализовать Spring Security integration.
6. Реализовать API:
   - `POST /auth/sms/request`;
   - `POST /auth/sms/verify`;
   - `POST /auth/logout`.
7. Реализовать страницы:
   - `/auth/phone`;
   - `/auth/code`;
   - `/session-ended`.
8. Добавить CSRF protection для web forms.
9. Добавить тесты:
   - request SMS;
   - verify success;
   - invalid code;
   - expired code;
   - attempts limit;
   - resend timer;
   - resend max count;
   - logout.

### Зависимости

- Этап 2.

### Критерии готовности

- Клиент может войти по телефону и SMS-коду.
- Код не хранится в открытом виде.
- Все лимиты работают серверно.
- Защищенные страницы недоступны без сессии.
- Logout отзывает сессию.

### Риски и проверки

- Риск: лимиты реализованы только в Alpine.js.
  - Проверить: все лимиты покрыты service tests.
- Риск: SMS-коды/токены попадают в логи.
  - Проверить: sanitizer и запрет body logging.
- Риск: real SMS provider не готов.
  - Проверить: mock provider не мешает подключить реальный provider через profile.

---

## Этап 4. REST API contract implementation

### Цель

Реализовать весь OpenAPI-контракт поверх application services.

### Задачи

1. Создать API DTO по OpenAPI.
2. Реализовать API controllers:
   - auth;
   - schedule;
   - bookings;
   - profile;
   - ratings.
3. Реализовать API error model.
4. Настроить OpenAPI validation или contract tests.
5. Добавить security для API через session token/bearer token, согласно спецификации.
6. Добавить integration tests на каждый endpoint:
   - happy path;
   - validation error;
   - unauthorized;
   - business conflict;
   - server error mapping.

### Зависимости

- Этап 3 для auth/security.
- Этап 2 для persistence.

### Критерии готовности

- Все endpoints из OpenAPI существуют.
- Response/request DTO соответствуют спецификации.
- Ошибки возвращаются предсказуемо и без stack trace.
- Контрактные тесты проходят.

### Риски и проверки

- Риск: SSR controllers реализуют одну логику, API другую.
  - Проверить: оба слоя вызывают одни application services.
- Риск: OpenAPI и реализация расходятся.
  - Проверить: contract tests и review по `analysis/api/openapi.yaml`.

---

## Этап 5. Расписание и фильтрация

### Цель

Реализовать получение расписания из собственной БД и отображение на SSR странице.

### Задачи

1. Реализовать `ScheduleQueryService`:
   - default period: ближайшие 7 календарных дней от текущей даты;
   - фильтры: дата/диапазон, программа/меню, шеф, уровень, наличие свободных мест, доступность проката;
   - И-логика фильтров.
2. Реализовать расчет `availableSeats`:
   - `maxSeats - count(active bookings)`;
   - исключить отмененные клиентом/студией брони, если применимо.
3. Реализовать признак доступности проката.
4. Реализовать API `GET /schedule/slots`.
5. Реализовать page `/schedule`:
   - список дней и карточек;
   - empty state «Пока нет доступных классов.»;
   - bottom sheet filters;
   - active filters indicator.
6. Добавить tests:
   - default 7 days;
   - each filter;
   - combined filters AND logic;
   - empty result;
   - available seats calculation.

### Зависимости

- Этап 2.
- Этап 3.
- Этап 4 для API contract, если выполнять строго последовательно.

### Критерии готовности

- Авторизованный клиент видит расписание.
- По умолчанию показываются ближайшие 7 дней.
- Фильтры работают совместно по И-логике.
- Карточка содержит время, программу, шефа, свободные места, духовку и прокат.

### Риски и проверки

- Риск: available seats хранится и рассинхронизируется.
  - Проверить: либо вычислять из активных броней, либо обновлять в транзакции с блокировками.
- Риск: фильтр «наличие проката» неверно учитывает stock.
  - Проверить: тест со stock 0 и stock > 0.

---

## Этап 6. Детали слота и booking options

### Цель

Реализовать просмотр деталей класса и подготовку данных для бронирования.

### Задачи

1. Реализовать `SlotQueryService`:
   - получить slot by id;
   - программа/menuItems;
   - chef/specializations/rating;
   - address/level/time;
   - available seats;
   - studio cancellation status/reason.
2. Реализовать API:
   - `GET /schedule/slots/{slotId}`;
   - `GET /schedule/slots/{slotId}/booking-options`.
3. Реализовать page `/slots/{slotId}`.
4. Реализовать conditions:
   - CTA «Забронировать» только если есть места и slot не отменен студией;
   - no seats notice;
   - studio cancellation reason.
5. Booking options:
   - rental items with stockAvailable;
   - allergen dictionary;
   - saved allergies клиента.
6. Добавить tests.

### Зависимости

- Этап 5.

### Критерии готовности

- Детали класса отображаются полностью.
- Нельзя начать бронирование отмененного студией слота.
- Нельзя начать бронирование слота без мест.
- Booking options возвращают прокат, аллергены и saved allergies.

### Риски и проверки

- Риск: детали используют устаревший slot state.
  - Проверить: каждый opening details читает БД заново.
- Риск: booking options включают недоступный прокат как выбираемый.
  - Проверить: UI disabled и server-side validation.

---

## Этап 7. Создание брони

### Цель

Реализовать атомарное создание брони со списанием места, проверкой проката и сохранением аллергий.

### Задачи

1. Реализовать `BookingCreationService` с транзакцией.
2. Алгоритм создания брони:
   - проверить client session;
   - заблокировать slot или выполнить эквивалентную concurrency-safe проверку;
   - проверить slot не отменен студией;
   - проверить `availableSeats > 0`;
   - проверить rental items существуют и доступны;
   - проверить allergens существуют в справочнике;
   - создать booking со статусом «Активна»;
   - сохранить selected rental items with price snapshot;
   - сохранить booking allergens;
   - обновить client saved allergies;
   - вернуть booking details.
3. Реализовать API `POST /bookings`.
4. Реализовать booking draft в server-side session для SSR flow.
5. Реализовать страницы:
   - equipment;
   - allergies;
   - summary;
   - success.
6. Реализовать no seats bottom sheet.
7. Реализовать защиту от double submit:
   - disabled state;
   - optional idempotency key.
8. Добавить tests:
   - successful booking;
   - no seats;
   - studio cancelled;
   - unavailable rental;
   - invalid allergen;
   - concurrent booking over capacity;
   - saved allergies update.

### Зависимости

- Этап 6.

### Критерии готовности

- Бронь создается только при наличии свободных мест.
- Одна бронь резервирует ровно 1 место.
- Недоступный прокат нельзя выбрать и нельзя отправить напрямую через POST.
- Аллергии вне справочника отклоняются.
- Saved allergies обновляются после успешной брони.
- При отказе бронь не создается частично.

### Риски и проверки

- Риск: overbooking при параллельных запросах.
  - Проверить: concurrency test через несколько transactions/threads.
- Риск: аллергии сохраняются в профиль до создания брони.
  - Проверить: update saved allergies только после successful booking transaction.
- Риск: price меняется после выбора проката.
  - Проверить: price snapshot берется на момент создания брони.

---

## Этап 8. Профиль, история, детали брони и мои аллергии

### Цель

Реализовать личный кабинет клиента на данных собственной БД.

### Задачи

1. Реализовать `ProfileService`:
   - get profile;
   - get saved allergies.
2. Реализовать `BookingQueryService`:
   - booking history by current client;
   - booking details by id with ownership check;
   - rating availability flags.
3. Реализовать API:
   - `GET /profile`;
   - `GET /profile/allergies`;
   - `GET /bookings`;
   - `GET /bookings/{bookingId}`.
4. Реализовать страницы:
   - `/profile`;
   - `/profile/bookings`;
   - `/profile/bookings/{bookingId}`;
   - `/profile/allergies`.
5. UI:
   - phone;
   - история;
   - статусы брони;
   - payment status;
   - selected rental;
   - allergies;
   - reason при отмене студией;
   - CTA cancel/rate по доступности.
6. Добавить tests:
   - access only own bookings;
   - profile allergies read-only;
   - empty history;
   - cancelled by studio reason;
   - payment status mapping.

### Зависимости

- Этап 7.

### Критерии готовности

- Клиент видит только свои данные.
- История отображает статусы и оплату.
- Детали брони содержат все требуемые поля.
- Мои аллергии доступны только для просмотра.

### Риски и проверки

- Риск: IDOR vulnerability по `bookingId`.
  - Проверить: ownership check в service/repository.
- Риск: редактирование аллергий появляется в профиле.
  - Проверить: только read-only view.

---

## Этап 9. Отмена брони клиентом

### Цель

Реализовать отмену активной брони клиентом с правилом более 12 часов до начала класса.

### Задачи

1. Реализовать `BookingCancellationService`.
2. Правила:
   - booking принадлежит текущему клиенту;
   - status active;
   - slot не отменен студией;
   - до `slot.startsAt` больше 12 часов;
   - при успешной отмене status → cancelled by client, `cancelledAt` заполнен.
3. Реализовать API `POST /bookings/{bookingId}/cancel`.
4. Реализовать bottom sheet confirmation.
5. Реализовать late cancellation notice.
6. Добавить tests:
   - success >12h;
   - reject <=12h;
   - reject not owner;
   - reject already cancelled;
   - reject studio cancelled;
   - boundary exactly 12h.

### Зависимости

- Этап 8.

### Критерии готовности

- Отменить можно только допустимую бронь.
- При поздней отмене статус не меняется.
- UI показывает корректное объяснение.
- API возвращает бизнес-ошибку для запрещенной отмены.

### Риски и проверки

- Риск: ошибка часового пояса.
  - Проверить: использовать BusinessClock и явно заданный timezone бизнеса.
- Риск: клиентский UI разрешает отмену, а backend запрещает.
  - Проверить: backend response является источником истины.

---

## Этап 10. Отмена класса студией как доменное состояние

### Цель

Поддержать отображение отмены класса студией и запрет повторной записи на такой слот, даже без полноценной админки.

### Задачи

1. Ввести `SlotStatus`: active/cancelled_by_studio.
2. Добавить `studioCancellationReason`.
3. Обновить schedule/details/history queries.
4. Обновить booking creation rule:
   - reject cancelled_by_studio slot.
5. Обновить booking details:
   - показывать статус «Отменена студией» и reason, если есть.
6. Для local/test предусмотреть seed slot/booking со статусом studio cancelled.
7. Если команде нужно ручное переключение статуса для демо — реализовать только internal maintenance script/profile, не клиентскую функцию.
8. Добавить tests.

### Зависимости

- Этап 5.
- Этап 7.
- Этап 8.

### Критерии готовности

- Отмененный студией слот отображается с причиной.
- Повторная запись запрещена.
- Бронь на отмененный студией класс показывает соответствующий статус.
- Нет клиентского сценария отмены класса студией.

### Риски и проверки

- Риск: добавить полноценную админ-функцию вне MVP.
  - Проверить: никаких публичных/admin UI без требования.
- Риск: отмененный слот остается бронируемым через API.
  - Проверить: API create booking test.

---

## Этап 11. Оценка шефа

### Цель

Реализовать одноразовую оценку шефа после завершенного посещенного класса.

### Задачи

1. Реализовать `RatingService`.
2. Правила:
   - booking принадлежит клиенту;
   - slot завершен;
   - booking attended = true;
   - rating по booking еще нет;
   - stars 1..5;
   - comment optional.
3. Реализовать API `POST /ratings`.
4. После сохранения пересчитать `Chef.avgRating`.
5. Реализовать страницу `/ratings/{bookingId}`.
6. Обновить history/details CTA availability.
7. Добавить tests:
   - success;
   - reject not completed;
   - reject not attended;
   - reject duplicate;
   - reject invalid stars;
   - avgRating recalculated.

### Зависимости

- Этап 8.

### Критерии готовности

- Клиент может оценить шефа один раз.
- Повторная оценка запрещена БД constraint и service logic.
- Средний рейтинг шефа обновляется.
- Комментарии не отображаются публично.

### Риски и проверки

- Риск: без админки невозможно выставить attended.
  - Проверить: для MVP использовать seed/test data и явно зафиксировать отсутствие клиентского сценария отметки посещения.
- Риск: комментарии попадают в логи.
  - Проверить: sanitizer.

---

## Этап 12. Валидация, ошибки и безопасность

### Цель

Сделать единые правила валидации, обработки ошибок и защиты приложения.

### Задачи

1. Bean Validation для всех form/API DTO.
2. Domain exceptions:
   - no seats;
   - slot cancelled;
   - late cancellation;
   - invalid rental;
   - invalid allergen;
   - duplicate rating;
   - unauthorized;
   - forbidden.
3. Page exception handler:
   - inline form errors;
   - server error page;
   - business-specific messages.
4. API exception handler:
   - 400/401/403/404/409/422/500;
   - единый error response.
5. Security:
   - protected routes;
   - CSRF for SSR forms;
   - HttpOnly/SameSite cookies;
   - secure cookies in prod;
   - session fixation protection;
   - no token/code in URL;
   - ownership checks.
6. Logging:
   - correlation id;
   - no sensitive body logging;
   - structured logs.

### Зависимости

- Этапы 3–11.

### Критерии готовности

- Все ошибки на русском языке в UI.
- API не раскрывает stack trace.
- CSRF включен.
- Protected routes/API требуют авторизацию.
- Нет логирования SMS-кодов, токенов, полного телефона, аллергий и комментариев.

### Риски и проверки

- Риск: generic 500 вместо бизнес-ошибок.
  - Проверить: tests на каждый domain exception.
- Риск: IDOR через API.
  - Проверить: ownership tests для bookings/ratings/profile.

---

## Этап 13. Docker, локальный запуск и конфигурация окружений

### Цель

Обеспечить воспроизводимый запуск приложения и БД.

### Задачи

1. Dockerfile multi-stage:
   - build jar;
   - build Tailwind assets;
   - runtime image;
   - non-root user.
2. docker-compose:
   - app;
   - PostgreSQL;
   - optional pgAdmin не включать в MVP, если не нужно.
3. `.env.example`:
   - DB URL/user/password;
   - session secret;
   - SMS provider mode;
   - business timezone;
   - log level.
4. Profiles:
   - local mock SMS;
   - test Testcontainers;
   - prod external SMS.
5. Health checks.
6. README:
   - local run;
   - docker run;
   - migrations;
   - mock SMS usage;
   - test commands.

### Зависимости

- Этап 0.
- Этап 1.
- Этап 3.

### Критерии готовности

- `docker compose up` поднимает app + DB.
- Миграции применяются автоматически.
- Можно пройти login через mock SMS.
- README достаточно для нового разработчика.

### Риски и проверки

- Риск: runtime image содержит build tools.
  - Проверить: multi-stage build.
- Риск: реальные secrets в репозитории.
  - Проверить: только `.env.example`, `.env` в `.gitignore`.

---

## Этап 14. Тестирование

### Цель

Проверить бизнес-правила, API contract, SSR pages, persistence и критические сценарии MVP.

### Задачи

#### Unit tests

- phone normalization/validation;
- SMS code limits;
- booking policy;
- cancellation policy;
- rating policy;
- money/date/time formatters;
- DTO/page model mappers.

#### Persistence tests

- migrations on clean DB;
- repositories;
- constraints;
- indexes for key queries, если проверяется отдельно;
- unique rating by booking;
- unique phone.

#### Application integration tests

- auth flow;
- schedule filters;
- booking creation;
- concurrent booking;
- rental stock conflict;
- allergy save to profile;
- cancellation;
- rating and avgRating recalculation.

#### API tests

- every OpenAPI endpoint;
- auth required;
- validation errors;
- business conflicts;
- response structure.

#### MVC/SSR tests

- protected routes redirect;
- forms render errors;
- schedule page;
- slot details;
- booking flow;
- profile/history/details;
- cancel bottom sheet paths;
- rating page;
- logout.

#### Manual smoke

1. Вход по телефону.
2. Resend SMS.
3. Расписание 7 дней.
4. Фильтрация.
5. Детали слота.
6. Бронь без проката/аллергий.
7. Бронь с прокатом/аллергиями.
8. Нет мест.
9. Профиль.
10. История.
11. Детали брони.
12. Отмена >12h.
13. Запрет отмены <=12h.
14. Отмена студией отображается.
15. Оценка шефа.
16. Повторная оценка запрещена.
17. Logout.
18. Ошибка сервера без stack trace.
19. Проверка адаптивности 360px/390px/768px/1366px/1920px.

### Зависимости

- Все функциональные этапы.

### Критерии готовности

- Unit/integration/API/MVC tests проходят.
- Конкурентное бронирование не приводит к overbooking.
- Smoke checklist пройден.
- Нет blocker/critical дефектов.

### Риски и проверки

- Риск: нет теста конкурентной брони.
  - Проверить: отдельный integration test с параллельными запросами.
- Риск: API соответствует UI, но не OpenAPI.
  - Проверить: contract tests/spec review.

---

## Этап 15. Логирование, персональные данные и эксплуатация

### Цель

Подготовить backend к безопасной эксплуатации.

### Задачи

1. Structured logs:
   - timestamp ISO 8601;
   - level;
   - component;
   - correlation_id;
   - event_type;
   - technical message.
2. Correlation id:
   - входящие web/API requests;
   - logs;
   - error responses, если допустимо.
3. События:
   - app start;
   - auth success/failure;
   - SMS send failure;
   - booking created;
   - booking creation failed;
   - booking cancelled;
   - rating submitted;
   - unexpected errors.
4. Персональные данные:
   - маскировать телефон;
   - не логировать SMS code;
   - не логировать allergies;
   - не логировать rating comments;
   - не логировать auth tokens.
5. Retention:
   - реализовать cleanup/anonymization для персональных данных старше 3 месяцев после последней активности, если это требуется для MVP-релиза;
   - либо зафиксировать как обязательную production-задачу, если MVP демонстрационный.
6. Actuator:
   - health;
   - info;
   - metrics, если нужно.

### Зависимости

- Этапы 3–14.

### Критерии готовности

- Логи пригодны для диагностики.
- В логах нет чувствительных данных.
- Есть health endpoint.
- Retention/cleanup подход описан и реализован или явно вынесен как production blocker.

### Риски и проверки

- Риск: SQL/application logs содержат параметры с ПД.
  - Проверить: отключить verbose SQL bind logging в prod.
- Риск: 152-ФЗ игнорируется.
  - Проверить: юридические тексты и согласия вынесены отдельной внешней зависимостью.

---

## Этап 16. Финальная проверка и релиз MVP

### Цель

Подтвердить готовность MVP и отсутствие лишних функций вне требований.

### Задачи

1. Traceability review по функциональным требованиям.
2. Проверка всех screens и bottom sheets.
3. Проверка OpenAPI endpoints.
4. Проверка DB migrations на чистой БД.
5. Проверка Docker fresh start.
6. Проверка security checklist.
7. Проверка logs checklist.
8. Проверка адаптивности.
9. Подготовка release notes.
10. Финальный smoke на чистом окружении.

### Зависимости

- Этапы 0–15.

### Критерии готовности

- Все Must Have требования покрыты.
- Приложение стартует из Docker.
- OpenAPI endpoints реализованы.
- SSR UI покрывает все экраны.
- Критические бизнес-правила покрыты тестами.
- Нет функций вне MVP scope.

### Риски и проверки

- Риск: в MVP случайно добавлены админские сценарии.
  - Проверить: удалить публичные/admin UI, если они не описаны.
- Риск: демо-данные попадают в production profile.
  - Проверить: разделение migrations/seeds/profiles.

---

## 7. Итоговый порядок реализации по спринтам

### Спринт 1 — Технический фундамент

1. Этап 0 — подготовка проекта.
2. Этап 1 — БД, миграции, seed data.
3. Этап 2 — persistence и domain model.

Результат: приложение стартует, БД работает, доменная модель и repositories готовы.

### Спринт 2 — Auth и API foundation

4. Этап 3 — авторизация, SMS, сессии.
5. Этап 4 — REST API contract implementation.
6. Начало Этапа 12 — базовая security/error handling.

Результат: есть защищенный backend/API foundation и login/logout flow.

### Спринт 3 — Расписание и бронирование

7. Этап 5 — расписание и фильтры.
8. Этап 6 — детали слота и booking options.
9. Этап 7 — создание брони.

Результат: клиент может найти класс и создать бронь, а backend атомарно применяет бизнес-правила.

### Спринт 4 — Личный кабинет и post-booking сценарии

10. Этап 8 — профиль, история, детали, аллергии.
11. Этап 9 — отмена брони.
12. Этап 10 — отмена класса студией как состояние.
13. Этап 11 — оценка шефа.

Результат: клиент видит свои данные, отменяет допустимые брони и оценивает шефа.

### Спринт 5 — Hardening и релиз

14. Этап 12 — валидация, ошибки, безопасность.
15. Этап 13 — Docker и локальный запуск.
16. Этап 14 — тестирование.
17. Этап 15 — логирование, ПД, эксплуатация.
18. Этап 16 — финальная проверка и релиз.

Результат: MVP готов к демонстрации/релизу, воспроизводимо запускается и покрыт тестами.

---

## 8. Общие риски и проверки

### 8.1. Бизнес-риски

1. Нет админки, но нужны начальные данные.
   - Проверка: seed data/local fixtures готовы.
2. Реальная SMS-интеграция может быть недоступна.
   - Проверка: mock provider для local/test и интерфейс для реального provider.
3. Посещение класса и отмена студией требуют внешнего процесса.
   - Проверка: в MVP это только состояние в БД/test data, не клиентская функция.

### 8.2. Технические риски

1. Overbooking.
   - Проверка: транзакции, locks/optimistic version, concurrency tests.
2. Oversell проката.
   - Проверка: stock check в той же транзакции, что и booking creation.
3. Дублирование бизнес-логики между SSR и API.
   - Проверка: общий application layer.
4. IDOR в историях/бронях/оценках.
   - Проверка: ownership checks.
5. Утечка ПД в логах.
   - Проверка: sanitizer и log review.

### 8.3. UX-риски

1. UI становится SPA.
   - Проверка: все основные действия через server-side routes/forms.
2. Ошибки показываются техническими текстами.
   - Проверка: error mapping на русском языке.
3. Неправильное поведение без JS.
   - Проверка: server-side validation и обычные form submits работают.

### 8.4. Release-риски

1. Миграции не применяются на чистой БД.
   - Проверка: Testcontainers migration test.
2. Docker не воспроизводит local environment.
   - Проверка: fresh `docker compose up` smoke.
3. Demo seed data смешаны с production.
   - Проверка: profiles and migration separation.

---

## 9. Минимальная Definition of Done

Для каждой функциональной задачи:

1. Есть migration/schema changes, если нужны.
2. Есть domain/application logic.
3. Есть API endpoint, если он указан в OpenAPI.
4. Есть SSR page/fragment, если сценарий имеет экран.
5. Есть server-side validation.
6. Есть обработка business errors.
7. Есть security/ownership checks.
8. Есть unit/integration/MVC/API tests по критической логике.
9. Нет функций вне требований и OpenAPI.
10. Нет логирования чувствительных данных.
11. UI адаптивен минимум до 360px.
12. Docker/local запуск не сломан.
