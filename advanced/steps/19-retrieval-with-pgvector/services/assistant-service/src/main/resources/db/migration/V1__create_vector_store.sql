-- pgvector adds the "vector" column type and similarity operators to PostgreSQL.
create extension if not exists vector;

-- The table Spring AI's PgVectorStore reads and writes. 768 = the size of a nomic-embed-text embedding.
create table vector_store (
    id        uuid primary key default gen_random_uuid(),
    content   text,
    metadata  json,
    embedding vector(768)
);

-- HNSW: an approximate nearest-neighbour index, for the cosine distance the store uses.
create index vector_store_embedding_idx on vector_store using hnsw (embedding vector_cosine_ops);

-- One row per policy file: lets startup skip files that did not change since they were embedded.
create table policy_sources (
    source       text primary key,
    content_hash text not null,
    chunks       int  not null,
    embedded_at  timestamptz not null default now()
);
