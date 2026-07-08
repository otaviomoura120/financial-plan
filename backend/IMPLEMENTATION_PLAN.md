# Plano — Estabilização do Módulo Financeiro Core + Transferência entre Contas + Telas de Frontend

> **Passo 0 (ao aprovar este plano):** exportar este arquivo para `backend/IMPLEMENTATION_PLAN.md` (raiz da pasta `backend/`), para servir de referência persistida no repositório durante a execução das tarefas.

## Contexto

A parte de segurança (Auth0, Spaces, Roles, EndpointPermission/RoleEndpointPermission, GroupMenu, Invites) já está pronta. O pedido original era planejar a implementação das "funcionalidades de fato" do app (BankAccount, Category, SubCategory, PaymentMethod, Transaction, Reports), com spec de campos/regras por entidade e tarefas ordenadas.

A exploração do código revelou que **o domain e o application layer desses cinco itens já estão praticamente todos implementados** — não é um módulo a construir do zero. Porém foram encontrados gaps críticos que tornam parte do módulo não-funcional em runtime, apesar do código parecer completo:

- `TransactionRepositoryImpl` e `SubCategoryRepositoryImpl` são **stubs vazios** (`save/update/findById` retornam `null`, `delete` é no-op). Hoje `POST /transactions` e `POST /reports` (que depende de `TransactionRepository.findByFilter`) não persistem/consultam nada de verdade.
- Nenhum service de `application/transaction/` chama `BankAccount.credit()/debit()` — registrar uma transação não afeta o saldo da conta.
- Não existe suporte a transferência entre contas.
- Segurança fina (`@PreAuthorize`) não está aplicada nos controllers do módulo core (BankAccount/Category/PaymentMethod/Transaction/Report), diferente de Roles/EndpointPermissions/GroupMenu.
- Nenhuma página de frontend existe ainda para essas 5 entidades.

Decisões já validadas com o usuário:
1. Priorizar **estabilizar o módulo atual** antes de módulos futuros (dívidas, cartões, contas fixas etc.) — esses ficam fora deste plano.
2. Saldo da BankAccount deve ser **atualizado automaticamente** a partir do ciclo de vida da Transaction (criar/editar/excluir).
3. Saldo **pode ficar negativo** — não há guard de "saldo insuficiente" (ex: representa cheque especial). Reversão de uma transação antiga (update/delete) **nunca é bloqueada**.
4. Adicionar suporte a **transferência entre contas** como um novo `TransactionType.TRANSFER`, com campo `destinationBankAccountId` na própria `Transaction` (não duas transactions linkadas, não uma entidade separada).
5. Nos Reports, `TRANSFER` aparece na listagem de transações do período, mas **não entra** em `totalIncome`/`totalExpense`/`balance`.
6. Documentar as entidades como estão hoje + propor ajustes de regra explicitamente marcados como propostos.
7. Além do backend, o plano cobre a criação das telas de frontend (hoje inexistentes) que consomem esses endpoints — começando pelos cadastros básicos (Payment Methods, Bank Accounts, Categories/SubCategories) e só depois as páginas principais de uso diário (Transactions, Reports), seguindo o padrão de CRUD já usado em `pages/roles/index.vue`.
8. Toda vez que um endpoint novo (backend) ganhar `@PreAuthorize`, `backend/docs/seed.sql` precisa ser atualizado com o registro correspondente em `endpoint_permissions` (e `role_endpoint_permissions` quando aplicável) — regra permanente, não só para esta rodada.

## Regras de execução (valem para todas as tarefas abaixo)

- **Toda tarefa de backend só pode ser marcada como concluída depois que os testes unitários (Groovy/Spock) dela passarem.** Não é permitido fechar uma tarefa com `./gradlew test` quebrado ou sem a spec correspondente escrita — a cobertura de teste faz parte da definição de pronto, não é uma etapa separada no fim.
- **Toda tarefa (backend ou frontend) que cria/altera um fluxo deve atualizar ou criar a documentação correspondente** antes de ser considerada concluída (`backend/docs/APP_OVERVIEW.md`, `backend/docs/seed.sql`, ou um novo arquivo em `frontend/docs/`, conforme indicado em cada tarefa).
- Cada tarefa tem uma caixa de marcação `- [ ]` — o agente que executar deve marcar `- [x]` somente quando código + testes (se backend) + docs estiverem todos feitos.

---

## Divisão em grupos de desenvolvimento

> Cada grupo abaixo é dimensionado para caber numa única sessão/agente sem estourar a janela de contexto — cada tarefa de backend já implica código + spec Groovy + doc + `./gradlew test`, o que consome bastante contexto por item. Tarefas marcadas como **independente/coringa** não têm restrição de ordem e devem ser encaixadas em qualquer sessão que sobrar espaço, em vez de reservar uma sessão exclusiva só para elas.

### Backend

| Grupo | Tarefas | Por que agrupadas assim |
|---|---|---|
| **B1** | T2, T3 | Mesma unidade domain+infra da `Transaction`: novo enum/campo (`TRANSFER`) e a persistência real que já nasce mapeando esse campo — evita reabrir os mesmos arquivos em sessões separadas. |
| **B2** | T4, T5 | Validação de FKs e o helper de efeito de saldo — preparam, mas ainda não tocam, os três services de ciclo de vida da Transaction. |
| **B3** | T6, T7 | Integração de criar/editar — mexem nos mesmos arquivos (`CreateTransactionService`, `UpdateTransactionService`, `TransactionBalanceEffectService`); fazer juntas mantém o raciocínio de apply/revert fresco na mesma sessão. |
| **B4** | T8, T9 | Fecha o ciclo de vida (delete + revert) e, em seguida, valida o resultado agregado (reports) — sequência natural de confirmação do que foi feito em B3. |
| **B5** | T9b | Isolamento multi-tenant no filtro de Transaction — sensível (vazamento de dados entre spaces), melhor isolada em sua própria sessão. |
| **B6** | T9c | 4 services novos + 4 specs novas (listagens) — grande o suficiente para ficar sozinha, sem perder o fio entre as 4 entidades. |
| **B7** | T10 | Segurança fina em 5 controllers + atualização de `seed.sql` — grande e sensível, não deve ser misturada com lógica de negócio de outra tarefa. |
| **B8** | T12 | Gate final — depende de todas as anteriores, roda sozinha ao final como critério objetivo de conclusão. |
| **coringa** | T1, T11, T13 | Independentes entre si e sem overlap de arquivo com a cadeia principal (T2→T12) — encaixar como "extra" em qualquer grupo acima quando sobrar contexto na sessão. |

> Os grupos abaixo (`P`/`CC`/`AP`/`RPT`/`GATE`) pertencem aos módulos novos de **Cartão de Crédito** e **Contas a Pagar** (ver seções "Spec das novas entidades" e "Tarefas de Backend/Frontend — Cartão de Crédito e Contas a Pagar" mais adiante). Usam prefixos próprios em vez de continuar B9/B10 porque são dois módulos grandes que não fazem parte da cadeia de estabilização B1-B8 — os prefixos deixam claro onde termina "core" e começa "novo", e permitem que CC e AP avancem em paralelo por sessões diferentes (só sincronizam em P1 no início e RPT1/GATE1 no fim).

| Grupo | Tarefas | Por que agrupadas assim |
|---|---|---|
| **P1** | P1 | Fundação compartilhada (rastreabilidade de pagamento em `Transaction`) — precisa existir antes de CC6/CC7/AP5/AP6. |
| **CC1** | CC1 | `CreditCard` + `CreditCardInvoiceCycle` — domínio+persistência da entidade e da calculadora de ciclo de fatura. |
| **CC2** | CC2 | CRUD de `CreditCard` + controller já com `@PreAuthorize` + seed. |
| **CC3** | CC3 | `CreditCardInvoicePayment` — domínio+persistência, isolada por ser pequena e só usada a partir de CC5/CC6. |
| **CC4** | CC4 | `CreditCardTransaction` — domínio+persistência com isolamento por Space desde o início, já com campos de parcelamento. |
| **CC5** | CC5 | CRUD de `CreditCardTransaction` + guarda de "mês já pago" + criação de compra parcelada (N linhas). |
| **CC5b** | CC5b | Antecipação das últimas N parcelas de uma compra para a fatura aberta solicitada — ação dedicada, sem mover dinheiro. |
| **CC6** | CC6 | Pagamento de fatura + listagem — reaproveita `TransactionBalanceEffectService` do core. |
| **CC7** | CC7 | Desfazer pagamento de fatura — ação dedicada e isolada (decisão validada: sem cascade automático). |
| **AP1** | AP1 | `Bill` (template) — domínio+persistência. |
| **AP2** | AP2 | `BillInstance` (ocorrência) — domínio+persistência com isolamento por Space desde o início. |
| **AP3** | AP3 | CRUD de `Bill` (básico + agenda dedicada) + auto-criação de instância avulsa + controller. |
| **AP4** | AP4 | Geração sob demanda de instâncias + listagem "contas do mês". |
| **AP5** | AP5 | Pagamento de conta + edição de valor. |
| **AP6** | AP6 | Desfazer pagamento de conta — ação dedicada, espelha CC7. |
| **RPT1** | RPT1 | Saldo previsto em `GenerateReportService` — única tarefa que toca o Report para os dois módulos. |
| **GATE1** | GATE1 | Gate final dos dois módulos novos. |

### Frontend

| Grupo | Tarefas | Por que agrupadas assim |
|---|---|---|
| **F1** | F1 | Piloto do padrão (tela mais simples: só `name` + `active`) — valida o template antes das próximas telas. |
| **F2** | F2 | Bank Accounts — mesmo padrão de F1, com a regra extra de saldo não-editável. |
| **F3** | F3 | Categories + SubCategories — inclui um dialog secundário (sub-recurso), por isso maior que F1/F2. |
| **F4** | F4 | Transactions — página principal, maior complexidade de formulário (campos condicionais por `type`); depende dos selects de F1-F3. |
| **F5** | F5 | Reports — só leitura, reaproveita formatação de linha de F4. |
| **coringa** | F6 | Atualização de `PRODUCT.md`, sem dependência técnica de nenhuma tela — encaixar a qualquer momento, inclusive em paralelo aos grupos acima. |

> Grupos dos módulos novos (ver "Tarefas de Frontend — Cartão de Crédito e Contas a Pagar" mais adiante):

| Grupo | Tarefas | Por que agrupadas assim |
|---|---|---|
| **FCC1** | FCC1 | Cadastro de Cartões — piloto do padrão para o módulo de cartão. |
| **FCC2** | FCC2 | Lançamentos no cartão + parcelamento e antecipação de parcelas — dialog secundário a partir da linha do cartão. |
| **FCC3** | FCC3 | Fatura, pagamento e reversão — maior complexidade (confirmação dedicada de "desfazer"). |
| **FAP1** | FAP1 | Cadastro de Contas a Pagar (template + agenda). |
| **FAP2** | FAP2 | Contas do mês — pagar/editar valor/desfazer. |
| **FRPT1** | FRPT1 | Atualização da tela de Reports com saldo previsto. |

---

## Spec por entidade

### BankAccount (`domain/BankAccount.java`)
**Campos:** id, version, space (Space), name, bankName, balance (BigDecimal), active, createdDate, updatedDate.
**Regras atuais:** `validate()` exige name não-blank, space não-nulo, balance não-nulo. `credit(amount)`/`debit(amount)` exigem amount > 0; nenhum dos dois valida saldo resultante. `update(name, bankName)`, `deactivate()` (soft delete), lock otimista manual via `setVersion`.
**Ajustes propostos:** nenhuma mudança na classe é necessária — `credit()`/`debit()` já servem para o efeito de INCOME/EXPENSE/TRANSFER sem alteração, já que saldo negativo é permitido e reversão não precisa de guard.

