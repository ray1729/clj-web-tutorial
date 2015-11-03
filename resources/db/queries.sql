-- :name q-create-ad :query :one
-- :doc Insert an ad into the ads table
INSERT INTO ads(title, content, width, height, url, size, content_type, image)
VALUES(:title, :content, :width, :height, :url, :size, :content-type, :image)
