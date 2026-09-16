-- =====================================================================
-- rescue-stock 城市防灾应急物资调度系统
-- 调拨闭环：申请占用 -> 审批 -> 出库扣减 -> 签收/拒签/撤销 -> 原批次回补
-- 并发正确性依赖：行锁(FOR UPDATE) + 条件 UPDATE + 幂等唯一键
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 账号（RBAC：STREET_ADMIN 街道管理员 / WAREHOUSE_KEEPER 仓库员
--        / APPROVER 审批员 / OBSERVER 只读观察员）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(64)  NOT NULL UNIQUE,
  display_name  VARCHAR(64)  NOT NULL,
  role          VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 应急仓库
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS warehouse;
CREATE TABLE warehouse (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  name           VARCHAR(128) NOT NULL,
  district       VARCHAR(64)  NOT NULL,
  address        VARCHAR(255) NOT NULL,
  manager_id     BIGINT NULL,
  capacity_level VARCHAR(16)  NOT NULL DEFAULT 'L2',
  contact_phone  VARCHAR(32)  NOT NULL,
  status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE / DISABLED
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 应急物资档案
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS supply_item;
CREATE TABLE supply_item (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_code            VARCHAR(64) NOT NULL UNIQUE,
  name                VARCHAR(128) NOT NULL,
  category            VARCHAR(32) NOT NULL,              -- SupplyCategory
  unit                VARCHAR(16) NOT NULL,
  safety_stock        INT NOT NULL DEFAULT 0,
  expire_days         INT NOT NULL DEFAULT 0,
  storage_requirement VARCHAR(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 库存批次
--   quantity          物理入库总量（不变，盘点除外）
--   held_quantity     被“已申请未出库”调拨单占用的数量
--   可 用 available = quantity - held_quantity
--   出库扣减直接改 quantity 与 held_quantity；回补同理，二者始终同事务推进
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS inventory_batch;
CREATE TABLE inventory_batch (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_id    BIGINT NOT NULL,
  supply_item_id  BIGINT NOT NULL,
  batch_no        VARCHAR(64) NOT NULL,
  quantity        INT NOT NULL,
  held_quantity   INT NOT NULL DEFAULT 0,
  expire_at       DATETIME NULL,
  inbound_source  VARCHAR(128) NOT NULL DEFAULT '',
  quality_status  VARCHAR(16) NOT NULL DEFAULT 'OK',     -- OK / NEAR_EXPIRE / DAMAGED
  KEY idx_batch_lookup (warehouse_id, supply_item_id),
  KEY idx_batch_expire (expire_at),
  CONSTRAINT chk_batch_qty   CHECK (quantity >= 0),
  CONSTRAINT chk_batch_held  CHECK (held_quantity >= 0 AND held_quantity <= quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 避难安置点
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS shelter;
CREATE TABLE shelter (
  id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
  name               VARCHAR(128) NOT NULL,
  district           VARCHAR(64)  NOT NULL,
  capacity           INT NOT NULL DEFAULT 0,
  current_population INT NOT NULL DEFAULT 0,
  contact_person     VARCHAR(64)  NOT NULL DEFAULT '',
  contact_phone      VARCHAR(32)  NOT NULL DEFAULT '',
  risk_level         VARCHAR(16)  NOT NULL DEFAULT 'LOW', -- LOW/MEDIUM/HIGH/CRITICAL
  open_status        VARCHAR(16)  NOT NULL DEFAULT 'STANDBY', -- ShelterStatus
  CONSTRAINT chk_shelter_pop CHECK (current_population >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 灾害事件
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS disaster_event;
CREATE TABLE disaster_event (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  title       VARCHAR(128) NOT NULL,
  district    VARCHAR(64)  NOT NULL,
  level       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
  status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',      -- ACTIVE/CLOSED
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 调拨单
--   幂等：request_id 由申请端生成，重复提交直接返回原单，绝不二次占库
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dispatch_order;
CREATE TABLE dispatch_order (
  id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id          VARCHAR(64) NOT NULL UNIQUE,
  event_id            BIGINT NULL,
  source_warehouse_id BIGINT NOT NULL,
  shelter_id          BIGINT NOT NULL,
  priority            VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  status              VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED', -- DispatchStatus
  remark              VARCHAR(255) NOT NULL DEFAULT '',
  requested_by        BIGINT NULL,
  approved_by         BIGINT NULL,
  refused_by          BIGINT NULL,
  canceled_by         BIGINT NULL,
  reject_reason       VARCHAR(255) NOT NULL DEFAULT '',
  refuse_reason       VARCHAR(255) NOT NULL DEFAULT '',
  cancel_reason       VARCHAR(255) NOT NULL DEFAULT '',
  submitted_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approved_at         DATETIME NULL,
  dispatched_at       DATETIME NULL,
  received_at         DATETIME NULL,
  refused_at          DATETIME NULL,
  canceled_at         DATETIME NULL,
  KEY idx_order_status (status),
  KEY idx_order_shelter (shelter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 调拨行（申请口径：要哪种物资、要多少）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dispatch_line;
CREATE TABLE dispatch_line (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  dispatch_order_id BIGINT NOT NULL,
  supply_item_id   BIGINT NOT NULL,
  requested_qty    INT NOT NULL,
  outbound_qty     INT NOT NULL DEFAULT 0,
  KEY idx_line_order (dispatch_order_id),
  CONSTRAINT chk_line_req CHECK (requested_qty > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 批次去向（一张单调拨单对每个批次的占用/出库/回补明细）
--   stage: HELD（申请占用）/ OUT（已出库）/ RETURNED（拒签或撤销已回补）
--   回补严格按这里记录的原批次、原数量逐条归还，杜绝凭空回补
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dispatch_batch_allocation;
CREATE TABLE dispatch_batch_allocation (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  dispatch_order_id BIGINT NOT NULL,
  dispatch_line_id BIGINT NOT NULL,
  inventory_batch_id BIGINT NOT NULL,
  allocated_qty    INT NOT NULL,
  returned_qty     INT NOT NULL DEFAULT 0,
  stage            VARCHAR(16) NOT NULL DEFAULT 'HELD',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_alloc_order (dispatch_order_id),
  KEY idx_alloc_batch (inventory_batch_id),
  CONSTRAINT chk_alloc_pos      CHECK (allocated_qty > 0),
  CONSTRAINT chk_alloc_returned CHECK (returned_qty >= 0 AND returned_qty <= allocated_qty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 库存流水（与状态变更高度一致的双写账本）
-- direction: HOLD 占用 / RELEASE 释放 / OUTBOUND 出库 / RETURN 回补
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS stock_ledger;
CREATE TABLE stock_ledger (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  inventory_batch_id BIGINT NOT NULL,
  supply_item_id   BIGINT NOT NULL,
  warehouse_id     BIGINT NOT NULL,
  dispatch_order_id BIGINT NULL,
  direction        VARCHAR(16) NOT NULL,
  change_qty       INT NOT NULL,          -- 恒为正数，方向看 direction
  quantity_after   INT NOT NULL,          -- 写后 quantity
  held_after       INT NOT NULL,          -- 写后 held_quantity
  actor            VARCHAR(64) NOT NULL DEFAULT '',
  remark           VARCHAR(255) NOT NULL DEFAULT '',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ledger_batch (inventory_batch_id),
  KEY idx_ledger_order (dispatch_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 调拨状态流转时间线（审批详情 / ApprovalTimeline 数据源）
-- action: SUBMIT/APPROVE/REJECT/OUTBOUND/RECEIVE/REFUSE/CANCEL
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS dispatch_status_log;
CREATE TABLE dispatch_status_log (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  dispatch_order_id BIGINT NOT NULL,
  action           VARCHAR(16) NOT NULL,
  from_status      VARCHAR(16) NOT NULL DEFAULT '',
  to_status        VARCHAR(16) NOT NULL,
  actor            VARCHAR(64) NOT NULL DEFAULT '',
  note             VARCHAR(255) NOT NULL DEFAULT '',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_statuslog_order (dispatch_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 操作审计日志（横切：所有写操作都落审计）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor       VARCHAR(64) NOT NULL DEFAULT '',
  role        VARCHAR(32) NOT NULL DEFAULT '',
  action      VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id   VARCHAR(64) NOT NULL DEFAULT '',
  detail      VARCHAR(512) NOT NULL DEFAULT '',
  result      VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 种子数据
-- 密码均为 password123 的 SHA-256 哈希（演示用，本地库不接第三方）
-- =====================================================================
INSERT INTO app_user (id, username, display_name, role, password_hash) VALUES
  (1, 'admin',    '街道管理员', 'STREET_ADMIN',
   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f'),
  (2, 'keeper',   '中心仓仓库员', 'WAREHOUSE_KEEPER',
   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f'),
  (3, 'approver', '应急审批员', 'APPROVER',
   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f'),
  (4, 'observer', '只读观察员', 'OBSERVER',
   'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f');

INSERT INTO warehouse (id, name, district, address, manager_id, capacity_level, contact_phone, status) VALUES
  (1, '城东应急中心仓库', '城东区', '城东区抗洪大道 12 号', 2, 'L3', '0571-88000001', 'ACTIVE'),
  (2, '城北物资储备库',   '城北区', '城北区防汛路 88 号',   2, 'L2', '0571-88000002', 'ACTIVE'),
  (3, '城南社区仓库',     '城南区', '城南区安置大道 6 号',  2, 'L1', '0571-88000003', 'ACTIVE');

INSERT INTO supply_item (id, sku_code, name, category, unit, safety_stock, expire_days, storage_requirement) VALUES
  (1, 'WATER-550',  '瓶装饮用水 550ml', 'WATER',       '瓶', 200, 720, '常温避晒'),
  (2, 'FOOD-MRE',   '自热应急口粮',     'FOOD',        '份', 200, 360, '常温干燥'),
  (3, 'MED-KIT',    '急救医疗包',       'MEDICAL',     '个',  50, 720, '防潮'),
  (4, 'BLANKET',    '应急保温毯',       'SHELTER',     '条', 100, 0,   '干燥'),
  (5, 'TOOL-LIFT',  '液压破拆工具',     'RESCUE_TOOL', '套',  10, 0,   '常规'),
  (6, 'TENT-FAM',   '家庭应急帐篷',     'SHELTER',     '顶',  20, 0,   '干燥');

-- 批次：饮用水在 1 号仓有两个批次，天然用于“多避难点争抢同一批次”演示
INSERT INTO inventory_batch
  (id, warehouse_id, supply_item_id, batch_no, quantity, held_quantity, expire_at, inbound_source, quality_status) VALUES
  (1, 1, 1, 'W20260901-01', 100, 0, DATE_ADD(NOW(), INTERVAL 400 DAY), '市级调拨', 'OK'),
  (2, 1, 1, 'W20260910-02', 60,  0, DATE_ADD(NOW(), INTERVAL 20 DAY),  '社会捐赠', 'NEAR_EXPIRE'),
  (3, 1, 2, 'F20260901-01', 120, 0, DATE_ADD(NOW(), INTERVAL 200 DAY), '集中采购', 'OK'),
  (4, 1, 3, 'M20260801-01', 40,  0, DATE_ADD(NOW(), INTERVAL 500 DAY), '集中采购', 'OK'),
  (5, 1, 4, 'B20260701-01', 150, 0, NULL,                              '集中采购', 'OK'),
  (6, 2, 1, 'W20260905-03', 200, 0, DATE_ADD(NOW(), INTERVAL 300 DAY), '市级调拨', 'OK'),
  (7, 2, 6, 'T20260601-01', 30,  0, NULL,                              '集中采购', 'OK'),
  (8, 3, 2, 'F20260901-08', 80,  0, DATE_ADD(NOW(), INTERVAL 15 DAY),  '社会捐赠', 'NEAR_EXPIRE');

INSERT INTO shelter (id, name, district, capacity, current_population, contact_person, contact_phone, risk_level, open_status) VALUES
  (1, '城东实验学校避难点', '城东区', 500, 320, '王主任', '13900000001', 'HIGH',     'OPEN'),
  (2, '城北体育馆避难点',   '城北区', 800, 210, '李主任', '13900000002', 'MEDIUM',   'OPEN'),
  (3, '城南社区文化站',     '城南区', 300, 0,   '赵主任', '13900000003', 'LOW',      'STANDBY'),
  (4, '城东第二小学安置点', '城东区', 400, 180, '孙主任', '13900000004', 'CRITICAL', 'FULL');

INSERT INTO disaster_event (id, title, district, level, status) VALUES
  (1, '城东区 9·15 台风内涝', '城东区', 'HIGH', 'ACTIVE'),
  (2, '城北内涝防范',         '城北区', 'MEDIUM', 'ACTIVE');
