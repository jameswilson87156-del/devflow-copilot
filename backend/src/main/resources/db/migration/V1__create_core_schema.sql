CREATE TABLE project_context (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(128) NOT NULL,
    tech_stack VARCHAR(512),
    readme_content TEXT,
    directory_structure TEXT,
    current_requirement TEXT,
    coding_rules TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key VARCHAR(128) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_type VARCHAR(64) NOT NULL,
    template_content TEXT NOT NULL,
    variables VARCHAR(1024),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prompt_template_key UNIQUE (template_key)
);

CREATE TABLE generation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    generation_type VARCHAR(64) NOT NULL,
    input_summary VARCHAR(512),
    input_content TEXT,
    output_content TEXT,
    status VARCHAR(64) NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    provider_name VARCHAR(64),
    model_name VARCHAR(128),
    prompt_template_id BIGINT,
    prompt_template_name VARCHAR(128),
    prompt_template_version INT,
    rendered_prompt TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    cost_time_ms BIGINT,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_generation_project FOREIGN KEY (project_id) REFERENCES project_context (id),
    CONSTRAINT fk_generation_template FOREIGN KEY (prompt_template_id) REFERENCES prompt_template (id)
);

CREATE INDEX idx_generation_project ON generation_record (project_id);
CREATE INDEX idx_generation_type ON generation_record (generation_type);
CREATE INDEX idx_generation_status ON generation_record (status);
CREATE INDEX idx_generation_created_at ON generation_record (created_at);

CREATE TABLE log_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    raw_log TEXT NOT NULL,
    exception_type VARCHAR(128),
    possible_reason TEXT,
    diagnose_steps TEXT,
    fix_prompt TEXT,
    risk_tips TEXT,
    risk_level VARCHAR(32),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_project FOREIGN KEY (project_id) REFERENCES project_context (id)
);

CREATE INDEX idx_log_project ON log_analysis (project_id);
CREATE INDEX idx_log_created_at ON log_analysis (created_at);

CREATE TABLE ai_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    task_title VARCHAR(256) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'Medium',
    status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'Medium',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES project_context (id)
);

CREATE INDEX idx_task_project ON ai_task (project_id);
CREATE INDEX idx_task_status ON ai_task (status);
