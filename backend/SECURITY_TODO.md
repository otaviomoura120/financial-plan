# Pendências de segurança antes do deploy

Levantamento feito em 2026-10-20 na revisão de segurança pré-deploy.

## ✅ Já corrigido (2026-10-21)

- **IDOR entre Spaces**: o Space agora vem só do header `X-Space-Id`, e toda operação por id confere `belongsToSpace()`.
- **`userId` vindo do body**: transações, transações de cartão e recorrências usam o usuário autenticado.
- **`POST /users`**: o `auth0Sub` vem do JWT.
- **Header `X-Space-Id` inválido**: agora retorna 403 em vez de 500.

Detalhes em `docs/auth-flow.md`, seção "The header is the only source of the Space for data endpoints".

### ✅ Corrigido em 2026-10-21 (itens 1 a 4 abaixo)

- **1. Credenciais:** `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, com os valores de desenvolvimento como padrão. Em produção, definir as três variáveis e usar um usuário MySQL sem privilégio de root.
- **2. Schema:** Flyway com `V1__baseline_schema.sql` e `ddl-auto=validate` (sobrescrevível por `JPA_DDL_AUTO`). Bancos existentes são marcados como V1 sem reexecutar o script. O `docs/seed.sql` continua manual: precisa ser aplicado uma vez num banco de produção novo. Funcionamento e regras em `docs/database-migrations.md`.
- **3. CORS:** origens em `APP_CORS_ALLOWED_ORIGINS` (lista separada por vírgula, padrão `app.frontend-url`), headers restritos a `Authorization`, `Content-Type` e `X-Space-Id`, e `allowCredentials=false`.
- **4. Busca de usuário:** `GET /users/search` foi substituído por `GET /spaces/{id}/members/search`, liberado só para quem pode convidar naquele Space (`SecurityService.userCanInviteToSpace`). A resposta traz apenas o nome mascarado (`"Joã* S****"`), sem id nem e-mail.
  - **Limitação:** a checagem de permissão é regra de negócio, não proteção. Todo usuário vira OWNER do próprio Space no onboarding e pode convidar nele, então qualquer pessoa que crie uma conta consegue usar a busca. O endpoint continua revelando se um e-mail tem conta (200/404). A proteção real é a máscara mais o **rate limit** (ver Infraestrutura), que segue pendente. Para eliminar a descoberta por completo, seria preciso remover a busca e convidar sempre pelo e-mail digitado.


### ✅ Corrigido em 2026-10-21 (itens 6 a 8 abaixo)

- **6. Lib do Auth0:** `auth0-springboot-api:1.0.0-beta.1` foi substituída por `spring-boot-starter-security-oauth2-resource-server`. O token é validado por assinatura (JWKS do tenant), expiração, issuer (`AUTH0_ISSUER_URI`) e audience (`AUTH0_AUDIENCE`). O `JwtValidationSpec` cobre os casos válidos e inválidos usando as properties reais.
- **7. Stack trace:** o `printStackTrace()` saiu. O erro genérico é logado uma vez só, pelo logger, com o stack trace.
- **8. Não encontrado:** `NotFoundException` → 404 quando o recurso da URL não existe ou é de outro Space. Um ID inválido no body continua 422.

### ✅ Segunda revisão (2026-10-26), corrigido em 2026-10-29

- **🔴 E-mail do usuário vindo do body:** o `POST /users` gravava o e-mail digitado no onboarding, e os convites são ligados ao e-mail. Qualquer pessoa podia se cadastrar com o e-mail de outra, ver os convites dela em `GET /invites` e aceitá-los. Agora o backend busca o e-mail no `/userinfo` do Auth0 (`IdentityProvider` / `Auth0IdentityProvider`), exige `email_verified`, confere o `sub` e recusa e-mail já cadastrado. A migration `V2` cria o índice único em `users.email`. Usuários cadastrados antes disso não foram revalidados.
- **🟠 `creatorId` no `POST /spaces`:** o dono do Space era o usuário informado no body. Agora é sempre o usuário autenticado.
- **🟠 Parcelas sem limite:** o limite de 60 só era checado depois do loop que monta os meses, então um `totalInstallments` enorme esgotava a memória. Agora é checado antes (`CreditCardTransaction.requireValidInstallmentCount`).
- **🟡 HTML no e-mail de convite:** os nomes do Space e da role entravam no HTML sem escape, o que permitia phishing pelo domínio do sistema. Agora são escapados, e o assunto não aceita quebras de linha.
- **🟡 Access token no navegador:** `/api/_auth/token`, `composables/useApi.ts`, `utils/api.ts` e `NavSearchBar.vue` (sobra do template) foram removidos. O token fica só no servidor Nitro.
- **🟡 Headers de segurança no frontend:** `routeRules` no `nuxt.config.ts` com `frame-ancestors 'none'`, `X-Frame-Options`, `nosniff`, `Referrer-Policy`, `Permissions-Policy` e HSTS. Ainda não há CSP para scripts; ver pendências.
- **🟡 Data de início das recorrências sem limite no passado:** contas e assinaturas recorrentes geram um registro por mês desde a data de início, então um início em `0001-01-01` criava cerca de 24 mil linhas numa única requisição. Agora a data de início não pode ser anterior a 5 anos atrás (`RecurrenceStartPolicy`). O limite vale na criação e na edição que muda a data; uma recorrência que envelhece além dos 5 anos continua editável.
- O erro TS2322 em `frontend/server/utils/backendFetch.ts` também foi corrigido.

### ⏳ Pendências da segunda revisão

- **🔵 Convite recusado pode ser aceito:** o `AcceptInviteService` não checa o status `DECLINED`.
- **🔵 CSP para scripts:** exige nonce por requisição (módulo `nuxt-security`) e teste completo das telas.
- **🔵 `apiBaseUrl` público:** a URL do backend está em `runtimeConfig.public`, mas só o servidor Nitro a usa. Pode ir para a parte privada da config.
- **🔵 Envio de convites para qualquer e-mail:** quem pode convidar consegue disparar e-mails para endereços arbitrários. Entra no rate limit abaixo.
---

## 🟠 Alto

### 1. Credenciais do banco fixas no código
- **Onde:** `src/main/resources/application.properties`
  ```properties
  spring.datasource.url=jdbc:mysql://localhost:3306/financial_plan
  spring.datasource.username=root
  spring.datasource.password=root
  ```
- **Risco:** a senha fica versionada. Usar `root` dá ao app privilégio total no MySQL, então qualquer falha de SQL ou de credencial vaza o banco inteiro.
- **Correção:**
  - Ler de variáveis de ambiente: `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, sem valor default em produção.
  - Criar um usuário MySQL próprio, com acesso só ao schema `financial_plan` e apenas aos privilégios necessários (`SELECT, INSERT, UPDATE, DELETE`, mais DDL apenas se o item 2 não for feito).

