# ITSEC Asia — Backend Technical Test

Monorepo microservice backend built with **Spring Boot**, **Gradle multi-module**, dan **Docker Compose**. Menerapkan hexagonal architecture, JWT authentication dengan MFA, RBAC, event-driven auditing, dan API gateway terpusat.

---

## Arsitektur

```
Client
  │
  ▼
┌─────────────┐      Redis Pub/Sub
│ API Gateway │ ───────────────────────────────────────┐
│  :8080      │                                        │
└──────┬──────┘                                        ▼
       │ routes                               ┌─────────────────┐
  ┌────┼────────────────┐                     │  Audit Service  │
  ▼    ▼                ▼                     │  :8083  PgSQL   │
┌────────┐  ┌─────────────┐                   └─────────────────┘
│  Auth  │  │   Article   │
│ :8081  │  │   :8082     │
│ PgSQL  │  │   PgSQL     │
│ Redis  │  │   Redis     │
└────────┘  └─────────────┘
```

### Hexagonal Architecture (Ports and Adapters)

Setiap service distruktur dengan 4 layer:

| Layer                                    | Tanggung Jawab                                   |
| ---------------------------------------- | ------------------------------------------------ |
| **Domain**                               | Model bisnis murni — nol dependency ke framework |
| **Application**                          | Use case, port interface, request/response DTO   |
| **API** _(Primary Adapter)_              | REST Controller, request validation              |
| **Infrastructure** _(Secondary Adapter)_ | JPA, Redis, SMTP implementation                  |

---

## Struktur Monorepo

```
/
├── shared/                  # DTOs & filter JWT yang dishare antar service
├── api-gateway/             # Spring Cloud Gateway + rate limiting
├── auth-service/            # Registrasi, login, MFA OTP, JWT, refresh token
├── article-service/         # CRUD artikel dengan RBAC
├── audit-service/           # Subscriber event Redis, persistensi ke PostgreSQL
├── docker/                  # SQL init script untuk PostgreSQL
├── postman/                 # Postman collection siap pakai
├── python-challenges/       # Task 1: coding challenges Python
├── docker-compose.yml
└── .env.example
```

---

## Cara Menjalankan

### Prasyarat

- Docker & Docker Compose
- JDK 21 (untuk build lokal)

### 1. Setup environment

```bash
cp .env.example .env
# Edit .env jika perlu mengubah credentials
```

### 2. Jalankan semua service

```bash
docker-compose up --build
```

> Build pertama memakan waktu lebih lama karena Gradle mengunduh dependencies.
> Service Java baru aktif setelah PostgreSQL, Redis, dan Mailpit sehat (healthcheck otomatis).

### Port yang Tersedia

| Service                   | URL                   |
| ------------------------- | --------------------- |
| API Gateway (entry point) | http://localhost:8080 |
| Auth Service              | http://localhost:8081 |
| Article Service           | http://localhost:8082 |
| Audit Service             | http://localhost:8083 |
| Mailpit (email inbox MFA) | http://localhost:8025 |

### Swagger UI

Setiap service menyediakan dokumentasi interaktif:

- Auth: http://localhost:8081/swagger-ui.html
- Article: http://localhost:8082/swagger-ui.html
- Audit: http://localhost:8083/swagger-ui.html

---

## API Endpoints

Semua request melewati gateway di `http://localhost:8080`.

### Authentication

| Method   | Path               | Auth          | Deskripsi                               |
| -------- | ------------------ | ------------- | --------------------------------------- |
| `POST`   | `/auth/register`   | —             | Registrasi user baru                    |
| `POST`   | `/auth/login`      | —             | Login; jika valid → kirim OTP ke email  |
| `POST`   | `/auth/verify-otp` | —             | Verifikasi OTP → return JWT             |
| `POST`   | `/auth/refresh`    | —             | Tukar refresh token → access token baru |
| `POST`   | `/auth/logout`     | JWT           | Revoke token                            |
| `GET`    | `/auth/users`      | `SUPER_ADMIN` | Daftar semua user                       |
| `PUT`    | `/auth/users/{id}` | `SUPER_ADMIN` | Update fullname/email/roles user        |
| `DELETE` | `/auth/users/{id}` | `SUPER_ADMIN` | Hapus user                              |

Catatan: user baru dari `/auth/register` selalu dapat role `VIEWER` (default, self-service escalation dicegah). Promosi ke `EDITOR`/`CONTRIBUTOR`/`SUPER_ADMIN` cuma bisa lewat `PUT /auth/users/{id}` oleh `SUPER_ADMIN`.

Login mendukung **remember-device**: kirim `rememberToken` (dari hasil `verify-otp` sebelumnya) di body `/auth/login` — kalau valid & cocok device, OTP di-skip dan langsung dapat access/refresh token.

### Articles

