INSERT INTO users (username, email, password_hash, created_at)
VALUES (
    'demo',
    'demo@threatpulse.app',
    '$argon2id$v=19$m=65536,t=3,p=1$dGhyZWF0cHVsc2VkZW1v$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA',
    NOW()
) ON CONFLICT DO NOTHING;

INSERT INTO user_technologies (user_id, technology_name, created_at)
SELECT id, tech, NOW()
FROM users,
     unnest(ARRAY['spring-boot', 'postgresql', 'vue', 'docker', 'kafka', 'redis']) AS tech
WHERE email = 'demo@threatpulse.app'
ON CONFLICT DO NOTHING;

INSERT INTO user_preferences (user_id, min_severity, email_alerts_enabled, created_at)
SELECT id, 'HIGH'::severity_level_enum, false, NOW()
FROM users WHERE email = 'demo@threatpulse.app'
ON CONFLICT DO NOTHING;

INSERT INTO alert_rules (user_id, name, min_severity, technologies_filter, active, created_at)
SELECT id, 'Critical threats', 'CRITICAL'::severity_level_enum, NULL, true, NOW()
FROM users WHERE email = 'demo@threatpulse.app';

INSERT INTO alert_rules (user_id, name, min_severity, technologies_filter, active, created_at)
SELECT id, 'Spring & database alerts', 'HIGH'::severity_level_enum, ARRAY['spring-boot', 'postgresql', 'redis'], true, NOW()
FROM users WHERE email = 'demo@threatpulse.app';