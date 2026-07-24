INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'shiprocket_email', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'shiprocket_email');

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'shiprocket_password', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'shiprocket_password');

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'shiprocket_pickup_location', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'shiprocket_pickup_location');
