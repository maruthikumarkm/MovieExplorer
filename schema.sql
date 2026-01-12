

-- Create database
CREATE DATABASE IF NOT EXISTS movie_explorer;

-- Use database
USE movie_explorer;

-- Drop table if already exists (safe re-run)
DROP TABLE IF EXISTS users;

-- Users table (matches frontend JS)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


