-- =============================================================================
-- SEED DATA — financial_plan
-- =============================================================================
-- Execution order matters due to FK constraints:
--   1. endpoint_permissions (no FK dependencies)
--   2. group_menus          (no FK dependencies)
--   3. group_menu_children  (FK → group_menus.id)
--
-- After populating a Space and its Roles (OWNER, ADMIN, MEMBER), run:
--   4. role_endpoint_permissions
--
-- Run against an empty database or adjust WHERE clause ids accordingly.
-- =============================================================================

-- =============================================================================
-- 1. ENDPOINT PERMISSIONS — API
--    Controls access to backend REST endpoints protected by @PreAuthorize.
--    The `endpoint` field is a Java regex matched against the full request URI.
--    The SecurityService finds allowed EndpointPermissions for the user's roles
--    (via role_endpoint_permissions with permission='ALLOW') and checks if any
--    matches the HTTP method and path.
-- =============================================================================

INSERT INTO endpoint_permissions
    (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
VALUES

-- IndexController — GET /
(1, '/',                                      'Página Inicial',             1,  'API', 'GET',        'Geral',     NOW(), NOW()),

-- RoleController — /roles
(1, '/roles',                                 'Listar Funções',             10, 'API', 'GET',        'Role',      NOW(), NOW()),
(1, '/roles',                                 'Criar Função',               11, 'API', 'POST',       'Role',      NOW(), NOW()),
(1, '/roles/[0-9]+',                          'Gerenciar Função',           12, 'API', 'PUT,DELETE', 'Role',      NOW(), NOW()),
(1, '/roles/[0-9]+/assign-user/[0-9]+',       'Atribuir Função a Usuário',  13, 'API', 'PUT',        'Role',      NOW(), NOW()),
(1, '/roles/[0-9]+/permissions',              'Listar Permissões da Função',14, 'API', 'GET',        'Role',      NOW(), NOW()),
(1, '/roles/[0-9]+/permissions/[0-9]+',       'Alterar Acesso da Função',   15, 'API', 'PATCH',      'Role',      NOW(), NOW()),

-- EndpointPermissionController — /endpoint-permissions
(1, '/endpoint-permissions',                  'Listar Permissões',          20, 'API', 'GET',        'internal_management', NOW(), NOW()),
(1, '/endpoint-permissions',                  'Criar Permissão',            21, 'API', 'POST',       'internal_management', NOW(), NOW()),
(1, '/endpoint-permissions/[0-9]+',           'Gerenciar Permissão',        22, 'API', 'PUT,DELETE', 'internal_management', NOW(), NOW()),

-- GroupMenuController — /group-menus
(1, '/group-menus',                           'Listar Grupos de Menu',      30, 'API', 'GET',        'internal_management', NOW(), NOW()),
(1, '/group-menus',                           'Criar Grupo de Menu',        31, 'API', 'POST',       'internal_management', NOW(), NOW()),
(1, '/group-menus/[0-9]+',                    'Gerenciar Grupo de Menu',    32, 'API', 'PUT,DELETE', 'internal_management', NOW(), NOW()),
(1, '/group-menus/[0-9]+/children',           'Adicionar Item ao Menu',     33, 'API', 'POST',       'internal_management', NOW(), NOW()),
(1, '/group-menus/children/[0-9]+',           'Gerenciar Item do Menu',     34, 'API', 'PUT,DELETE', 'internal_management', NOW(), NOW()),

-- SpaceController — /spaces
(1, '/spaces/[0-9]+',                         'Gerenciar Espaço',           40, 'API', 'PUT,DELETE',     'Espaço', NOW(), NOW()),
(1, '/spaces/user/[0-9]+',                    'Listar Espaços do Usuário',  41, 'API', 'GET',            'Espaço', NOW(), NOW()),
(1, '/spaces/[0-9]+/members',                 'Listar Membros do Espaço',   42, 'API', 'GET',            'Espaço', NOW(), NOW()),
(1, '/spaces/[0-9]+/members/[0-9]+',          'Gerenciar Membros do Espaço',43, 'API', 'PUT,DELETE',     'Espaço', NOW(), NOW()),
(1, '/spaces/[0-9]+/invites',                 'Listar Convites do Espaço',  44, 'API', 'GET',            'Espaço', NOW(), NOW()),
(1, '/spaces/[0-9]+/invites',                 'Convidar para Espaço',       45, 'API', 'POST',           'Espaço', NOW(), NOW()),
(1, '/spaces/[0-9]+/invites/[0-9]+',          'Cancelar Convite do Espaço', 46, 'API', 'DELETE',         'Espaço', NOW(), NOW()),

-- BankAccountController — /bank-accounts
-- name reuses the FRONT_PAGE row 'Contas Bancárias' (section 2) so ADMIN/MEMBER
-- already ALLOW it via the existing ep.name IN (...) joins in section 5 below.
(1, '/bank-accounts',                         'Contas Bancárias',           50, 'API', 'GET,POST',   'Conta',        NOW(), NOW()),
(1, '/bank-accounts/[0-9]+',                  'Contas Bancárias',           51, 'API', 'PUT,DELETE', 'Conta',        NOW(), NOW()),
(1, '/bank-accounts/[0-9]+/status',           'Contas Bancárias',           61, 'API', 'PATCH',      'Conta',        NOW(), NOW()),

-- CategoryController — /categories (includes /subcategories sub-resource)
(1, '/categories',                            'Categorias',                 52, 'API', 'GET,POST',   'Configuração', NOW(), NOW()),
(1, '/categories/[0-9]+',                     'Categorias',                 53, 'API', 'PUT,DELETE', 'Configuração', NOW(), NOW()),
(1, '/categories/subcategories',              'Categorias',                 54, 'API', 'POST',       'Configuração', NOW(), NOW()),
(1, '/categories/subcategories/[0-9]+',       'Categorias',                 55, 'API', 'PUT,DELETE', 'Configuração', NOW(), NOW()),
(1, '/categories/[0-9]+/status',              'Categorias',                 63, 'API', 'PATCH',      'Configuração', NOW(), NOW()),
(1, '/categories/subcategories/[0-9]+/status','Categorias',                 64, 'API', 'PATCH',      'Configuração', NOW(), NOW()),

-- PaymentMethodController — /payment-methods
(1, '/payment-methods',                       'Formas de Pagamento',        56, 'API', 'GET,POST',   'Conta',        NOW(), NOW()),
(1, '/payment-methods/[0-9]+',                'Formas de Pagamento',        57, 'API', 'PUT,DELETE', 'Conta',        NOW(), NOW()),
(1, '/payment-methods/[0-9]+/status',         'Formas de Pagamento',        62, 'API', 'PATCH',      'Conta',        NOW(), NOW()),

-- TransactionController — /transactions
(1, '/transactions',                          'Transações',                 58, 'API', 'GET,POST',   'Financeiro',   NOW(), NOW()),
(1, '/transactions/[0-9]+',                   'Transações',                 59, 'API', 'PUT,DELETE', 'Financeiro',   NOW(), NOW()),

-- ReportController — /reports
(1, '/reports',                               'Relatórios',                 60, 'API', 'POST',       'Financeiro',   NOW(), NOW()),

-- CreditCardController — /credit-cards
(1, '/credit-cards',                          'Cartões de Crédito',         65, 'API', 'GET,POST',   'Conta',        NOW(), NOW()),
(1, '/credit-cards/[0-9]+',                   'Cartões de Crédito',         66, 'API', 'PUT,DELETE', 'Conta',        NOW(), NOW()),

-- CreditCardTransactionController — /credit-card-transactions
-- name reuses 'Cartões de Crédito' (same as CreditCardController above) so it inherits
-- ALLOW automatically via the ADMIN/MEMBER ep.name IN (...) joins in section 5 below.
(1, '/credit-card-transactions',                                          'Cartões de Crédito', 67, 'API', 'GET,POST',   'Conta', NOW(), NOW()),
(1, '/credit-card-transactions/[0-9]+',                                   'Cartões de Crédito', 68, 'API', 'PUT,DELETE', 'Conta', NOW(), NOW()),
(1, '/credit-card-transactions/installment-groups/[a-zA-Z0-9-]+',         'Cartões de Crédito', 69, 'API', 'GET',        'Conta', NOW(), NOW()),
(1, '/credit-card-transactions/installment-groups/[a-zA-Z0-9-]+/anticipate','Cartões de Crédito', 70, 'API', 'POST',     'Conta', NOW(), NOW()),

-- CreditCardInvoiceController — /credit-cards/invoices, /credit-cards/{id}/invoices/{referenceMonth}
(1, '/credit-cards/invoices',                                                        'Cartões de Crédito', 71, 'API', 'GET',  'Conta', NOW(), NOW()),
(1, '/credit-cards/[0-9]+/invoices/[0-9]{4}-[0-9]{2}-[0-9]{2}/pay',                   'Cartões de Crédito', 72, 'API', 'POST', 'Conta', NOW(), NOW()),
(1, '/credit-cards/[0-9]+/invoices/[0-9]{4}-[0-9]{2}-[0-9]{2}/undo-payment',          'Cartões de Crédito', 73, 'API', 'POST', 'Conta', NOW(), NOW()),

-- BillController — /bills (agora CRUD de BillRecurring)
(1, '/bills',                                 'Contas a Pagar',             74, 'API', 'GET,POST',   'Conta', NOW(), NOW()),
(1, '/bills/[0-9]+',                          'Contas a Pagar',             75, 'API', 'PUT,DELETE', 'Conta', NOW(), NOW()),
(1, '/bills/[0-9]+/schedule',                 'Contas a Pagar',             76, 'API', 'PUT',        'Conta', NOW(), NOW()),

-- BillInstanceController — /bills/instances (agora CRUD de Bill, a conta lançada)
(1, '/bills/instances',                       'Contas a Pagar',             77, 'API', 'GET,POST',   'Conta', NOW(), NOW()),
(1, '/bills/instances/[0-9]+',                'Contas a Pagar',             81, 'API', 'PUT,DELETE', 'Conta', NOW(), NOW()),
(1, '/bills/instances/[0-9]+/pay',            'Contas a Pagar',             79, 'API', 'POST',       'Conta', NOW(), NOW()),
(1, '/bills/instances/[0-9]+/undo-payment',   'Contas a Pagar',             80, 'API', 'POST',       'Conta', NOW(), NOW());


-- =============================================================================
-- 2. ENDPOINT PERMISSIONS — FRONT_PAGE
--    Controls which pages appear in the sidebar for each role.
--    The `endpoint` field is a Java regex matched against GroupMenuChildren.endpoint
--    via String.matches() (full-string match). Use exact paths unless wildcards
--    are needed for nested pages (e.g. '/transactions.*' covers /transactions/new).
-- =============================================================================

INSERT INTO endpoint_permissions
    (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
VALUES

(1, '/',                    'Início',                1,  'FRONT_PAGE', 'GET', 'Dashboard',    NOW(), NOW()),
(1, '/transactions',        'Transações',            2,  'FRONT_PAGE', 'GET', 'Financeiro',   NOW(), NOW()),
(1, '/reports',             'Relatórios',            3,  'FRONT_PAGE', 'GET', 'Financeiro',   NOW(), NOW()),
(1, '/bank-accounts',       'Contas Bancárias',      4,  'FRONT_PAGE', 'GET', 'Conta',        NOW(), NOW()),
(1, '/payment-methods',     'Formas de Pagamento',   5,  'FRONT_PAGE', 'GET', 'Conta',        NOW(), NOW()),
(1, '/categories',          'Categorias',            6,  'FRONT_PAGE', 'GET', 'Configuração', NOW(), NOW()),
(1, '/users',               'Usuários',              7,  'FRONT_PAGE', 'GET', 'Administração',NOW(), NOW()),
(1, '/spaces',              'Espaços',               8,  'FRONT_PAGE', 'GET', 'Administração',NOW(), NOW()),
(1, '/roles',               'Funções',               9,  'FRONT_PAGE', 'GET', 'Administração',NOW(), NOW()),
(1, '/endpoint-permissions','Permissões de Acesso',  10, 'FRONT_PAGE', 'GET', 'internal_management',NOW(), NOW()),
(1, '/group-menus',         'Estrutura de Menu',     11, 'FRONT_PAGE', 'GET', 'internal_management',NOW(), NOW()),
(1, '/credit-cards',        'Cartões de Crédito',    12, 'FRONT_PAGE', 'GET', 'Conta',        NOW(), NOW()),
(1, '/bills',                'Contas a Pagar',       13, 'FRONT_PAGE', 'GET', 'Conta',        NOW(), NOW());


-- =============================================================================
-- 3. GROUP MENUS — sidebar navigation groups
-- =============================================================================

INSERT INTO group_menus (name, icon, created_at, updated_at)
VALUES
('Dashboard',           'tabler-home',          NOW(), NOW()),
('Financeiro',          'tabler-cash',          NOW(), NOW()),
('Contas e Pagamentos', 'tabler-building-bank', NOW(), NOW()),
('Configurações',       'tabler-category',      NOW(), NOW()),
('Administração',       'tabler-shield-lock',   NOW(), NOW());


-- =============================================================================
-- 4. GROUP MENU CHILDREN — individual sidebar items
-- =============================================================================

INSERT INTO group_menu_children (name, endpoint, icon, group_menu_id, created_at, updated_at)

-- Dashboard
SELECT 'Início',              '/',                    'tabler-smart-home',       id, NOW(), NOW() FROM group_menus WHERE name = 'Dashboard'

UNION ALL

-- Financeiro
SELECT 'Transações',          '/transactions',        'tabler-arrows-exchange',  id, NOW(), NOW() FROM group_menus WHERE name = 'Financeiro'
UNION ALL
SELECT 'Relatórios',          '/reports',             'tabler-report-analytics', id, NOW(), NOW() FROM group_menus WHERE name = 'Financeiro'

UNION ALL

-- Contas e Pagamentos
SELECT 'Contas Bancárias',    '/bank-accounts',       'tabler-credit-card',      id, NOW(), NOW() FROM group_menus WHERE name = 'Contas e Pagamentos'
UNION ALL
SELECT 'Formas de Pagamento', '/payment-methods',     'tabler-wallet',           id, NOW(), NOW() FROM group_menus WHERE name = 'Contas e Pagamentos'
UNION ALL
SELECT 'Cartões de Crédito',  '/credit-cards',        'tabler-credit-card',     id, NOW(), NOW() FROM group_menus WHERE name = 'Contas e Pagamentos'
UNION ALL
SELECT 'Contas a Pagar',      '/bills',               'tabler-calendar-due',    id, NOW(), NOW() FROM group_menus WHERE name = 'Contas e Pagamentos'

UNION ALL

-- Configurações
SELECT 'Categorias',          '/categories',          'tabler-category',         id, NOW(), NOW() FROM group_menus WHERE name = 'Configurações'

UNION ALL

-- Administração
SELECT 'Usuários',            '/users',               'tabler-users',            id, NOW(), NOW() FROM group_menus WHERE name = 'Administração'
UNION ALL
SELECT 'Espaços',             '/spaces',              'tabler-building',         id, NOW(), NOW() FROM group_menus WHERE name = 'Administração'
UNION ALL
SELECT 'Funções',             '/roles',               'tabler-user-check',       id, NOW(), NOW() FROM group_menus WHERE name = 'Administração'
UNION ALL
SELECT 'Permissões de Acesso','/endpoint-permissions','tabler-key',              id, NOW(), NOW() FROM group_menus WHERE name = 'Administração'
UNION ALL
SELECT 'Estrutura de Menu',   '/group-menus',         'tabler-layout-sidebar',   id, NOW(), NOW() FROM group_menus WHERE name = 'Administração';


-- =============================================================================
-- 5. ROLE ENDPOINT PERMISSIONS
--    Run this section AFTER creating a Space and its default roles (OWNER,
--    ADMIN, MEMBER). Assumes those roles exist in the `roles` table.
--
--    OWNER  → ALLOW em tudo
--    ADMIN  → ALLOW nas permissões básicas (financial + user management)
--    MEMBER → ALLOW apenas nas permissões básicas de leitura/escrita financeira
-- =============================================================================

-- OWNER: acesso total
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER';

-- ADMIN: acesso às permissões básicas (API)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name IN ('Página Inicial', 'Listar Funções', 'Transações', 'Relatórios',
                   'Contas Bancárias', 'Formas de Pagamento', 'Categorias', 'Cartões de Crédito', 'Contas a Pagar',
                   'Listar Espaços do Usuário', 'Listar Membros do Espaço', 'Listar Convites do Espaço')
WHERE r.name = 'ADMIN';

-- ADMIN: DENY para o restante
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'DENY', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name NOT IN ('Página Inicial', 'Listar Funções', 'Transações', 'Relatórios',
                       'Contas Bancárias', 'Formas de Pagamento', 'Categorias', 'Cartões de Crédito', 'Contas a Pagar',
                       'Listar Espaços do Usuário', 'Listar Membros do Espaço', 'Listar Convites do Espaço')
WHERE r.name = 'ADMIN';

-- MEMBER: acesso às permissões básicas (API)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name IN ('Página Inicial', 'Transações', 'Relatórios',
                   'Contas Bancárias', 'Formas de Pagamento', 'Categorias', 'Cartões de Crédito', 'Contas a Pagar',
                   'Listar Espaços do Usuário', 'Listar Membros do Espaço')
WHERE r.name = 'MEMBER';

-- MEMBER: DENY para o restante
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'DENY', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name NOT IN ('Página Inicial', 'Transações', 'Relatórios',
                       'Contas Bancárias', 'Formas de Pagamento', 'Categorias', 'Cartões de Crédito', 'Contas a Pagar',
                       'Listar Espaços do Usuário', 'Listar Membros do Espaço')
WHERE r.name = 'MEMBER';

-- =============================================================================
-- NOTA: após rodar este seed em um banco que tinha a coluna permitted_roles,
-- execute manualmente:
--   ALTER TABLE endpoint_permissions DROP COLUMN permitted_roles;
-- =============================================================================

-- =============================================================================
-- NOTA: se o banco foi criado antes de DECLINED ser adicionado ao InviteStatus,
-- execute manualmente para incluir o novo valor no ENUM:
--   ALTER TABLE space_invites
--     MODIFY COLUMN status ENUM('PENDING','ACCEPTED','CANCELLED','DECLINED') NOT NULL;
-- =============================================================================

-- =============================================================================
-- 6. INCREMENTAL — novos endpoints PATCH .../status (Exclusão real + Ativar/Inativar)
--    Os 4 INSERTs abaixo já estão embutidos na seção 1 (linhas 65, 72, 73, 78) para quem
--    roda o seed inteiro num banco vazio. Esta seção é para quem já tinha rodado o seed
--    ANTES dessas 4 linhas existirem — insere só o que falta e regrava os ALLOW de
--    role_endpoint_permissions para OWNER/ADMIN/MEMBER (a seção 5 só roda uma vez, então
--    endpoint_permissions criados depois dela não ganham ALLOW sozinhos). Idempotente
--    (seguro rodar mais de uma vez) via WHERE NOT EXISTS.
-- =============================================================================

-- 6.1 — endpoint_permissions (API) que ainda não existirem
-- (colunas da subquery precisam de alias explícito — MySQL rejeita nomes de coluna
-- duplicados em derived table, e NOW() repetido vira "NOW()" duas vezes sem alias)
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bank-accounts/[0-9]+/status' AS endpoint, 'Contas Bancárias' AS name,
           61 AS sequence, 'API' AS type, 'PATCH' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bank-accounts/[0-9]+/status' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/payment-methods/[0-9]+/status' AS endpoint, 'Formas de Pagamento' AS name,
           62 AS sequence, 'API' AS type, 'PATCH' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/payment-methods/[0-9]+/status' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/categories/[0-9]+/status' AS endpoint, 'Categorias' AS name,
           63 AS sequence, 'API' AS type, 'PATCH' AS permitted_methods, 'Configuração' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/categories/[0-9]+/status' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/categories/subcategories/[0-9]+/status' AS endpoint, 'Categorias' AS name,
           64 AS sequence, 'API' AS type, 'PATCH' AS permitted_methods, 'Configuração' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/categories/subcategories/[0-9]+/status' AND type = 'API');

-- 6.2 — role_endpoint_permissions: OWNER ganha ALLOW em tudo que ainda não tem (cobre os 4 novos + qualquer outro endpoint futuro)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 6.3 — role_endpoint_permissions: ADMIN ganha ALLOW nos 4 novos endpoints (mesmo `name` já presente na allow-list original)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep
    ON ep.endpoint IN ('/bank-accounts/[0-9]+/status', '/payment-methods/[0-9]+/status',
                        '/categories/[0-9]+/status', '/categories/subcategories/[0-9]+/status')
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 6.4 — role_endpoint_permissions: MEMBER ganha ALLOW nos 4 novos endpoints (mesmo motivo)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep
    ON ep.endpoint IN ('/bank-accounts/[0-9]+/status', '/payment-methods/[0-9]+/status',
                        '/categories/[0-9]+/status', '/categories/subcategories/[0-9]+/status')
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- =============================================================================
-- 7. INCREMENTAL — CreditCard module (Grupo CC2)
--    Mesmo raciocínio da seção 6: para bancos que já rodaram a seção 5 antes deste
--    módulo existir. 'Cartões de Crédito' é um nome novo (não reaproveita nenhum já
--    presente nas listas ADMIN/MEMBER da seção 5), por isso precisa de INSERT dedicado
--    de role_endpoint_permissions aqui, e não só do catch-all de OWNER da seção 6.2.
--    Idempotente via WHERE NOT EXISTS.
-- =============================================================================

-- 7.1 — endpoint_permissions (API) que ainda não existirem
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-cards' AS endpoint, 'Cartões de Crédito' AS name,
           65 AS sequence, 'API' AS type, 'GET,POST' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-cards' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-cards/[0-9]+' AS endpoint, 'Cartões de Crédito' AS name,
           66 AS sequence, 'API' AS type, 'PUT,DELETE' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-cards/[0-9]+' AND type = 'API');

-- 7.2 — endpoint_permissions (FRONT_PAGE) se ainda não existir
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-cards' AS endpoint, 'Cartões de Crédito' AS name,
           12 AS sequence, 'FRONT_PAGE' AS type, 'GET' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-cards' AND type = 'FRONT_PAGE');

-- 7.3 — group_menu_children: item na sidebar sob 'Contas e Pagamentos', se ainda não existir
INSERT INTO group_menu_children (name, endpoint, icon, group_menu_id, created_at, updated_at)
SELECT 'Cartões de Crédito', '/credit-cards', 'tabler-credit-card', gm.id, NOW(), NOW()
FROM group_menus gm
WHERE gm.name = 'Contas e Pagamentos'
  AND NOT EXISTS (SELECT 1 FROM group_menu_children gmc WHERE gmc.endpoint = '/credit-cards');

-- 7.4 — role_endpoint_permissions: OWNER ganha ALLOW (cobre o catch-all já existente em 6.2,
-- mas mantido explícito aqui para o caso de esta seção rodar antes/independente da 6)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND ep.name = 'Cartões de Crédito'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 7.5 — role_endpoint_permissions: ADMIN ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 7.6 — role_endpoint_permissions: MEMBER ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- =============================================================================
-- 8. INCREMENTAL — CreditCardTransaction (Grupo CC5)
--    Mesmo raciocínio da seção 7. As 3 linhas abaixo reaproveitam o name 'Cartões de
--    Crédito' (mesmo já usado pelo CreditCardController), então em um banco novo elas já
--    herdam ALLOW sozinhas via os joins de ep.name da seção 5 — esta seção só é necessária
--    para quem já rodou a seção 5/7 antes deste módulo existir. Idempotente via WHERE NOT
--    EXISTS; 8.2/8.3/8.4 casam por name, então cobrem as 3 linhas novas de uma vez.
-- =============================================================================

-- 8.1 — endpoint_permissions (API) que ainda não existirem
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-card-transactions' AS endpoint, 'Cartões de Crédito' AS name,
           67 AS sequence, 'API' AS type, 'GET,POST' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-card-transactions' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-card-transactions/[0-9]+' AS endpoint, 'Cartões de Crédito' AS name,
           68 AS sequence, 'API' AS type, 'PUT,DELETE' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-card-transactions/[0-9]+' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-card-transactions/installment-groups/[a-zA-Z0-9-]+' AS endpoint, 'Cartões de Crédito' AS name,
           69 AS sequence, 'API' AS type, 'GET' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-card-transactions/installment-groups/[a-zA-Z0-9-]+' AND type = 'API');

-- 8.2 — role_endpoint_permissions: OWNER ganha ALLOW em 'Cartões de Crédito' (cobre as 3 linhas novas)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND ep.name = 'Cartões de Crédito'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 8.3 — role_endpoint_permissions: ADMIN ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 8.4 — role_endpoint_permissions: MEMBER ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- =============================================================================
-- 9. INCREMENTAL — antecipação de parcelas (Grupo CC5b)
--    Mesmo raciocínio das seções 7/8. Reaproveita o name 'Cartões de Crédito' — em um
--    banco novo já herda ALLOW sozinho via a seção 5; esta seção só é necessária para
--    quem já rodou a seção 5/8 antes deste endpoint existir.
-- =============================================================================

-- 9.1 — endpoint_permissions (API) se ainda não existir
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-card-transactions/installment-groups/[a-zA-Z0-9-]+/anticipate' AS endpoint,
           'Cartões de Crédito' AS name, 70 AS sequence, 'API' AS type, 'POST' AS permitted_methods,
           'Conta' AS ep_group, NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM endpoint_permissions
    WHERE endpoint = '/credit-card-transactions/installment-groups/[a-zA-Z0-9-]+/anticipate' AND type = 'API'
);

-- 9.2 — role_endpoint_permissions: OWNER ganha ALLOW em 'Cartões de Crédito' (cobre a linha nova)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND ep.name = 'Cartões de Crédito'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 9.3 — role_endpoint_permissions: ADMIN ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 9.4 — role_endpoint_permissions: MEMBER ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- =============================================================================
-- 10. INCREMENTAL — fatura de cartão: listar/pagar/desfazer (Grupos CC6/CC7)
--    Mesmo raciocínio das seções 7/8/9. Reaproveita o name 'Cartões de Crédito' — em um
--    banco novo já herda ALLOW sozinho via a seção 5; esta seção só é necessária para
--    quem já rodou a seção 5/9 antes destes 3 endpoints existirem.
-- =============================================================================

-- 10.1 — endpoint_permissions (API) que ainda não existirem
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-cards/invoices' AS endpoint, 'Cartões de Crédito' AS name,
           71 AS sequence, 'API' AS type, 'GET' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-cards/invoices' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-cards/[0-9]+/invoices/[0-9]{4}-[0-9]{2}-[0-9]{2}/pay' AS endpoint,
           'Cartões de Crédito' AS name, 72 AS sequence, 'API' AS type, 'POST' AS permitted_methods,
           'Conta' AS ep_group, NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-cards/[0-9]+/invoices/[0-9]{4}-[0-9]{2}-[0-9]{2}/pay' AND type = 'API'
);

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/credit-cards/[0-9]+/invoices/[0-9]{4}-[0-9]{2}-[0-9]{2}/undo-payment' AS endpoint,
           'Cartões de Crédito' AS name, 73 AS sequence, 'API' AS type, 'POST' AS permitted_methods,
           'Conta' AS ep_group, NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM endpoint_permissions WHERE endpoint = '/credit-cards/[0-9]+/invoices/[0-9]{4}-[0-9]{2}-[0-9]{2}/undo-payment' AND type = 'API'
);

