# 城市防灾应急物资调度系统（rescue-stock）

面向街道、社区和应急仓库的防灾物资储备与调拨平台，覆盖**申请 → 审批 → 出库 → 签收 / 拒签 / 撤销**的物资调拨闭环，并以应急态势大屏实时回显库存、占用与各状态调拨数量。

核心保证：申请先占用可用库存、审批不扣减库存、出库按批次扣减并记录去向、拒签或撤销后按**原批次、原数量**回补；同一申请重复提交不重复占库，多避难点并发争抢同一批次时**只有一个成功**，任一步失败都不会留下半占用或凭空回补。

---

## 快速启动（Docker Compose，推荐）

```bash
cp .env.example .env && docker compose up -d
```

启动后：

- 前端：<http://localhost:20102>
- 后端健康检查：<http://localhost:21102/health>
- 首次启动 `db` 容器会自动执行 `database/init.sql`，建表并写入仓库 / 物资 / 批次 / 避难点 / 事件 / 演示账号种子数据。

演示账号（密码均为 `password123`）：

| 用户名 | 角色 | 主要权限 |
|---|---|---|
| `admin` | 街道管理员 STREET_ADMIN | 申请、审批、出库、签收、撤销（全流程） |
| `keeper` | 仓库员 WAREHOUSE_KEEPER | 申请、出库、签收、拒签 |
| `approver` | 审批员 APPROVER | 审批通过、驳回 |
| `observer` | 只读观察员 OBSERVER | 仅查看，所有写操作返回 403 |

重置全部数据：

```bash
docker compose down -v && docker compose up -d
```

---

## 调拨闭环说明

### 状态机（DispatchStatus）

```
                申请占库            审批(不扣库存)        出库(按批次扣减)
   (新建) ──▶ SUBMITTED ──────▶ APPROVED ──────────▶ DISPATCHED ──────▶ RECEIVED(终态)
                │  │                │                    │  │
   未出库→释放占用  │                │                    │  ├─拒签 REFUSED：原批次物理回补
   已出库→原批次回补└─驳回 REJECTED   └─撤销 CANCELLED      └─撤销 CANCELLED：按去向表原批次回补
   RECEIVED 为终态，签收后不可再撤销/拒签/出库
```

- **申请 SUBMITTED**：在单个事务内建单并按批次（FEFO 临期优先）**占用可用库存** `held_quantity += n`，登记批次去向 `dispatch_batch_allocation(HELD)`，写 `HOLD` 流水。
- **审批 APPROVED / 驳回 REJECTED**：通过**完全不碰库存**；驳回则按原批次释放占用（`RELEASE`）。
- **出库 DISPATCHED**：严格按申请时登记的批次去向扣减，`quantity -= n` 且 `held_quantity -= n` 同步推进，去向置 `OUT`，回填每行出库量，写 `OUTBOUND` 流水。
- **签收 RECEIVED**：终态，不改变库存（物资已在避难点）；签收后任何撤销/拒签/出库都返回 409。
- **拒签 REFUSED / 撤销 CANCELLED（出库后）**：按原批次、原数量做**物理回补** `quantity += n`，去向置 `RETURNED`，写 `RETURN` 流水。
- **撤销 CANCELLED（未出库：SUBMITTED/APPROVED）**：仅释放占用 `held_quantity -= n`（`RELEASE`），物理库存本就未动。
- 撤销与拒签都对订单行先加 `FOR UPDATE` 锁并以状态 CAS 推进，因此**并发时只有一个成功**，失败方读到终态直接 409，不会重复回补。

### 库存模型

批次表 `inventory_batch` 只落地两个事实量：`quantity`（在库物理量）与 `held_quantity`（被在途申请占用量），**可用量永远实时计算** `available = quantity - held_quantity`，并由数据库 `CHECK (held_quantity BETWEEN 0 AND quantity)` 兜底，不存在“可用量”和“占用量”两份不一致的数据。

### 并发与一致性如何保证

1. **幂等（requestId + 内容指纹双重判定）**：申请带客户端生成的 `requestId`，`dispatch_order.request_id` 唯一键兜底。
   - **相同内容**的重复提交（允许明细顺序不同、同一物资拆成多行——服务端按物资 id 合并归一化）直接返回原单，绝不二次占库；
   - 同一 `requestId` 但**内容不同**（换了源仓库 / 避难点 / 事件 / 物资或数量）返回 `409 IDEMPOTENT_CONFLICT` 并指明原单号，**不会**静默吞掉后到的申请，本次请求不占用任何库存。
