# API de Agenda Corporativa (Unimobili)

API REST para gerenciamento de agendas corporativas: autenticação JWT, usuários,
eventos (agendamentos), agendas e relatórios gerenciais. Sem frontend — toda
interação é via HTTP, documentada em Swagger/OpenAPI.

## Stack

- Java 17 _(o alvo do projeto é Java 21; usando 17 temporariamente)_, Spring Boot 3.3
- Spring Security + JWT (oauth2-resource-server / Nimbus, HMAC)
- Spring Data JPA / Hibernate, PostgreSQL, Flyway
- Bean Validation, Lombok, MapStruct, springdoc-openapi
- Testes: JUnit 5, Testcontainers (PostgreSQL real), JaCoCo (cobertura)
- Docker / Docker Compose

## Pré-requisitos

- JDK 17+
- Maven 3.9+
- Docker (Docker Desktop em execução)

## Subindo o banco

```bash
docker compose up -d db
```

Sobe um PostgreSQL 16 em `localhost:5432` (db `agenda`, usuário/senha `agenda`/`agenda`),
alinhado aos defaults de `src/main/resources/application.yml`.

## Executando a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. No boot, o Flyway aplica as migrations,
incluindo o **seed do gerente inicial**.

### Variáveis de ambiente

| Variável | Default | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/agenda` | URL do banco |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `agenda` / `agenda` | Credenciais do banco |
| `JWT_SECRET` | (default de dev) | Segredo HMAC do JWT (≥ 32 bytes em produção) |
| `GERENTE_EMAIL` / `GERENTE_PASSWORD` | `gerente@unimobili.com` / `admin12345` | Credenciais do gerente seedado |

> Troque `JWT_SECRET` e a senha do gerente em produção.

## Usando a API

1. **Login** para obter o token:

   ```bash
   curl -s http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"gerente@unimobili.com","senha":"admin12345"}'
   ```

2. Use o `accessToken` retornado no header `Authorization: Bearer <token>` nas
   rotas protegidas.

### Documentação (Swagger)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

No Swagger UI, clique em **Authorize** e cole o `accessToken` para testar os
endpoints protegidos.

### Endpoints principais

- `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`
- `GET/POST/PUT/DELETE /users` (GERENTE)
- `GET/POST /events`, `GET/PUT/DELETE /events/{id}`, `PATCH /events/{id}/status`
- `GET /schedules`, `GET /schedules/{externalUserId}`
- `GET /reports/events`, `/events-by-user`, `/cancellations`, `/occupancy` (GERENTE)

## Testes e cobertura

Os testes de integração usam **Testcontainers** e exigem o Docker em execução.

```bash
mvn test            # roda os testes (sobe um PostgreSQL via Testcontainers)
mvn verify          # testes + portão de cobertura JaCoCo (mínimo 80% de linhas)
```

Relatório de cobertura: `target/site/jacoco/index.html`.

> **Windows + Docker Desktop:** se o Testcontainers não encontrar o Docker, exporte
> `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` antes de rodar os testes.

## Arquitetura

Camadas: `controller` · `service` · `repository` · `entity` · `dto` · `mapper`
· `security` · `config` · `web/error`. Auditoria via Spring Data JPA Auditing
(`createdAt/updatedAt/createdBy/updatedBy`). Agenda é uma **visão derivada** dos
eventos (não há tabela própria). Erros seguem um corpo padronizado
`{timestamp, status, error, message, path}`.
