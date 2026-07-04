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

### Frontend

| Grupo | Tarefas | Por que agrupadas assim |
|---|---|---|
| **F1** | F1 | Piloto do padrão (tela mais simples: só `name` + `active`) — valida o template antes das próximas telas. |
| **F2** | F2 | Bank Accounts — mesmo padrão de F1, com a regra extra de saldo não-editável. |
| **F3** | F3 | Categories + SubCategories — inclui um dialog secundário (sub-recurso), por isso maior que F1/F2. |
| **F4** | F4 | Transactions — página principal, maior complexidade de formulário (campos condicionais por `type`); depende dos selects de F1-F3. |
| **F5** | F5 | Reports — só leitura, reaproveita formatação de linha de F4. |
| **coringa** | F6 | Atualização de `PRODUCT.md`, sem dependência técnica de nenhuma tela — encaixar a qualquer momento, inclusive em paralelo aos grupos acima. |

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

- [ ] **T1 — Persistência real de SubCategory**
Criar `infrastructure/repository/jpa/JpaSubCategoryRepository.java` (`extends JpaRepository<SubCategoryEntityJpa, Long>` + `findByCategoryId`). Reescrever `SubCategoryRepositoryImpl.java` com mapeamento direto (sem Space), seguindo o padrão de `PaymentMethodRepositoryImpl.java`. Independente do resto, pode ser feito a qualquer momento.
**Testes (obrigatório):** specs de `CreateSubCategoryService`/`UpdateSubCategoryService`/`DeleteSubCategoryService` (mockando `SubCategoryRepository`, `CategoryRepository`) cobrindo sucesso e categoria pai inexistente — `./gradlew test` verde antes de fechar.
**Docs:** revisar `backend/docs/APP_OVERVIEW.md` (seção SubCategory / REST API Reference) — confirmar que o contrato já descrito bate com o comportamento real agora que não é mais stub; nenhuma mudança de contrato esperada.
*Pronto quando:* `POST/PUT/DELETE /categories/subcategories/*` persistem e consultam de verdade.

### [Grupo B1] Fundação: domínio TRANSFER + persistência de Transaction

- [ ] **T2 — Suporte a TRANSFER no domain Transaction**
Editar `domain/enums/TransactionType.java` (add `TRANSFER`). Editar `domain/Transaction.java`: novo campo `destinationBankAccountId`, atualizar construtor, `update(...)`, e `validate()` com a ramificação descrita acima.
**Testes (obrigatório):** `domain/TransactionSpec.groovy` (criar se não existir) cobrindo `validate()` para os 3 tipos: INCOME/EXPENSE (categoryId/paymentMethodId obrigatórios), TRANSFER (destinationBankAccountId obrigatório e diferente de bankAccountId, categoryId/paymentMethodId dispensados).
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md` seção "Transaction" com o novo campo `destinationBankAccountId` e o valor `TRANSFER`.
*Pronto quando:* `Transaction.validate()` cobre os três tipos corretamente e a spec passa.

- [ ] **T3 — Persistência real de Transaction (já com destinationBankAccountId)**
Criar `infrastructure/repository/jpa/JpaTransactionRepository.java` — `extends JpaRepository<TransactionEntityJpa, Long> & JpaSpecificationExecutor<TransactionEntityJpa>` (specification para suportar os filtros opcionais de `findByFilter`). Adicionar coluna `destinationBankAccountId` em `TransactionEntityJpa`. Reescrever `TransactionRepositoryImpl.java` com mapeamento direto de campos escalares, seguindo a estrutura de `BankAccountRepositoryImpl.java` (sem resolver Space).
*Depende de:* T2 (campo/enum já precisam existir para mapear).
**Testes (obrigatório):** cobertura indireta via specs de T4/T6 (que mockam `TransactionRepository`, então a implementação JPA em si é validada por teste manual/integration — documentar no PR os cenários manuais executados: create/update/delete/findByFilter contra o MySQL local).
**Docs:** nenhuma mudança de contrato público — sem edição de doc necessária.
*Pronto quando:* `POST/PUT/DELETE /transactions` persistem de verdade; `findByFilter` retorna resultados corretos para cada combinação de filtros.

### [Grupo B2] Validação de FKs + helper de efeito de saldo

- [ ] **T4 — Atualizar DTOs e validação de FKs em CreateTransactionService**
Adicionar `destinationBankAccountId` em `CreateTransactionRequest`, `UpdateTransactionRequest`, `TransactionResponse` (`application/transaction/dto/`). Em `CreateTransactionService`, injetar `BankAccountRepository`, `CategoryRepository`, `SubCategoryRepository`, `PaymentMethodRepository`, `UserRepository` e validar existência de cada FK antes de montar a `Transaction` (padrão de `CreateBankAccountService`, que verifica `Space` existe): `bankAccountId` sempre; `destinationBankAccountId` só se `TRANSFER`; `categoryId`/`paymentMethodId` só se não-`TRANSFER`; `subCategoryId` se informado; `userId` sempre.
*Depende de:* T3.
**Testes (obrigatório):** `CreateTransactionServiceSpec.groovy` — sucesso INCOME/EXPENSE/TRANSFER, cada FK inexistente retornando `DomainException`, TRANSFER com `bankAccountId == destinationBankAccountId` rejeitado.
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md`, seção "Key Flows → 2. Recording a Transaction", com o novo campo e a validação de FKs.
*Pronto quando:* `POST /transactions` com qualquer FK inexistente retorna 422 em vez de criar registro órfão, e a spec passa.

