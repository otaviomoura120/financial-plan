-- =============================================================================
-- SEED DATA — financial_plan
-- =============================================================================
-- Execution order matters due to FK constraints:
--   1. endpoint_permissions (no FK dependencies)
--   2. group_menus          (no FK dependencies)
--   3. group_menu_children  (FK → group_menus.id)
--
-- group_menu_children uses explicit IDs via subquery to avoid
-- depending on auto-increment state of the environment.
-- Run against an empty database or adjust WHERE clause ids accordingly.
-- =============================================================================

-- =============================================================================
-- 1. ENDPOINT PERMISSIONS — API
--    Controls access to backend REST endpoints protected by @PreAuthorize.
--    The `endpoint` field is a Java regex matched against the full request URI.
--    The SecurityService finds the first match (by sequence asc) where BOTH
--    the HTTP method and the path regex match, then checks permittedRoles.
-- =============================================================================

INSERT INTO endpoint_permissions
    (version, endpoint, name, sequence, type, permitted_methods, permitted_roles, created_at, updated_at)
VALUES

-- IndexController — GET /
(1, '/',                                      'Página Inicial',             1,  'API', 'GET',        'OWNER,ADMIN,MEMBER', NOW(), NOW()),

-- RoleController — /roles
(1, '/roles',                                 'Listar Funções',             10, 'API', 'GET',        'OWNER,ADMIN',        NOW(), NOW()),
(1, '/roles',                                 'Criar Função',               11, 'API', 'POST',       'OWNER',              NOW(), NOW()),
(1, '/roles/[0-9]+',                          'Gerenciar Função',           12, 'API', 'PUT,DELETE', 'OWNER',              NOW(), NOW()),
(1, '/roles/[0-9]+/assign-user/[0-9]+',       'Atribuir Função a Usuário',  13, 'API', 'PUT',        'OWNER',              NOW(), NOW()),

-- EndpointPermissionController — /endpoint-permissions
(1, '/endpoint-permissions',                  'Listar Permissões',          20, 'API', 'GET',        'OWNER',              NOW(), NOW()),
(1, '/endpoint-permissions',                  'Criar Permissão',            21, 'API', 'POST',       'OWNER',              NOW(), NOW()),
(1, '/endpoint-permissions/[0-9]+',           'Gerenciar Permissão',        22, 'API', 'PUT,DELETE', 'OWNER',              NOW(), NOW()),

-- GroupMenuController — /group-menus
(1, '/group-menus',                           'Listar Grupos de Menu',      30, 'API', 'GET',        'OWNER',              NOW(), NOW()),
(1, '/group-menus',                           'Criar Grupo de Menu',        31, 'API', 'POST',       'OWNER',              NOW(), NOW()),
(1, '/group-menus/[0-9]+',                    'Gerenciar Grupo de Menu',    32, 'API', 'PUT,DELETE', 'OWNER',              NOW(), NOW()),
(1, '/group-menus/[0-9]+/children',           'Adicionar Item ao Menu',     33, 'API', 'POST',       'OWNER',              NOW(), NOW()),
(1, '/group-menus/children/[0-9]+',           'Gerenciar Item do Menu',     34, 'API', 'PUT,DELETE', 'OWNER',              NOW(), NOW());


-- =============================================================================
-- 2. ENDPOINT PERMISSIONS — FRONT_PAGE
--    Controls which pages appear in the sidebar for each role.
--    The `endpoint` field is a Java regex matched against GroupMenuChildren.endpoint
--    via String.matches() (full-string match). Use exact paths unless wildcards
--    are needed for nested pages (e.g. '/transactions.*' covers /transactions/new).
--
--    permitted_methods is not evaluated for FRONT_PAGE rules (menu filtering
--    only checks permittedRoles), but the column is required — use 'GET'.
-- =============================================================================

INSERT INTO endpoint_permissions
    (version, endpoint, name, sequence, type, permitted_methods, permitted_roles, created_at, updated_at)
VALUES

-- Visible to all roles
(1, '/',                    'Início',                1,  'FRONT_PAGE', 'GET', 'OWNER,ADMIN,MEMBER', NOW(), NOW()),
(1, '/transactions',        'Transações',            2,  'FRONT_PAGE', 'GET', 'OWNER,ADMIN,MEMBER', NOW(), NOW()),
(1, '/reports',             'Relatórios',            3,  'FRONT_PAGE', 'GET', 'OWNER,ADMIN,MEMBER', NOW(), NOW()),
(1, '/bank-accounts',       'Contas Bancárias',      4,  'FRONT_PAGE', 'GET', 'OWNER,ADMIN,MEMBER', NOW(), NOW()),
(1, '/payment-methods',     'Formas de Pagamento',   5,  'FRONT_PAGE', 'GET', 'OWNER,ADMIN,MEMBER', NOW(), NOW()),
(1, '/categories',          'Categorias',            6,  'FRONT_PAGE', 'GET', 'OWNER,ADMIN,MEMBER', NOW(), NOW()),

-- Visible to OWNER only (administration area)
(1, '/users',               'Usuários',              7,  'FRONT_PAGE', 'GET', 'OWNER',              NOW(), NOW()),
(1, '/spaces',              'Espaços',               8,  'FRONT_PAGE', 'GET', 'OWNER',              NOW(), NOW()),
(1, '/roles',               'Funções',               9,  'FRONT_PAGE', 'GET', 'OWNER',              NOW(), NOW()),
(1, '/endpoint-permissions','Permissões de Acesso',  10, 'FRONT_PAGE', 'GET', 'OWNER',              NOW(), NOW()),
(1, '/group-menus',         'Estrutura de Menu',     11, 'FRONT_PAGE', 'GET', 'OWNER',              NOW(), NOW());


-- =============================================================================
-- 3. GROUP MENUS — sidebar navigation groups
-- =============================================================================

INSERT INTO group_menus (name, icon, created_at, updated_at)
VALUES
('Dashboard',           'tabler-home',          NOW(), NOW()),
('Financeiro',          'tabler-cash',          NOW(), NOW()),
('Contas e Pagamentos', 'tabler-building-bank', NOW(), NOW()),
('Configurações',       'tabler-settings',      NOW(), NOW()),
('Administração',       'tabler-shield-lock',   NOW(), NOW());


-- =============================================================================
-- 4. GROUP MENU CHILDREN — individual sidebar items
--    Each `endpoint` value must fully match (String.matches) at least one
--    FRONT_PAGE EndpointPermission `endpoint` regex so the menu filtering works.
--
--    group_menu_id is resolved via subquery to avoid hard-coded ID assumptions.
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
