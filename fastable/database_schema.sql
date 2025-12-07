-- ============================================
-- FASTable Database Schema - OPTIMIZED
-- ============================================
-- Purpose: Store ONLY profile pictures
-- User details stored in: Local DB + Firebase
-- ============================================

-- Instructions:
-- 1. Open phpMyAdmin (http://localhost/phpmyadmin)
-- 2. Select the 'fastable' database (or create if not exists)
-- 3. Click on the 'SQL' tab at the top
-- 4. Copy and paste this entire SQL script
-- 5. Click 'Go' button to execute
-- ============================================

-- Drop and recreate for clean schema
DROP TABLE IF EXISTS user_profile_pictures;

-- Optimized table - ONLY profile pictures
CREATE TABLE user_profile_pictures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,  -- Firebase UID (unique identifier)
    profile_picture_url VARCHAR(500) NOT NULL,  -- File path: uploads/profile_xxx.png
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table Description:
-- ============================================
-- id: Auto-incrementing primary key
-- user_id: Firebase UID (for identifying user)
-- profile_picture_url: Server file path to image
-- created_at: When picture was first uploaded
-- updated_at: When picture was last updated
-- ============================================
-- Total: 4 columns (minimal, optimized)
-- Storage: ~200 bytes per user (excluding image file)
-- ============================================

