
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    CONSTRAINT roles_name_check CHECK (name IN ('CUSTOMER', 'SUPPORT_AGENT', 'ADMIN'))
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX app_users_username_unique_lower ON app_users (LOWER(username));

CREATE TABLE app_user_roles (
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (id, name) VALUES
    ('10000000-0000-0000-0000-000000000001', 'CUSTOMER'),
    ('10000000-0000-0000-0000-000000000002', 'SUPPORT_AGENT'),
    ('10000000-0000-0000-0000-000000000003', 'ADMIN');
