# PROCESS.md — Ledger-Api

> Política do projeto. Não é backlog nem história: o estado vivo mora em `TODO.md`;
> as decisões em `docs/decisions/`; os slices encerrados em `docs/history/`.

**Contrato de releitura:** o agente lê o `TODO.md` inteiro a cada turno; este `PROCESS.md`
no primeiro turno da sessão e antes de gates/debates; os ADRs por link, relidos quando o
tópico é tocado.

## Fluxo oficial (1 problema por vez)

```
0. Backlog — lista tudo, ordena por prioridade
-> 1. Foco — puxa 1 item do topo, congela o resto
-> 2. Entender — causa + impacto no ledger
-> 3. Soluções — 2-3 opções + [Opinião] do tutor
-> 4. Decisão — tradeoffs explícitos + [Sugestão] do tutor
   ... ⏸ ESPERA sua decisão (sigo / ajusto / veto)
```

## Regras de processo (imutáveis)

1. **Débito exige gatilho** — nunca "later"; sempre "later, quando X".
2. **Estado intermediário sem débito é proibido.** Um passo pode não ter comportamento
   observável, desde que (a) compile/rode e (b) registre um débito D com gatilho explícito.
3. **Cobertura é orçamento finito.** O sistema tem `n` caminhos; testar perto de `n` reduz
   a chance de bug, mas nunca a zera. Gastar em contratos externos e regras de negócio,
   não em comportamento de biblioteca.
4. **Pirâmide alvo 70/20/10.** Medir o desvio no conjunto da suíte, a cada rodada; não
   declarar convergência. Escada: regra de domínio → unitário; rota/contrato → integração;
   invariante entre endpoints → e2e. Não subir camada sem necessidade.
5. **Gates.** Decisão de arquitetura ESPERA decisão humana. Gates de arquitetura (regra 5)
   exigem entrada adversarial (regra 6).
6. **Gate de arquitetura exige entrada adversarial.** O agente apresenta o espaço de
   opções incluindo posições FORA do frame inicial; se todas as opções pressupõem a mesma
   coisa, a premissa não foi testada. Registrar o steelman contra a opção preferida.
7. **Cadência de teste.** Toda task fecha com o teste correspondente no nível certo —
   unitário para regra de domínio, integração para contrato HTTP, e2e para jornada. O
   teste da task é executado e verde antes de marcar a task como paga. A cada **3 tasks
   fechadas**, roda a regressão completa (`./mvnw clean verify`) e mede a distribuição da
   pirâmide. A rodada de 3 não substitui o teste da task. Task sem comportamento
   executável declara a verificação usada; se for estado intermediário, exige débito D
   com gatilho. O passo 4 (Decisão) registra qual teste prova a task.
8. **Debate por slice.** Ao encerrar um slice vertical (itens fechados + dívidas triadas
   + rodada verde). Gatilho de segurança: 8 conclusões sem fechar → debate antecipado.
9. **Delegação.** Revogada em 2026-11-11 — não há divisão de trabalho.
10. **Fluxo 0–4** (acima): 1 problema por vez; 2–3 opções com viés explícito; ESPERA na
    fase 4 (Decisão).
11. **Anti bang-bang.** Nenhum slice é implementado inteiro de uma vez. Antes do primeiro
    código, o slice é cortado em fatias verticais pequenas; cada fatia entrega um contrato
    observável ou um comportamento de erro, com teste no nível certo. Se o slice tem mais
    de uma operação, ele já nasce dividido. “Ledger” não é um slice: é o guarda-chuva de
    `deposits`, `transfers` e `statements`; cada um fecha sozinho.
    **Default:** só o parágrafo acima. O parágrafo de granularidade viva é acionado pelo
    humano quando requerido: a granularidade pode ser refinada quando a fatia se mostrar
    grande ou pequena demais; a mudança exige registro no `TODO.md` e no changelog deste
    arquivo, sem renumerar task já fechada. Se uma fatia passar de um contrato observável,
    ela é dividida antes de continuar.
