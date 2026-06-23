# PRD — API de Agenda Corporativa

> Vocabulário de domínio: **GERENTE**, **INTERNO** (funcionário interno),
> **EXTERNO** (funcionário externo), **Agenda** (visão derivada dos eventos de
> um EXTERNO), **Evento**, **status** (AGENDADO, CONFIRMADO, REALIZADO,
> CANCELADO, NAO_COMPARECEU).

## Problem Statement

Empresas precisam coordenar compromissos entre funcionários internos e externos,
mas hoje não há um sistema central que controle quem pode agendar, evite
sobreposição de horários na agenda de um funcionário externo, registre o ciclo
de vida de cada compromisso e dê visão gerencial sobre ocupação e cancelamentos.
Sem isso, há conflitos de horário, falta de rastreabilidade de quem criou/alterou
um compromisso e ausência de relatórios para decisão.

## Solution

Uma API REST (sem frontend) que centraliza o gerenciamento de agendas
corporativas. Funcionários internos e gerentes criam e gerenciam eventos na
agenda de funcionários externos, com validação automática de conflitos de
horário. Cada funcionário externo enxerga apenas a própria agenda. Gerentes têm
acesso total, gerenciam usuários e consultam relatórios. O acesso é autenticado
via JWT e autorizado por papel. Toda a API é documentada via Swagger/OpenAPI.

## User Stories

1. Como usuário, quero fazer login com e-mail e senha, para receber um token de acesso e usar a API.
2. Como usuário, quero renovar meu acesso via refresh token, para continuar autenticado sem refazer login.
3. Como usuário autenticado, quero que todas as rotas (exceto login/refresh/docs) exijam token válido, para que dados corporativos fiquem protegidos.
4. Como GERENTE, quero listar usuários de forma paginada, para administrar a base de funcionários.
5. Como GERENTE, quero ver os detalhes de um usuário pelo id, para conferir seus dados.
6. Como GERENTE, quero criar um usuário definindo nome, e-mail, senha inicial, telefone, cargo e papel, para cadastrar funcionários.
7. Como GERENTE, quero que o e-mail de usuário seja único, para evitar cadastros duplicados.
8. Como GERENTE, quero que senhas tenham no mínimo 8 caracteres e sejam armazenadas com hash, para garantir segurança.
9. Como GERENTE, quero editar um usuário (nome, e-mail, telefone, cargo, papel, ativo), para manter o cadastro atualizado, sem alterar a senha por esse fluxo.
10. Como GERENTE, quero desativar um usuário (soft delete via `ativo=false`), para removê-lo do uso sem apagar histórico.
11. Como INTERNO ou EXTERNO, quero ser impedido de gerenciar usuários, para que apenas gerentes administrem a base.
12. Como INTERNO ou GERENTE, quero criar um evento na agenda de um funcionário externo, para registrar um compromisso.
13. Como criador de evento, quero que o sistema preencha automaticamente o "criado por" com meu usuário, para fins de auditoria.
14. Como usuário, quero que um evento só possa ser associado a um funcionário EXTERNO ativo, para manter a integridade do domínio.
15. Como usuário, quero que o horário de término seja maior que o de início, para evitar eventos inválidos.
16. Como usuário, quero ser impedido de criar eventos no passado, para que a agenda represente compromissos futuros.
17. Como usuário, quero que o sistema rejeite eventos que se sobreponham a outro evento ocupante do mesmo externo, para evitar conflitos de agenda.
18. Como usuário, quero que eventos CANCELADO e NAO_COMPARECEU liberem o horário, para permitir reagendar slots desfeitos.
19. Como INTERNO, quero editar/cancelar/mudar status apenas dos eventos que eu criei, para respeitar a propriedade do compromisso.
20. Como GERENTE, quero editar/cancelar/mudar status de qualquer evento, para ter controle total.
21. Como EXTERNO, quero ser impedido de criar, editar ou excluir eventos, pois apenas recebo compromissos.
22. Como usuário, quero mudar o status de um evento respeitando transições válidas (AGENDADO→CONFIRMADO/CANCELADO; CONFIRMADO→REALIZADO/CANCELADO/NAO_COMPARECEU), para manter a consistência do ciclo de vida.
23. Como usuário, quero que transições de status inválidas sejam rejeitadas, para impedir estados absurdos (ex.: REALIZADO→AGENDADO).
24. Como usuário, quero cancelar um evento via DELETE (soft delete → status CANCELADO), preservando o histórico para relatórios.
25. Como usuário autenticado, quero listar eventos de forma paginada e filtrável (período, externo, interno, status), para encontrar compromissos.
26. Como usuário autenticado, quero ver os detalhes de um evento pelo id, para conferir suas informações.
27. Como INTERNO ou GERENTE, quero visualizar a agenda de qualquer funcionário externo, para planejar compromissos.
28. Como EXTERNO, quero visualizar apenas a minha própria agenda e os detalhes dos meus eventos, para preservar a privacidade entre externos.
29. Como usuário, quero consultar a agenda de um externo por período, para ver a ocupação em uma janela de tempo.
30. Como GERENTE, quero um relatório de eventos filtrável (período, externo, interno, status), para analisar a operação.
31. Como GERENTE, quero um relatório de eventos agrupados por funcionário externo, para ver volume por pessoa.
32. Como GERENTE, quero um relatório de cancelamentos (CANCELADO + NAO_COMPARECEU) com totais e taxa, para acompanhar falhas de comparecimento.
33. Como GERENTE, quero um relatório de ocupação (horas ocupadas por externo no período), para avaliar a carga de cada um.
34. Como INTERNO ou EXTERNO, quero ser impedido de acessar relatórios, pois são exclusivos de gerentes.
35. Como administrador do sistema, quero que exista um GERENTE inicial provisionado automaticamente, para conseguir operar a API desde o primeiro boot.
36. Como consumidor da API, quero respostas de erro padronizadas (timestamp, status, error, message, path), para tratar falhas de forma previsível.
37. Como consumidor da API, quero documentação Swagger com descrição e exemplos de request/response, para integrar com facilidade.
38. Como auditor, quero que toda entidade registre quando e por quem foi criada e alterada, para rastreabilidade.
39. Como operador, quero subir a aplicação e o PostgreSQL via Docker Compose, para rodar localmente com facilidade.

