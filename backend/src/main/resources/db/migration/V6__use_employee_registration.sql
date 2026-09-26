ALTER TABLE users RENAME COLUMN email TO registration;

UPDATE users
SET registration = CASE
    WHEN registration = 'admin@linepulse.local' THEN 'ADM001'
    WHEN registration = 'technician@linepulse.local' THEN 'TEC001'
    WHEN registration = 'operator@linepulse.local' THEN 'OPE001'
    ELSE registration
END;

ALTER TABLE audit_events RENAME COLUMN actor_email TO actor_registration;