- [ ] **T5 — Helper compartilhado de efeito de saldo**
Criar uma classe de aplicação (ex: `application/transaction/TransactionBalanceEffectService.java`) injetável em Create/Update/Delete, com dois métodos:
- `apply(Transaction t)`: INCOME → `bankAccountRepository.findById(t.bankAccountId).credit(amount)` + update; EXPENSE → mesma conta, `debit`; TRANSFER → `debit` na conta de origem + `credit` na conta de destino, ambas persistidas.
- `revert(Transaction t)`: espelha o inverso de cada caso (INCOME→debit, EXPENSE→credit, TRANSFER→credit na origem + debit no destino).
Isso evita duplicar a lógica de "qual conta(s) afetar por tipo" nos três services de Transaction.
*Depende de:* T2, T3.
**Testes (obrigatório):** `TransactionBalanceEffectServiceSpec.groovy` cobrindo `apply`/`revert` para os 3 tipos, mockando `BankAccountRepository`.
**Docs:** criar `backend/docs/transaction-balance-effect.md` explicando a regra de efeito/reversão por tipo (INCOME/EXPENSE/TRANSFER) — referência de manutenção futura para quem mexer nessa lógica.
*Pronto quando:* a spec isolada confirma que aplica/reverte corretamente os 3 tipos.

### [Grupo B3] Integração de criação e atualização

- [ ] **T6 — Integrar em CreateTransactionService**
Depois de validar FKs (T4) e antes de salvar, chamar `balanceEffectService.apply(transaction)`. Envolver em `@Transactional` para atomicidade entre update(s) de BankAccount e save da Transaction.
*Depende de:* T4, T5.
**Testes (obrigatório):** estender `CreateTransactionServiceSpec.groovy` de T4 com verificação de que `BankAccountRepository.update` foi chamado com o saldo correto para cada tipo.
**Docs:** atualizar `backend/docs/transaction-balance-effect.md` (T5) e a seção "Key Flows → 2" do `APP_OVERVIEW.md` confirmando que criar uma transação já reflete no saldo.
*Pronto quando:* criar INCOME/EXPENSE/TRANSFER reflete corretamente o(s) saldo(s) das conta(s) envolvidas e a spec passa.

