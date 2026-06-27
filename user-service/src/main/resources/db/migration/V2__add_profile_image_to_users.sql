-- Align users table with User.profileImage (JPA @Column profile_image)
ALTER TABLE users
ADD COLUMN profile_image VARCHAR(1024) NULL;
