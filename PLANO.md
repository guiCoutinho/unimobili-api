# Plano de Projeto — API de Agenda Corporativa

> Documento de planejamento e modelagem. **Nenhum código foi escrito ainda.**
> Consolida a Descrição original + as decisões tomadas na sessão de *grilling*.

---

## 1. Visão geral

API REST para gerenciamento de agendas corporativas, sem frontend. Toda
interação via HTTP documentada em Swagger/OpenAPI.

- **Stack:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA/Hibernate,
  JWT, Bean Validation, Lombok, MapStruct, Maven.
- **Banco:** PostgreSQL (migrations via Flyway).
- **Infra:** Docker + Docker Compose.
- **Testes:** JUnit 5, Mockito, Testcontainers — cobertura mínima 80%.
- **Naming:** groupId `com.unimobili`, artifactId `agenda-api`, pacote raiz
  `com.unimobili.agenda`.

---

## 2. Decisões resolvidas (grilling)

| # | Tema | Decisão |
|---|------|---------|
| 1 | Notificações | **Fora de escopo.** Texto residual do objetivo; nada modelado/implementado. |
| 2 | Agenda (Schedule) | **Visão derivada**, sem tabela. Agenda = conjunto de eventos de um externo. |
| 3 | Refresh token | **JWT stateless**: access ~15min + refresh ~7d. Sem tabela. |
| 4 | Bootstrap admin | **Seed via Flyway**: gerente inicial com credenciais de variável de ambiente (hash BCrypt). |
| 5 | Papéis | **Um papel por usuário** (enum: INTERNO, EXTERNO, GERENTE → authority `ROLE_X`). |
| 6 | Conflito de horário | **Por externo**, ignorando CANCELADO e NAO_COMPARECEU. |
| 7 | Exclusão | **Soft delete** em ambos: evento → status CANCELADO; usuário → `ativo=false`. |
| 8 | Status do evento | **Máquina de estados** com transições válidas (ver §6). |
| 9 | Propriedade de eventos | **INTERNO só altera eventos que criou**; GERENTE altera todos; EXTERNO nenhum. |
| 10 | Visibilidade EXTERNO | EXTERNO **vê apenas a própria agenda**; INTERNO/GERENTE veem todas. |
| 11 | Validação externalUser | Deve existir, ter role **EXTERNO** e `ativo=true`. `createdBy` automático do usuário autenticado. |
| 12 | Data/fuso | **UTC (Instant / `timestamptz`)**, ISO-8601 com offset. Sem eventos no passado; edição de eventos terminais/passados bloqueada. |
| 13 | Listagens | **Paginadas** (Spring Data `Pageable`: `page`, `size`, `sort`). |
| 14 | Relatórios | Definições confirmadas (ver §8). |
| 15 | Exportação | **Adiada.** Relatórios em JSON por enquanto. |
| 16 | PUT /users | **Só GERENTE**; altera nome/email/telefone/cargo/role/ativo. **Senha não muda** no PUT. |

---

## 3. Modelo de dados

### Entidade `User`
| Campo | Tipo | Observações |
|-------|------|-------------|
| id | UUID (PK) | gerado |
| nome | String | obrigatório |
| email | String | **único**, obrigatório |
| senha | String | BCrypt; mín. 8 chars na entrada |
| telefone | String | opcional |
| cargo | String | título do cargo (texto livre) |
| role | Enum | INTERNO \| EXTERNO \| GERENTE |
| ativo | boolean | default `true`; soft delete usa `false` |
| createdAt / updatedAt | Instant | auditoria |
| createdBy / updatedBy | UUID | auditoria |

### Entidade `Event`
| Campo | Tipo | Observações |
|-------|------|-------------|
| id | UUID (PK) | gerado |
| titulo | String | obrigatório |
| descricao | String | opcional |
| dataHoraInicio | Instant (`timestamptz`) | obrigatório, não no passado |
| dataHoraFim | Instant (`timestamptz`) | obrigatório, > início |
| status | Enum | AGENDADO \| CONFIRMADO \| REALIZADO \| CANCELADO \| NAO_COMPARECEU |
| externalUser | FK → User | obrigatório; deve ser role EXTERNO ativo |
| createdBy | FK → User | preenchido automaticamente |
| createdAt / updatedAt | Instant | auditoria |
| updatedBy | UUID | auditoria |