## Implementation Decisions

**Módulos (arquitetura em camadas, pacote raiz `com.unimobili.agenda`):**
`controller`, `service`, `repository`, `entity`, `dto`, `mapper` (MapStruct),
`security`, `config`, `exception`. Princípios SOLID.

**Entidades:**
- `User`: id (UUID), nome, email (único), senha (BCrypt), telefone, cargo,
  role (enum INTERNO|EXTERNO|GERENTE — **um papel por usuário**), ativo (boolean),
  campos de auditoria.
- `Event`: id (UUID), titulo, descricao, dataHoraInicio, dataHoraFim, status
  (enum), externalUser (FK→User), createdBy (FK→User), campos de auditoria.

**Agenda:** não é entidade própria — é uma **visão derivada** dos eventos de um
externo. `GET /schedules` e `GET /schedules/{externalUserId}` consultam eventos.

**Data/hora:** armazenadas em UTC (`Instant` / coluna `timestamptz`),
entrada/saída ISO-8601 com offset. Criação de evento no passado é rejeitada.

**Conflito de horário:** verificado por externalUser, contra eventos ocupantes
(AGENDADO, CONFIRMADO, REALIZADO). CANCELADO e NAO_COMPARECEU liberam o slot.
Regra de overlap: `novoInicio < existenteFim AND existenteInicio < novoFim`.

**Máquina de estados do status:**
```
AGENDADO       → CONFIRMADO, CANCELADO
CONFIRMADO     → REALIZADO, CANCELADO, NAO_COMPARECEU
REALIZADO      → (terminal)
CANCELADO      → (terminal)
NAO_COMPARECEU → (terminal)
```
Transições inválidas → 409.

**Autorização:**
- Gestão de usuários: somente GERENTE.
- Criar/editar/cancelar/status de evento: INTERNO (apenas os que **criou**) e
  GERENTE (todos). EXTERNO: somente leitura da própria agenda.
- Relatórios: somente GERENTE.

**Soft delete:** `DELETE /events/{id}` → status CANCELADO; `DELETE /users/{id}`
→ `ativo=false`. Sem remoção física, para preservar histórico/integridade.

**Segurança (JWT):** login retorna access token (curto) + refresh token (JWT
**stateless**, sem tabela). `POST /auth/refresh` emite novo access. Senhas com
BCrypt. Rotas protegidas via Spring Security + autorização por papel.

**Bootstrap:** migration Flyway cria o GERENTE inicial; credenciais via variável
de ambiente; senha em hash BCrypt.