| Method   | Path             | Auth                                              | Deskripsi                                                   |
| -------- | ---------------- | ------------------------------------------------- | ----------------------------------------------------------- |
| `POST`   | `/articles`      | `SUPER_ADMIN`, `EDITOR`, `CONTRIBUTOR`            | Buat artikel baru (CONTRIBUTOR hanya untuk dirinya sendiri) |
| `GET`    | `/articles`      | — (publik, filter `isPublic`) / JWT               | Daftar artikel                                              |
| `GET`    | `/articles/{id}` | — (publik jika `isPublic`) / JWT                  | Lihat artikel                                               |
| `PUT`    | `/articles/{id}` | Owner (`EDITOR`/`CONTRIBUTOR`) atau `SUPER_ADMIN` | Update artikel milik sendiri                                |
| `DELETE` | `/articles/{id}` | Owner `EDITOR` atau `SUPER_ADMIN`                 | Hapus artikel (CONTRIBUTOR tidak bisa)                      |

`VIEWER` (dan anonim) hanya bisa lihat artikel dengan `isPublic = true`. `EDITOR`/`SUPER_ADMIN` lihat semua.

### Audit

| Method | Path          | Auth          | Deskripsi             |
| ------ | ------------- | ------------- | --------------------- |
| `GET`  | `/audit/logs` | `SUPER_ADMIN` | Lihat semua log audit |

---

## Flow Autentikasi

```
POST /auth/login
  → validasi credentials + cek lockout
  → generate OTP (6 digit, BCrypt-hashed, TTL 5 menit di Redis)
  → kirim email via Mailpit
  → return { temporaryToken }

POST /auth/verify-otp  { temporaryToken, code }
  → validasi OTP dari Redis
  → issue JWT access token (15 menit) + refresh token (7 hari, SHA-256 di PgSQL)
  → issue rememberToken (30 hari, di Redis) untuk skip OTP di device yang sama
  → return { accessToken, refreshToken, rememberToken }

POST /auth/login  { usernameOrEmail, password, rememberToken? }
  → kalau rememberToken valid & cocok user → langsung { accessToken, refreshToken }, OTP di-skip
```

**Proteksi brute-force:** setelah 5 gagal login (window 10 menit) → akun dikunci 30 menit via Redis TTL.

---

## Desain Database

**Schema `auth` (PostgreSQL)**

- `users` — data user + status lock
- `roles` / `user_roles` — role-based access
- `otp_tokens` — OTP ter-hash dengan tracking attempt
- `login_attempts` — counter percobaan login

**Schema `article` (PostgreSQL)**

- `articles` — konten artikel + `is_public`; `author_id` tidak punya FK lintas schema (menjaga batas service)

**Schema `audit` (PostgreSQL)**

- `audit_logs` — append-only: actor, action, entity, IP, user agent (di-parse jadi browser/OS/device type), request path, HTTP method, status, timestamp

---

## Postman Collection

Import file `postman/ITSEC-Backend.postman_collection.json` ke Postman.
Collection sudah menyertakan contoh request untuk semua endpoint.

---

## Python Challenges (Task 1)

Lihat [`python-challenges/README.md`](python-challenges/README.md) untuk detail solusi dan cara menjalankan test.

```bash
cd python-challenges
pytest test_solutions.py
```

---

## Keputusan Teknis Utama

| Topik                 | Pilihan                      | Alasan Singkat                                                    |
| --------------------- | ---------------------------- | ----------------------------------------------------------------- |
| Password hashing      | BCrypt (strength 12)         | Salt built-in, cost factor tinggi — brute-force tidak feasible    |
| OTP storage           | Redis + BCrypt hash          | TTL otomatis, OTP diperlakukan seperti password                   |
| Refresh token storage | PostgreSQL                   | Butuh persistensi & full revocation audit trail                   |
| Access token denylist | Redis                        | Fast lookup, auto-expire sesuai sisa TTL token                    |
| Audit ingestion       | Redis Pub/Sub                | Fully decoupled — Auth/Article Service tidak tunggu Audit Service |
| Audit storage         | PostgreSQL                   | Konsisten dengan requirement RDBMS PostgreSQL di seluruh stack    |
| Rate limiting         | Redis Token Bucket (gateway) | Throttle di edge, sebelum menyentuh downstream service            |
| `shared` module       | Spring Boot library          | Centralize JWT filter & DTO — trade-off: coupling saat deploy     |
| Test coverage         | JaCoCo, min. 80% line        | Di-enforce di `check`/`build` untuk auth/article/audit-service    |

---

## Known Limitations

Sengaja tidak dikerjakan untuk scope technical test ini; dicatat agar keputusan desainnya transparan:

| Area | Kondisi sekarang | Perbaikan yang disarankan |
| ---- | ---------------- | ------------------------- |
| Refresh token | Tidak dirotasi; token yang sama valid sampai 7 hari atau di-revoke | Rotasi tiap `/auth/refresh` + deteksi reuse token lama |
| Sanitasi input | `title`/`content` artikel dan `User-Agent` audit log disimpan mentah (risiko stored XSS jika client merender sebagai HTML) | Sanitasi HTML saat simpan (OWASP Java HTML Sanitizer); client render sebagai teks |
| CSP | Tidak ada header `Content-Security-Policy` | Tambahkan CSP di `SecurityConfig` |
| Audit event | Redis Pub/Sub tanpa persistence; event hilang jika audit-service down | Message broker durable (Kafka/RabbitMQ) atau Redis Streams |
| Integration test | Hanya unit test (mock); belum ada test adapter terhadap Postgres/Redis asli | Testcontainers |
