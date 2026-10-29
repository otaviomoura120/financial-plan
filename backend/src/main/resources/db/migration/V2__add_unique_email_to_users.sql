-- Invites are matched by e-mail, so two accounts must never share one.
-- Fails if the table already has duplicated e-mails: resolve them by hand before running.
alter table users add constraint uk_users_email unique (email);
