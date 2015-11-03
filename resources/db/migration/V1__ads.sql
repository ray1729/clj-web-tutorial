CREATE TABLE ads (
       ad_id        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
       title        VARCHAR(128) NOT NULL,
       content      VARCHAR(1024) NOT NULL DEFAULT '',
       width        INTEGER NOT NULL,
       height       INTEGER NOT NULL,
       url          VARCHAR(1024) NOT NULL,
       is_active    BOOLEAN NOT NULL DEFAULT TRUE,
       created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY(ad_id)
);

CREATE TABLE images (
       ad_id         INTEGER NOT NULL,
       size          INTEGER NOT NULL,
       content_type  VARCHAR(128) NOT NULL,
       content_bytes BLOB NOT NULL,
       created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY(ad_id),
       FOREIGN KEY(ad_id) REFERENCES ads(ad_id)
);

CREATE TABLE clicks (
       click_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY,
       ad_id          INTEGER NOT NULL,
       client_address VARCHAR(16) NOT NULL,
       created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY(click_id),
       FOREIGN KEY(ad_id) REFERENCES ads(ad_id)
);