- [ ] **T7 — Integrar em UpdateTransactionService**
Capturar a Transaction antiga completa antes de `update(...)`. Chamar `balanceEffectService.revert(old)`, então aplicar as mesmas validações de FK de T4 sobre os novos valores, montar a transaction atualizada e chamar `balanceEffectService.apply(updated)`. Cobre os casos de mudança de `type`, `amount`, `bankAccountId` e/ou `destinationBankAccountId`. `@Transactional`.
*Depende de:* T6.
**Testes (obrigatório):** `UpdateTransactionServiceSpec.groovy` — mudança de amount, mudança de type, mudança de bankAccountId/destinationBankAccountId, confirmando reversão + reaplicação corretas nas contas certas.
**Docs:** atualizar `backend/docs/transaction-balance-effect.md` com o fluxo de update (revert + reapply).
*Pronto quando:* editar qualquer combinação desses campos deixa o(s) saldo(s) corretos, sem duplicar nem perder efeito, e a spec passa.

### [Grupo B4] Fechamento do ciclo de vida + validação de Reports

- [ ] **T8 — Integrar em DeleteTransactionService**
Hoje o service só faz `transactionRepository.delete(id)` sem buscar antes. Mudar para: `findById` (lançar `DomainException("Transaction not found")` se `null`), `balanceEffectService.revert(transaction)`, então `delete(id)`. `@Transactional`.
*Depende de:* T5, T3.
**Testes (obrigatório):** `DeleteTransactionServiceSpec.groovy` — reversão correta do saldo, e cenário "transaction not found" retornando `DomainException`.
**Docs:** atualizar `backend/docs/transaction-balance-effect.md` com o fluxo de delete (revert).
*Pronto quando:* excluir uma transação restaura o(s) saldo(s) ao estado anterior à criação dela, e a spec passa.

- [ ] **T9 — Validar Reports fim-a-fim**
Testar com transações reais (INCOME/EXPENSE/TRANSFER) criadas via T6, confirmando que TRANSFER aparece na lista de `transactions[]` mas não entra em `totalIncome`/`totalExpense`/`balance`.
*Depende de:* T3, T6.
**Testes (obrigatório):** `GenerateReportServiceSpec.groovy` (criar — hoje não existe nenhum teste para este service) cobrindo filtros combinados e a exclusão de TRANSFER dos totais.
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md` seção "Key Flows → 3. Financial Report" explicitando que TRANSFER não entra nos totais.
*Pronto quando:* os valores batem com o esperado e a spec passa.

### [Grupo B5] Isolamento multi-tenant no filtro de Transaction

- [ ] **T9b — Escopo por Space no filtro de Transaction (gap de isolamento multi-tenant)**
`Transaction` não guarda `spaceId` diretamente (só `bankAccountId`), e `TransactionRepository.findByFilter` hoje não recebe `spaceId` — uma consulta sem filtro de conta vazaria transações de **todos os spaces**. Adicionar `spaceId` como parâmetro obrigatório em `findByFilter(...)` e em `ReportFilterRequest`; na implementação JPA (`TransactionRepositoryImpl`, via `Specification`), restringir com uma subquery: `bankAccountId IN (SELECT id FROM bank_accounts WHERE space_id = :spaceId)`. Atualizar `GenerateReportService`/`ReportController` para exigir `spaceId` no request.
*Depende de:* T3.
**Testes (obrigatório):** estender `GenerateReportServiceSpec.groovy` (T9) com um cenário de duas contas em spaces diferentes, confirmando que o filtro por `spaceId` isola corretamente.
**Docs:** atualizar `backend/docs/APP_OVERVIEW.md` seção Reports, deixando claro que `spaceId` é obrigatório no filtro.
*Pronto quando:* um `POST /reports` de um space nunca retorna transações de bank accounts de outro space, e a spec passa.

### [Grupo B6] Endpoints de listagem

- [ ] **T9c — Endpoints GET/listagem faltantes (bloqueiam o frontend)**
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

- [ ] **T10 — Segurança fina nos controllers do módulo core**
Adicionar `@PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")` (padrão de `EndpointPermissionController.java`) em **todos** os métodos (incluindo os novos GETs de T9c) de `BankAccountController`, `CategoryController`, `PaymentMethodController`, `TransactionController`, `ReportController`.

