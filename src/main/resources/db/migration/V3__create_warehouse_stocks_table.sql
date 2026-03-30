CREATE TABLE warehouse_stocks (
    id            VARCHAR(36) PRIMARY KEY,
    product_id    VARCHAR(36) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    warehouse_id  VARCHAR(36) NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    quantity      INTEGER     NOT NULL DEFAULT 0,
    location      VARCHAR(50) NOT NULL,
    updated_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, warehouse_id)
);

CREATE INDEX idx_warehouse_stocks_product_id ON warehouse_stocks(product_id);
CREATE INDEX idx_warehouse_stocks_warehouse_id ON warehouse_stocks(warehouse_id);