-- 10.2 — role_endpoint_permissions: OWNER ganha ALLOW em 'Cartões de Crédito' (cobre as 3 linhas novas)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND ep.name = 'Cartões de Crédito'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 10.3 — role_endpoint_permissions: ADMIN ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 10.4 — role_endpoint_permissions: MEMBER ganha ALLOW em 'Cartões de Crédito'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Cartões de Crédito'
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- =============================================================================
-- 11. INCREMENTAL — Bill/BillInstance module (Grupos AP1-AP6)
--    Mesmo raciocínio das seções 7/8/9/10. Reaproveita um único name 'Contas a Pagar'
--    em todos os 7 novos endpoints (BillController + BillInstanceController) — em um
--    banco novo já herda ALLOW sozinho via a seção 5 (já inclui 'Contas a Pagar' nas
--    listas ADMIN/MEMBER); esta seção só é necessária para quem já rodou a seção 5
--    antes deste módulo existir. Idempotente via WHERE NOT EXISTS.
-- =============================================================================

-- 11.1 — endpoint_permissions (API) que ainda não existirem
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills' AS endpoint, 'Contas a Pagar' AS name,
           74 AS sequence, 'API' AS type, 'GET,POST' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/[0-9]+' AS endpoint, 'Contas a Pagar' AS name,
           75 AS sequence, 'API' AS type, 'PUT,DELETE' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/[0-9]+' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/[0-9]+/schedule' AS endpoint, 'Contas a Pagar' AS name,
           76 AS sequence, 'API' AS type, 'PUT' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/[0-9]+/schedule' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/instances' AS endpoint, 'Contas a Pagar' AS name,
           77 AS sequence, 'API' AS type, 'GET' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/instances' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/instances/[0-9]+/amount' AS endpoint, 'Contas a Pagar' AS name,
           78 AS sequence, 'API' AS type, 'PUT' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/instances/[0-9]+/amount' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/instances/[0-9]+/pay' AS endpoint, 'Contas a Pagar' AS name,
           79 AS sequence, 'API' AS type, 'POST' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/instances/[0-9]+/pay' AND type = 'API');

INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/instances/[0-9]+/undo-payment' AS endpoint, 'Contas a Pagar' AS name,
           80 AS sequence, 'API' AS type, 'POST' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/instances/[0-9]+/undo-payment' AND type = 'API');

-- 11.2 — endpoint_permissions (FRONT_PAGE) se ainda não existir
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills' AS endpoint, 'Contas a Pagar' AS name,
           13 AS sequence, 'FRONT_PAGE' AS type, 'GET' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills' AND type = 'FRONT_PAGE');

-- 11.3 — group_menu_children: item na sidebar sob 'Contas e Pagamentos', se ainda não existir
INSERT INTO group_menu_children (name, endpoint, icon, group_menu_id, created_at, updated_at)
SELECT 'Contas a Pagar', '/bills', 'tabler-calendar-due', gm.id, NOW(), NOW()
FROM group_menus gm
WHERE gm.name = 'Contas e Pagamentos'
  AND NOT EXISTS (SELECT 1 FROM group_menu_children gmc WHERE gmc.endpoint = '/bills');

-- 11.4 — role_endpoint_permissions: OWNER ganha ALLOW em 'Contas a Pagar' (cobre as 7 linhas novas)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND ep.name = 'Contas a Pagar'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 11.5 — role_endpoint_permissions: ADMIN ganha ALLOW em 'Contas a Pagar'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Contas a Pagar'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 11.6 — role_endpoint_permissions: MEMBER ganha ALLOW em 'Contas a Pagar'
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.name = 'Contas a Pagar'
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- =============================================================================
-- 12. INCREMENTAL — Bill/BillRecurring model split (BillInstance passou a ser a
--    entidade principal, renomeada para Bill; a antiga Bill virou BillRecurring,
--    só a config de recorrência). BillInstanceController ganhou POST/PUT/DELETE
--    em /bills/instances[/{id}] (criar avulsa, editar completa, excluir) e perdeu
--    o antigo PUT /bills/instances/{id}/amount. BillController (/bills) não mudou
--    de path/método. Idempotente via WHERE NOT EXISTS / guarda de UPDATE.
-- =============================================================================

