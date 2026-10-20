-- Development-only fictional seed. Copy this into db/migration only for a local development database.
INSERT INTO departments(name,location,phone) VALUES ('General Medicine','Building A','010000000') ON CONFLICT (name) DO NOTHING;
INSERT INTO medicines(name,unit,unit_price,stock_quantity) VALUES ('Paracetamol 500mg','tablet',0.10,500),('Amoxicillin 500mg','capsule',0.35,250) ON CONFLICT (name) DO NOTHING;
