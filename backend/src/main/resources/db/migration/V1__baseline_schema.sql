-- Baseline schema, generated from the JPA mappings (MySQL 8 dialect) on 2026-10-21.
-- Existing databases created by ddl-auto=update are marked as this version by
-- spring.flyway.baseline-on-migrate and skip it; empty databases run it.

    create table bank_accounts (
        active bit not null,
        balance decimal(38,2),
        version integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        space_id bigint,
        bank_name varchar(255),
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table bill_recurrings (
        active bit not null,
        default_amount decimal(38,2),
        end_date date,
        installments integer,
        start_date date,
        version integer,
        category_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        space_id bigint,
        subcategory_id bigint,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table bills (
        amount decimal(38,2),
        deleted bit not null,
        due_date date,
        paid_date date,
        reference_month date,
        version integer,
        bank_account_id bigint,
        bill_recurring_id bigint,
        category_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        payment_transaction_id bigint,
        space_id bigint,
        subcategory_id bigint,
        name varchar(255),
        status enum ('PAID','PENDING'),
        primary key (id)
    ) engine=InnoDB;

    create table categories (
        active bit not null,
        version integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        space_id bigint,
        updated_at datetime(6),
        name varchar(255),
        kind enum ('EXPENSE','INCOME'),
        primary key (id)
    ) engine=InnoDB;

    create table credit_card_invoice_payments (
        due_date date,
        paid_amount decimal(38,2),
        paid_date date,
        reference_month date,
        version integer,
        bank_account_id bigint,
        created_at datetime(6),
        credit_card_id bigint,
        id bigint not null auto_increment,
        payment_transaction_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table credit_card_transaction_recurrings (
        active bit not null,
        default_amount decimal(38,2),
        start_date date,
        version integer,
        category_id bigint,
        created_at datetime(6),
        credit_card_id bigint,
        id bigint not null auto_increment,
        sub_category_id bigint,
        user_id bigint,
        description varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table credit_card_transactions (
        amount decimal(38,2),
        anticipated bit not null,
        credit bit not null,
        installment_number integer,
        original_reference_month date,
        purchase_date date,
        reference_month date,
        total_installments integer,
        version integer,
        category_id bigint,
        created_at datetime(6),
        credit_card_id bigint,
        credit_card_transaction_recurring_id bigint,
        id bigint not null auto_increment,
        sub_category_id bigint,
        user_id bigint,
        description varchar(255),
        installment_group_id varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table credit_cards (
        active bit not null,
        closing_day integer,
        credit_limit decimal(38,2),
        due_day integer,
        version integer,
        bank_account_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        space_id bigint,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table endpoint_permissions (
        sequence integer,
        version integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        endpoint varchar(255),
        ep_group varchar(255),
        icon varchar(255),
        name varchar(255),
        permitted_methods varchar(255),
        type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table group_menu_children (
        version integer,
        created_at datetime(6),
        group_menu_id bigint,
        id bigint not null auto_increment,
        updated_at datetime(6),
        endpoint varchar(255),
        icon varchar(255),
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table group_menus (
        version integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        icon varchar(255),
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table role_endpoint_permissions (
        version integer,
        created_at datetime(6),
        endpoint_permission_id bigint,
        id bigint not null auto_increment,
        role_id bigint,
        updated_at datetime(6),
        permission varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table roles (
        version integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        space_id bigint,
        updated_at datetime(6),
        description varchar(255),
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table space_invites (
        version integer,
        created_at datetime(6),
        expires_at datetime(6),
        id bigint not null auto_increment,
        role_id bigint,
        space_id bigint,
        email varchar(255) not null,
        token varchar(255) not null,
        status enum ('ACCEPTED','CANCELLED','DECLINED','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table space_members (
        version integer,
        id bigint not null auto_increment,
        joined_at datetime(6),
        role_id bigint,
        space_id bigint,
        user_id bigint,
        primary key (id)
    ) engine=InnoDB;

    create table spaces (
        version integer,
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        description varchar(255),
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table sub_categories (
        active bit not null,
        version integer,
        category_id bigint,
        id bigint not null auto_increment,
        name varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table transactions (
        amount decimal(38,2),
        transaction_date date,
        version integer,
        bank_account_id bigint,
        category_id bigint,
        created_at datetime(6),
        destination_bank_account_id bigint,
        id bigint not null auto_increment,
        source_id bigint,
        sub_category_id bigint,
        user_id bigint,
        description varchar(255),
        source_type enum ('BILL_INSTANCE_PAYMENT','CREDIT_CARD_INVOICE_PAYMENT'),
        type enum ('EXPENSE','INCOME','TRANSFER'),
        primary key (id)
    ) engine=InnoDB;

    create table users (
        active bit not null,
        master_admin bit,
        version integer,
        birthdate datetime(6),
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        auth0_sub varchar(255),
        email varchar(255),
        genre varchar(255),
        marital_status varchar(255),
        name varchar(255),
        nickname varchar(255),
        observation varchar(255),
        phone_number varchar(255),
        profile_photo varchar(255),
        primary key (id)
    ) engine=InnoDB;

    alter table bills 
       add constraint UKno0dgayns3m8p1pse8h2t4n2w unique (bill_recurring_id, reference_month);

    alter table credit_card_invoice_payments 
       add constraint UKs78yw0gityyvcuekkereal0lr unique (credit_card_id, reference_month);

    create index idx_credit_card_transactions_card_reference_month 
       on credit_card_transactions (credit_card_id, reference_month);

    alter table space_invites 
       add constraint UK4tr9blxy7vylt2jq348xprxk3 unique (token);

    create index idx_transactions_bank_account_date 
       on transactions (bank_account_id, transaction_date);

    create index idx_transactions_destination_bank_account_date 
       on transactions (destination_bank_account_id, transaction_date);

    alter table users 
       add constraint UKgfc2lxchf8kon40q0tsem9vku unique (auth0_sub);

    alter table bank_accounts 
       add constraint FK246ckbf8ikk3udskgkgxcwgsm 
       foreign key (space_id) 
       references spaces (id);

    alter table bill_recurrings 
       add constraint FKcfik92njkt84vd7gqr118a4vm 
       foreign key (category_id) 
       references categories (id);

    alter table bill_recurrings 
       add constraint FKnyb149fuu065loq0sw6k2llu2 
       foreign key (space_id) 
       references spaces (id);

    alter table bill_recurrings 
       add constraint FKqglghe02iud6ydpgdbv8vvph9 
       foreign key (subcategory_id) 
       references sub_categories (id);

    alter table bills 
       add constraint FK31h6w8aj9ugj43xjj0n3pxfa3 
       foreign key (bill_recurring_id) 
       references bill_recurrings (id);

    alter table bills 
       add constraint FKn4my8n8xmvieiq3dqtdtggx1e 
       foreign key (category_id) 
       references categories (id);

    alter table bills 
       add constraint FKtdbu0rwgl0hvboyml0w5g2fl7 
       foreign key (space_id) 
       references spaces (id);

    alter table bills 
       add constraint FK1k0pyxq0quo5fg92q422k1mpl 
       foreign key (subcategory_id) 
       references sub_categories (id);

    alter table categories 
       add constraint FKj5t0d1np3ejgledbpb1tbb3c5 
       foreign key (space_id) 
       references spaces (id);

    alter table credit_card_invoice_payments 
       add constraint FKe1ihkahdt84ghc84w0hwwtlk0 
       foreign key (credit_card_id) 
       references credit_cards (id);

    alter table credit_card_transaction_recurrings 
       add constraint FK94pn47r40k94ww0fu7k51ys5j 
       foreign key (category_id) 
       references categories (id);

    alter table credit_card_transaction_recurrings 
       add constraint FKqpyfwgna48band9fkd5gwf075 
       foreign key (credit_card_id) 
       references credit_cards (id);

    alter table credit_card_transaction_recurrings 
       add constraint FKayu30530jf2k90x5j2gu07gf3 
       foreign key (sub_category_id) 
       references sub_categories (id);

    alter table credit_card_transaction_recurrings 
       add constraint FK85hgongy6ybpjjlc30bojgeam 
       foreign key (user_id) 
       references users (id);

    alter table credit_card_transactions 
       add constraint FK4k89twxn6bb6gndelou7eamy9 
       foreign key (category_id) 
       references categories (id);

    alter table credit_card_transactions 
       add constraint FKkddy4atim0vhsbi1f2elp6opv 
       foreign key (credit_card_id) 
       references credit_cards (id);

    alter table credit_card_transactions 
       add constraint FK720qfkqb5u5clbd1cidrirxy 
       foreign key (credit_card_transaction_recurring_id) 
       references credit_card_transaction_recurrings (id);

    alter table credit_card_transactions 
       add constraint FK8vnsc52tndtlnjueq3tsp7g15 
       foreign key (sub_category_id) 
       references sub_categories (id);

    alter table credit_card_transactions 
       add constraint FK6abcfy2urtuov1sghp9h8wc3v 
       foreign key (user_id) 
       references users (id);

    alter table credit_cards 
       add constraint FKpl5m1bvr2a51yjdoyw0vocbtg 
       foreign key (bank_account_id) 
       references bank_accounts (id);

    alter table credit_cards 
       add constraint FKhurcbvy3p7aicvra1fvuyvvtq 
       foreign key (space_id) 
       references spaces (id);

    alter table group_menu_children 
       add constraint FK706torytmeocuv1sdxojf6cgw 
       foreign key (group_menu_id) 
       references group_menus (id);

    alter table role_endpoint_permissions 
       add constraint FKs0dgj0w1ptniaj80knpr5oop2 
       foreign key (endpoint_permission_id) 
       references endpoint_permissions (id) 
       on delete cascade;

    alter table role_endpoint_permissions 
       add constraint FKh5pbpr4djbdcithcwc7g1184c 
       foreign key (role_id) 
       references roles (id) 
       on delete cascade;

    alter table roles 
       add constraint FKrj77bg9xm2wr7hk3hddeu2dmx 
       foreign key (space_id) 
       references spaces (id) 
       on delete cascade;

    alter table space_invites 
       add constraint FKlx5xquxl362gw4vo3x9swtgvj 
       foreign key (role_id) 
       references roles (id) 
       on delete cascade;

    alter table space_invites 
       add constraint FKnv8drdh87pj1b81j6grri8p0m 
       foreign key (space_id) 
       references spaces (id) 
       on delete cascade;

    alter table space_members 
       add constraint FKagl1gfmdtvmlu1wdup63uf4r0 
       foreign key (role_id) 
       references roles (id) 
       on delete cascade;

    alter table space_members 
       add constraint FKlp53a15gkiaxsau8wr82h1gh5 
       foreign key (space_id) 
       references spaces (id) 
       on delete cascade;

    alter table space_members 
       add constraint FK2kry92siw2tc6h1f2ad41ge2l 
       foreign key (user_id) 
       references users (id) 
       on delete cascade;

    alter table sub_categories 
       add constraint FKjwy7imy3rf6r99x48ydq45otw 
       foreign key (category_id) 
       references categories (id);

    alter table transactions 
       add constraint FK59wy892j0r3ye2oxj71rrj02 
       foreign key (bank_account_id) 
       references bank_accounts (id);

    alter table transactions 
       add constraint FKsqqi7sneo04kast0o138h19mv 
       foreign key (category_id) 
       references categories (id);

    alter table transactions 
       add constraint FKl33s06f0yrmtaqhog80j059m6 
       foreign key (destination_bank_account_id) 
       references bank_accounts (id);

    alter table transactions 
       add constraint FKde2cpbto3uor6yhqdb65b2mot 
       foreign key (sub_category_id) 
       references sub_categories (id);

    alter table transactions 
       add constraint FKqwv7rmvc8va8rep7piikrojds 
       foreign key (user_id) 
       references users (id);
