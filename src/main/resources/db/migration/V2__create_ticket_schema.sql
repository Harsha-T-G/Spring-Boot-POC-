
CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    reference_number VARCHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    customer_id UUID NOT NULL REFERENCES app_users(id),
    assigned_agent_id UUID REFERENCES app_users(id),
    resolution_summary VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT tickets_title_length CHECK (CHAR_LENGTH(title) BETWEEN 5 AND 120),
    CONSTRAINT tickets_description_length CHECK (CHAR_LENGTH(description) BETWEEN 20 AND 1000),
    CONSTRAINT tickets_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT tickets_status_check CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    CONSTRAINT tickets_resolution_summary_length CHECK (
        resolution_summary IS NULL OR CHAR_LENGTH(resolution_summary) BETWEEN 20 AND 500
    ),
    CONSTRAINT tickets_state_consistency CHECK (
        (status = 'OPEN' AND assigned_agent_id IS NULL AND resolved_at IS NULL AND resolution_summary IS NULL)
        OR (status = 'IN_PROGRESS' AND assigned_agent_id IS NOT NULL AND resolved_at IS NULL AND resolution_summary IS NULL)
        OR (status = 'RESOLVED' AND assigned_agent_id IS NOT NULL AND resolved_at IS NOT NULL AND resolution_summary IS NOT NULL)
    )
);

CREATE UNIQUE INDEX tickets_reference_number_unique ON tickets (reference_number);
CREATE INDEX tickets_customer_id_idx ON tickets (customer_id);
CREATE INDEX tickets_assigned_agent_id_idx ON tickets (assigned_agent_id);
CREATE INDEX tickets_status_idx ON tickets (status);
CREATE INDEX tickets_priority_idx ON tickets (priority);
CREATE INDEX tickets_created_at_idx ON tickets (created_at DESC, id);

CREATE TABLE ticket_tags (
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (ticket_id, tag),
    CONSTRAINT ticket_tags_non_blank CHECK (CHAR_LENGTH(BTRIM(tag)) > 0)
);
