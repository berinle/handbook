-- add index to table
CREATE INDEX IF NOT EXISTS idx_handbook_chunks_embedding ON handbook_chunks USING ivfflat (embedding vector_cosine_ops); 