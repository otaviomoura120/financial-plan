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
- **Rate limiting**, principalmente em `POST /users`, `GET /users/search` e `/invites/{token}/accept`.
- **MySQL sem porta exposta** para a internet: só acessível pela rede interna do backend.
- **Backups** automáticos do banco, com teste de restauração.
- **Backend sem exposição direta:** se todo acesso passa pelo proxy Nitro, deixar a API acessível só pela rede interna. Observação: hoje `/api/_auth/token` entrega o access token ao navegador, então quem estiver logado consegue chamar a API direto. A autorização do backend já trata isso, mas fechar a rede reduz a superfície de ataque.
- **Segredos do frontend:** `NUXT_AUTH0_SESSION_SECRET` forte (`openssl rand -hex 64`) e `NUXT_AUTH0_CLIENT_SECRET` fora do repositório.
- **Frontend em modo produção:** `pnpm build` e `node .output/server/index.mjs`, nunca `pnpm dev`.
- **Logs:** não logar tokens nem dados pessoais; definir retenção.

---

## Observações

- No frontend há um erro de TypeScript pré-existente em `frontend/server/utils/backendFetch.ts` (TS2322). Não é de segurança, mas faz o `pnpm build` falhar se o build rodar o `vue-tsc`.
- Os itens 1 a 4 já foram corrigidos. Os itens 5 a 8 e a parte de infraestrutura devem ser feitos antes de liberar para usuários reais.
