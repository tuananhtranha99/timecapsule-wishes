-- V1__initial_schema.sql: Initial database schema for Time-Capsule Wishes

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recipients (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    birthday DATE,
    relationship VARCHAR(100),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipients_user_id ON recipients(user_id);
CREATE INDEX idx_recipients_birthday ON recipients(birthday);

CREATE TABLE milestones (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(50) NOT NULL,
    occurred_at DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_milestones_recipient FOREIGN KEY (recipient_id) REFERENCES recipients(id) ON DELETE CASCADE
);

CREATE INDEX idx_milestones_recipient_id ON milestones(recipient_id);
CREATE INDEX idx_milestones_occurred_at ON milestones(occurred_at);

CREATE TABLE generated_wishes (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    occasion_type VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    generated_text TEXT NOT NULL,
    edited_text TEXT,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_generated_wishes_recipient FOREIGN KEY (recipient_id) REFERENCES recipients(id) ON DELETE CASCADE
);

CREATE INDEX idx_generated_wishes_recipient_id ON generated_wishes(recipient_id);

CREATE TABLE generated_wish_milestones (
    wish_id UUID NOT NULL,
    milestone_id UUID NOT NULL,
    PRIMARY KEY (wish_id, milestone_id),
    CONSTRAINT fk_gwm_wish FOREIGN KEY (wish_id) REFERENCES generated_wishes(id) ON DELETE CASCADE,
    CONSTRAINT fk_gwm_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE
);

CREATE INDEX idx_gwm_milestone_id ON generated_wish_milestones(milestone_id);