### 2. `ddl-auto=update` em produção
- **Onde:** `application.properties` → `spring.jpa.hibernate.ddl-auto=update`
- **Risco:** o Hibernate altera o schema sozinho ao subir. Isso exige um usuário com privilégio de DDL e pode gerar mudanças não revisadas ou perda de dados em renomeações.
- **Correção:** usar `ddl-auto=validate` em produção e versionar o schema com Flyway ou Liquibase. Separar por profile (`application-prod.properties`).

### 3. CORS fixo em localhost
- **Onde:** `infrastructure/config/SecurityConfig.java` → `setAllowedOrigins(List.of("http://localhost:5173"))`
- **Risco:** em produção, ou o frontend quebra, ou alguém "resolve" abrindo para `*`. Com `allowCredentials(true)`, abrir para `*` seria perigoso.
- **Correção:**
  - Mover a lista de origens para uma property (ex.: `app.cors.allowed-origins=${APP_FRONTEND_URL}`). Já existe `app.frontend-url`, que pode ser reaproveitada.
  - Aceitar apenas o domínio real em HTTPS.
  - Restringir `setAllowedHeaders` aos headers usados (`Authorization`, `Content-Type`, `X-Space-Id`) em vez de `*`.
  - Observação: como o frontend chama o backend pelo proxy Nitro (server-to-server), o CORS pode nem ser necessário. Nesse caso, desligar.