**Auditoria:** Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`,
`@CreatedBy`, `@LastModifiedBy`) + `AuditorAware<UUID>` lendo o `SecurityContext`.

**Índices:** `users(email)` único; `events(external_user_id, data_hora_inicio,
data_hora_fim)` para checagem de conflito; `events(status)`.

---

## 4. Endpoints

### Auth
- `POST /auth/login` — e-mail + senha → access + refresh token.
- `POST /auth/refresh` — refresh token → novo access token.

### Usuários (gestão restrita a GERENTE)
- `GET /users` (paginado) · `GET /users/{id}`
- `POST /users` (define senha inicial) · `PUT /users/{id}` (sem senha)
- `DELETE /users/{id}` → soft delete (`ativo=false`)

### Agendas (visão derivada)
- `GET /schedules` — externos + seus eventos (filtro de período)
- `GET /schedules/{externalUserId}` — eventos do externo
- EXTERNO só enxerga a própria.

### Eventos
- `GET /events` (paginado, filtros) · `GET /events/{id}`
- `POST /events` (INTERNO/GERENTE) · `PUT /events/{id}` (dono/GERENTE)
- `DELETE /events/{id}` → soft cancel · `PATCH /events/{id}/status`

### Relatórios (somente GERENTE)
- `GET /reports/events` · `GET /reports/events-by-user`
- `GET /reports/cancellations` · `GET /reports/occupancy`
- Filtros: período, externo, interno, status.

---

## 5. Permissões (resumo)

| Ação | INTERNO | EXTERNO | GERENTE |
|------|:------:|:------:|:------:|
| Ver agendas/eventos | todas | só a própria | todas |
| Criar evento | ✅ | ❌ | ✅ |
| Editar/cancelar/status evento | só os que criou | ❌ | todos |
| Gerenciar usuários | ❌ | ❌ | ✅ |
| Relatórios | ❌ | ❌ | ✅ |

---

## 6. Máquina de estados do evento

```
AGENDADO     → CONFIRMADO, CANCELADO
CONFIRMADO   → REALIZADO, CANCELADO, NAO_COMPARECEU
REALIZADO    → (terminal)
CANCELADO    → (terminal)
NAO_COMPARECEU → (terminal)
```
Transição inválida → 409/400.

---

## 7. Regras de validação

**Usuário:** email único; senha mín. 8 chars; campos obrigatórios (nome, email, role).

**Evento:** título obrigatório; início e fim obrigatórios; `fim > início`;
início não no passado; externalUser obrigatório (EXTERNO ativo);
sem sobreposição na agenda do externo.

**Conflito (overlap):** existe conflito se, para o mesmo externalUser e
status ocupante (AGENDADO/CONFIRMADO/REALIZADO):
`novoInicio < existenteFim AND existenteInicio < novoFim`.

---

## 8. Relatórios (semântica)

- **/reports/events** — lista de eventos filtrada (período, externo, interno, status).
- **/reports/events-by-user** — contagem de eventos agrupada por externo.
- **/reports/cancellations** — eventos CANCELADO + NAO_COMPARECEU com totais e taxa.
- **/reports/occupancy** — soma de horas ocupadas (eventos não cancelados) por
  externo no período.
- Exportação (CSV/Excel) **adiada**; saída atual em JSON.

---

## 9. Segurança

- JWT: access curto + refresh stateless. Filtro de autenticação no Spring Security.
- Todas as rotas protegidas exceto `/auth/login`, `/auth/refresh`, Swagger e health.
- Autorização por role via `@PreAuthorize` / `SecurityFilterChain`.
- Senhas com BCrypt.

---

## 10. Tratamento de erros

`@RestControllerAdvice` global com payload padronizado:
```json
{ "timestamp": "", "status": 400, "error": "Validation Error", "message": "", "path": "" }
```
Mapear: validação (400), não autenticado (401), sem permissão (403),
não encontrado (404), conflito de agenda/transição (409).

---

## 11. Arquitetura em camadas

`controller · service · repository · entity · dto · mapper (MapStruct) ·
security · config · exception`. Princípios SOLID.

---

## 12. Documentação

Swagger/OpenAPI completo: descrição, exemplos de request e response em todos
os endpoints (springdoc-openapi).

---

## 13. Testes (≥ 80%)

- Unitários de services (Mockito).
- Integração de endpoints (Testcontainers + Postgres real).
- Segurança (acesso por role, rotas protegidas).
- Validação (entradas inválidas, conflitos, transições).

---

## 14. Entregáveis

Estrutura Maven · modelagem do banco · entidades JPA · DTOs · repositories ·
services · controllers · config JWT · config Swagger · `docker-compose.yml`
(Postgres) · migrations Flyway (incl. seed do gerente) · testes automatizados ·
README de execução local.

---

## 15. Plano de execução sugerido (marcos)

> Você optou por gerar só este documento agora. Quando quiser implementar,
> sugiro a ordem abaixo, com aprovação entre marcos.

1. **Scaffold** — projeto Maven, dependências, Docker Compose, Flyway + seed gerente.
2. **Auth** — JWT login/refresh, Spring Security, BCrypt.
3. **Usuários** — CRUD + soft delete + regras de role.
4. **Eventos** — CRUD, validações, conflito, máquina de estados, propriedade.
5. **Agendas** — visão derivada + filtros + visibilidade por role.
6. **Relatórios** — 4 endpoints (JSON).
7. **Qualidade** — Swagger completo, testes ≥80%, README.
