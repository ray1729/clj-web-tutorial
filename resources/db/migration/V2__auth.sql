CREATE TABLE users (
       user_id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
       user_name VARCHAR(128) NOT NULL,
       password VARCHAR(1024) NOT NULL,
       PRIMARY KEY(user_id),
       UNIQUE(user_name)
);

CREATE TABLE roles (
       role_id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
       role_name VARCHAR(128) NOT NULL,
       PRIMARY KEY(role_id),
       UNIQUE(role_name)
);

INSERT INTO roles(role_name) VALUES ('admin');

CREATE TABLE user_roles (
       user_id INTEGER NOT NULL,
       role_id INTEGER NOT NULL,
       PRIMARY KEY(user_id, role_id),
       FOREIGN KEY(user_id) REFERENCES users(user_id),
       FOREIGN KEY(role_id) REFERENCES roles(role_id)
);