### 4. Enumeração de e-mails em `GET /users/search`
- **Onde:** `infrastructure/controller/UserController.java` → `search(@RequestParam String email)`, sem `@PreAuthorize`
- **Risco:** qualquer usuário logado descobre se um e-mail está cadastrado e obtém `id` e `name` dessa pessoa.
- **Correção:**
  - Restringir a quem tem permissão de convidar membros num Space, por exemplo com `userHasPermissionForURL` e um registro em `endpoint_permissions`/`seed.sql`.
  - Ou eliminar a busca: o fluxo de convite já funciona por e-mail (`InviteSpaceMemberService`), sem precisar revelar se o usuário existe.
  - Considerar rate limit nesse endpoint.

---

## 🟡 Médio

### 5. SMTP apontando para o sandbox do Mailtrap
- **Onde:** `application.properties` → `spring.mail.host=sandbox.smtp.mailtrap.io`, porta `2525`
- **Risco:** funcional mais do que de segurança. Em produção os convites não chegam a ninguém.
- **Correção:** configurar o provedor real (SES, SendGrid, SMTP do domínio) via variáveis de ambiente, por profile. Manter `starttls` obrigatório.

### 6. Lib de autenticação em versão beta
- **Onde:** `build.gradle` → `com.auth0:auth0-springboot-api:1.0.0-beta.1`
- **Risco:** é a peça que valida o JWT, e está em versão beta, com menos garantias de estabilidade e de correções de segurança.
- **Correção:** avaliar a migração para `spring-boot-starter-oauth2-resource-server` com `issuer-uri=https://financial-plan.us.auth0.com/` e validação de `audience`. É o caminho oficial e estável do Spring. Se mantiver a lib, acompanhar os releases.

### 7. Stack trace no stdout em erros genéricos
- **Onde:** `infrastructure/config/GlobalHandlerException.java` → `handleGenericException` chama `exception.printStackTrace()` além do `logger.error`
- **Risco:** baixo, porque a resposta HTTP é genérica. Mas a exceção é logada duas vezes, e o stdout pode ir para lugares sem controle de acesso.
- **Correção:** remover o `printStackTrace()` e usar só o logger, com a exceção como segundo argumento para manter o stack no log estruturado.

### 8. Respostas de "não encontrado" com status 422
- **Onde:** os services lançam `DomainException("... not found")`, mapeada para 422.
- **Risco:** nenhum para isolamento, porque registro de outro Space e registro inexistente respondem igual. É só semântica HTTP.
- **Correção (opcional):** criar uma `NotFoundException` mapeada para 404.

---

## 🔧 Infraestrutura (fora do código)

- **HTTPS obrigatório** no proxy reverso (Nginx/Caddy/ALB), com redirect de HTTP.
- **Rate limiting**, principalmente em `POST /users`, `GET /spaces/{id}/members/search`, `POST /spaces/{id}/invites` e `/invites/{token}/accept`.
- **MySQL sem porta exposta** para a internet: só acessível pela rede interna do backend.
- **Backups** automáticos do banco, com teste de restauração.
- **Backend sem exposição direta:** se todo acesso passa pelo proxy Nitro, deixar a API acessível só pela rede interna. Desde 2026-10-29 o access token não chega mais ao navegador, então fechar a rede passa a ser a última camada.
- **Segredos do frontend:** `NUXT_AUTH0_SESSION_SECRET` forte (`openssl rand -hex 64`) e `NUXT_AUTH0_CLIENT_SECRET` fora do repositório.
- **Frontend em modo produção:** `pnpm build` e `node .output/server/index.mjs`, nunca `pnpm dev`.
- **Logs:** não logar tokens nem dados pessoais; definir retenção.

---

## Observações

- Os itens 1 a 4 e 6 a 8 e os itens da segunda revisão já foram corrigidos. Faltam o item 5 (SMTP, aguardando a contratação do serviço de e-mail), as pendências da segunda revisão e a parte de infraestrutura, que devem ser feitos antes de liberar para usuários reais.
- **Auth0:** configurar um provedor de e-mail próprio em Branding → Email Provider. Sem ele, o e-mail de verificação pode não chegar, e o cadastro exige e-mail verificado.
