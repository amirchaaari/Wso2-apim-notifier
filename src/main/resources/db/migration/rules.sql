INSERT INTO notification_rule (
    use_case_type,
    enabled,
    severity,
    threshold_value,
    lookback_seconds,
    description,
    created_at,
    updated_at
) VALUES (
             'BRUTE_FORCE_LOGIN',
             true,
             'HIGH',
             5,
             300,
             'Alert on repeated failed login attempts from the same IP',
             NOW(),
             NOW()
         );