### Category / PaymentMethod
Estrutura idêntica entre si: id, version, space, name, active, createdDate, updatedDate. `validate()` exige space + name; `rename()`, `deactivate()`, lock otimista. Sem ajustes propostos — fora do escopo desta rodada.

### SubCategory (`domain/SubCategory.java`)
**Campos:** id, version, categoryId (Long — FK simples, não objeto), name, active, createdDate, updatedDate.
**Regras atuais:** `validate()` exige name + categoryId; `rename()`, `deactivate()`.
**Gap:** persistência é stub (ver T1). `SubCategoryEntityJpa` não tem colunas de timestamp — `createdDate`/`updatedDate` do domain hoje seriam perdidos na persistência; aceitar como limitação conhecida (fora de escopo alterar o schema).

### PaymentMethod
(ver Category acima — mesmo padrão)

### Transaction (`domain/Transaction.java`) — entidade central, com mudanças propostas
**Campos atuais:** id, version, type (TransactionType: INCOME/EXPENSE), userId, bankAccountId, categoryId, subCategoryId (opcional), paymentMethodId, amount (BigDecimal > 0), transactionDate (LocalDate), description, createdDate, updatedDate.
**Regras atuais:** `validate()` exige type/userId/bankAccountId/categoryId/paymentMethodId/amount>0/transactionDate; `subCategoryId` opcional. `update(...)` não permite mudar userId. `isIncome()`/`isExpense()`. Sem soft-delete (não tem `active`) — hard delete é o comportamento esperado.
**Gap crítico:** `TransactionRepositoryImpl` é stub total (ver T3). Nada integra com `BankAccount.credit()/debit()` (ver T6-T8). `CreateTransactionService` não valida existência de nenhuma FK.

**Ajustes propostos (mudança de schema):**
- Novo valor de enum `TransactionType.TRANSFER`.
- Novo campo `destinationBankAccountId: Long` (nulo exceto quando `type=TRANSFER`).
- `validate()` passa a ramificar: se `TRANSFER` → exige `bankAccountId` (origem), `destinationBankAccountId` (destino), e que sejam diferentes; `categoryId`/`paymentMethodId` tornam-se opcionais para esse tipo. Se `INCOME`/`EXPENSE` → mantém as regras atuais (categoryId/paymentMethodId obrigatórios).
- Efeito no saldo: INCOME credita `bankAccountId`; EXPENSE debita `bankAccountId`; TRANSFER debita `bankAccountId` (origem) e credita `destinationBankAccountId` (destino).

### Report (`application/report/GenerateReportService.java`)
Sem domain próprio — deriva de `TransactionRepository.findByFilter`. `totalIncome`/`totalExpense` já são somados via `Transaction.isIncome()`/`isExpense()`, que naturalmente **já excluem `TRANSFER`** (nem income nem expense) sem precisar de mudança de lógica — só precisa ser validado depois que a persistência estiver corrigida (T9).

---

## Tarefas de Backend (ordem de dependência)

> **[coringa]** independente da cadeia principal — encaixar em qualquer sessão/grupo abaixo quando sobrar contexto.

- [x] **T1 — Persistência real de SubCategory**
Criar `infrastructure/repository/jpa/JpaSubCategoryRepository.java` (`extends JpaRepository<SubCategoryEntityJpa, Long>` + `findByCategoryId`). Reescrever `SubCategoryRepositoryImpl.java` com mapeamento direto (sem Space), seguindo o padrão de `PaymentMethodRepositoryImpl.java`. Independente do resto, pode ser feito a qualquer momento.
**Testes (obrigatório):** specs de `CreateSubCategoryService`/`UpdateSubCategoryService`/`DeleteSubCategoryService` (mockando `SubCategoryRepository`, `CategoryRepository`) cobrindo sucesso e categoria pai inexistente — `./gradlew test` verde antes de fechar.
**Docs:** revisar `backend/docs/APP_OVERVIEW.md` (seção SubCategory / REST API Reference) — confirmar que o contrato já descrito bate com o comportamento real agora que não é mais stub; nenhuma mudança de contrato esperada.
*Pronto quando:* `POST/PUT/DELETE /categories/subcategories/*` persistem e consultam de verdade.

### [Grupo B1] Fundação: domínio TRANSFER + persistência de Transaction

- [x] **T2 — Suporte a TRANSFER no domain Transaction**
Editar `domain/enums/TransactionType.java` (add `TRANSFER`). Editar `domain/Transaction.java`: novo campo `destinationBankAccountId`, atualizar construtor, `update(...)`, e `validate()` com a ramificação descrita acima.
**Testes (obrigatório):** `domain/TransactionSpec.groovy` (criar se não existir) cobrindo `validate()` para os 3 tipos: INCOME/EXPENSE (categoryId/paymentMethodId obrigatórios), TRANSFER (destinationBankAccountId obrigatório e diferente de bankAccountId, categoryId/paymentMethodId dispensados).
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md` seção "Transaction" com o novo campo `destinationBankAccountId` e o valor `TRANSFER`.
*Pronto quando:* `Transaction.validate()` cobre os três tipos corretamente e a spec passa.

- [x] **T3 — Persistência real de Transaction (já com destinationBankAccountId)**
Criar `infrastructure/repository/jpa/JpaTransactionRepository.java` — `extends JpaRepository<TransactionEntityJpa, Long> & JpaSpecificationExecutor<TransactionEntityJpa>` (specification para suportar os filtros opcionais de `findByFilter`). Adicionar coluna `destinationBankAccountId` em `TransactionEntityJpa`. Reescrever `TransactionRepositoryImpl.java` com mapeamento direto de campos escalares, seguindo a estrutura de `BankAccountRepositoryImpl.java` (sem resolver Space).
*Depende de:* T2 (campo/enum já precisam existir para mapear).
**Testes (obrigatório):** cobertura indireta via specs de T4/T6 (que mockam `TransactionRepository`, então a implementação JPA em si é validada por teste manual/integration — documentar no PR os cenários manuais executados: create/update/delete/findByFilter contra o MySQL local).
**Docs:** nenhuma mudança de contrato público — sem edição de doc necessária.
*Pronto quando:* `POST/PUT/DELETE /transactions` persistem de verdade; `findByFilter` retorna resultados corretos para cada combinação de filtros.

### [Grupo B2] Validação de FKs + helper de efeito de saldo

- [x] **T4 — Atualizar DTOs e validação de FKs em CreateTransactionService**
Adicionar `destinationBankAccountId` em `CreateTransactionRequest`, `UpdateTransactionRequest`, `TransactionResponse` (`application/transaction/dto/`). Em `CreateTransactionService`, injetar `BankAccountRepository`, `CategoryRepository`, `SubCategoryRepository`, `PaymentMethodRepository`, `UserRepository` e validar existência de cada FK antes de montar a `Transaction` (padrão de `CreateBankAccountService`, que verifica `Space` existe): `bankAccountId` sempre; `destinationBankAccountId` só se `TRANSFER`; `categoryId`/`paymentMethodId` só se não-`TRANSFER`; `subCategoryId` se informado; `userId` sempre.
*Depende de:* T3.
**Testes (obrigatório):** `CreateTransactionServiceSpec.groovy` — sucesso INCOME/EXPENSE/TRANSFER, cada FK inexistente retornando `DomainException`, TRANSFER com `bankAccountId == destinationBankAccountId` rejeitado.
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md`, seção "Key Flows → 2. Recording a Transaction", com o novo campo e a validação de FKs.
*Pronto quando:* `POST /transactions` com qualquer FK inexistente retorna 422 em vez de criar registro órfão, e a spec passa.

- [x] **T5 — Helper compartilhado de efeito de saldo**
Criar uma classe de aplicação (ex: `application/transaction/TransactionBalanceEffectService.java`) injetável em Create/Update/Delete, com dois métodos:
- `apply(Transaction t)`: INCOME → `bankAccountRepository.findById(t.bankAccountId).credit(amount)` + update; EXPENSE → mesma conta, `debit`; TRANSFER → `debit` na conta de origem + `credit` na conta de destino, ambas persistidas.
- `revert(Transaction t)`: espelha o inverso de cada caso (INCOME→debit, EXPENSE→credit, TRANSFER→credit na origem + debit no destino).
Isso evita duplicar a lógica de "qual conta(s) afetar por tipo" nos três services de Transaction.
*Depende de:* T2, T3.
**Testes (obrigatório):** `TransactionBalanceEffectServiceSpec.groovy` cobrindo `apply`/`revert` para os 3 tipos, mockando `BankAccountRepository`.
**Docs:** criar `backend/docs/transaction-balance-effect.md` explicando a regra de efeito/reversão por tipo (INCOME/EXPENSE/TRANSFER) — referência de manutenção futura para quem mexer nessa lógica.
*Pronto quando:* a spec isolada confirma que aplica/reverte corretamente os 3 tipos.

### [Grupo B3] Integração de criação e atualização

- [x] **T6 — Integrar em CreateTransactionService**
Depois de validar FKs (T4) e antes de salvar, chamar `balanceEffectService.apply(transaction)`. Envolver em `@Transactional` para atomicidade entre update(s) de BankAccount e save da Transaction.
*Depende de:* T4, T5.
**Testes (obrigatório):** estender `CreateTransactionServiceSpec.groovy` de T4 com verificação de que `BankAccountRepository.update` foi chamado com o saldo correto para cada tipo.
**Docs:** atualizar `backend/docs/transaction-balance-effect.md` (T5) e a seção "Key Flows → 2" do `APP_OVERVIEW.md` confirmando que criar uma transação já reflete no saldo.
*Pronto quando:* criar INCOME/EXPENSE/TRANSFER reflete corretamente o(s) saldo(s) das conta(s) envolvidas e a spec passa.

- [x] **T7 — Integrar em UpdateTransactionService**
Capturar a Transaction antiga completa antes de `update(...)`. Chamar `balanceEffectService.revert(old)`, então aplicar as mesmas validações de FK de T4 sobre os novos valores, montar a transaction atualizada e chamar `balanceEffectService.apply(updated)`. Cobre os casos de mudança de `type`, `amount`, `bankAccountId` e/ou `destinationBankAccountId`. `@Transactional`.
*Depende de:* T6.
**Testes (obrigatório):** `UpdateTransactionServiceSpec.groovy` — mudança de amount, mudança de type, mudança de bankAccountId/destinationBankAccountId, confirmando reversão + reaplicação corretas nas contas certas.
**Docs:** atualizar `backend/docs/transaction-balance-effect.md` com o fluxo de update (revert + reapply).
*Pronto quando:* editar qualquer combinação desses campos deixa o(s) saldo(s) corretos, sem duplicar nem perder efeito, e a spec passa.

### [Grupo B4] Fechamento do ciclo de vida + validação de Reports

- [x] **T8 — Integrar em DeleteTransactionService**
Hoje o service só faz `transactionRepository.delete(id)` sem buscar antes. Mudar para: `findById` (lançar `DomainException("Transaction not found")` se `null`), `balanceEffectService.revert(transaction)`, então `delete(id)`. `@Transactional`.
*Depende de:* T5, T3.
**Testes (obrigatório):** `DeleteTransactionServiceSpec.groovy` — reversão correta do saldo, e cenário "transaction not found" retornando `DomainException`.
**Docs:** atualizar `backend/docs/transaction-balance-effect.md` com o fluxo de delete (revert).
*Pronto quando:* excluir uma transação restaura o(s) saldo(s) ao estado anterior à criação dela, e a spec passa.

