INSERT INTO roads (id, road_name, city, current_density, avg_speed, status) VALUES
  (1, 'Sector 18 Main Road', 'Noida', 35, 42.0, 'MODERATE'),
  (2, 'Noida-Greater Noida Expressway', 'Noida', 25, 58.0, 'CLEAR'),
  (3, 'DND Flyway', 'Delhi NCR', 72, 22.0, 'HEAVY'),
  (4, 'Botanical Garden Road', 'Noida', 44, 35.0, 'MODERATE'),
  (5, 'Akshardham Road', 'Delhi', 18, 60.0, 'CLEAR'),
  (6, 'Kalindi Kunj Road', 'Delhi NCR', 65, 24.0, 'HEAVY')
ON CONFLICT (id) DO NOTHING;
