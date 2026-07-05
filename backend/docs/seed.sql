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

-- CategoryController — /categories (includes /subcategories sub-resource)
(1, '/categories',                            'Categorias',                 52, 'API', 'GET,POST',   'Configuração', NOW(), NOW()),
(1, '/categories/[0-9]+',                     'Categorias',                 53, 'API', 'PUT,DELETE', 'Configuração', NOW(), NOW()),
(1, '/categories/subcategories',              'Categorias',                 54, 'API', 'POST',       'Configuração', NOW(), NOW()),
(1, '/categories/subcategories/[0-9]+',       'Categorias',                 55, 'API', 'PUT,DELETE', 'Configuração', NOW(), NOW()),

-- PaymentMethodController — /payment-methods
(1, '/payment-methods',                       'Formas de Pagamento',        56, 'API', 'GET,POST',   'Conta',        NOW(), NOW()),
(1, '/payment-methods/[0-9]+',                'Formas de Pagamento',        57, 'API', 'PUT,DELETE', 'Conta',        NOW(), NOW()),

-- TransactionController — /transactions
(1, '/transactions',                          'Transações',                 58, 'API', 'GET,POST',   'Financeiro',   NOW(), NOW()),
(1, '/transactions/[0-9]+',                   'Transações',                 59, 'API', 'PUT,DELETE', 'Financeiro',   NOW(), NOW()),

-- ReportController — /reports
(1, '/reports',                               'Relatórios',                 60, 'API', 'POST',       'Financeiro',   NOW(), NOW());


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
(1, '/group-menus',         'Estrutura de Menu',     11, 'FRONT_PAGE', 'GET', 'internal_management',NOW(), NOW());


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
                   'Contas Bancárias', 'Formas de Pagamento', 'Categorias',
                   'Listar Espaços do Usuário', 'Listar Membros do Espaço', 'Listar Convites do Espaço')
WHERE r.name = 'ADMIN';

-- ADMIN: DENY para o restante
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'DENY', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name NOT IN ('Página Inicial', 'Listar Funções', 'Transações', 'Relatórios',
                       'Contas Bancárias', 'Formas de Pagamento', 'Categorias',
                       'Listar Espaços do Usuário', 'Listar Membros do Espaço', 'Listar Convites do Espaço')
WHERE r.name = 'ADMIN';

-- MEMBER: acesso às permissões básicas (API)
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'ALLOW', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name IN ('Página Inicial', 'Transações', 'Relatórios',
                   'Contas Bancárias', 'Formas de Pagamento', 'Categorias',
                   'Listar Espaços do Usuário', 'Listar Membros do Espaço')
WHERE r.name = 'MEMBER';

-- MEMBER: DENY para o restante
INSERT INTO role_endpoint_permissions (version, role_id, endpoint_permission_id, permission, created_at, updated_at)
SELECT 0, r.id, ep.id, 'DENY', NOW(), NOW()
FROM roles r JOIN endpoint_permissions ep
    ON ep.name NOT IN ('Página Inicial', 'Transações', 'Relatórios',
                       'Contas Bancárias', 'Formas de Pagamento', 'Categorias',
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