**Atualizar `backend/docs/seed.sql` a cada endpoint novo** (regra permanente para qualquer tela nova daqui pra frente, não só desta rodada): adicionar um `INSERT INTO endpoint_permissions` (type=API) por endpoint, seguindo a numeração de `sequence` já usada. Importante: reaproveitar exatamente os mesmos `name` que já existem nas linhas `FRONT_PAGE` da seção 2 do seed (`'Contas Bancárias'`, `'Categorias'`, `'Formas de Pagamento'`, `'Transações'`, `'Relatórios'`) — os blocos de `ADMIN`/`MEMBER` na seção 5 já fazem `JOIN ... ON ep.name IN (...)` usando esses nomes, então as novas linhas API herdam `ALLOW` automaticamente sem precisar editar os blocos de ADMIN/MEMBER. `OWNER` já recebe tudo via `CROSS JOIN`. Lembrar que `CreateEndpointPermissionService` cria `DENY` automático para roles existentes ao criar uma `EndpointPermission` em runtime — o seed é o caminho recomendado para popular de uma vez.
*Independente das demais (pode rodar em paralelo), mas recomenda-se fazer depois de T3-T9c para não confundir bug de persistência com 403 de autorização.*
**Testes (obrigatório):** teste de integração/manual documentado (não há Spock de `@PreAuthorize` isolado no padrão atual do projeto) — confirmar via chamada real com token de um usuário sem `ALLOW` recebendo 403, e com `ALLOW` funcionando; se o projeto já tiver algum padrão de teste de `SecurityService`/`@PreAuthorize` (ver `SecurityServiceSpec.groovy`), seguir o mesmo padrão para os novos endpoints.
**Docs:** `backend/docs/seed.sql` (obrigatório, ver acima) + atualizar `backend/docs/APP_OVERVIEW.md` seção "Access Control Flow" se o padrão de proteção mudar.

> **[coringa]** independente — distribuir entre as sessões dos grupos acima em vez de reservar uma sessão só para isso.

- [ ] **T11 — Specs Groovy/Spock remanescentes (fora do fluxo de Transaction)**
Cobrir os services que ficaram sem spec e não fazem parte da cadeia T1-T9c: `UpdateBankAccountServiceSpec`, `DeleteBankAccountServiceSpec`, `UpdateCategoryServiceSpec`, `DeleteCategoryServiceSpec`, `UpdatePaymentMethodServiceSpec`, `DeletePaymentMethodServiceSpec` — padrão de `CreateBankAccountServiceSpec.groovy` (mockando as interfaces de repository, nunca os `*RepositoryImpl`).
*Depende de:* nenhuma (independente, pode ser feito a qualquer momento em paralelo).
**Testes (obrigatório):** as próprias specs listadas acima, `./gradlew test` verde.
**Docs:** nenhuma — são apenas testes de comportamento já documentado.

### [Grupo B8] Gate final

- [ ] **T12 — ArchUnit + suíte completa (gate final)**
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

- [ ] **F1 — Payment Methods** (`pages/payment-methods/`)
Tela mais simples (só `name` + `active`) — bom ponto de partida para validar o padrão. CRUD completo + `server/api/payment-methods/*`.
*Depende de:* backend T10 (endpoint com `@PreAuthorize` + seed) e o GET de T9c.
**Verificação:** rodar `pnpm dev`, abrir `/payment-methods`, criar/editar/excluir manualmente no navegador (este projeto não tem suíte de teste de frontend — a verificação é funcional/manual, seguindo a skill `verify`).
**Docs:** criar `frontend/docs/payment-methods.md` (seguir o formato de `frontend/docs/user-invite.md`) descrevendo rotas, campos do form e componentes usados.

### [Grupo F2] Bank Accounts

