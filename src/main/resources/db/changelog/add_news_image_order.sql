--liquibase formatted sql

--changeset news-gallery:add-image-order
--comment: Persist the organizer-selected display order for each News gallery while leaving non-News URL rows unordered.
ALTER TABLE url ADD COLUMN IF NOT EXISTS news_image_order INTEGER;

--changeset news-gallery:backfill-image-order
--comment: Give existing News images a stable initial order based on their URL row IDs.
WITH ranked_news_images AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY news_id ORDER BY id) - 1 AS image_order
    FROM url
    WHERE news_id IS NOT NULL
)
UPDATE url AS image_url
SET news_image_order = ranked_news_images.image_order
FROM ranked_news_images
WHERE image_url.id = ranked_news_images.id
  AND image_url.news_image_order IS NULL;
