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
