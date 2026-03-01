# Secret

Монорепозиторий проекта `Secret`:

- `app/` - Android-клиент на Jetpack Compose
- `backend/` - Node.js backend на Express + WebSocket + MySQL
- `web-client/` - браузерный клиент на Nuxt 3

Проект реализует приватные чаты с текстом, файлами, аудио, видео, профилями, QR-привязкой устройства и real-time обновлениями.

## Состав проекта

```text
Secret/
├── app/              Android-приложение
├── backend/          API, WebSocket, загрузка файлов, MySQL
├── web-client/       Web-клиент
├── gradle/           Gradle wrapper и version catalog
├── build.gradle.kts  Корневой Gradle
└── settings.gradle.kts
```

## Основной функционал

- регистрация пользователя с автогенерацией профиля
- профиль: ник, аватар, локальное сохранение сессии
- создание чатов с названием от 4 символов
- опциональный пароль на чат
- вход в чат по имени
- удаление чатов и сообщений
- отправка:
  - текста
  - файлов
  - изображений
  - аудио
  - видео
- WebSocket-обновления в реальном времени
- QR-привязка профиля между web и Android
- роли администратора / super admin
- web и Android клиенты

## Технологии

### Android

- Kotlin
- Jetpack Compose
- Retrofit
- OkHttp
- Gson
- ZXing
- Coil

### Backend

- Node.js
- Express
- `ws`
- `mysql2`
- `multer`
- `bcryptjs`

### Web

- Nuxt 3
- `html5-qrcode`
- `qrcode`

## Требования

- Node.js 20+  
  Локально проект уже собирался на Node.js 24
- npm
- MySQL 8+
- Android Studio
- JDK 11+

## Быстрый старт

### 1. Backend

```bash
cd /Users/mac/AndroidStudioProjects/Secret/backend
npm install
cp .env.example .env
npm start
```

Backend поднимает:

- HTTP API
- WebSocket
- статику для загруженных файлов

### 2. Web client

```bash
cd /Users/mac/AndroidStudioProjects/Secret/web-client
npm install
npm start
```

### 3. Android client

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:assembleDebug
```

Готовый debug APK:

```text
/Users/mac/AndroidStudioProjects/Secret/app/build/outputs/apk/debug/app-debug.apk
```

## Backend

### Переменные окружения

Файл-шаблон:

```text
/Users/mac/AndroidStudioProjects/Secret/backend/.env.example
```

Текущие переменные:

```env
PORT=3100
HOST=0.0.0.0

DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_mysql_password
DB_NAME=secret_chat
DB_CONNECTION_LIMIT=10
DB_AUTO_CREATE=true

SUPER_USER_IDS=
KEY_ADMIN=KEY_ADMIN
```

### База данных

Backend использует `MySQL`.

Имя БД по умолчанию:

```text
secret_chat
```

Минимальный старт:

```sql
CREATE DATABASE IF NOT EXISTS secret_chat
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Если `DB_AUTO_CREATE=true`, backend сам создаст необходимые таблицы.

### Основные backend-модули

- `backend/src/server.js` - основной сервер, API, WebSocket, инициализация БД
- `backend/uploads/` - загруженные файлы

### Основные API-маршруты

- `POST /api/users/register`
- `GET /api/users/:userId`
- `PUT /api/users/:userId`
- `GET /api/chats`
- `POST /api/chats/create`
- `POST /api/chats/join`
- `PUT /api/chats/:chatId/icon`
- `DELETE /api/chats/:chatId`
- `GET /api/chats/:chatId/messages`
- `POST /api/messages`
- `DELETE /api/messages/:messageId`
- `POST /api/upload`
- `POST /api/profile-link/sessions`
- `POST /api/profile-link/sessions/:sessionId/complete`
- `POST /api/admin/activate`
- `GET /api/health`

WebSocket endpoint:

- `/ws`

## Android client

### Основные файлы

- `/Users/mac/AndroidStudioProjects/Secret/app/src/main/java/com/appswebnetkz/secret/ui/SecretApp.kt` - основная Compose UI
- `/Users/mac/AndroidStudioProjects/Secret/app/src/main/java/com/appswebnetkz/secret/viewmodel/ChatViewModel.kt` - логика клиента
- `/Users/mac/AndroidStudioProjects/Secret/app/src/main/java/com/appswebnetkz/secret/data/repository/ChatRepository.kt` - работа с API, сокетами, файлами
- `/Users/mac/AndroidStudioProjects/Secret/app/src/main/java/com/appswebnetkz/secret/data/network/NetworkModule.kt` - базовые адреса API/WS