- [x] **T9 — Validar Reports fim-a-fim**
Testar com transações reais (INCOME/EXPENSE/TRANSFER) criadas via T6, confirmando que TRANSFER aparece na lista de `transactions[]` mas não entra em `totalIncome`/`totalExpense`/`balance`.
*Depende de:* T3, T6.
**Testes (obrigatório):** `GenerateReportServiceSpec.groovy` (criar — hoje não existe nenhum teste para este service) cobrindo filtros combinados e a exclusão de TRANSFER dos totais.
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md` seção "Key Flows → 3. Financial Report" explicitando que TRANSFER não entra nos totais.
*Pronto quando:* os valores batem com o esperado e a spec passa.

### [Grupo B5] Isolamento multi-tenant no filtro de Transaction

- [x] **T9b — Escopo por Space no filtro de Transaction (gap de isolamento multi-tenant)**
`Transaction` não guarda `spaceId` diretamente (só `bankAccountId`), e `TransactionRepository.findByFilter` hoje não recebe `spaceId` — uma consulta sem filtro de conta vazaria transações de **todos os spaces**. Adicionar `spaceId` como parâmetro obrigatório em `findByFilter(...)` e em `ReportFilterRequest`; na implementação JPA (`TransactionRepositoryImpl`, via `Specification`), restringir com uma subquery: `bankAccountId IN (SELECT id FROM bank_accounts WHERE space_id = :spaceId)`. Atualizar `GenerateReportService`/`ReportController` para exigir `spaceId` no request.
*Depende de:* T3.
**Testes (obrigatório):** estender `GenerateReportServiceSpec.groovy` (T9) com um cenário de duas contas em spaces diferentes, confirmando que o filtro por `spaceId` isola corretamente.
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md` seção Reports, deixando claro que `spaceId` é obrigatório no filtro.
*Pronto quando:* um `POST /reports` de um space nunca retorna transações de bank accounts de outro space, e a spec passa.

### [Grupo B6] Endpoints de listagem

- [x] **T9c — Endpoints GET/listagem faltantes (bloqueiam o frontend)**
Nenhum destes controllers tem endpoint de listagem hoje — sem isso as telas novas não têm como popular a tabela:
- `GET /bank-accounts?spaceId=` → novo `ListBankAccountsService`, reaproveitando `BankAccountRepository.findBySpaceId` (já existe).
- `GET /categories?spaceId=` → novo `ListCategoriesService`; para cada `Category`, buscar suas `SubCategory` via `SubCategoryRepository.findByCategoryId` e popular `CategoryResponse.subCategories` (hoje sempre retornado vazio nos outros services — aqui passa a ser real).
- `GET /payment-methods?spaceId=` → mesmo padrão simples.
- `GET /transactions?spaceId=&from=&to=&...` → novo `ListTransactionsService`, reaproveitando `TransactionRepository.findByFilter` (pós T3/T9b), com `from`/`to` opcionais (sem filtro de data = todas as transações do space).
*Depende de:* T1 (para Category/SubCategory retornar dados reais), T3 e T9b (para Transaction).
**Testes (obrigatório):** uma spec por novo service (`ListBankAccountsServiceSpec`, `ListCategoriesServiceSpec`, `ListPaymentMethodsServiceSpec`, `ListTransactionsServiceSpec`), cobrindo o caso feliz e space vazio (lista vazia).
**Docs:** atualizar a tabela "REST API Reference" do `backend/docs/APP_OVERVIEW.md`, adicionando a linha `GET` em cada uma das 4 seções (Bank Accounts, Categories, Payment Methods, Transactions).
*Pronto quando:* cada um desses GETs retorna os dados reais do space ativo e as specs passam.

### [Grupo B7] Segurança fina nos controllers

- [x] **T10 — Segurança fina nos controllers do módulo core**
Adicionar `@PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")` (padrão de `EndpointPermissionController.java`) em **todos** os métodos (incluindo os novos GETs de T9c) de `BankAccountController`, `CategoryController`, `PaymentMethodController`, `TransactionController`, `ReportController`.

**Atualizar `backend/docs/seed.sql` a cada endpoint novo** (regra permanente para qualquer tela nova daqui pra frente, não só desta rodada): adicionar um `INSERT INTO endpoint_permissions` (type=API) por endpoint, seguindo a numeração de `sequence` já usada. Importante: reaproveitar exatamente os mesmos `name` que já existem nas linhas `FRONT_PAGE` da seção 2 do seed (`'Contas Bancárias'`, `'Categorias'`, `'Formas de Pagamento'`, `'Transações'`, `'Relatórios'`) — os blocos de `ADMIN`/`MEMBER` na seção 5 já fazem `JOIN ... ON ep.name IN (...)` usando esses nomes, então as novas linhas API herdam `ALLOW` automaticamente sem precisar editar os blocos de ADMIN/MEMBER. `OWNER` já recebe tudo via `CROSS JOIN`. Lembrar que `CreateEndpointPermissionService` cria `DENY` automático para roles existentes ao criar uma `EndpointPermission` em runtime — o seed é o caminho recomendado para popular de uma vez.
*Independente das demais (pode rodar em paralelo), mas recomenda-se fazer depois de T3-T9c para não confundir bug de persistência com 403 de autorização.*
**Testes (obrigatório):** teste de integração/manual documentado (não há Spock de `@PreAuthorize` isolado no padrão atual do projeto) — confirmar via chamada real com token de um usuário sem `ALLOW` recebendo 403, e com `ALLOW` funcionando; se o projeto já tiver algum padrão de teste de `SecurityService`/`@PreAuthorize` (ver `SecurityServiceSpec.groovy`), seguir o mesmo padrão para os novos endpoints.
**Docs:** `backend/docs/seed.sql` (obrigatório, ver acima) + atualizar `backend/docs/APP_OVERVIEW.md` seção "Access Control Flow" se o padrão de proteção mudar.

> **[coringa]** independente — distribuir entre as sessões dos grupos acima em vez de reservar uma sessão só para isso.

- [x] **T11 — Specs Groovy/Spock remanescentes (fora do fluxo de Transaction)**
Cobrir os services que ficaram sem spec e não fazem parte da cadeia T1-T9c: `UpdateBankAccountServiceSpec`, `DeleteBankAccountServiceSpec`, `UpdateCategoryServiceSpec`, `DeleteCategoryServiceSpec`, `UpdatePaymentMethodServiceSpec`, `DeletePaymentMethodServiceSpec` — padrão de `CreateBankAccountServiceSpec.groovy` (mockando as interfaces de repository, nunca os `*RepositoryImpl`).
*Depende de:* nenhuma (independente, pode ser feito a qualquer momento em paralelo).
**Testes (obrigatório):** as próprias specs listadas acima, `./gradlew test` verde.
**Docs:** nenhuma — são apenas testes de comportamento já documentado.

### [Grupo B8] Gate final

- [x] **T12 — ArchUnit + suíte completa (gate final)**
Rodar `./gradlew test` (inclui `ArchitectureTest` + todas as specs de T1-T11). Confirmar que nenhuma regra de camada foi violada (o novo `TransactionBalanceEffectService` fica em `application`, não em `domain`/`infrastructure`).
*Depende de:* todas as anteriores.
**Testes (obrigatório):** este É o gate de teste — `./gradlew test` 100% verde é o critério de pronto.
**Docs:** nenhuma nova — apenas confirmar que todas as docs das tarefas anteriores foram de fato commitadas.

> **[coringa/opcional]** baixa prioridade, encaixar em qualquer sessão livre.

- [ ] **T13 — (opcional, baixa prioridade) Limpeza do código órfão Address**
Remover `domain/Address.java` e `infrastructure/repository/jpa/AddressEntityJpa.java` (sem `validate()`, sem uso em nenhum lugar). Confirmar com `grep -r "Address" src/main` antes. Totalmente independente, pode ser feito a qualquer momento.
**Testes (obrigatório):** `./gradlew test` continua verde após a remoção (nenhuma spec deve referenciar essas classes).
**Docs:** nenhuma — código nunca foi documentado publicamente.

### Fora de escopo (apenas registrado, não vira tarefa)
`UpdateSpaceService` não chama `validate()`; `DeleteSpaceService` faz hard delete sem checar existência/cascata de members-roles-invites; `AssignRoleRequest` é DTO órfão. Todos fora do módulo financeiro core.

---

## Tarefas de Frontend (Nuxt 4 + Vue 3 + Vuetify + Pinia) — `/home/dev/project/frontend`

Hoje **nenhuma página financeira existe** — nem `bank-accounts`, `categories`, `payment-methods`, `transactions` nem `reports` têm arquivo `.vue`, rota Nitro em `server/api/`, ou store. É greenfield. O menu lateral já é dirigido dinamicamente pelo backend (`useMenuStore` consome `GET /menu-structure`), e o `seed.sql` já tem as entradas de `group_menus`/`group_menu_children` para essas 5 telas — então **nenhuma edição de navegação estática é necessária**; os itens aparecem sozinhos assim que os `EndpointPermission` (FRONT_PAGE, já seedados) e API (T10) estiverem com `ALLOW` para o usuário.

**Padrão a replicar** (referência: `pages/roles/index.vue` + `components/dialogs/AddEditRoleDialog.vue` + `components/dialogs/ConfirmDialog.vue` + `server/api/roles/*`, ver `frontend/CLAUDE.md`):
- 1 página por entidade em `pages/<entidade>/index.vue`: `VTable` com filtro/paginação client-side (`computed` + `TablePagination`), estado local via `ref` (sem Pinia store por entidade — só `useSpaceStore`/`useMenuStore`/`useInviteStore` são globais), `watch(() => spaceStore.activeSpace, refetch)` para recarregar ao trocar de space.
- 1 dialog `components/dialogs/AddEdit<Entidade>Dialog.vue` reaproveitado para criar/editar (prop nulável alterna o modo), `VForm` + `rules` simples, `AppTextField`.
- `ConfirmDialog` genérico antes de excluir.
- `useApiError()` + `ApiErrorAlert` para erros; `useSnackbar()` para toast de sucesso/erro.
- 1 conjunto de rotas Nitro por entidade em `server/api/<entidade>/`: `index.get.ts`, `index.post.ts`, `[id].put.ts`, `[id].delete.ts`, cada uma só repassando para o backend via `useAuth0(event).getAccessToken()` + `buildBackendHeaders` (idêntico a `server/api/roles/*`).
- Textos de UI em pt-BR; código/variáveis em inglês (regra do `frontend/CLAUDE.md`).

**Ordem: primeiro as telas de cadastro básico (dependências de dropdown), depois as páginas principais que as consomem.**

### [Grupo F1] Payment Methods (piloto do padrão)

- [x] **F1 — Payment Methods** (`pages/payment-methods/`)
Tela mais simples (só `name` + `active`) — bom ponto de partida para validar o padrão. CRUD completo + `server/api/payment-methods/*`.
*Depende de:* backend T10 (endpoint com `@PreAuthorize` + seed) e o GET de T9c.
**Verificação:** rodar `pnpm dev`, abrir `/payment-methods`, criar/editar/excluir manualmente no navegador (este projeto não tem suíte de teste de frontend — a verificação é funcional/manual, seguindo a skill `verify`).
**Docs:** criar `frontend/docs/payment-methods.md` (seguir o formato de `frontend/docs/user-invite.md`) descrevendo rotas, campos do form e componentes usados.

