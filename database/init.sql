CREATE TABLE IF NOT EXISTS warehouse (
  id INTEGER PRIMARY KEY,
  name TEXT,
  district TEXT,
  address TEXT,
  manager_id TEXT,
  capacity_level TEXT,
  contact_phone TEXT,
  status TEXT
);

CREATE TABLE IF NOT EXISTS supply_item (
  id INTEGER PRIMARY KEY,
  sku_code TEXT,
  name TEXT,
  category TEXT,
  unit TEXT,
  safety_stock TEXT,
  expire_days TEXT,
  storage_requirement TEXT
);

CREATE TABLE IF NOT EXISTS inventory_batch (
  id INTEGER PRIMARY KEY,
  warehouse_id TEXT,
  supply_item_id TEXT,
  batch_no TEXT,
  quantity TEXT,
  expire_at TEXT,
  inbound_source TEXT,
  quality_status TEXT
);

CREATE TABLE IF NOT EXISTS shelter (
  id INTEGER PRIMARY KEY,
  name TEXT,
  district TEXT,
  capacity TEXT,
  current_population TEXT,
  contact_person TEXT,
  risk_level TEXT,
  open_status TEXT
);

CREATE TABLE IF NOT EXISTS dispatch_order (
  id INTEGER PRIMARY KEY,
  event_id TEXT,
  source_warehouse_id TEXT,
  shelter_id TEXT,
  priority TEXT,
  status TEXT,
  requested_by TEXT,
  approved_by TEXT,
  dispatched_at TEXT
);

CREATE TABLE IF NOT EXISTS audit_log (
  id INTEGER PRIMARY KEY,
  actor TEXT,
  action TEXT,
  target_type TEXT,
  target_id TEXT,
  created_at TEXT
);
