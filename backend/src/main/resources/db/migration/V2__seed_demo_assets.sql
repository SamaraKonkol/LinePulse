INSERT INTO plants (id, name, code, active) VALUES
('10000000-0000-0000-0000-000000000001', 'Planta Jaraguá', 'JGS', TRUE);

INSERT INTO sectors (id, plant_id, name, code, active) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Montagem', 'MONT', TRUE);

INSERT INTO production_lines (id, sector_id, name, code, active) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Linha 01', 'L01', TRUE);

INSERT INTO machines (id, production_line_id, name, asset_code, manufacturer, model, serial_number, status, installed_at) VALUES
('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Prensa hidráulica', 'PR-04', 'HidraTech', 'HP-420', 'HT-420-8821', 'RUNNING', '2022-03-18'),
('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'Esteira transportadora', 'ES-12', 'Moveline', 'ML-1200', 'MV-1200-1452', 'MAINTENANCE', '2021-08-05');