2. **统一加锁顺序，杜绝逆序死锁**：服务端先把明细按物资 id 归一化，再用**一条 SQL** 一次性锁定本单所需全部候选批次，并强制 `FORCE INDEX(PRIMARY) … ORDER BY id FOR UPDATE`。所有申请都按同一全局批次 id 顺序持锁，因此“相同物资、明细顺序相反”的并发申请不会交叉持锁、不会死锁；批内仍按 FEFO（临期优先）选择占用。
3. **行锁串行化争抢**：多个避难点抢同一批次时在行锁上排队，真正增减用条件 `UPDATE … WHERE quantity-held >= ?`，影响行数必须为 1，否则整单回滚——从机制上杜绝超占、超扣、凭空回补。
4. **单事务原子性**：库存动作 + 状态 CAS（`WHERE status = 期望值`）+ 批次去向 + 库存流水 + 状态时间线 + 审计同提交同回滚，冲突或失败不留任何占用、流水或状态变化。
5. **回补有凭据**：回补量恒等于该单 `dispatch_batch_allocation` 中实际占/出过且未回补的数量；已 `RETURNED` 的去向跳过，重复拒签/撤销直接 409。

### 闭环接口（统一前缀 `/api`）

| 动作 | 方法 & 路径 | 允许角色 |
|---|---|---|
| 申请（占库，幂等） | `POST /api/dispatch-orders` | admin / keeper |
| 调拨列表 | `GET /api/dispatch-orders?status=&shelterId=` | 全部 |
| 审批详情（明细+去向+时间线） | `GET /api/dispatch-orders/{id}` | 全部 |
| 审批通过 | `POST /api/dispatch-orders/{id}/approve` | admin / approver |
| 驳回（释放占用） | `POST /api/dispatch-orders/{id}/reject` | admin / approver |
| 出库（按批次扣减） | `POST /api/dispatch-orders/{id}/outbound` | admin / keeper |
| 签收 | `POST /api/dispatch-orders/{id}/receive` | admin / keeper |
| 拒签（原批次回补） | `POST /api/dispatch-orders/{id}/refuse` | admin / keeper |
| 撤销（释放/回补） | `POST /api/dispatch-orders/{id}/cancel` | admin |
| 态势总览 | `GET /api/dashboard/overview` | 全部 |
| 批次/库存流水 | `GET /api/batches`、`/api/batches/ledger` | 全部 |
| 登录 | `POST /api/auth/login` | 公开 |

`curl` 示例：

```bash
# 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:21102/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"password123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 申请 150 瓶饮用水（requestId 务必由调用方生成并稳定复用）
curl -s -X POST http://localhost:21102/api/dispatch-orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"requestId":"REQ-DEMO-1","eventId":1,"sourceWarehouseId":1,"shelterId":1,
       "priority":"HIGH","lines":[{"supplyItemId":1,"requestedQty":150}]}'

# 审批 -> 出库 -> 签收
curl -s -X POST http://localhost:21102/api/dispatch-orders/1/approve -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:21102/api/dispatch-orders/1/outbound -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:21102/api/dispatch-orders/1/receive  -H "Authorization: Bearer $TOKEN"
```

---

## 访问地址

| 入口 | 地址 |
|---|---|
| 前端（态势大屏 / 仓库库存 / 避难点 / 调拨审批 / 事件响应） | <http://localhost:20102> |
| 后端健康检查 | <http://localhost:21102/health> |
| 后端 API Base（经前端 nginx 反代同源 `/api`） | <http://localhost:20102/api/...> |

---

## 本地开发方式

- 前端（端口 20102，已在 `vite.config.ts` 把 `/api` 代理到 `:21102`）：

  ```bash
  cd frontend
  npm install
  npm run dev        # 开发
  npm run build      # vue-tsc 类型检查 + vite 构建
  ```

