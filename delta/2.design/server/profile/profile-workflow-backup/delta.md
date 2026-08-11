# profile-workflow-backup

- 对齐：UC-PRO-005～007、[`../../delta.md`](../../delta.md) §3（周日 **03:00**，保留 **4** 份）
- 范围：**全库**快照（Profile、Roadmap、Job、Action、CV、Behavioral）；编排归属 Profile 域调度
- 恢复：**必须先 preview，再 confirm**；不得无确认覆盖生产库
- 状态：**设计完成**

## 1. 备份策略

| 项 | 值 |
|---|---|
| 调度 | 每周日 **03:00** 本地时区（Spring `@Scheduled(cron = "0 0 3 ? * SUN")`） |
| 保留 | 调度备份最多 **4** 份；超出删除最旧 |
| 存储 | `./data/backups/`（实现仓配置；见 design-check） |
| 格式 | 单文件快照包 `job-helper-backup-{timestamp}.zip`（PostgreSQL pg_dump + 元数据 JSON） |
| 类型 | `SCHEDULED`（自动） / `MANUAL_EXPORT`（用户触发） |

## 2. 备份包结构

```text
job-helper-backup-20260731T030000.zip
├── manifest.json          # schemaVersion, domains[], checksum, createdAt
├── database.dump          # pg_dump custom format
└── files/                 # 可选：导出附件占位（v1 可空）
```

**manifest.json 摘要**：

```json
{
  "appSchemaVersion": "1.0.0",
  "backupType": "SCHEDULED",
  "domainsIncluded": ["profile", "roadmap", "job", "action", "cv", "behavioral"],
  "profileVersion": 3,
  "checksumSha256": "..."
}
```

日志与元数据**不得**写入明文 PII 正文。

## 3. 工作流：自动备份（UC-PRO-005）

| 步骤 | 行为 |
|---|---|
| B1 | 调度触发；检查存储可写 |
| B2 | 执行 pg_dump 全库 |
| B3 | 打包 + 计算 SHA256 |
| B4 | 写入 `profile_backup_metadata` |
| B5 | 保留策略：按 `created_at` 保留最新 4 条 `SCHEDULED`，删除旧文件 |
| B6 | 失败：记录诊断；**不**删改主库；下次周期或手动 export 补救 |

## 4. 工作流：手动导出（UC-PRO-006）

| 步骤 | 行为 |
|---|---|
| E1 | 用户 `POST /profile/backups/export` |
| E2 | 展示敏感内容与全域范围说明 |
| E3 | 生成与调度相同格式包至用户路径 |
| E4 | 登记 `MANUAL_EXPORT` 元数据（可选索引） |
| E5 | 不修改主库 |

## 5. 工作流：恢复（UC-PRO-007）

### 5.1 预览（必须）

| 步骤 | 行为 |
|---|---|
| R1 | `GET .../restore-preview`：校验文件存在、SHA256、schema 兼容性 |
| R2 | 解析 manifest，列出将替换的域与关键计数 |
| R3 | 生成短时 `restorePreviewToken`（如 15 分钟） |
| R4 | 返回 warnings：恢复后**建议**重算 Gate/Roadmap，**不自动执行** |

### 5.2 确认恢复

| 步骤 | 行为 |
|---|---|
| R5 | `POST .../restore` 携带 `confirmOverwrite=true` + token |
| R6 | 停止应用写入或短锁（实现细节） |
| R7 | pg_restore 覆盖当前库 |
| R8 | 校验恢复后应用可启动 |
| R9 | 若失败：报告部分状态，主库保持可诊断 |
| R10 | 成功：创建 `PENDING` RecomputeRequest（scopes 由 manifest diff 推导），用户仍须预览确认 |

**v1 恢复模式**：仅支持**全库覆盖**（非增量 merge）。

## 6. 服务与 API 映射

| 服务方法 | API |
|---|---|
| `listBackups()` | GET `/profile/backups` |
| `exportManual()` | POST `/profile/backups/export` |
| `previewRestore()` | GET `/profile/backups/{id}/restore-preview` |
| `confirmRestore()` | POST `/profile/backups/{id}/restore` |
| `runScheduledBackup()` | 内部调度，无 REST |

## 7. 失败与边界

- 磁盘满：备份失败，主库只读不受影响
- 恢复中断：不承诺自动回滚；保留 pre-restore 手动 export 为运维补救
-  schema 不兼容：`BACKUP_INCOMPATIBLE`，禁止 confirm

PlantUML：[`workflow-profile-backup.puml`](workflow-profile-backup.puml)
