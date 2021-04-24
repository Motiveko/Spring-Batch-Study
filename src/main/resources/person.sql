create table person (
    id bigint primary key auto_increment,
    name varchar(255),
    age varchar(255),
    address varchar(255)
);

insert into person
(name, age, address)
values
('고동기','20','서울'),
('김동기','31','부산'),
('최동기','44','인천');