12. **Git é humano.** A cada entrega de task, o humano controla o índice, o `commit` e o
    `push`. O modelo não executa `git add`, `commit` nem `push`; ele inspeciona o último
    versionamento (`git status`, `git diff`, `git log -1`) e sugere o corpo da mensagem
    **em inglês**, referenciando a task e os testes executados. Se a task já estiver
    commitada, o modelo aponta isso em vez de sugerir duplicata.
13. **Documento vivo de decisões.** Ao encerrar um slice, **depois do debate de
    arquitetura e antes de marcar o slice como fechado**, revisar
    `docs/decisions/README.md` e sincronizar os ADRs tocados: status, código/teste
    correspondente e eventual decisão obsoleta ou `superseded`. Atualizar `README.md`,
    `TODO.md` e `docs/history/slice-*.md` no mesmo movimento. Se uma task alterar uma
    decisão antes do fim do slice, o ADR é atualizado imediatamente; a sincronização
    completa é no fechamento. Nenhum ADR descreve comportamento que não existe; nenhuma
    decisão implementada fica só no código.

## Pirâmide de testes (alvo)

| Camada | O que valida | Como roda |
|---|---|---|
| **Unit (~70%)** | Regra isolada (domínio/entidade), sem Spring e sem banco | JUnit + AssertJ, milissegundos |
| **Integração (~20%)** | Contrato HTTP por endpoint, atravessando Controller→Service→Repository→Postgres real | `@SpringBootTest(RANDOM_PORT)` + `RestTestClient` + Testcontainers |
| **E2E (~10%)** | Jornada multi-endpoint com estado acumulado e regra dura do negócio (ex.: depósito + transferência idempotente + extratos conciliados) | Fluxo encadeado; só após `deposit`/`transfer`/extrato |

Distribuição viva no `TODO.md`. O e2e nasce no extrato, junto com D8/D20 (ADR-012); a
transferência fecha sem e2e para não duplicar os ITs e só prova a jornada quando o extrato
existe.

## Changelog de emendas

- **2026-11-26** — exemplo da camada e2e atualizado para “extratos conciliados” após o
  reforço do cenário Cucumber; nenhuma regra de processo mudou.
- **2026-11-22** — e2e da transferência adiado para o extrato (D20), junto com D8/BDD;
  justificativa no ADR-012: o `TransferHappyPathIT` já cobre o fluxo atual e, sem extrato,
  a jornada não prova auditabilidade nem ordem do ledger.
- **2026-11-14** — regra 7 revista: teste por task + regressão completa a cada 3 tasks;
  regra 11 anti bang-bang, com granularidade viva acionada pelo humano; regra 12 git
  humano (modelo só inspeciona e sugere mensagem em inglês); regra 13 documento vivo de
  decisões, com mapa em `docs/decisions/README.md` e revisão após o debate de arquitetura.
- **2026-11-11** — remoção das etapas 5 (Explicação) e 6 (Avaliação) do fluxo; o fluxo
  termina na etapa 4 (Decisão); removido o resquício “build/automação é a última escolha”
  da regra 5.
- **2026-11-11** — extinção da divisão de trabalho: regra 9 revogada; rótulos de dono
  removidos dos documentos.
- **2026-11-09** — criação do `PROCESS.md` (split D17): consolidação das regras 1–10, do
  fluxo 0–6, da pirâmide e da delegação; contrato de releitura; remoção da coluna "Hoje"
  da pirâmide (contagem viva vai para o `TODO.md`).
- **2026-11-08** — regra 6 (entrada adversarial) nasce do incidente S4/SRP: "invariantes na
  entidade" nunca esteve no espaço de opções até a tensão externa.
- **2026-11-05 a 2026-11-08** — regras 2 (estado intermediário) e 3 (limite de teste)
  consolidadas durante o planejamento do slice `deposits`.

> Emendas a este arquivo exigem changelog datado. Gates (regras 5/6) só mudam com entrada
> adversarial (regra 6) — sem isso, o incidente S4 vira instituição.
