--liquibase formatted sql

--changeset schedule-cardinality:allow-multiple-schedules-per-tournament splitStatements:false
--comment: Schedule.tournament used to be @OneToOne, which made Hibernate create a unique constraint/index on schedule.tournament_id, so only one schedule item could exist per tournament and a second insert failed with a DataIntegrityViolation. The mapping is now @ManyToOne; drop whatever unique constraint or unique index guards that column (the Hibernate-generated name is a hash, so it is looked up dynamically). Idempotent.
DO $$
DECLARE
    constraint_name text;
    index_name text;
BEGIN
    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'schedule'
          AND c.contype = 'u'
          AND (
              SELECT array_agg(a.attname::text ORDER BY a.attname)
              FROM unnest(c.conkey) AS k(attnum)
              JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
          ) = ARRAY['tournament_id']
    LOOP
        EXECUTE format('ALTER TABLE schedule DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    FOR index_name IN
        SELECT i.relname
        FROM pg_index ix
        JOIN pg_class i ON i.oid = ix.indexrelid
        JOIN pg_class t ON t.oid = ix.indrelid
        WHERE t.relname = 'schedule'
          AND ix.indisunique
          AND ix.indnatts = 1
          AND (
              SELECT a.attname
              FROM pg_attribute a
              WHERE a.attrelid = t.oid AND a.attnum = ix.indkey[0]
          ) = 'tournament_id'
    LOOP
        EXECUTE format('DROP INDEX %I', index_name);
    END LOOP;
END $$;
