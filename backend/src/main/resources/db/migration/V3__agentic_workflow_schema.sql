CREATE TABLE generation_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    generation_record_id BIGINT NOT NULL,
    prompt_version INT,
    input_variables TEXT,
    rendered_prompt_summary VARCHAR(1024),
    provider_name VARCHAR(64),
    model_name VARCHAR(128),
    status VARCHAR(64) NOT NULL,
    latency_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trace_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id)
);

CREATE INDEX idx_trace_generation ON generation_trace (generation_record_id);
CREATE INDEX idx_trace_status ON generation_trace (status);

CREATE TABLE agent_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    generation_record_id BIGINT,
    title VARCHAR(256) NOT NULL,
    goal TEXT,
    status VARCHAR(64) NOT NULL,
    provider_name VARCHAR(64),
    model_name VARCHAR(128),
    latency_ms BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_run_project FOREIGN KEY (project_id) REFERENCES project_context (id),
    CONSTRAINT fk_agent_run_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id)
);

CREATE INDEX idx_agent_run_project ON agent_run (project_id);
CREATE INDEX idx_agent_run_generation ON agent_run (generation_record_id);
CREATE INDEX idx_agent_run_status ON agent_run (status);

CREATE TABLE agent_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type VARCHAR(64) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    summary TEXT,
    latency_ms BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES agent_run (id)
);

CREATE INDEX idx_agent_step_run ON agent_step (run_id);
CREATE INDEX idx_agent_step_order ON agent_step (run_id, step_order);

CREATE TABLE tool_call_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    step_id BIGINT,
    tool_name VARCHAR(128) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    status VARCHAR(64) NOT NULL,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tool_call_run FOREIGN KEY (run_id) REFERENCES agent_run (id),
    CONSTRAINT fk_tool_call_step FOREIGN KEY (step_id) REFERENCES agent_step (id)
);

CREATE INDEX idx_tool_call_run ON tool_call_record (run_id);
CREATE INDEX idx_tool_call_step ON tool_call_record (step_id);

CREATE TABLE human_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    generation_record_id BIGINT NOT NULL,
    review_status VARCHAR(64) NOT NULL,
    reviewer VARCHAR(128),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_human_review_run FOREIGN KEY (run_id) REFERENCES agent_run (id),
    CONSTRAINT fk_human_review_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id)
);

CREATE INDEX idx_human_review_run ON human_review (run_id);
CREATE INDEX idx_human_review_generation ON human_review (generation_record_id);

CREATE TABLE knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    source_type VARCHAR(64) NOT NULL DEFAULT 'manual',
    source_uri VARCHAR(512),
    content TEXT NOT NULL,
    chunk_count INT NOT NULL DEFAULT 0,
    embedding_model VARCHAR(128),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_document_title ON knowledge_document (title);

CREATE TABLE knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    content_summary VARCHAR(512),
    keywords VARCHAR(512),
    embedding_model VARCHAR(128),
    embedding_vector TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
);

CREATE INDEX idx_knowledge_chunk_document ON knowledge_chunk (document_id);
CREATE INDEX idx_knowledge_chunk_order ON knowledge_chunk (document_id, chunk_index);

CREATE TABLE generation_knowledge_reference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    generation_record_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    score DOUBLE,
    citation_label VARCHAR(256),
    snippet VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_generation_reference_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id),
    CONSTRAINT fk_generation_reference_chunk FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk (id),
    CONSTRAINT fk_generation_reference_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
);

CREATE INDEX idx_generation_reference_generation ON generation_knowledge_reference (generation_record_id);
CREATE INDEX idx_generation_reference_chunk ON generation_knowledge_reference (chunk_id);