- [ ] **F2 — Bank Accounts** (`pages/bank-accounts/`)
Form de criação: `name`, `bankName`, `initialBalance`. Form de edição: só `name`/`bankName` (balance não é editável diretamente, só via transações — refletir isso na UI, sem campo de saldo editável no form de edição, só exibido como read-only na tabela). Exclusão = soft delete (`active=false`, backend já faz isso).
*Depende de:* mesma base de F1 (padrão), backend T9c/T10.
**Verificação:** manual no navegador — criar conta, conferir saldo inicial exibido, editar nome/banco, desativar.
**Docs:** criar `frontend/docs/bank-accounts.md`.

### [Grupo F3] Categories + SubCategories

- [ ] **F3 — Categories + SubCategories** (`pages/categories/`)
Lista de categorias (`name`, `active`). Gerenciar subcategorias como sub-recurso: seguir o padrão de `RolePermissionsDialog.vue` (dialog secundário aberto a partir de uma linha da tabela) — um `ManageSubCategoriesDialog.vue` que lista/cria/edita/exclui as subcategorias daquela categoria (`server/api/categories/[id]/subcategories/*`, espelhando `server/api/roles/[id]/permissions/*`).
*Depende de:* backend T1 (persistência real de SubCategory) + T9c.
**Verificação:** manual no navegador — criar categoria, abrir subcategorias, criar/editar/excluir subcategoria.
**Docs:** criar `frontend/docs/categories.md`.

### [Grupo F4] Transactions (página principal)

- [ ] **F4 — Transactions** (`pages/transactions/`) — página principal
Form: `type` (INCOME/EXPENSE/TRANSFER — select), `bankAccountId` (select, populado por F2/GET bank-accounts do space ativo), `destinationBankAccountId` (só aparece se `type=TRANSFER`, mesma lista de contas, excluindo a selecionada em `bankAccountId`), `categoryId`+`subCategoryId` (selects em cascata, só aparecem se `type≠TRANSFER`, populados por F3), `paymentMethodId` (select, só se `type≠TRANSFER`, populado por F1), `amount`, `transactionDate` (date picker), `description` (opcional). Lista com filtro de período (date range, default mês atual) usando o novo `GET /transactions`.
*Depende de:* F1, F2, F3 (para os selects) e backend T2-T9c completos (domain+persistência+efeito de saldo+listagem funcionando).
**Verificação:** manual no navegador — criar INCOME/EXPENSE/TRANSFER, conferir que o saldo das contas envolvidas muda (checando em `/bank-accounts`), editar e excluir uma transação e conferir reversão do saldo.
**Docs:** criar `frontend/docs/transactions.md`.

### [Grupo F5] Reports

- [ ] **F5 — Reports** (`pages/reports/`)
Página só de leitura: formulário de filtro (`from`/`to` obrigatórios, demais opcionais — mesmos campos de `ReportFilterRequest`) + cards de resumo (`totalIncome`/`totalExpense`/`balance`, seguir estilo de "informativo, não alarmante" do `PRODUCT.md`) + tabela das transações do período (reaproveitar a formatação de linha usada em F4, sem ações de editar/excluir aqui). `server/api/reports/index.post.ts` proxy simples.
*Depende de:* F4 (mesmos componentes de formatação de linha), backend T9b (escopo por space).
**Verificação:** manual no navegador — filtrar por período, conferir totais e que TRANSFER aparece na lista mas não nos totais.
**Docs:** criar `frontend/docs/reports.md`.

> **[coringa]** sem dependência técnica de nenhuma tela — encaixar a qualquer momento, inclusive em paralelo aos grupos acima.

- [ ] **F6 — Ajustar `frontend/PRODUCT.md`**
O doc atual descreve o produto só como "admin control plane" (roles/permissions/spaces) e não menciona nada financeiro — está desatualizado frente ao propósito real do app (`backend/docs/APP_OVERVIEW.md`). Atualizar para refletir que o público final também inclui o usuário comum controlando as próprias finanças, não só admins operacionais.
*Baixa prioridade, pode ser feito a qualquer momento.*
**Docs:** esta tarefa É a própria atualização de doc (`frontend/PRODUCT.md`).

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
