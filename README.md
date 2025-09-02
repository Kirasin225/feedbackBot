# Бот анонімних відгуків (Java / Spring Boot + Telegram)

Коротко: приймає анонімні фідбеки через Telegram, аналізує через OpenAI, зберігає в PostgreSQL, дублює в Google Docs і для критичних фідбеків створює картку в Trello. Адмінка доступна за `/admin`.

## Вимоги
- Java 21+
- Maven (або `mvnw` / `mvnw.cmd`)
- Docker & Docker Compose
- Облікові дані/ключі: Telegram, OpenAI, Google (Docs), Trello (опціонально)

## Файли налаштувань
- Основне — `src/main/resources/application.yml`
- Docker — `docker-compose.yml`
- Змінні середовища — ` .env` (не комітити реальні секрети)
- Google credentials — `src/main/resources/google/credentials.json`

## Змінні в ` .env` (заповнити)
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_BOT_USERNAME`
- `OPENAI_API_KEY`
- `GOOGLE_DOC_ID`
- `GOOGLE_CREDENTIALS_PATH` (наприклад `classpath:google/credentials.json`)
- `TRELLO_ENABLED` (true/false)
- `TRELLO_KEY`
- `TRELLO_TOKEN`
- `TRELLO_LIST_ID`

## Локальний запуск (рекомендований порядок)
1. Скопіювати приклад і заповнити секрети:
    - `cp .env.example .env` і відредагувати ` .env`
2. Створити папку для Google credentials і помістити туди `credentials.json`:
    - `src/main/resources/google/credentials.json`
3. Запустити PostgreSQL з Docker Compose:
    - `docker compose --env-file .env up -d PostgreSQL`
      (сервіс у `docker-compose.yml` має назву `PostgreSQL`)
4. Зібрати проєкт:
    - Windows: `mvnw.cmd clean package -DskipTests`
    - Linux/macOS: `./mvnw clean package -DskipTests`
5. Запустити додаток (важливо: JVM timezone):
    - Через Maven (Windows):  
      `mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC"`
    - Або запустити jar:  
      `java -Duser.timezone=UTC -jar target/feedback-bot-0.0.1.jar`
6. (IntelliJ) Додати в Run/Debug Config VM options:  
   `-Duser.timezone=UTC`

> Чому `-Duser.timezone=UTC`? JVM може передати системну зону (наприклад `Europe/Kiev`), яку PostgreSQL іноді відкидає. Можна також вказати коректне ім'я `Europe/Kyiv`.

## Приклади для Docker образу додатку
Якщо контейнер додатку повинен отримувати JVM-опції — додайте змінну середовища в `docker-compose.yml`:
```yaml
services:
  app:
    image: feedback-app:latest
    environment:
      - JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
    ports:
      - "8080:8080"
    depends_on:
      - PostgreSQL