### [Grupo F2] Bank Accounts

- [x] **F2 — Bank Accounts** (`pages/bank-accounts/`)
Form de criação: `name`, `bankName`, `initialBalance`. Form de edição: só `name`/`bankName` (balance não é editável diretamente, só via transações — refletir isso na UI, sem campo de saldo editável no form de edição, só exibido como read-only na tabela). Exclusão = soft delete (`active=false`, backend já faz isso).
*Depende de:* mesma base de F1 (padrão), backend T9c/T10.
**Verificação:** manual no navegador — criar conta, conferir saldo inicial exibido, editar nome/banco, desativar.
**Docs:** criar `frontend/docs/bank-accounts.md`.

### [Grupo F3] Categories + SubCategories

- [x] **F3 — Categories + SubCategories** (`pages/categories/`)
Lista de categorias (`name`, `active`). Gerenciar subcategorias como sub-recurso: seguir o padrão de `RolePermissionsDialog.vue` (dialog secundário aberto a partir de uma linha da tabela) — um `ManageSubCategoriesDialog.vue` que lista/cria/edita/exclui as subcategorias daquela categoria (`server/api/categories/[id]/subcategories/*`, espelhando `server/api/roles/[id]/permissions/*`).
*Depende de:* backend T1 (persistência real de SubCategory) + T9c.
**Verificação:** manual no navegador — criar categoria, abrir subcategorias, criar/editar/excluir subcategoria.
**Docs:** criar `frontend/docs/categories.md`.

### [Grupo F4] Transactions (página principal)

- [x] **F4 — Transactions** (`pages/transactions/`) — página principal
Form: `type` (INCOME/EXPENSE/TRANSFER — select), `bankAccountId` (select, populado por F2/GET bank-accounts do space ativo), `destinationBankAccountId` (só aparece se `type=TRANSFER`, mesma lista de contas, excluindo a selecionada em `bankAccountId`), `categoryId`+`subCategoryId` (selects em cascata, só aparecem se `type≠TRANSFER`, populados por F3), `paymentMethodId` (select, só se `type≠TRANSFER`, populado por F1), `amount`, `transactionDate` (date picker), `description` (opcional). Lista com filtro de período (date range, default mês atual) usando o novo `GET /transactions`.
*Depende de:* F1, F2, F3 (para os selects) e backend T2-T9c completos (domain+persistência+efeito de saldo+listagem funcionando).
**Verificação:** manual no navegador — criar INCOME/EXPENSE/TRANSFER, conferir que o saldo das contas envolvidas muda (checando em `/bank-accounts`), editar e excluir uma transação e conferir reversão do saldo.
**Docs:** criar `frontend/docs/transactions.md`.

### [Grupo F5] Reports

- [x] **F5 — Reports** (`pages/reports/`)
Página só de leitura: formulário de filtro (`from`/`to` obrigatórios, demais opcionais — mesmos campos de `ReportFilterRequest`) + cards de resumo (`totalIncome`/`totalExpense`/`balance`, seguir estilo de "informativo, não alarmante" do `PRODUCT.md`) + tabela das transações do período (reaproveitar a formatação de linha usada em F4, sem ações de editar/excluir aqui). `server/api/reports/index.post.ts` proxy simples.
*Depende de:* F4 (mesmos componentes de formatação de linha), backend T9b (escopo por space).
**Verificação:** manual no navegador — filtrar por período, conferir totais e que TRANSFER aparece na lista mas não nos totais.
**Docs:** criar `frontend/docs/reports.md`.

> **[coringa]** sem dependência técnica de nenhuma tela — encaixar a qualquer momento, inclusive em paralelo aos grupos acima.

- [x] **F6 — Ajustar `frontend/PRODUCT.md`**
O doc atual descreve o produto só como "admin control plane" (roles/permissions/spaces) e não menciona nada financeiro — está desatualizado frente ao propósito real do app (`backend/docs/APP_OVERVIEW.md`). Atualizar para refletir que o público final também inclui o usuário comum controlando as próprias finanças, não só admins operacionais.
*Baixa prioridade, pode ser feito a qualquer momento.*
**Docs:** esta tarefa É a própria atualização de doc (`frontend/PRODUCT.md`).

---

# Módulos novos: Cartão de Crédito e Contas a Pagar

## Contexto

O plano acima (T1-T13/F1-F6) cobre a estabilização do módulo financeiro core e deixa "dívidas, cartões, contas fixas etc." explicitamente fora de escopo (ver Contexto no topo do arquivo, item 1 das decisões). Esta seção **adiciona o desenho** (ainda não a implementação) de dois módulos que dependem desse core já estabilizado:

1. **Cartão de Crédito** — múltiplos cartões, cada um com limite e fatura (fechamento + vencimento). Lançamentos no cartão não afetam o saldo da conta bancária diretamente — só quando a fatura é paga.
2. **Contas a Pagar** — uma conta vira uma expense no dia em que é marcada como paga; pode ser recorrente (luz, internet) ou avulsa.

### Decisões validadas com o usuário para estes módulos

1. **Reversão de pagamento exige confirmação dedicada.** Editar/excluir pelo fluxo genérico de Transações é **bloqueado** (`DomainException`) para transações vinculadas a um pagamento de fatura/conta. A reversão só acontece por uma ação própria e explícita — "Desfazer pagamento" — em cada módulo, com sua própria confirmação na UI. Sem cascade automático dentro do `DeleteTransactionService` do core.
2. **Cartão de crédito tem `closingDay` (fechamento) E `dueDay` (vencimento)** — mais fiel ao ciclo real de fatura do que só uma data de vencimento.
3. **Saldo previsto no relatório considera só pendências com vencimento dentro do período filtrado** (`from`/`to`).
4. **Contas recorrentes (`Bill`) têm `recurring`/`startDate` editáveis** via uma ação dedicada (`UpdateBillScheduleService`), separada da edição básica. Como a geração de instâncias é sob demanda e sempre lê o estado atual do `Bill`, mudar a agenda só afeta instâncias ainda não geradas.

---

## Spec das novas entidades — Cartão de Crédito e Contas a Pagar

### Extensão compartilhada: `Transaction` ganha rastreabilidade de origem
**Novos campos em `domain/Transaction.java`:** `sourceType` (novo enum `domain/enums/TransactionSourceType`: `CREDIT_CARD_INVOICE_PAYMENT`, `BILL_INSTANCE_PAYMENT`), `sourceId` (Long). Ambos nulos para transações normais. Novo método `isLinkedToSource()`.
**Regra em `UpdateTransactionService`:** se `sourceType != null` → `DomainException("Cannot edit a transaction generated by a bill/invoice payment")`.
**Regra em `DeleteTransactionService`:** se `sourceType != null` → `DomainException("Cannot delete a transaction generated by a bill/invoice payment; use the dedicated undo action")`. **Sem cascade aqui** — a reversão vive em serviços dedicados nos módulos CC/AP (ver CC7/AP6), que chamam `TransactionRepository`/`TransactionBalanceEffectService` diretamente.

### CreditCard (`domain/CreditCard.java`) — nova, tenancy direta (padrão BankAccount/PaymentMethod)
**Campos:** id, version, space (Space), name, limit (BigDecimal), closingDay (Integer 1-31), dueDay (Integer 1-31), active, createdDate, updatedDate.
**`validate()`:** name não-blank; space não-nulo; limit não-nulo e > 0; closingDay/dueDay não-nulos, entre 1 e 31.
**Métodos:** `update(name, limit, closingDay, dueDay)`, `deactivate()`, lock otimista manual (`setVersion`).
**Limite é informativo, não bloqueante** — mesma filosofia do saldo negativo permitido em `BankAccount`; sem validação de "limite excedido" ao lançar uma `CreditCardTransaction`.

### CreditCardInvoiceCycle (`domain/CreditCardInvoiceCycle.java`) — nova, calculadora pura (sem estado, sem repositório)
Métodos estáticos usados por CC4/CC5/CC6:
- `resolveClosingDate(YearMonth month, int closingDay)` → `LocalDate` do fechamento naquele mês, clampado ao último dia do mês (evita estourar fevereiro).
- `resolveReferenceMonth(LocalDate purchaseDate, int closingDay)` → mês (primeiro dia) da fatura à qual a compra pertence: se `purchaseDate <= resolveClosingDate(mês da compra, closingDay)`, é o próprio mês da compra; senão é o mês seguinte.
- `resolveDueDate(LocalDate referenceMonth, int closingDay, int dueDay)` → se `dueDay <= closingDay`, o vencimento cai no mês **seguinte** ao `referenceMonth`; senão, cai dentro do próprio `referenceMonth`. Sempre clampado ao tamanho do mês.

### CreditCardTransaction (`domain/CreditCardTransaction.java`) — nova, tenancy indireta (padrão Transaction/SubCategory)
**Campos:** id, version, creditCardId (Long), userId (Long), categoryId (Long), subCategoryId (Long, opcional), amount (BigDecimal > 0), purchaseDate (LocalDate), description (opcional), createdDate, updatedDate, **+ campos de parcelamento (ver abaixo)**.
**`validate()`:** creditCardId, userId, categoryId, amount>0, purchaseDate obrigatórios; subCategoryId opcional.
**Sem soft-delete** (hard delete, como `Transaction`).
**Regra de aplicação:** create/update/delete são bloqueados se já existir `CreditCardInvoicePayment` para `(creditCardId, transaction.referenceMonth)` — `DomainException("Cannot modify a transaction from a paid invoice")`.

#### Parcelamento e antecipação de parcelas

Uma compra parcelada em X vezes é modelada como **1 registro por parcela** (X linhas de `CreditCardTransaction`, uma por fatura futura), vinculadas por um identificador de grupo — não como 1 registro com projeção calculada em memória. Compra à vista é só o caso `totalInstallments=1` (mesmo modelo, sem ramificação).

**Mudança central de modelo — `referenceMonth` deixa de ser derivado e passa a ser armazenado:** hoje a fatura de uma `CreditCardTransaction` seria calculada em tempo de leitura via `CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, closingDay)`. Isso não serve para parcelas — a parcela 5/12 não tem uma `purchaseDate` 5 meses no futuro, ela pertence a uma fatura futura por construção, não por cálculo de data. Por isso `referenceMonth` (LocalDate, primeiro dia do mês) passa a ser **calculado e persistido na criação**:
- Parcela 1 (ou compra à vista): `referenceMonth = CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, card.closingDay)`.
- Parcela i (i > 1): `referenceMonth = referenceMonth(parcela 1).plusMonths(i - 1)`.

`purchaseDate` continua existindo, mas passa a significar só "quando a compra original foi feita" (igual em todas as parcelas do grupo) — deixa de ser usada para cálculo de fatura em tempo de leitura. `CreditCardInvoiceCycle` continua usado (na criação, para a parcela 1, e em `resolveDueDate`/`resolveClosingDate` para metadados de exibição da fatura), só não é mais reinvocado a cada listagem. Isso também simplifica a guarda "mês já pago" acima, que passa a ler `transaction.referenceMonth` direto.

