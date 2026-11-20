-- Runs once, when the PostgreSQL container starts with an empty data volume.
-- Database per service: each service owns its tables, and no service reads another's.
-- One server keeps the laptop light; in production these could be separate servers.
create database catalog owner shoplite;
create database orders owner shoplite;
create database reviews owner shoplite;
create database assistant owner shoplite;
