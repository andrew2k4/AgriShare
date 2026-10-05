CREATE TABLE app_users (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL UNIQUE,
    email        VARCHAR(255),
    display_name VARCHAR(255),
    role         VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL
);