**Campos novos:**
- `referenceMonth` (LocalDate, não-nulo) — ver acima.
- `installmentGroupId` (String/UUID, não-nulo) — gerado uma vez por compra (`UUID.randomUUID().toString()`), compartilhado por todas as parcelas da mesma compra (grupo de 1 elemento na compra à vista).
- `installmentNumber` (Integer, 1-based, não-nulo) — posição da parcela dentro do grupo (ex: 3 de 12).
- `totalInstallments` (Integer, não-nulo, default 1) — tamanho do grupo.
- `anticipated` (boolean, default false) — marca se esta parcela já foi movida de mês via antecipação.
- `originalReferenceMonth` (LocalDate, nulo exceto quando `anticipated=true`) — preserva o mês em que a parcela cairia originalmente, para auditoria/exibição.

**`validate()` ganha:** `referenceMonth`/`installmentGroupId`/`installmentNumber`/`totalInstallments` não-nulos; `totalInstallments` entre 1 e 60 (limite de sanidade); `installmentNumber` entre 1 e `totalInstallments`.

**Novo método de domínio — `anticipateTo(LocalDate targetReferenceMonth)`:** se `anticipated` já é `true`, mantém o `originalReferenceMonth` já gravado (não sobrescreve com valor intermediário); senão grava `originalReferenceMonth = this.referenceMonth`. Em seguida `this.referenceMonth = targetReferenceMonth; this.anticipated = true;` + `updatedDate = Instant.now()`.

**Cálculo do valor de cada parcela (regra de aplicação, fica no service de criação, não no domínio):** dividir o valor total da compra por `totalInstallments` com arredondamento de 2 casas; a última parcela absorve o resíduo, garantindo que a soma das parcelas seja exatamente igual ao valor total informado (evita sobra/falta de centavos).

### CreditCardInvoicePayment (`domain/CreditCardInvoicePayment.java`) — nova, só existe quando paga
**Decisão de design:** não existe uma "CreditCardInvoice" com status persistida a cada mês. Fatura em aberto é sempre computada em memória (soma das `CreditCardTransaction` do mês, agrupadas pelo `referenceMonth` já armazenado em cada linha — ver parcelamento em `CreditCardTransaction`); só quando paga nasce esta linha (existência = pago).
**Campos:** id, version, creditCardId, referenceMonth (LocalDate, único por creditCardId), dueDate (calculado no momento do pagamento), paidAmount (BigDecimal, soma travada), paidDate, paymentTransactionId (Long, FK para a Transaction EXPENSE gerada), bankAccountId, createdDate, updatedDate.
**`validate()`:** creditCardId, referenceMonth, dueDate, paidAmount>0 obrigatórios. Sem `update()` de negócio. Único por `(credit_card_id, reference_month)`.

### Bill (`domain/Bill.java`) — nova, tenancy direta (template recorrente OU avulso)
**Campos:** id, version, space (Space), name, categoryId (Long, opcional), defaultAmount (BigDecimal > 0), startDate (LocalDate — âncora do primeiro vencimento e, se recorrente, do dia-do-mês), recurring (boolean), active, createdDate, updatedDate.
**`validate()`:** name não-blank; space não-nulo; defaultAmount > 0; startDate não-nulo.
**Um único Bill cobre avulso (`recurring=false`, gera 1 `BillInstance` na criação) e recorrente** (`recurring=true`, gera 1 `BillInstance`/mês sob demanda).
**Métodos:**
- `update(name, categoryId, defaultAmount)` — campos básicos.
- `updateSchedule(recurring, startDate)` — agenda; dedicado e separado do básico. Como a geração é sob demanda e sempre lê o `Bill` atual, isso só afeta instâncias futuras (as já geradas mantêm seu `dueDate` antigo).
- `deactivate()` — pausa geração futura; instâncias já geradas não são tocadas.

### BillInstance (`domain/BillInstance.java`) — nova, tenancy indireta
**Campos:** id, version, billId, referenceMonth (LocalDate, único por billId), dueDate, amount (copiado de `Bill.defaultAmount` na geração, editável enquanto PENDING), status (enum `BillInstanceStatus`: PENDING/PAID), paidDate, paymentTransactionId, bankAccountId, createdDate, updatedDate.
**`validate()`:** billId, referenceMonth, dueDate, amount>0 obrigatórios.
**Métodos:** `updateAmount(newAmount)` (só se PENDING), `markAsPaid(paidDate, paymentTransactionId, bankAccountId)` (só se PENDING), `revertToPending()` (usado só pelo Undo dedicado). Único por `(bill_id, reference_month)`.

### Geração sob demanda — sem scheduler novo
- **Cartão:** fatura aberta nunca é materializada — sempre computada em memória a partir de `CreditCardTransaction` existentes, agrupadas pelo `referenceMonth` armazenado em cada linha (parcelas já nascem com o `referenceMonth` correto na criação — ver CC5 — sem depender de `CreditCardInvoiceCycle` em tempo de leitura). Só a linha de pagamento (`CreditCardInvoicePayment`) é escrita, no momento do pagamento.
- **Contas a pagar recorrentes:** precisam de materialização antecipada (usuário edita/paga antes de qualquer Transaction existir). `EnsureBillInstancesGeneratedService(spaceId, upToDate)` gera as `BillInstance` PENDING faltantes entre o último mês já gerado e `min(mês de upToDate, mês atual + 1)` — cap fixo evita gerar centenas de linhas de uma vez. Idempotente via índice único `(bill_id, reference_month)`. Chamado por `ListBillInstancesService` e por `GenerateReportService`.
- **Por que não um job:** zero infra de `@Scheduled`/Quartz hoje no projeto; criar uma só para isso adicionaria uma categoria de falha nova (job travado, timezone, idempotência sob restart) para um resultado que a geração preguiçosa já entrega (tela sempre certa ao abrir). Só vira necessário se o produto quiser lembrete por e-mail/push **antes** de o usuário abrir o app — módulo futuro separado, fora de escopo.

### Saldo previsto — cálculo e exposição
**Novos campos em `ReportResponse`:** `currentBalance` (soma de `BankAccount.balance` ativas do space, ou só da conta filtrada), `pendingCreditCardInvoices` (lista `{creditCardId, creditCardName, referenceMonth, dueDate, amount}`) + `pendingCreditCardTotal`, `pendingBillInstances` (lista `{billInstanceId, billId, billName, referenceMonth, dueDate, amount}`) + `pendingBillTotal`, `projectedBalance = currentBalance - pendingCreditCardTotal - pendingBillTotal`.
**Filtro:** uma fatura/instância pendente só entra na soma se seu `dueDate` cair dentro de `[from, to]` do `ReportFilterRequest` (decisão validada). `ReportFilterRequest` não precisa de campo novo (já tem `spaceId`/`from`/`to` via T9b do plano core).
**Fontes:** faturas pendentes = `CreditCardTransaction` do período agrupadas por `(creditCardId, referenceMonth)` (campo já armazenado — inclui parcelas futuras de compras parceladas e parcelas antecipadas), excluindo grupos com `CreditCardInvoicePayment` já existente. Contas pendentes = `EnsureBillInstancesGeneratedService` + `BillInstanceRepository.findBySpaceAndPeriod` com `status=PENDING`.

---

## Tarefas de Backend — Cartão de Crédito e Contas a Pagar (grupos `P`/`CC`/`AP`/`RPT`/`GATE`)

### [Grupo P1] Fundação compartilhada: rastreabilidade de pagamento em Transaction

- [x] **P1 — `sourceType`/`sourceId` em Transaction + bloqueio de edição/exclusão**
Editar `domain/enums/TransactionSourceType.java` (novo). Editar `domain/Transaction.java`: campos `sourceType`/`sourceId`, `isLinkedToSource()`. Editar `TransactionEntityJpa`/`TransactionRepositoryImpl` (2 colunas nullable). Editar `UpdateTransactionService` e `DeleteTransactionService` para rejeitar (`DomainException`) qualquer transação vinculada — sem cascade, só bloqueio.
*Depende de:* core T3, T7, T8 (mexe nos arquivos já estabilizados por eles).
**Testes (obrigatório):** estender `TransactionSpec.groovy` + `UpdateTransactionServiceSpec.groovy` + `DeleteTransactionServiceSpec.groovy` com o cenário "transação vinculada rejeita update/delete".
**Docs:** `APP_OVERVIEW.md` seção Transaction — novos campos + regra de bloqueio.
*Pronto quando:* update/delete de uma Transaction com `sourceType` preenchido retornam 422, e as specs passam.

### Módulo Cartão de Crédito

### [Grupo CC1] Domínio e persistência: CreditCard + CreditCardInvoiceCycle

- [x] **CC1 — CreditCard + CreditCardInvoiceCycle: domain + persistência**
Criar `domain/CreditCard.java`, `domain/CreditCardInvoiceCycle.java` (calculadora pura), `domain/repository/CreditCardRepository.java` (`save/update/findById/findBySpaceId/delete`), `CreditCardEntityJpa`, `JpaCreditCardRepository`, `CreditCardRepositoryImpl` (padrão `BankAccountRepositoryImpl`, tenancy direta).
*Depende de:* nenhuma.
**Testes (obrigatório):** `CreditCardSpec.groovy` (validate) + `CreditCardInvoiceCycleSpec.groovy` (resolveReferenceMonth/resolveDueDate cobrindo closingDay antes/depois de dueDay, e clamp de mês curto).
**Docs:** subseção "CreditCard" em `backend/docs/APP_OVERVIEW.md`.
*Pronto quando:* specs passam; persistência validada manualmente.

### [Grupo CC2] CRUD de CreditCard + controller + seed

- [x] **CC2 — CreditCard: services CRUD + controller + `@PreAuthorize` desde o início**
`CreateCreditCardService`, `UpdateCreditCardService`, `DeactivateCreditCardService`, `ListCreditCardsService` + DTOs + `CreditCardController` (`POST/PUT/DELETE/GET /credit-cards`) já com `@PreAuthorize` (não repetir o gap do T10 core).
*Depende de:* CC1.
**Testes (obrigatório):** uma spec por service.
**Docs:** `backend/docs/seed.sql` (API + FRONT_PAGE `/credit-cards` + `group_menu_children` sob o grupo já existente `'Contas e Pagamentos'` + listas ADMIN/MEMBER) + `APP_OVERVIEW.md`.
*Pronto quando:* CRUD funciona fim-a-fim e retorna 403 sem `ALLOW`.

### [Grupo CC3] Domínio e persistência: CreditCardInvoicePayment

- [x] **CC3 — CreditCardInvoicePayment: domain + persistência**
Criar `domain/CreditCardInvoicePayment.java`, `domain/repository/CreditCardInvoicePaymentRepository.java` (`save/findById/findByCreditCardIdAndReferenceMonth/deleteById`), JPA entity (índice único `credit_card_id+reference_month`), `JpaCreditCardInvoicePaymentRepository`, `CreditCardInvoicePaymentRepositoryImpl`.
*Depende de:* CC1.
**Testes (obrigatório):** `CreditCardInvoicePaymentSpec.groovy` (validate).
**Docs:** nenhuma ainda (junto com CC6).
*Pronto quando:* spec passa; `save`/`findByCreditCardIdAndReferenceMonth` funcionam manualmente.

### [Grupo CC4] Domínio e persistência: CreditCardTransaction

