INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'sendgrid_api_key', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'sendgrid_api_key');

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'twilio_account_sid', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'twilio_account_sid');

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'twilio_auth_token', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'twilio_auth_token');

INSERT INTO admin_settings (setting_key, setting_value)
SELECT 'twilio_phone_number', ''
WHERE NOT EXISTS (SELECT 1 FROM admin_settings WHERE setting_key = 'twilio_phone_number');
