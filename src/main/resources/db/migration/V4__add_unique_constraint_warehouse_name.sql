-- Adiciona constraint de unicidade para nome do depósito
ALTER TABLE warehouses ADD CONSTRAINT uk_warehouses_name UNIQUE (name);