- [x] **CC4 — CreditCardTransaction: domain + persistência (isolamento por Space desde o início)**
Criar `domain/CreditCardTransaction.java` (já com os campos de parcelamento — ver spec acima: `referenceMonth`, `installmentGroupId`, `installmentNumber`, `totalInstallments`, `anticipated`, `originalReferenceMonth`), `domain/repository/CreditCardTransactionRepository.java` (`save/update/findById/findByFilter(spaceId, creditCardId, categoryId, subCategoryId, from, to)/findByInstallmentGroupId(String)/delete`), JPA entity, `JpaCreditCardTransactionRepository` (`JpaSpecificationExecutor`), `CreditCardTransactionRepositoryImpl` com filtro por `spaceId` via subquery **já embutido** (aprender com o gap de T9b do core). Índice não-único em `(credit_card_id, reference_month)` para acelerar o agrupamento de fatura (não é único como em `CreditCardInvoicePayment` — várias parcelas, inclusive de compras diferentes ou antecipadas, coexistem na mesma fatura).
*Depende de:* CC1.
**Testes (obrigatório):** `CreditCardTransactionSpec.groovy` (incluindo `validate()` dos campos de parcelamento: `totalInstallments` fora de 1-60, `installmentNumber` fora de 1-`totalInstallments`, e o método `anticipateTo`).
**Docs:** subseção "CreditCardTransaction" em `APP_OVERVIEW.md` (incluindo o modelo de parcelamento).
*Pronto quando:* spec passa; `findByFilter`/`findByInstallmentGroupId` isolam por `spaceId` em teste manual com 2 spaces.

### [Grupo CC5] CRUD de CreditCardTransaction com guarda de fatura paga + parcelamento

- [x] **CC5 — CreditCardTransaction: services CRUD + guarda "mês já pago" + parcelamento + controller**
`CreateCreditCardTransactionService` (valida FKs; ganha parâmetro opcional `totalInstallments` no request — default 1: se `<= 1`, comportamento simples de hoje (1 linha, `installmentGroupId` novo, `installmentNumber=1`); se `> 1`, gera as `totalInstallments` linhas em uma única chamada `@Transactional`, mesmo `installmentGroupId`, `installmentNumber` de 1 a N, `referenceMonth` sequencial mês a mês a partir da parcela 1, valor dividido com arredondamento de 2 casas e a última parcela absorvendo o resíduo), `UpdateCreditCardTransactionService`, `DeleteCreditCardTransactionService` (operam por linha individual — sem cascata entre parcelas do mesmo grupo; editar/excluir uma parcela não renumera nem recalcula as demais), `ListCreditCardTransactionsService`, `ListInstallmentGroupService` (novo — retorna todas as parcelas de um `installmentGroupId`, usado pela UI para mostrar progresso e pela antecipação para listar candidatas). As 3 primeiras leem `transaction.referenceMonth` direto (já não recalculam via `CreditCardInvoiceCycle`) + `CreditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth` para rejeitar alteração de mês já pago. Controller `/credit-card-transactions` com `@PreAuthorize`, incluindo `GET /credit-card-transactions/installment-groups/{installmentGroupId}`.
*Depende de:* CC1, CC3, CC4.
**Testes (obrigatório):** uma spec por service, incluindo "mês já pago rejeita" e "compra em N parcelas" (`installmentGroupId` compartilhado, `referenceMonth` sequencial, soma dos valores igual ao total, arredondamento correto quando não divide exato).
**Docs:** `seed.sql` + `APP_OVERVIEW.md`.
*Pronto quando:* CRUD funciona, a guarda é respeitada nas 3 operações, e criar com `totalInstallments>1` gera as N linhas corretamente.

### [Grupo CC5b] Antecipação de parcelas

- [ ] **CC5b — AnticipateCreditCardInstallmentsService + controller**
`execute(installmentGroupId, targetReferenceMonth, installmentsToAnticipate)`: (1) busca todas as linhas do grupo via `findByInstallmentGroupId` (vazio → `DomainException`); (2) valida que a fatura alvo está **aberta** — `CreditCardInvoicePaymentRepository.findByCreditCardIdAndReferenceMonth` deve ser nulo, senão `DomainException("Cannot anticipate into a paid invoice")`; (3) filtra elegíveis = linhas do grupo com `referenceMonth > targetReferenceMonth` (estritamente futuras em relação ao alvo); (4) se `installmentsToAnticipate > elegíveis.size()`, `DomainException("Not enough remaining installments to anticipate")`; (5) ordena elegíveis por `installmentNumber` decrescente e pega as `installmentsToAnticipate` primeiras (as últimas parcelas, de trás pra frente); (6) para cada uma, `transaction.anticipateTo(targetReferenceMonth)` + `repository.update(transaction)`. `@Transactional`. **Não move dinheiro nem chama `TransactionBalanceEffectService`** — só reatribui a quais faturas as parcelas pertencem; o efeito no saldo só acontece quando a fatura alvo for de fato paga via `PayCreditCardInvoiceService` (CC6), como qualquer outra fatura. Controller: `POST /credit-card-transactions/installment-groups/{installmentGroupId}/anticipate` (body `{targetReferenceMonth, installmentsToAnticipate}`), `@PreAuthorize`.
*Depende de:* CC4, CC5 (não depende de CC6/CC7).
**Testes (obrigatório):** `AnticipateCreditCardInstallmentsServiceSpec.groovy` — sucesso (move exatamente as N últimas, preserva as intermediárias, marca `anticipated=true` e grava `originalReferenceMonth`), fatura alvo já paga rejeitada, `installmentsToAnticipate` maior que o disponível rejeitado, grupo inexistente rejeitado, antecipar uma parcela já antecipada antes não sobrescreve o `originalReferenceMonth` original.
**Docs:** seção de antecipação em `backend/docs/credit-card-invoice.md` (junto com CC6) + `seed.sql` + `APP_OVERVIEW.md`.
*Pronto quando:* antecipar N parcelas de uma compra as move todas para a fatura aberta solicitada, mantendo as demais parcelas do grupo intactas, e a spec passa.

**Fora de escopo (parcelamento/antecipação, apenas registrado, não vira tarefa):**
- Desfazer uma antecipação (mover a parcela de volta ao mês original) — não foi solicitado; se necessário no futuro, seria um `UndoAnticipation` simétrico usando o `originalReferenceMonth` já preservado no modelo.
- Renumeração/recalculo do grupo ao editar ou excluir uma parcela individual — comportamento aceito como está (cada linha é independente após a criação).
- Um relatório dedicado de "compras parceladas em aberto" (progresso por compra) — `ListInstallmentGroupService` (CC5) já dá a base de dados para isso, mas uma tela/relatório dedicado fica para quando a UI do módulo (FCC2/FCC3) for desenhada.

### [Grupo CC6] Pagamento de fatura + listagem

- [ ] **CC6 — PayCreditCardInvoiceService + ListCreditCardInvoicesService**
`ListCreditCardInvoicesService(spaceId, creditCardId?, from, to)`: agrupa `CreditCardTransaction` por `(creditCardId, referenceMonth)` **direto pelo campo armazenado** `referenceMonth` (não mais via `CreditCardInvoiceCycle.resolveReferenceMonth(purchaseDate, closingDay)` em tempo de leitura — `CreditCardInvoiceCycle` continua usado só para calcular `dueDate`/`closingDate` de exibição a partir do `referenceMonth` de cada grupo), marca pago/aberto conforme existência de `CreditCardInvoicePayment`. `PayCreditCardInvoiceService.execute(creditCardId, referenceMonth, {bankAccountId, categoryId, paymentMethodId, paidDate})`: rejeita se já paga ou soma zero; soma as `CreditCardTransaction` do mês (incluindo parcelas antecipadas de outras compras, que já chegam com `referenceMonth` ajustado); chama `CreateTransactionService` (reaproveita `TransactionBalanceEffectService` de T5/T6 do core, `type=EXPENSE`, `sourceType=CREDIT_CARD_INVOICE_PAYMENT`); persiste `CreditCardInvoicePayment` com o `paymentTransactionId`. `@Transactional`. Controller: `GET /credit-cards/invoices?spaceId=&creditCardId=&from=&to=`, `POST /credit-cards/{id}/invoices/{referenceMonth}/pay`.
*Depende de:* CC3, CC5, P1, core T5/T6.
**Testes (obrigatório):** `PayCreditCardInvoiceServiceSpec.groovy` (sucesso, já paga, mês vazio, FK inexistente), `ListCreditCardInvoicesServiceSpec.groovy` (mistura aberto/pago).
**Docs:** criar `backend/docs/credit-card-invoice.md` (formato de `transaction-balance-effect.md`) explicando o ciclo de fatura e por que não é materializada até o pagamento; `seed.sql`; `APP_OVERVIEW.md`.
*Pronto quando:* pagar gera a Transaction correta e debita a conta escolhida.

### [Grupo CC7] Desfazer pagamento de fatura (ação dedicada)

- [ ] **CC7 — UndoCreditCardInvoicePaymentService + controller**
`UndoCreditCardInvoicePaymentService(creditCardId, referenceMonth)`: busca `CreditCardInvoicePayment` (senão `DomainException`), busca a `Transaction` via `paymentTransactionId`, chama `TransactionBalanceEffectService.revert(transaction)` + `TransactionRepository.delete(transaction.id)` diretamente (não via `DeleteTransactionService`, que bloqueia transações vinculadas), depois `creditCardInvoicePaymentRepository.deleteById(payment.id)`. `@Transactional`. Controller: `POST /credit-cards/{id}/invoices/{referenceMonth}/undo-payment`, `@PreAuthorize`.
*Depende de:* CC6, P1.
**Testes (obrigatório):** `UndoCreditCardInvoicePaymentServiceSpec.groovy` (sucesso — saldo revertido e fatura volta a aberta; fatura inexistente/não paga rejeitada).
**Docs:** `credit-card-invoice.md` (seção de reversão) + `seed.sql` + `APP_OVERVIEW.md`.
*Pronto quando:* desfazer o pagamento reverte o saldo da conta e a fatura volta a aparecer como aberta em `ListCreditCardInvoicesService`.

### Módulo Contas a Pagar

### [Grupo AP1] Domínio e persistência: Bill

- [ ] **AP1 — Bill: domain + persistência**
Criar `domain/Bill.java`, `domain/repository/BillRepository.java`, JPA entity, `JpaBillRepository`, `BillRepositoryImpl` (tenancy direta).
*Depende de:* nenhuma.
**Testes (obrigatório):** `BillSpec.groovy` (validate, update, updateSchedule).
**Docs:** subseção "Bill" em `APP_OVERVIEW.md`.
*Pronto quando:* spec passa; persistência validada manualmente.

### [Grupo AP2] Domínio e persistência: BillInstance

- [ ] **AP2 — BillInstance: domain + persistência (isolamento por Space desde o início)**
Criar `domain/enums/BillInstanceStatus.java`, `domain/BillInstance.java`, `domain/repository/BillInstanceRepository.java` (`save/update/findById/findByBillIdAndReferenceMonth/findByBillId/findBySpaceAndPeriod`), JPA entity (índice único), `JpaBillInstanceRepository`, `BillInstanceRepositoryImpl` com filtro por `spaceId` via subquery já embutido.
*Depende de:* AP1.
**Testes (obrigatório):** `BillInstanceSpec.groovy` (validate, updateAmount bloqueado se PAID, markAsPaid bloqueado se já PAID, revertToPending).
**Docs:** subseção "BillInstance" em `APP_OVERVIEW.md`.
*Pronto quando:* spec passa; `findBySpaceAndPeriod` isola por space em teste manual.

### [Grupo AP3] CRUD de Bill (básico + agenda) + instância automática avulsa + controller

