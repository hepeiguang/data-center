#!/usr/bin/env node
/*
 * 一键落地 osint 演示数据 + 发布 intel 数据服务样例。
 *
 * 用法（在 dolphinscheduler-data-api/deploy/scripts 下）：
 *   npm install mysql2 --no-save
 *   DB_HOST=123.57.2.64 DB_USER=root DB_PASS=****** DB_NAME=osint node seed-demo-osint.js
 *
 * 环境变量：
 *   DB_HOST / DB_PORT / DB_USER / DB_PASS / DB_NAME   业务库（默认 DB_NAME=osint）
 *   META_DB           DS 元数据库名；缺省时在 information_schema 中自动查找 magic_api_file
 *   DATA_API_BASE     验证用网关地址，默认 http://123.57.2.64:12346；置空跳过验证
 *
 * 行为：
 *   1) 执行 deploy/sql/demo-osint-seed.sql（重建 osint_entity/osint_ioc/osint_article/osint_article_entity）
 *   2) 若发现 magic_api_file（cluster profile），把 deploy/magic-api-seed/api/intel 下的脚本 upsert 进去
 *      否则只落本地文件，提示按文件存储方式拷贝
 *   3) 从元库取 admin 的 Access Token，逐个调用 intel 分组接口做冒烟验证（不打印 token）
 */
const fs = require('fs');
const path = require('path');
const http = require('http');

const DB = {
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME || 'osint',
  multipleStatements: true,
  charset: 'utf8mb4',
  connectTimeout: 10000,
};
const DATA_API_BASE = process.env.DATA_API_BASE === undefined ? 'http://127.0.0.1:12346' : process.env.DATA_API_BASE;
const PREFIX = 'magic-api';
const SEED_SQL = path.resolve(__dirname, '../sql/demo-osint-seed.sql');
const SEED_DIR = path.resolve(__dirname, '../magic-api-seed/api/intel');

function request(base, apiPath, token) {
  return new Promise((resolve) => {
    const url = new URL(base + apiPath);
    const req = http.get(
      { hostname: url.hostname, port: url.port, path: url.pathname + url.search, headers: { token } },
      (res) => {
        let body = '';
        res.on('data', (c) => (body += c));
        res.on('end', () => resolve({ status: res.statusCode, body }));
      },
    );
    req.on('error', (e) => resolve({ status: 0, body: e.message }));
    req.setTimeout(15000, () => req.destroy(new Error('timeout')));
  });
}

(async () => {
  if (!DB.host || !DB.user) {
    console.error('缺少 DB_HOST / DB_USER，终止');
    process.exit(2);
  }
  const mysql = require('mysql2/promise');
  const conn = await mysql.createConnection(DB);

  const [, seedInfo] = await conn.query(fs.readFileSync(SEED_SQL, 'utf8'));
  console.log('seed:', JSON.stringify(seedInfo[seedInfo.length - 1]));

  let metaDbs = [];
  try {
    [metaDbs] = await conn.query(
      "select table_schema as db from information_schema.tables where table_name = 'magic_api_file'",
    );
  } catch (e) {
    console.log('skip publish: meta DB lookup failed -', e.message);
  }
  if (metaDbs.length === 0) {
    console.log('未发现 magic_api_file（文件存储模式）：请把 deploy/magic-api-seed/api/intel 拷贝到实例的 data/magic-api/api/intel 后重启 data-api');
  } else {
    const metaDb = process.env.META_DB || metaDbs[0].db;
    const files = fs.readdirSync(SEED_DIR);
    const rows = [['api/intel/', 'this is directory']].concat(
      files.map((f) => [`api/intel/${f}`, fs.readFileSync(path.join(SEED_DIR, f), 'utf8').replace(/^\uFEFF/, '')]),
    );
    const statements = rows.map(() => '(?,?)').join(',');
    await conn.query(
      `INSERT INTO \`${metaDb}\`.magic_api_file(file_path, file_content) VALUES ${statements} ` +
        'ON DUPLICATE KEY UPDATE file_content = VALUES(file_content)',
      rows.flatMap(([p, c]) => [`${PREFIX}/${p}`, c]),
    );
    console.log(`published ${rows.length - 1} scripts into ${metaDb}.magic_api_file (refresh <= 30s)`);
  }

  if (DATA_API_BASE) {
    let tokens = [];
    try {
      [tokens] = await conn.query(
        'select t.token from t_ds_access_token t join t_ds_user u on u.id = t.user_id where u.user_name = ? limit 1',
        ['admin'],
      );
    } catch (e) {
      console.log('skip verify: access token lookup failed -', e.message);
    }
    if (tokens.length === 0) {
      console.log('未找到 admin Access Token，跳过网关验证');
    } else {
      const token = tokens[0].token;
      const checks = [
        '/data-api/gateway/intel/entities?size=3',
        '/data-api/gateway/intel/entities/1',
        '/data-api/gateway/intel/ioc/stats',
        '/data-api/gateway/intel/articles?size=2',
        '/data-api/gateway/intel/search?keyword=Northwind',
      ];
      let notFound = 0;
      for (const p of checks) {
        const r = await request(DATA_API_BASE, p, token);
        if (r.status === 404) {
          notFound++;
        }
        console.log(`${r.status} ${p} -> ${r.body.slice(0, 180).replace(/\s+/g, ' ')}`);
      }
      if (notFound > 0) {
        console.log('存在 404：实例可能为文件存储模式（magic-api.resource.type=file），请把 deploy/magic-api-seed/api/intel 拷到实例的 data/magic-api/api/intel 后重启，或等 30s 再试一次');
      }
    }
  }
  await conn.end();
})().catch((e) => {
  console.error('FAILED:', e.message);
  process.exit(1);
});