**Auditoria:** Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`,
`@CreatedBy`, `@LastModifiedBy`) + `AuditorAware<UUID>` lendo o SecurityContext.

**Tempo:** uso de um `java.time.Clock` injetável (bean) em vez de
`Instant.now()` direto, para validações de "sem passado" e conflito serem
testáveis de forma determinística.

**Erros:** `@RestControllerAdvice` global com payload padronizado
`{timestamp, status, error, message, path}`. Mapeamento: 400 (validação),
401 (não autenticado), 403 (sem permissão), 404 (não encontrado),
409 (conflito de agenda/transição inválida).

**API:**
- Auth: `POST /auth/login`, `POST /auth/refresh`.
- Usuários: `GET /users` (paginado), `GET /users/{id}`, `POST /users`,
  `PUT /users/{id}` (sem senha), `DELETE /users/{id}`.
- Agendas: `GET /schedules`, `GET /schedules/{externalUserId}` (filtro período).
- Eventos: `GET /events` (paginado, filtros), `GET /events/{id}`, `POST /events`,
  `PUT /events/{id}`, `DELETE /events/{id}`, `PATCH /events/{id}/status`.
- Relatórios: `GET /reports/events`, `/reports/events-by-user`,
  `/reports/cancellations`, `/reports/occupancy` (filtros: período, externo,
  interno, status).

**Listagens:** paginadas via Spring Data `Pageable` (`page`, `size`, `sort`),
retorno `Page`.

**Documentação:** springdoc-openapi com descrição e exemplos de request/response
em todos os endpoints.

## Testing Decisions

**O que é um bom teste:** valida comportamento externo observável (resposta HTTP
+ efeito no banco), não detalhes de implementação. Não se mocka service/repository
internos.

**Seam principal — fronteira HTTP:** testes de integração ponta-a-ponta com
`@SpringBootTest`, MockMvc (ou RestAssured) e **Testcontainers** rodando um
PostgreSQL real. Cada teste autentica via JWT real, dispara requests reais e
verifica resposta e estado persistido.

**Seam secundário — `Clock` injetável:** nos testes, substituir o bean `Clock`
por um relógio fixo, para validar de forma determinística "sem eventos no
passado" e a detecção de conflito por horário.

**Módulos cobertos pelos testes (via HTTP):**
- Auth: login válido/ inválido, refresh, acesso a rota protegida sem/with token.
- Usuários: CRUD, unicidade de e-mail, senha mínima, soft delete, restrição por papel.
- Eventos: criação válida, externalUser inválido (não-EXTERNO/inativo),
  fim ≤ início, evento no passado, conflito de horário (incl. cancelados liberando
  slot), propriedade (INTERNO só os próprios), transições de status válidas/inválidas.
- Agendas: visibilidade (EXTERNO só a própria; INTERNO/GERENTE todas), filtro de período.
- Relatórios: acesso só GERENTE; correção das métricas (eventos, por usuário,
  cancelamentos/taxa, ocupação).
- Erros: formato padronizado e códigos corretos (400/401/403/404/409).

**Prior art:** projeto greenfield — sem testes existentes. Os testes de
integração HTTP + Testcontainers servirão de padrão/prior art para os próximos.

**Cobertura:** mínima de 80%.

## Out of Scope

- **Frontend** (qualquer UI).
- **Notificações** (e-mail/push/in-app) — citadas no objetivo original, mas sem
  entidade/endpoint/regra; explicitamente fora.
- **Exportação de relatórios** (CSV/Excel) — adiada; relatórios retornam JSON.
- **Troca/reset de senha** via API (incl. self-service); senha só é definida na
  criação do usuário.
- **Self-update de perfil** por usuários comuns.
- **Múltiplos papéis por usuário.**
- **Revogação/logout de refresh token** (refresh é stateless até expirar).
- **Horário de funcionamento/configuração de agenda** por externo.

## Further Notes

- Projeto greenfield: não há código, git ou issue tracker preexistentes.
- Stack: Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA/Hibernate,
  JWT, Bean Validation, Lombok, MapStruct, Maven; PostgreSQL; Flyway; Docker +
  Docker Compose; JUnit 5, Mockito, Testcontainers.
- Plano de execução em marcos (no `PLANO.md`): scaffold → auth → usuários →
  eventos → agendas → relatórios → qualidade (Swagger + testes ≥80%).