- 后端（Spring Boot 3 / Java 17，默认读 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`）：

  ```bash
  cd backend
  mvn spring-boot:run
  # 或 mvn -DskipTests package && java -jar target/rescue-stock-0.1.0.jar
  ```

  本地无 MySQL 时，仅启动数据库即可：`docker compose up -d db`（映射 `DB_PORT`，默认 33060）。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Vue Router + Pinia + Element Plus + ECharts |
| 后端 | Spring Boot 3 + Java 17 + Spring JDBC（`JdbcTemplate` 行锁/条件更新）+ MyBatis-Plus + JWT |
| 数据库 | MySQL 8.0（InnoDB 行锁、CHECK 约束、唯一键幂等） |
| 部署 | Docker Compose（db / backend / frontend 三容器，命名卷） |
| 认证鉴权 | JWT + RBAC（街道管理员 / 仓库员 / 审批员 / 只读观察员） |

---

## 项目目录结构

```text
.
├── docker-compose.yml          # 顶层 name: rescue-stock；三服务 + db_data 命名卷 + healthcheck
├── .env / .env.example         # COMPOSE_PROJECT_NAME / 端口 / DB / JWT
├── database/init.sql           # 建表 + 约束 + 全部本地种子数据
├── frontend/                   # Vue 3 前端（Nginx 托管，/api 反代 backend:8080）
│   └── src/
│       ├── api/                # 按实体分文件：auth/DispatchOrder/Warehouse/SupplyItem/...
│       ├── stores/             # Pinia：auth/DispatchOrder/Catalog/InventoryBatch/Shelter/Dashboard
│       ├── types/              # 共享类型（DispatchOrder/InventoryBatch/Dashboard...）
│       ├── constants/          # 枚举、错误码、错误消息、日志模板、状态文案、RBAC 映射
│       ├── constructors/       # 各实体默认对象/申请表单/幂等 requestId 构造器
│       ├── components/common/  # StatusBadge / BatchTable / ApprovalTimeline / CapacityMeter / ...
│       ├── hooks/              # useDispatchFlow / useExpireWarning / usePagination
│       ├── pages/              # Dashboard/Warehouses/Shelters/Dispatch/Events/Login
│       ├── router/             # 路由表 + 登录守卫
│       ├── utils/              # request 封装 / formatters
│       └── mocks/              # 演示账号与种子说明（真实数据只来自本地数据库）
└── backend/                    # Spring Boot 3
    └── src/main/java/com/generated/rescueStock/
        ├── constants/          # DispatchStatus 状态机、SupplyCategory、ShelterStatus、
        │                       #   ErrorCodes、ErrorMessages、LogTemplates、LedgerDirection、UserRole
        ├── controllers/        # REST + ControllerSupport 二次包装异常
        ├── services/           # DispatchOrderService(幂等编排) / DispatchTxService(状态机事务)
        │                       #   StockReservationService(占用/出库/回补算法) / Dashboard / Auth ...
        ├── repositories/       # JdbcTemplate 数据访问（FOR UPDATE / 条件 UPDATE）
        ├── middlewares/        # Auth / Rbac / AuditLog / RateLimit / ErrorHandler
        ├── security/           # JwtService / CurrentUser / UserContextHolder
        ├── models/ dto/ types/ constructors/ routes/ config/ utils/