### Текущая конфигурация Android API

Сейчас Android-клиент смотрит на production backend:

```kotlin
BASE_API_URL = "https://secret.webnet.kz/api/"
BASE_WS_URL  = "wss://secret.webnet.kz/ws"
```

Файл:

```text
/Users/mac/AndroidStudioProjects/Secret/app/src/main/java/com/appswebnetkz/secret/data/network/NetworkModule.kt
```

### Сборка Android

Debug APK:

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:assembleDebug
```

Release AAB:

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:bundleRelease
```

Готовый release AAB:

```text
/Users/mac/AndroidStudioProjects/Secret/app/build/outputs/bundle/release/app-release.aab
```

### Release signing

Release-подпись подключается через:

```text
/Users/mac/AndroidStudioProjects/Secret/keystore.properties
```

Ожидаемые поля:

```properties
storeFile=keystore/secret-upload.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Ключи подписи и `keystore.properties` в git не хранятся.

## Web client

### Основные файлы

- `/Users/mac/AndroidStudioProjects/Secret/web-client/pages/index.vue` - основной экран
- `/Users/mac/AndroidStudioProjects/Secret/web-client/components/AudioMessagePlayer.vue`
- `/Users/mac/AndroidStudioProjects/Secret/web-client/components/VideoMessagePlayer.vue`
- `/Users/mac/AndroidStudioProjects/Secret/web-client/assets/main.css`
- `/Users/mac/AndroidStudioProjects/Secret/web-client/nuxt.config.ts`

### Запуск

```bash
cd /Users/mac/AndroidStudioProjects/Secret/web-client
npm install
npm start
```

### Runtime config

По умолчанию в `Nuxt` сейчас прописаны:

```ts
apiBase: "http://192.168.0.15:3000/api"
wsBase: "ws://192.168.0.15:3000/ws"
```

Для локального запуска лучше переопределять через env:

```bash
NUXT_PUBLIC_API_BASE=http://127.0.0.1:3100/api \
NUXT_PUBLIC_WS_BASE=ws://127.0.0.1:3100/ws \
npm start
```

Или для LAN:

```bash
NUXT_PUBLIC_API_BASE=http://192.168.0.15:3100/api \
NUXT_PUBLIC_WS_BASE=ws://192.168.0.15:3100/ws \
npm start
```

## Что не хранится в git

Исключено через `.gitignore`:

- `backend/.env`
- `backend/uploads/`
- `backend/*.log`
- `web-client/node_modules/`
- `web-client/.nuxt/`
- `web-client/.output/`
- `web-client/*.log`
- `keystore/`
- `keystore.properties`
- `build/`
- `local.properties`

## Типовые команды

### Проверка Android

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:compileDebugKotlin
```

### Debug APK

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:assembleDebug
```

### Release AAB

```bash
cd /Users/mac/AndroidStudioProjects/Secret
./gradlew :app:bundleRelease
```

### Backend

```bash
cd /Users/mac/AndroidStudioProjects/Secret/backend
npm start
```

### Web client

```bash
cd /Users/mac/AndroidStudioProjects/Secret/web-client
npm start
```

## Частые проблемы

### `EADDRINUSE`

Порт уже занят. Нужно остановить старый процесс, который слушает нужный порт.

Пример:

```bash
lsof -ti :3100 | xargs kill -9
```

### Backend не стартует из-за MySQL

Проверь:

- что MySQL запущен
- что создана БД `secret_chat`
- что в `backend/.env` корректные `DB_USER` / `DB_PASSWORD`

### Web client не запускается через `npm start`

В этом проекте `start` уже настроен и запускает `nuxt dev`:

```bash
cd /Users/mac/AndroidStudioProjects/Secret/web-client
npm start
```

### Android release build не подписывается

Проверь наличие:

- `keystore.properties`
- keystore-файла по пути из `storeFile`
- корректного `keyAlias`
- корректных паролей

## Репозиторий

GitHub:

[https://github.com/webnetkz/secret](https://github.com/webnetkz/secret)
