-- TranquilAI PostgreSQL Database Initialization
-- Creates separate databases for each microservice

CREATE DATABASE tranquilai_auth;
CREATE DATABASE tranquilai_users;
CREATE DATABASE tranquilai_content;
CREATE DATABASE tranquilai_activity;
CREATE DATABASE tranquilai_plans;
CREATE DATABASE tranquilai_progress;
CREATE DATABASE tranquilai_notifications;
CREATE DATABASE tranquilai_subscriptions;

-- Grant privileges to the configured POSTGRES_USER
GRANT ALL PRIVILEGES ON DATABASE tranquilai_auth TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_users TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_content TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_activity TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_plans TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_progress TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_notifications TO CURRENT_USER;
GRANT ALL PRIVILEGES ON DATABASE tranquilai_subscriptions TO CURRENT_USER;

-- PostgreSQL 15+ requires explicit schema-level grants for CREATE
-- Without this, Flyway migrations will fail with "permission denied for schema public"
\c tranquilai_auth
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_users
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_content
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_activity
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_plans
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_progress
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_notifications
GRANT ALL ON SCHEMA public TO CURRENT_USER;

\c tranquilai_subscriptions
GRANT ALL ON SCHEMA public TO CURRENT_USER;
