# CONTEXT — Glossário de domínio

Vocabulário ubíquo da API de Agenda Corporativa. Use estes termos nos nomes de
módulos, interfaces e testes.

## Papéis (Role)

- **GERENTE** — acesso total: gere usuários, vê tudo, consulta relatórios.
- **INTERNO** — funcionário interno: cria e gere eventos (apenas os que criou).
- **EXTERNO** — funcionário externo: possui agenda própria; só visualiza a própria.

## Conceitos

- **Evento** — um agendamento na agenda de um funcionário EXTERNO. Tem um ciclo de
  vida (AGENDADO → CONFIRMADO → REALIZADO/CANCELADO/NAO_COMPARECEU).
- **Agenda** — visão derivada do conjunto de eventos de um funcionário EXTERNO
  (não há tabela própria).
- **Conflito** — sobreposição de horário na agenda de um mesmo EXTERNO, considerando
  apenas status que ocupam o slot (AGENDADO/CONFIRMADO/REALIZADO).
- **Período** — janela de tempo (de/até) usada em filtros e relatórios.

## Arquitetura

- **Actor** — o usuário autenticado que executa uma ação, como objeto de domínio
  (id + Role). Concentra as decisões de autorização que a rota não expressa:
  propriedade de evento (INTERNO só os próprios) e visibilidade por linha
  (EXTERNO só a própria agenda). É um value object puro (sem Spring/JPA), testável
  pela interface; construído a partir do JWT por `CurrentActor`. O gating grosso por
  papel/rota permanece no `SecurityConfig`.
