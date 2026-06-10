CREATE TABLE user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('player', 'admin') NOT NULL DEFAULT 'player',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE player (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_user_id BIGINT NULL,
    is_guest BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE TABLE user_player_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_user_id BIGINT NOT NULL,
    target_player_id BIGINT NOT NULL,
    permission_level ENUM('none', 'propose', 'auto_approve') NOT NULL DEFAULT 'none',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_upp_source_user
        FOREIGN KEY (source_user_id) REFERENCES user_account(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_upp_target_player
        FOREIGN KEY (target_player_id) REFERENCES player(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_user_player_permission UNIQUE (source_user_id, target_player_id)
);

CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT NULL,
    updated_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_course_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE TABLE hole (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    hole_number INT NOT NULL,
    name VARCHAR(200) NULL,
    length_meters INT NOT NULL,
    par_value INT NOT NULL,
    notes TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT NULL,
    updated_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_hole_course
        FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_hole_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_hole_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT uq_hole_course_number UNIQUE (course_id, hole_number)
);

CREATE TABLE hole_tee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hole_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_hole_tee_hole
        FOREIGN KEY (hole_id) REFERENCES hole(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_hole_tee_name UNIQUE (hole_id, name)
);

CREATE TABLE hole_basket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hole_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_hole_basket_hole
        FOREIGN KEY (hole_id) REFERENCES hole(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT uq_hole_basket_name UNIQUE (hole_id, name)
);

CREATE TABLE hole_variant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hole_id BIGINT NOT NULL,
    tee_id BIGINT NOT NULL,
    basket_id BIGINT NOT NULL,
    length_meters INT NOT NULL,
    par_value INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_hole_variant_hole
        FOREIGN KEY (hole_id) REFERENCES hole(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_hole_variant_tee
        FOREIGN KEY (tee_id) REFERENCES hole_tee(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_hole_variant_basket
        FOREIGN KEY (basket_id) REFERENCES hole_basket(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT uq_hole_variant UNIQUE (hole_id, tee_id, basket_id)
);

CREATE TABLE layout (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT NULL,
    updated_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_layout_course
        FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_layout_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_layout_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT uq_layout_course_name UNIQUE (course_id, name)
);

CREATE TABLE layout_hole (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    layout_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    hole_id BIGINT NOT NULL,
    hole_variant_id BIGINT NULL,
    CONSTRAINT fk_layout_hole_layout
        FOREIGN KEY (layout_id) REFERENCES layout(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_layout_hole_hole
        FOREIGN KEY (hole_id) REFERENCES hole(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_layout_hole_variant
        FOREIGN KEY (hole_variant_id) REFERENCES hole_variant(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT uq_layout_sequence UNIQUE (layout_id, sequence_number),
    CONSTRAINT uq_layout_variant UNIQUE (layout_id, hole_variant_id)
);

CREATE TABLE play_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    status ENUM('in_progress', 'completed', 'abandoned') NOT NULL DEFAULT 'in_progress',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_play_session_course
        FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_play_session_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES user_account(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE session_player (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    play_session_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    layout_id BIGINT NULL,
    display_name_snapshot VARCHAR(100) NOT NULL,
    start_order INT NOT NULL,
    added_by_user_id BIGINT NOT NULL,
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    approval_state ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'approved',
    approved_by_user_id BIGINT NULL,
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_player_session
        FOREIGN KEY (play_session_id) REFERENCES play_session(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_session_player_player
        FOREIGN KEY (player_id) REFERENCES player(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_session_player_layout
        FOREIGN KEY (layout_id) REFERENCES layout(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_session_player_added_by
        FOREIGN KEY (added_by_user_id) REFERENCES user_account(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_session_player_approved_by
        FOREIGN KEY (approved_by_user_id) REFERENCES user_account(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT uq_session_player UNIQUE (play_session_id, player_id),
    CONSTRAINT uq_session_start_order UNIQUE (play_session_id, start_order)
);

CREATE TABLE session_player_hole (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_player_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    course_id BIGINT NOT NULL,
    hole_id BIGINT NOT NULL,
    hole_variant_id BIGINT NULL,
    hole_number_snapshot INT NOT NULL,
    hole_name_snapshot VARCHAR(200) NULL,
    tee_name_snapshot VARCHAR(100) NULL,
    basket_name_snapshot VARCHAR(100) NULL,
    length_snapshot_meters INT NOT NULL,
    par_snapshot INT NOT NULL,
    throws_count INT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sph_session_player
        FOREIGN KEY (session_player_id) REFERENCES session_player(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_sph_course
        FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_sph_hole
        FOREIGN KEY (hole_id) REFERENCES hole(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_sph_hole_variant
        FOREIGN KEY (hole_variant_id) REFERENCES hole_variant(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT uq_sph_sequence UNIQUE (session_player_id, sequence_number)
);

ALTER TABLE player
ADD CONSTRAINT uq_player_owner_user UNIQUE (owner_user_id);

CREATE INDEX ix_player_name ON player(name);

CREATE INDEX ix_user_player_permission_target_player
ON user_player_permission(target_player_id);

CREATE INDEX ix_hole_course_active
ON hole(course_id, is_active, hole_number);

CREATE INDEX ix_hole_tee_hole_active_sort
ON hole_tee(hole_id, is_active, sort_order);

CREATE INDEX ix_hole_basket_hole_active_sort
ON hole_basket(hole_id, is_active, sort_order);

CREATE INDEX ix_hole_variant_hole_active
ON hole_variant(hole_id, is_active);

CREATE INDEX ix_layout_course_active
ON layout(course_id, is_active, name);

CREATE INDEX ix_layout_hole_hole
ON layout_hole(hole_id);

CREATE INDEX ix_play_session_status_started
ON play_session(status, started_at);

CREATE INDEX ix_play_session_created_by_started
ON play_session(created_by_user_id, started_at);

CREATE INDEX ix_play_session_course_started
ON play_session(course_id, started_at);

CREATE INDEX ix_session_player_player
ON session_player(player_id);

CREATE INDEX ix_session_player_player_session
ON session_player(player_id, play_session_id);

CREATE INDEX ix_session_player_approval
ON session_player(player_id, approval_state, approval_required);

CREATE INDEX ix_session_player_session
ON session_player(play_session_id);

CREATE INDEX ix_sph_hole_variant
ON session_player_hole(hole_variant_id);

CREATE INDEX ix_sph_hole
ON session_player_hole(hole_id);

CREATE INDEX ix_sph_session_player_completed
ON session_player_hole(session_player_id, is_completed, sequence_number);

CREATE INDEX ix_sph_session_player_throws
ON session_player_hole(session_player_id, throws_count);

CREATE INDEX ix_sph_variant_throws
ON session_player_hole(hole_variant_id, throws_count);

-- Add creator tracking for guest players
ALTER TABLE player
ADD COLUMN created_by_user_id BIGINT NULL AFTER owner_user_id,
ADD CONSTRAINT fk_player_created_by_user
    FOREIGN KEY (created_by_user_id) REFERENCES user_account(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

CREATE INDEX ix_player_created_by_user ON player(created_by_user_id);

ALTER TABLE user_account
ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_play_session_created_by_status_started
ON play_session(created_by_user_id, status, started_at);

CREATE INDEX ix_play_session_course_status_started
ON play_session(course_id, status, started_at);

CREATE INDEX ix_session_player_session_approval
ON session_player(play_session_id, approval_state);

CREATE INDEX ix_session_player_layout
ON session_player(layout_id);

CREATE INDEX ix_sph_course_variant
ON session_player_hole(course_id, hole_variant_id);

CREATE INDEX ix_sph_throws
ON session_player_hole(throws_count);