-- 12.1 — remove o endpoint_permission obsoleto de /amount (cascade apaga as
--    role_endpoint_permissions dependentes via ON DELETE CASCADE na FK)
DELETE FROM endpoint_permissions
WHERE endpoint = '/bills/instances/[0-9]+/amount' AND type = 'API';

-- 12.2 — /bills/instances passa a aceitar também POST (criar conta avulsa)
UPDATE endpoint_permissions
SET permitted_methods = 'GET,POST', updated_at = NOW()
WHERE endpoint = '/bills/instances' AND type = 'API' AND permitted_methods <> 'GET,POST';

-- 12.3 — novo endpoint_permission /bills/instances/{id} (editar completo / excluir)
INSERT INTO endpoint_permissions (version, endpoint, name, sequence, type, permitted_methods, ep_group, created_at, updated_at)
SELECT * FROM (
    SELECT 1 AS version, '/bills/instances/[0-9]+' AS endpoint, 'Contas a Pagar' AS name,
           81 AS sequence, 'API' AS type, 'PUT,DELETE' AS permitted_methods, 'Conta' AS ep_group,
           NOW() AS created_at, NOW() AS updated_at
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM endpoint_permissions WHERE endpoint = '/bills/instances/[0-9]+' AND type = 'API');

-- 12.4 — role_endpoint_permissions: OWNER ganha ALLOW em /bills/instances/{id}
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
CROSS JOIN endpoint_permissions ep
WHERE r.name = 'OWNER'
  AND ep.endpoint = '/bills/instances/[0-9]+' AND ep.type = 'API'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 12.5 — role_endpoint_permissions: ADMIN ganha ALLOW em /bills/instances/{id}
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.endpoint = '/bills/instances/[0-9]+' AND ep.type = 'API'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );

-- 12.6 — role_endpoint_permissions: MEMBER ganha ALLOW em /bills/instances/{id}
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r
JOIN endpoint_permissions ep ON ep.endpoint = '/bills/instances/[0-9]+' AND ep.type = 'API'
WHERE r.name = 'MEMBER'
  AND NOT EXISTS (
      SELECT 1 FROM role_endpoint_permissions rep
      WHERE rep.role_id = r.id AND rep.endpoint_permission_id = ep.id
  );
