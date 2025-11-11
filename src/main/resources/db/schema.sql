-- PostgreSQL Schema for Heimdall

-- Log Entries Table
CREATE TABLE IF NOT EXISTS log_entries (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) UNIQUE NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    source VARCHAR(100) NOT NULL,
    service_name VARCHAR(100),
    environment VARCHAR(50),
    severity VARCHAR(20) NOT NULL,
    log_content TEXT NOT NULL,
    log_hash VARCHAR(64) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for log_entries
CREATE INDEX IF NOT EXISTS idx_log_entries_timestamp ON log_entries(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_log_entries_service_env ON log_entries(service_name, environment);
CREATE INDEX IF NOT EXISTS idx_log_entries_severity ON log_entries(severity);
CREATE INDEX IF NOT EXISTS idx_log_entries_log_hash ON log_entries(log_hash);

-- Analysis Results Table
CREATE TABLE IF NOT EXISTS analysis_results (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT NOT NULL REFERENCES log_entries(id) ON DELETE CASCADE,
    bifrost_analysis_id BIGINT,
    request_id VARCHAR(36) UNIQUE NOT NULL,
    correlation_id VARCHAR(36),
    summary TEXT,
    root_cause TEXT,
    recommendation TEXT,
    severity VARCHAR(20),
    confidence DECIMAL(3,2),
    model VARCHAR(100),
    duration_seconds DECIMAL(10,2),
    analyzed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for analysis_results
CREATE INDEX IF NOT EXISTS idx_analysis_results_log_id ON analysis_results(log_id);
CREATE INDEX IF NOT EXISTS idx_analysis_results_analyzed_at ON analysis_results(analyzed_at DESC);
CREATE INDEX IF NOT EXISTS idx_analysis_results_severity ON analysis_results(severity);

-- Log Statistics Table
CREATE TABLE IF NOT EXISTS log_statistics (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    hour SMALLINT NOT NULL,
    service_name VARCHAR(100),
    environment VARCHAR(50),
    severity VARCHAR(20),
    count INTEGER NOT NULL DEFAULT 0,
    avg_size_bytes INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, hour, service_name, environment, severity)
);

-- Indexes for log_statistics
CREATE INDEX IF NOT EXISTS idx_log_statistics_date_hour ON log_statistics(date, hour);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT REFERENCES log_entries(id) ON DELETE SET NULL,
    analysis_id BIGINT REFERENCES analysis_results(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for notifications
CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON notifications(sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
