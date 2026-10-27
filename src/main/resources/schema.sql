-- The SQLite Hibernate dialect cannot add foreign keys or composite unique keys.
CREATE TABLE IF NOT EXISTS course (
    id integer,
    code varchar(32),
    term_end date not null,
    term_start date not null,
    title varchar(255) not null,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS course_session (
    id integer,
    course_id bigint not null,
    weekday integer not null check (weekday between 1 and 7),
    start_time time not null,
    end_time time not null,
    primary key (id),
    constraint course_session_time_ck check (end_time > start_time),
    constraint course_session_course_weekday_time_uk
        unique (course_id, weekday, start_time, end_time),
    constraint fk_course_session_course
        foreign key (course_id) references course (id)
);

CREATE TABLE IF NOT EXISTS enrollment (
    id integer,
    learner_ref varchar(255) not null,
    course_id bigint not null,
    primary key (id),
    constraint enrollment_course_learner_uk unique (course_id, learner_ref),
    constraint fk_enrollment_course foreign key (course_id) references course (id)
);

CREATE TABLE IF NOT EXISTS coursework (
    id integer,
    course_id bigint not null,
    kind varchar(32) not null,
    title varchar(255) not null,
    deadline timestamp not null,
    estimated_effort_hours numeric not null,
    primary key (id),
    constraint coursework_kind_ck check (kind in ('EXAM', 'ASSIGNMENT')),
    constraint fk_coursework_course
        foreign key (course_id) references course (id)
);

CREATE INDEX IF NOT EXISTS coursework_course_deadline_idx
    ON coursework (course_id, deadline);

CREATE TABLE IF NOT EXISTS submission (
    id integer,
    enrollment_id bigint not null,
    coursework_id bigint not null,
    submitted_at timestamp not null,
    primary key (id),
    constraint submission_enrollment_coursework_uk
        unique (enrollment_id, coursework_id),
    constraint fk_submission_enrollment
        foreign key (enrollment_id) references enrollment (id),
    constraint fk_submission_coursework
        foreign key (coursework_id) references coursework (id)
);

CREATE INDEX IF NOT EXISTS submission_enrollment_idx
    ON submission (enrollment_id);

CREATE TABLE IF NOT EXISTS work_plan (
    id integer,
    enrollment_id bigint not null,
    range_start date not null,
    range_end date not null,
    available_hours_per_day numeric not null,
    created_at timestamp not null,
    primary key (id),
    constraint work_plan_range_ck check (range_end > range_start),
    constraint fk_work_plan_enrollment
        foreign key (enrollment_id) references enrollment (id)
);

CREATE INDEX IF NOT EXISTS work_plan_enrollment_idx
    ON work_plan (enrollment_id);

CREATE TABLE IF NOT EXISTS task (
    id integer,
    work_plan_id bigint not null,
    coursework_id bigint not null,
    enrollment_id bigint not null,
    title varchar(255) not null,
    target_date date not null,
    estimated_work_hours numeric not null,
    primary key (id),
    constraint fk_task_work_plan
        foreign key (work_plan_id) references work_plan (id),
    constraint fk_task_coursework
        foreign key (coursework_id) references coursework (id),
    constraint fk_task_enrollment
        foreign key (enrollment_id) references enrollment (id)
);

CREATE INDEX IF NOT EXISTS task_work_plan_idx ON task (work_plan_id);
CREATE INDEX IF NOT EXISTS task_enrollment_idx ON task (enrollment_id);