- [ ] **AP3 — Bill: services CRUD completos + auto-criação de instância única (não-recorrente) + controller**
`CreateBillService` (se `recurring=false`, cria imediatamente 1 `BillInstance` PENDING com `dueDate=startDate`, na mesma transação), `UpdateBillService` (nome/categoria/defaultAmount), `UpdateBillScheduleService` (recurring/startDate — dedicado, só afeta futuro), `DeactivateBillService`, `ListBillsService` + controller `/bills` com `@PreAuthorize`.
*Depende de:* AP1, AP2.
**Testes (obrigatório):** uma spec por service, incluindo "Bill não-recorrente já nasce com 1 instância" e "updateSchedule não altera instâncias já geradas".
**Docs:** `seed.sql` (API + FRONT_PAGE `/bills` + `group_menu_children` sob `'Contas e Pagamentos'`) + `APP_OVERVIEW.md`.
*Pronto quando:* CRUD completo funciona, incluindo a agenda dedicada.

### [Grupo AP4] Geração preguiçosa + listagem "contas do mês"

- [ ] **AP4 — EnsureBillInstancesGeneratedService + ListBillInstancesService**
`EnsureBillInstancesGeneratedService(spaceId, upToDate)`: gera `BillInstance` PENDING faltantes por `Bill` ativo+recorrente, do último mês existente até `min(mês de upToDate, mês atual+1)`, idempotente. `ListBillInstancesService(spaceId, from, to)`: chama o ensure-service, depois `findBySpaceAndPeriod`. Controller: `GET /bills/instances?spaceId=&from=&to=`.
*Depende de:* AP2, AP3.
**Testes (obrigatório):** `EnsureBillInstancesGeneratedServiceSpec.groovy` (gera só o que falta, respeita o cap, idempotente), `ListBillInstancesServiceSpec.groovy`.
**Docs:** `seed.sql` + `APP_OVERVIEW.md`.
*Pronto quando:* abrir a tela de um mês nunca visitado já mostra as instâncias corretas.

### [Grupo AP5] Pagamento + edição de valor

- [ ] **AP5 — PayBillInstanceService + UpdateBillInstanceAmountService**
`UpdateBillInstanceAmountService(id, newAmount)` (bloqueia se PAID). `PayBillInstanceService.execute(id, {bankAccountId, paymentMethodId, categoryId?, paidDate})`: resolve categoria (request → `Bill.categoryId` → erro), chama `CreateTransactionService` (`type=EXPENSE`, `sourceType=BILL_INSTANCE_PAYMENT`), `billInstance.markAsPaid(...)`. `@Transactional`. Controller: `PUT /bills/instances/{id}/amount`, `POST /bills/instances/{id}/pay`.
*Depende de:* AP2, AP4, P1, core T5/T6.
**Testes (obrigatório):** `PayBillInstanceServiceSpec.groovy` (sucesso, já paga, sem categoria resolvível), `UpdateBillInstanceAmountServiceSpec.groovy`.
**Docs:** criar `backend/docs/recurring-bills.md` (formato de `transaction-balance-effect.md`); `seed.sql`; `APP_OVERVIEW.md`.
*Pronto quando:* editar valor e pagar funcionam fim-a-fim.

### [Grupo AP6] Desfazer pagamento de conta (ação dedicada)

- [ ] **AP6 — UndoBillInstancePaymentService + controller**
`UndoBillInstancePaymentService(billInstanceId)`: busca `BillInstance` (valida `status==PAID`, senão `DomainException`), busca a `Transaction` via `paymentTransactionId`, `TransactionBalanceEffectService.revert(transaction)` + `TransactionRepository.delete(...)` diretamente, `billInstance.revertToPending()` + `update()`. `@Transactional`. Controller: `POST /bills/instances/{id}/undo-payment`, `@PreAuthorize`.
*Depende de:* AP5, P1.
**Testes (obrigatório):** `UndoBillInstancePaymentServiceSpec.groovy` (sucesso, instância não paga rejeitada).
**Docs:** `recurring-bills.md` (seção de reversão) + `seed.sql` + `APP_OVERVIEW.md`.
*Pronto quando:* desfazer o pagamento reverte o saldo e a instância volta a PENDING.

### Fechamento compartilhado

### [Grupo RPT1] Saldo previsto no Report

- [ ] **RPT1 — Saldo previsto em GenerateReportService**
Editar `ReportFilterRequest`/`ReportResponse` (campos novos da spec acima). Editar `GenerateReportService`: injeta `BankAccountRepository`, `CreditCardRepository`, `CreditCardTransactionRepository`, `CreditCardInvoicePaymentRepository`, `EnsureBillInstancesGeneratedService`, `BillInstanceRepository`; calcula `currentBalance`, pendências de cartão e de contas, `projectedBalance`.
*Depende de:* CC6, AP5 (única tarefa que toca `GenerateReportService` para os dois módulos, evita reabrir o arquivo duas vezes).
**Nota sobre parcelas de cartão:** ao contrário de `BillInstance` (que depende de `EnsureBillInstancesGeneratedService` rodando sob demanda), as parcelas de `CreditCardTransaction` já nascem materializadas na criação da compra (ver CC5) — então `pendingCreditCardInvoices`/`pendingCreditCardTotal` já enxergam parcelas de meses futuros dentro do período filtrado (`from`/`to`) sem nenhum passo de geração adicional; só é preciso que a consulta agrupe pelo `referenceMonth` armazenado (ver CC4/CC6), não há necessidade de um serviço de geração análogo ao de Bills para o cartão.
**Testes (obrigatório):** estender `GenerateReportServiceSpec.groovy` (fatura aberta dentro/fora do período, fatura paga não conta, instância pendente dentro/fora do período, `projectedBalance` correto, parcelas futuras de uma compra parcelada aparecendo no período filtrado sem ação extra).
**Docs:** `APP_OVERVIEW.md` seção "Key Flows → 3. Financial Report".
*Pronto quando:* `POST /reports` retorna `projectedBalance` correto.

### [Grupo GATE1] Gate final dos dois módulos

- [ ] **GATE1 — ArchUnit + suíte completa (gate final CC+AP)**
Rodar `./gradlew test` (inclui `ArchitectureTest` + todas as specs de P1/CC1-7/AP1-6/RPT1). Confirmar que nenhum service novo vazou de camada.
*Depende de:* todas as anteriores desta seção.
**Testes (obrigatório):** `./gradlew test` 100% verde.
**Docs:** nenhuma nova — confirmar que todas foram commitadas.

---

## Tarefas de Frontend — Cartão de Crédito e Contas a Pagar

Reaproveitam integralmente o padrão já estabelecido no plano core (página + `AddEdit<Entidade>Dialog.vue` + `ConfirmDialog` + `server/api/<entidade>/*`), incluindo o padrão de dialog secundário aberto a partir de uma linha da tabela.

### [Grupo FCC1] Cadastro de Cartões

- [ ] **FCC1 — Credit Cards** (`pages/credit-cards/`)
Form: `name`, `limit`, `closingDay`, `dueDay`. CRUD completo, mesmo padrão de F2 (Bank Accounts).
*Depende de:* backend CC2.
**Verificação:** manual no navegador — criar/editar/desativar cartão.
**Docs:** criar `frontend/docs/credit-cards.md`.

### [Grupo FCC2] Lançamentos no cartão

- [ ] **FCC2 — Lançamentos de Cartão** (dialog secundário a partir da linha do cartão, `ManageCreditCardTransactionsDialog.vue`)
Lista/cria/edita/exclui `CreditCardTransaction` do cartão selecionado: `categoryId`+`subCategoryId` (cascata, selects de F3), `amount`, `purchaseDate`, `description`, `totalInstallments` (campo opcional "parcelar em X vezes" — se preenchido >1, a UI só cria, não edita/exclui em lote). Cada linha da lista mostra "N/total" quando fizer parte de um grupo parcelado (via `GET .../installment-groups/{id}`) e um selo "antecipada" quando `anticipated=true`. Ação "Antecipar parcelas" na linha de uma compra parcelada (dialog: escolher quantas das últimas parcelas antecipar para a fatura aberta atual) chamando `POST .../installment-groups/{id}/anticipate` (backend CC5b). Filtro de período.
*Depende de:* FCC1, F3 (selects de categoria), backend CC5, CC5b.
**Verificação:** manual no navegador — lançar compra parcelada em 6x e conferir as 6 linhas geradas; tentar editar/excluir uma compra de mês já pago (depois de FCC3 existir) e confirmar bloqueio; antecipar as 2 últimas parcelas e conferir que só elas mudam de fatura.
**Docs:** `frontend/docs/credit-cards.md` (seção adicional).

### [Grupo FCC3] Fatura, pagamento e reversão

- [ ] **FCC3 — Fatura do Cartão** (`pages/credit-cards/[id]/invoices.vue` ou dialog)
Lista meses (aberto/pago) com totais; dialog "Pagar Fatura" (`bankAccountId`, `categoryId`, `paymentMethodId`, `paidDate`); ação "Desfazer Pagamento" com confirmação explícita dedicada ("Tem certeza? O saldo da conta X será revertido em R$Y").
*Depende de:* FCC2, backend CC6, CC7.
**Verificação:** manual no navegador — pagar fatura, conferir saldo da conta escolhida em `/bank-accounts`, desfazer o pagamento e confirmar que a fatura volta a "aberta" e o saldo é revertido.
**Docs:** `frontend/docs/credit-cards.md` (seção adicional).

### [Grupo FAP1] Cadastro de Contas a Pagar

- [ ] **FAP1 — Bills** (`pages/bills/`)
Form básico (`name`, `categoryId`, `defaultAmount`) + seção separada "Agenda" (`recurring`/`startDate`, editável via ação própria). CRUD (update básico + update de agenda conforme AP3).
*Depende de:* backend AP3, F3 (categorias).
**Verificação:** manual no navegador — criar conta avulsa (confirmar 1 instância já aparece em FAP2) e conta recorrente; editar a agenda de uma recorrente e confirmar que só instâncias futuras mudam.
**Docs:** criar `frontend/docs/bills.md`.

### [Grupo FAP2] Contas do mês (pagar/desfazer)

- [ ] **FAP2 — Contas do Mês** (`pages/bills/instances.vue` ou seção da mesma página)
Lista `BillInstance` do período selecionado (default mês atual), com edição inline de `amount` (só se PENDING), dialog "Marcar como Paga" (`bankAccountId`, `categoryId?`, `paymentMethodId`, `paidDate`) e ação "Desfazer Pagamento" com confirmação dedicada.
*Depende de:* FAP1, backend AP4/AP5/AP6.
**Verificação:** manual no navegador — editar valor de uma pendente, pagar, conferir saldo da conta, desfazer o pagamento e confirmar retorno a pendente.
**Docs:** `frontend/docs/bills.md` (seção adicional).

### [Grupo FRPT1] Atualização da tela de Reports

- [ ] **FRPT1 — Saldo previsto em Reports** (atualização de `pages/reports/` de F5)
Adicionar cards de `currentBalance`/`projectedBalance` e duas listas (faturas pendentes, contas pendentes) ao lado dos cards já existentes de `totalIncome`/`totalExpense`/`balance`.
*Depende de:* F5 (já existente), backend RPT1.
**Verificação:** manual no navegador — conferir que `projectedBalance` bate com `currentBalance - faturas pendentes - contas pendentes` do período filtrado.
**Docs:** `frontend/docs/reports.md` (atualizar, não recriar).

---

## Verificação end-to-end (depois de todas as tarefas)

