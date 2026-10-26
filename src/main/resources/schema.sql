-- The SQLite Hibernate dialect cannot add foreign keys or composite unique keys.
CREATE TABLE IF NOT EXISTS course (
    id integer,
    code varchar(32),
    term_end date not null,
    term_start date not null,
    title varchar(255) not null,
    primary key (id)
);

CREATE TABLE IF NOT EXISTS enrollment (
    id integer,
    learner_ref varchar(255) not null,
    course_id bigint not null,
    primary key (id),
    constraint enrollment_course_learner_uk unique (course_id, learner_ref),
    constraint fk_enrollment_course foreign key (course_id) references course (id)
);

CREATE TABLE IF NOT EXISTS assignment (
    id integer,
    course_id bigint not null,
    title varchar(255) not null,
    due_date date not null,
    estimated_work_hours numeric not null,
    primary key (id),
    constraint fk_assignment_course foreign key (course_id) references course (id)
);

CREATE TABLE IF NOT EXISTS coursework (
    id integer,
    course_id bigint not null,
    kind varchar(32) not null,
    title varchar(255) not null,
    deadline timestamp not null,
    estimated_effort_hours float,
    primary key (id),
    constraint coursework_kind_ck check (kind in ('exam', 'assignment')),
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
        foreign key (enrollment_id) references enrollment (id)
);

CREATE INDEX IF NOT EXISTS submission_enrollment_idx
    ON submission (enrollment_id);