```

---

## 环境变量说明

根目录 `.env`（由 `.env.example` 复制）：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `COMPOSE_PROJECT_NAME` | `rescue-stock` | Compose 项目名，也是容器名前缀 |
| `FRONTEND_PORT` | `20102` | 前端宿主机端口（容器内 80） |
| `BACKEND_PORT` | `21102` | 后端宿主机端口（容器内 8080） |
| `DB_PORT` | `33060` | MySQL 宿主机映射端口（容器网络内固定 3306） |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `app_db` / `app_user` / `app_password` | 数据库名与账号 |
| `JWT_SECRET` | 本地开发占位 | JWT 签名密钥，生产务必修改 |

后端配置集中在 `backend/src/main/resources/application.yml`，再由 compose `environment` 注入，新增配置需同步这几处与 `.env.example`。

---

## Docker 部署说明

- `docker-compose.yml` 顶层 `name: rescue-stock`，不写 `version:`；所有服务 `container_name` 带 `${COMPOSE_PROJECT_NAME:-rescue-stock}-` 前缀。
- 端口：前端 `${FRONTEND_PORT:-20102}:80`，后端 `${BACKEND_PORT:-21102}:8080`。
- 数据库使用**命名卷 `db_data`**，不绑定挂载，因此在任意目录名（含中文目录）下都能正常启动。
- `db` 配置 `healthcheck`（`mysqladmin ping`）；`backend` 通过 `depends_on: condition: service_healthy` 等待数据库，并提供 `/health` 供自身 healthcheck；`frontend` 再等待 `backend` 健康。
- 前端 Nginx：`location /api/ { proxy_pass http://backend:8080/api/; }`，并配置 `try_files $uri $uri/ /index.html;` 支持前端路由；前端代码只请求相对 `/api`，不硬编码 localhost。

常见问题：

- **端口被占用**：修改 `.env` 的 `FRONTEND_PORT/BACKEND_PORT/DB_PORT` 后 `docker compose up -d`。
- **想清空演示数据重来**：`docker compose down -v`（删除命名卷）再 `up -d`。
- **改了后端代码镜像没更新**：`docker compose build backend && docker compose up -d backend`。
- **后端起不来卡在等数据库**：多为首次初始化较慢，`docker compose logs -f db` 观察，健康后会自动拉起后端。

---

## 枚举 / 常量出现位置清单

新增任何枚举值，都必须同步以下所有位置（这是刻意的高耦合修改面）。

### DispatchStatus（DRAFT/SUBMITTED/APPROVED/DISPATCHED/RECEIVED/REJECTED/REFUSED/CANCELLED）

- 后端
  - 枚举与**状态迁移表**：`backend/.../constants/DispatchStatus.java`
  - 错误码 / 错误消息：`constants/ErrorCodes.java`（ILLEGAL_STATUS_TRANSITION 等）、`constants/ErrorMessages.java`
  - 日志模板：`constants/LogTemplates.java`（APPROVE/REJECT/OUTBOUND/RECEIVE/REFUSE/CANCEL）
  - 状态推进与库存动作：`services/DispatchTxService.java`、`services/StockReservationService.java`
  - 展示/查询：`controllers/DispatchOrderController.java`、`services/DashboardService.java`（状态矩阵）、`database/init.sql`（`dispatch_order.status` 与种子）
- 前端
  - 常量与文案/配色/状态机：`constants/DispatchStatus.ts`
  - 类型：`types/DispatchOrder.ts`
  - 构造器：`constructors/DispatchOrderConstructor.ts`
  - 日志模板 / 错误消息：`constants/logTemplates.ts`、`constants/errorMessages.ts`、`constants/errorCodes.ts`
  - 筛选器：`pages/DispatchPage.vue`（filter-chips）、`stores/DispatchOrderStore.ts`
  - 展示组件：`components/common/StatusBadge.vue`、`components/common/ApprovalTimeline.vue`、态势大屏 `pages/DashboardPage.vue`、避难点接收记录 `pages/SheltersPage.vue`

### SupplyCategory（FOOD/WATER/MEDICAL/SHELTER/RESCUE_TOOL）

- 后端：`constants/SupplyCategory.java`；`services/StockReservationService.java`（按物资占库）、`repositories/InventoryBatchRepository.java`；`database/init.sql`（`supply_item.category` 种子）。
- 前端：`constants/SupplyCategory.ts`、`types/SupplyItem.ts`、`constructors/SupplyItemConstructor.ts`、`constants/statusText.ts`；筛选/展示见 `pages/WarehousesPage.vue`、`components/common/BatchTable.vue`、`pages/DashboardPage.vue`、新建调拨申请表单 `pages/DispatchPage.vue`。

### ShelterStatus（CLOSED/STANDBY/OPEN/FULL）

- 后端：`constants/ShelterStatus.java`；`services/ShelterService.java`、`controllers/ShelterController.java`、`database/init.sql`。
- 前端：`constants/ShelterStatus.ts`、`types/Shelter.ts`、`constructors/ShelterConstructor.ts`、`constants/statusText.ts`；筛选/展示见 `components/common/StatusBadge.vue`、`components/common/CapacityMeter.vue`、`pages/SheltersPage.vue`、态势开放避难点数量 `pages/DashboardPage.vue`。

### 横切枚举：库存方向 LedgerDirection（HOLD/RELEASE/OUTBOUND/RETURN）与去向阶段（HELD/OUT/RETURNED）

后端 `constants/LedgerDirection.java`、`repositories/*`、`stock_ledger`/`dispatch_batch_allocation` 表；前端 `constants/LedgerDirection.ts`、`types/StockLedger.ts`、`components/common/StatusBadge.vue`、`BatchTable`、大屏流水与审批详情批次去向。

---

## 为什么这个项目“牵一发动全身”

- **状态值贯穿全栈**：一个调拨状态同时出现在数据库列与种子、后端枚举状态机、错误码/错误消息、日志模板、service 的库存分支、controller、前端常量/类型/构造器、列表筛选器、StatusBadge、ApprovalTimeline、Dashboard。改一个状态要动十几处。
- **库存语义耦合在闭环里**：`quantity` 与 `held_quantity` 被申请、审批、出库、签收、拒签、撤销六个动作共享，批次去向表又是回补的唯一凭据；任何一个动作的 SQL 条件改错，都会破坏“无半占用 / 不凭空回补”的全局不变量。
- **共享工具与组件多页面依赖**：`utils/formatters`、`StatusBadge`、`BatchTable`、`ApprovalTimeline` 被大屏、仓库、避难点、调拨、事件多个页面共用，签名或文案一改处处受影响。
- **配置分散且需同步**：`.env.example`、`docker-compose.yml`、`application.yml`、前端请求封装、Nginx 反代任一处不一致，全链路就起不来。

---

## License

MIT