1. `./gradlew test` verde (unit specs + ArchitectureTest) — gate T12.
2. Fluxo manual de backend (via REST client/Postman/curl com token válido):
   - Criar BankAccount A e B.
   - Criar Transaction INCOME em A → saldo de A sobe.
   - Criar Transaction EXPENSE em A maior que o saldo → saldo fica negativo (permitido).
   - Criar Transaction TRANSFER de A para B → saldo de A desce, saldo de B sobe.
   - Editar a TRANSFER trocando o valor → saldos de A e B refletem a correção.
   - Excluir a TRANSFER → saldos voltam ao estado anterior.
   - `POST /reports` no período → TRANSFER aparece em `transactions[]` mas não em `totalIncome`/`totalExpense`.
   - Criar SubCategory, editar, excluir, listar por categoria → tudo persiste de verdade (antes era stub).
   - Chamar `POST /bank-accounts` sem o `@PreAuthorize` adequado (role sem ALLOW) → 403.
   - `GET /bank-accounts?spaceId=`, `GET /categories?spaceId=` (com subcategorias aninhadas), `GET /payment-methods?spaceId=`, `GET /transactions?spaceId=` retornam só dados do space informado.
3. Frontend (`pnpm dev` em `frontend/`, com o backend rodando e usuário autenticado):
   - Abrir `/payment-methods` → criar/editar/excluir uma forma de pagamento, sidebar já mostra o item sem precisar editar navegação estática.
   - Abrir `/bank-accounts` → criar conta, ver saldo inicial refletido.
   - Abrir `/categories` → criar categoria, abrir o dialog de subcategorias, criar/editar/excluir uma subcategoria.
   - Abrir `/transactions` → criar INCOME/EXPENSE/TRANSFER usando os selects populados pelas telas acima; conferir que o saldo das contas envolvidas muda de acordo (checar em `/bank-accounts`).
   - Abrir `/reports` → filtrar por período e conferir que os totais batem e que a TRANSFER aparece na lista mas não nos totais.
   - Trocar de space ativo e confirmar que todas as 5 telas recarregam com os dados do novo space (isolamento correto).
4. Módulos novos — Cartão de Crédito e Contas a Pagar (depois de P1/CC1-7/AP1-6/RPT1/GATE1 + FCC1-3/FAP1-2/FRPT1):
   - Criar um CreditCard com `closingDay`/`dueDay` distintos; lançar compras que caiam em 2 meses de fatura diferentes (uma antes e outra depois do fechamento) e confirmar que `CreditCardInvoiceCycle` agrupou corretamente.
   - Lançar uma compra parcelada em 6x → confirmar 6 `CreditCardTransaction` criadas, uma por mês seguinte, com valores somando exatamente o total (incluindo caso de divisão não-exata, ex: R$100,00 em 3x).
   - `POST /reports` com período cobrindo os próximos 6 meses → confirmar que `pendingCreditCardTotal`/`projectedBalance` já refletem as parcelas futuras sem nenhuma ação extra do usuário.
   - Antecipar as 2 últimas parcelas dessa compra para a fatura aberta atual → confirmar que só essas 2 mudam de fatura (as demais permanecem nos meses originais), que a fatura alvo passa a somar o valor extra, e que tentar antecipar para uma fatura já paga é rejeitado.
   - Pagar a fatura de um mês → conferir débito na conta bancária escolhida (`/bank-accounts`) e que a `Transaction` gerada aparece com `sourceType=CREDIT_CARD_INVOICE_PAYMENT` e não pode ser editada/excluída pela tela normal de Transações.
   - Tentar editar/excluir uma `CreditCardTransaction` de um mês já pago → deve rejeitar (`DomainException`).
   - Usar a ação dedicada "Desfazer Pagamento" da fatura → confirmar que pede confirmação explícita, reverte o saldo da conta e a fatura volta a aparecer como aberta.
   - Criar uma `Bill` avulsa (confirmar que já nasce com 1 `BillInstance` pendente) e uma recorrente; abrir a tela de "contas do mês" de um mês futuro nunca visitado e confirmar que a instância é gerada sob demanda.
   - Editar o valor de uma `BillInstance` pendente, marcar como paga → conferir débito na conta escolhida; usar "Desfazer Pagamento" → confirmar reversão do saldo e retorno a PENDING.
   - Editar a agenda (`recurring`/`startDate`) de uma `Bill` recorrente já em andamento → confirmar que instâncias já geradas (pendentes ou pagas) não mudam, só as futuras.
   - `POST /reports` no período → conferir que `projectedBalance` bate com `currentBalance - pendências (fatura + contas) com vencimento dentro do período filtrado`.

---

# Exclusão real + Ativar/Inativar (BankAccount, Category, SubCategory, PaymentMethod)

## Contexto

Nas telas já entregues (F1-F3), "Excluir" sempre foi soft delete disfarçado: `Delete*Service` chamava `entity.deactivate()` + `repository.update(entity)`, nunca `repository.delete(id)` (que já existia nas interfaces/impls, só estava morto). Pedido do usuário: separar em duas ações — (a) ativar/inativar nos dois sentidos, (b) excluir de fato, validando dependências (FK) antes e retornando ao front qual dependência bloqueia. Decisão validada: a exclusão funciona independente do status atual — a única trava é a checagem de FK.

Como pré-requisito, um levantamento do domínio mostrou que `Transaction` (6 campos) e `SubCategory` (`categoryId`) fugiam do padrão majoritário do projeto (objeto de domínio + FK física real via `@ManyToOne`, já usado em `Category`, `PaymentMethod`, `Role`, `SpaceMember` etc.) — guardavam só `Long` cru, sem objeto nem FK física. `BankAccount.space` também divergia (objeto no domínio, mas `Long spaceId` + query manual na JPA). Corrigido nos grupos REF1-REF4 abaixo como base para os grupos DEL1-DEL4, mantendo o contrato JSON da API inalterado (só a representação interna em domain/JPA mudou).

## Parte 1 — Backend: referências por objeto (REF)

- [x] **REF1** — `BankAccount.space`: `Long spaceId` → `@ManyToOne SpaceEntityJpa space` (FK física real), removendo a resolução manual (`resolveSpace`). Ajustada também a subquery `bankAccountIdsInSpace` em `TransactionRepositoryImpl` (Criteria API não faz travessia automática de propriedade aninhada como o Spring Data — precisa de `root.get("space").get("id")` explícito).
- [x] **REF2** — `SubCategory.categoryId` (Long) → `SubCategory.category` (`Category`, objeto + FK física real). `SubCategoryRepositoryImpl` builda `Category`/`Space` via helpers privados (padrão já usado em `CategoryRepositoryImpl`). `CreateSubCategoryService`/`ListCategoriesService`/`UpdateSubCategoryService` ajustados; contrato JSON de `SubCategoryResponse` inalterado.
- [x] **REF3** — `Transaction`: `userId`/`bankAccountId`/`destinationBankAccountId` → `User`/`BankAccount`/`BankAccount` (objetos + FK física real, dois `@ManyToOne` distintos para `BankAccountEntityJpa`). `CreateTransactionService`/`UpdateTransactionService` reaproveitam os objetos já resolvidos para validação em vez de descartá-los. `TransactionBalanceEffectService` continua buscando fresh via repository antes de `credit()`/`debit()` (preserva lock otimista).
- [x] **REF4** — `Transaction`: `categoryId`/`subCategoryId`/`paymentMethodId` → `Category`/`SubCategory`/`PaymentMethod` (mesma mecânica). `buildSpecification` em `TransactionRepositoryImpl` ajustado para os novos paths (`root.get("category").get("id")` etc.).
- [x] **REF-GATE** — `./gradlew test` verde (165 testes) após REF1-REF4.

## Parte 2 — Backend: exclusão real (hard delete) + ativar/inativar (DEL)

- [x] **DEL1** — `TransactionRepository`/`SubCategoryRepository` ganham métodos `existsBy*` (checagem de FK na aplicação, já que não há FK física entre `transactions`/`sub_categories` e as tabelas pai). Confirmado que os métodos derivados do Spring Data (`existsByCategoryId` etc.) funcionam sem alteração de nome mesmo depois de REF2-REF4 trocarem `Long` por associação, graças à travessia de propriedade aninhada.
- [x] **DEL2** — `BankAccount.activate()` + `UpdateBankAccountStatusService` (`PATCH /bank-accounts/{id}/status`) + `DeleteBankAccountService` reescrito (hard delete + `existsByBankAccountId`). Specs + `APP_OVERVIEW.md` + `seed.sql`.
- [x] **DEL3** — Mesmo padrão para `PaymentMethod` (`PATCH /payment-methods/{id}/status`, `existsByPaymentMethodId`).
- [x] **DEL4** — Mesmo padrão para `Category`+`SubCategory` (`PATCH /categories/{id}/status`, `PATCH /categories/subcategories/{id}/status`). `DeleteCategoryService` checa primeiro subcategorias vinculadas (`existsByCategoryId` do `SubCategoryRepository`), depois transações.
- [x] **DEL-GATE** — `./gradlew test` verde (192 testes, incluindo `ArchitectureTest`).

## Parte 3 — Frontend

- [x] **FDEL0** — Dois bugs pré-existentes corrigidos na cadeia de propagação de erro (ambos necessários — descobertos em duas rodadas, o segundo só apareceu ao testar exclusão com FK de verdade):
  1. `useApiError.ts`/`useSnackbar.ts` só extraíam `data.message` (objeto); como `GlobalHandlerException` devolve `DomainException` como string crua, a mensagem nunca chegava ao client. Ajustado para tratar `typeof data === 'string'`.
  2. As rotas Nitro (`server/api/**/[id].delete.ts` e as novas `.../status.patch.ts`) faziam só `return $fetch(...)` sem `try/catch` — quando o backend respondia 422, o erro do `$fetch` do lado servidor (ofetch) vazava sem tratamento e o Nitro devolvia pro browser uma mensagem genérica tipo `[DELETE] "http://localhost:8080/categories/2": 422`, perdendo o texto real do `DomainException`. Corrigido reaproveitando o padrão já existente em `server/api/invites/[token]/accept.post.ts` — `try/catch` + `throw createError({statusCode, statusMessage: fetchError.data, data: fetchError.data})` — aplicado nas 4 rotas `[id].delete.ts` (bank-accounts, payment-methods, categories, categories/subcategories) e nas 4 novas `.../status.patch.ts`.
- [x] **FDEL1** — `ConfirmDialog.vue` ganhou prop opcional `confirmColor` para a variante destrutiva (excluir).
- [x] **FDEL2** — Payment Methods: `server/api/payment-methods/[id]/status.patch.ts` novo; botão de toggle ativar/inativar sempre habilitado; botão excluir sem `disabled`, `ConfirmDialog` destrutivo, remove item da lista no sucesso.
- [x] **FDEL3** — Mesmo padrão para Bank Accounts.
- [x] **FDEL4** — Mesmo padrão para Categories (`pages/categories/index.vue`) e SubCategories (`ManageSubCategoriesDialog.vue`), nos dois níveis.

Docs atualizadas: `backend/docs/APP_OVERVIEW.md`, `backend/docs/seed.sql`, `frontend/docs/payment-methods.md`, `frontend/docs/bank-accounts.md`, `frontend/docs/categories.md`.

**Pendente de verificação manual (ambiente sem MySQL/Docker disponível nesta sessão):** o roteiro end-to-end contra um banco real (criar registros, `PATCH .../status`, `DELETE` com e sem dependência, conferir 422 com mensagem específica, testar as 3 páginas no navegador) não pôde ser executado aqui — só a suíte automatizada (specs Groovy/Spock com repositórios mockados) e `eslint` no frontend foram validados. Rodar o roteiro descrito no início deste arquivo ("Verificação end-to-end") antes de considerar esta seção 100% fechada.
