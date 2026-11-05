-- 数据服务演示数据集（osint 库）
-- 全部实体为虚构名称，IP 仅使用 192.0.2.0/24、198.51.100.0/24、203.0.113.0/24 文档网段，
-- 域名仅使用 example.com/.net/.org，可直接在 osint 库执行，重复执行会重建四张演示表的数据。
-- 生成规则确定（同一份脚本多次执行数据一致），便于回归与截图对照。

CREATE TABLE IF NOT EXISTS osint_entity (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  alias VARCHAR(255) DEFAULT NULL,
  entity_type VARCHAR(16) NOT NULL,
  region VARCHAR(32) NOT NULL,
  risk_score TINYINT UNSIGNED NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  first_seen DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_osint_entity_name (name),
  KEY idx_osint_entity_type (entity_type, risk_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS osint_ioc (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_id INT UNSIGNED NOT NULL,
  ioc_type VARCHAR(16) NOT NULL,
  value VARCHAR(255) NOT NULL,
  confidence TINYINT UNSIGNED NOT NULL,
  source VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  first_seen DATETIME NOT NULL,
  last_seen DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_osint_ioc_value (value),
  KEY idx_osint_ioc_entity (entity_id),
  KEY idx_osint_ioc_type_status (ioc_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS osint_article (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  source VARCHAR(64) NOT NULL,
  lang VARCHAR(8) NOT NULL,
  category VARCHAR(32) NOT NULL,
  sentiment VARCHAR(16) NOT NULL,
  publish_time DATETIME NOT NULL,
  url VARCHAR(512) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_osint_article_time (publish_time),
  KEY idx_osint_article_cat (category, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS osint_article_entity (
  article_id BIGINT UNSIGNED NOT NULL,
  entity_id INT UNSIGNED NOT NULL,
  relevance DECIMAL(4,2) NOT NULL DEFAULT 0.50,
  PRIMARY KEY (article_id, entity_id),
  KEY idx_osint_aae_entity (entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

TRUNCATE TABLE osint_article_entity;
TRUNCATE TABLE osint_ioc;
TRUNCATE TABLE osint_article;
TRUNCATE TABLE osint_entity;

DROP TABLE IF EXISTS seq_demo;
CREATE TABLE seq_demo (n INT PRIMARY KEY);
INSERT INTO seq_demo (n) VALUES
  (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
  (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
  (21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
  (31),(32),(33),(34),(35),(36),(37),(38),(39),(40),
  (41),(42),(43),(44),(45),(46),(47),(48),(49),(50),
  (51),(52),(53),(54),(55),(56),(57),(58),(59),(60);

-- 34 个实体：1-10 人物，11-18 组织，19-26 账号，27-30 域名，31-34 IP
INSERT INTO osint_entity (id, name, alias, entity_type, region, risk_score, status, first_seen, updated_at)
SELECT n,
  CASE
    WHEN n <= 10 THEN ELT(n, '幽灵线人','灰雀','Delta-07','Nightjar','白桦','Raven-3','海燕','Ironclad','翠鸟','Meridian')
    WHEN n <= 18 THEN ELT(n - 10, 'Northwind Trading Ltd.','Aurora Analytics Group','北辰数据服务有限公司','Helios Logistics','云杉咨询','Blue Harbor Capital','长风供应链','Silverline Media')
    WHEN n <= 26 THEN ELT(n - 18, '@delta_watch','@nightjar_osint','@aurora_insight','@helios_feed','@kestrel_live','@meridian_brief','@raven_trace','@sable_watch')
    WHEN n <= 30 THEN ELT(n - 26, 'delta-watch.example.com','aurora-insight.example.net','helios-feed.example.org','meridian-brief.example.com')
    ELSE ELT(n - 30, '203.0.113.24','198.51.100.77','192.0.2.145','203.0.113.201')
  END,
  CASE
    WHEN n <= 10 THEN CONCAT('代号 ', ELT(n, '幽灵线人','灰雀','Delta-07','Nightjar','白桦','Raven-3','海燕','Ironclad','翠鸟','Meridian'), '（虚构）')
    WHEN n <= 18 THEN CONCAT('fictional org #', n - 10)
    WHEN n <= 26 THEN '虚构社交账号'
    ELSE NULL
  END,
  CASE WHEN n <= 10 THEN 'PERSON' WHEN n <= 18 THEN 'ORG' WHEN n <= 26 THEN 'ACCOUNT' WHEN n <= 30 THEN 'DOMAIN' ELSE 'IP' END,
  ELT(1 + (n % 10), 'CN','US','RU','DE','SG','BR','IN','JP','GB','AE'),
  10 + ((n * 37) % 89),
  CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'MONITORED' ELSE 'ARCHIVED' END,
  TIMESTAMP('2025-01-01 00:00:00') + INTERVAL (n * 13) DAY,
  TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 7) % 57) DAY
FROM seq_demo WHERE n <= 34;

-- 108 条 IOC：人物邮箱/电话、组织域名/网址/样本哈希、账号主页、域名解析、IP 及其扫描页
INSERT INTO osint_ioc (id, entity_id, ioc_type, value, confidence, source, status, first_seen, last_seen)
SELECT n, n, 'EMAIL', CONCAT('person', LPAD(n, 2, '0'), '@example.com'),
       30 + ((n * 17) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 10
UNION ALL
SELECT 10 + n, n, 'PHONE', CONCAT('+86-138-0000-', LPAD(n, 4, '0')),
       30 + ((n * 13) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 10
UNION ALL
SELECT 20 + n, 10 + n, 'DOMAIN', CONCAT('org', LPAD(n, 2, '0'), '.example.com'),
       30 + ((n * 11) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 6 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 8
UNION ALL
SELECT 28 + n, 10 + n, 'URL', CONCAT('https://portal.example.com/org/', LPAD(n, 2, '0')),
       30 + ((n * 19) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 6 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 8
UNION ALL
SELECT 36 + n, 18 + n, 'ACCOUNT', CONCAT('https://social.example.com/', TRIM(LEADING '@' FROM ELT(n, '@delta_watch','@nightjar_osint','@aurora_insight','@helios_feed','@kestrel_live','@meridian_brief','@raven_trace','@sable_watch'))),
       30 + ((n * 23) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 8
UNION ALL
SELECT 44 + n, 18 + n, 'URL', CONCAT('https://archive.example.org/post/', 3000 + n * 7),
       30 + ((n * 29) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 8
UNION ALL
SELECT 52 + n, 26 + n, 'DOMAIN', ELT(n, 'delta-watch.example.com','aurora-insight.example.net','helios-feed.example.org','meridian-brief.example.com'),
       30 + ((n * 31) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 6 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 4
UNION ALL
SELECT 56 + n, 26 + n, 'URL', CONCAT('https://', ELT(n, 'delta-watch.example.com','aurora-insight.example.net','helios-feed.example.org','meridian-brief.example.com'), '/status'),
       30 + ((n * 37) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 6 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 4
UNION ALL
SELECT 60 + n, 30 + n, 'IP', ELT(n, '203.0.113.24','198.51.100.77','192.0.2.145','203.0.113.201'),
       30 + ((n * 41) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 4
UNION ALL
SELECT 64 + n, 30 + n, 'URL', CONCAT('https://scan.example.net/host/', ELT(n, '203.0.113.24','198.51.100.77','192.0.2.145','203.0.113.201')),
       30 + ((n * 43) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 7 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 4
UNION ALL
SELECT 68 + n, 1 + ((n * 7) % 34), 'MD5', MD5(CONCAT('sample-', n)),
       30 + ((n * 47) % 70), ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
       CASE WHEN n % 10 < 6 THEN 'ACTIVE' WHEN n % 10 < 9 THEN 'EXPIRED' ELSE 'FALSE_POSITIVE' END,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n * 5) DAY,
       TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 3) % 57) DAY
FROM seq_demo WHERE n <= 40;

-- 60 篇情报文章
INSERT INTO osint_article (id, title, source, lang, category, sentiment, publish_time, url)
SELECT n,
  CONCAT(
    ELT(1 + (n % 8),
      '跨境支付平台异常流量归因分析：', 'Cross-border payment anomaly attributed to ',
      '供应链数据泄露事件追踪：', 'Supply-chain leak traced to ',
      '公开账号传播链观察：', 'Public account propagation linked to ',
      '威胁情报周报：重点实体变化 ', 'OSINT aggregate: new IOCs for '),
    ELT(1 + (n % 8), 'Northwind Trading Ltd.','Aurora Analytics Group','北辰数据服务有限公司','Helios Logistics','云杉咨询','Blue Harbor Capital','长风供应链','Silverline Media')
  ),
  ELT(1 + (n % 4), '示例开源情报日报','Example OSINT Weekly','北向威胁简报','Demo Social Monitor'),
  CASE WHEN n % 2 = 0 THEN 'zh-CN' ELSE 'en' END,
  ELT(1 + (n % 5), 'THREAT','POLICY','MARKET','TECH','INCIDENT'),
  ELT(1 + (n % 3), 'POSITIVE','NEUTRAL','NEGATIVE'),
  TIMESTAMP('2026-09-01 00:00:00') + INTERVAL ((n * 29) % 58) DAY + INTERVAL ((n * 7) % 23) HOUR,
  CONCAT('https://news.example.com/a/', 10000 + n)
FROM seq_demo WHERE n <= 60;

-- 每篇文章关联 3 个实体（共 180 条）
INSERT INTO osint_article_entity (article_id, entity_id, relevance)
SELECT a.n, 1 + ((a.n * 7 + k.k * 11) % 34), ROUND(0.50 + ((a.n * 3 + k.k * 5) % 50) / 100, 2)
FROM seq_demo a
JOIN (SELECT 1 AS k UNION ALL SELECT 2 UNION ALL SELECT 3) k
WHERE a.n <= 60;

DROP TABLE seq_demo;

SELECT 'osint_entity' AS table_name, COUNT(*) AS row_count FROM osint_entity
UNION ALL SELECT 'osint_ioc', COUNT(*) FROM osint_ioc
UNION ALL SELECT 'osint_article', COUNT(*) FROM osint_article
UNION ALL SELECT 'osint_article_entity', COUNT(*) FROM osint_article_entity;
