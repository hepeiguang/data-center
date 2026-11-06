// Regenerate the sample APIs against the PostgreSQL agriculture schema (agri_sat),
// replacing the osint-MySQL demo APIs. Writes to both the live magic file store and
// the versioned seed, because data/magic-api is not tracked by git.
import fs from 'fs'
import path from 'path'

const LIVE = 'data/magic-api/api'
const SEED = 'deploy/magic-api-seed/api'
const SEP = '================================'
const NOW = Date.now()

const param = (name, description, dataType = 'String', defaultValue = null) => {
  // magic-api's DataType enum; a value outside it breaks JSON deserialization and makes
  // the whole resource load fail at startup (every gateway route then answers 404)
  const ALLOWED = ['MultipartFile', 'MultipartFiles', 'Any', 'Date', 'String', 'Array',
    'Float', 'Integer', 'Object', 'Byte', 'Double', 'Long', 'Boolean', 'Short']
  if (!ALLOWED.includes(dataType)) {
    throw new Error(`unsupported magic DataType "${dataType}" for param "${name}"`)
  }
  return {
    name,
    value: null,
    description,
    required: false,
    dataType,
    type: null,
    defaultValue,
    validateType: null,
    error: null,
    expression: null,
    children: null
  }
}
const pageParams = [
  param('page', '页码，从 1 开始', 'Integer', '1'),
  param('size', '每页条数，上限 200', 'Integer', '20')
]

const GROUPS = [
  { id: 'agri_group', name: 'agri', path: '/agri' },
  { id: 'smoke_group', name: 'smoke', path: '/smoke' }
]

const PAGING_PRELUDE = `var pageNo = page == null || page == '' ? 1 : page.asInt();
var pageSize = size == null || size == '' ? 20 : size.asInt();
if (pageNo < 1) {
  pageNo = 1;
}
if (pageSize < 1) {
  pageSize = 20;
}
if (pageSize > 200) {
  pageSize = 200;
}
var offset = (pageNo - 1) * pageSize;`

const APIS = [
  {
    group: 'agri_group',
    dir: 'agri',
    id: 'agri_scenes',
    name: 'scenes',
    path: '/scenes',
    description: '卫星场景清单：按区域/卫星/云量/时间筛选，返回分页结果与影像地址',
    parameters: [
      ...pageParams,
      param('regionId', '区域编码，如 R001'),
      param('satelliteId', '卫星编码，如 SAT01'),
      // magic's DataType enum has no "Number": an invalid value here aborts the whole
      // resource load at startup, which 404s every gateway route
      param('maxCloudCover', '最大云量百分比', 'Integer'),
      param('from', 'acquisition 起始日期 yyyy-MM-dd'),
      param('to', 'acquisition 截止日期 yyyy-MM-dd')
    ],
    script: `${PAGING_PRELUDE}
var where = " where 1 = 1";
if (regionId != null && regionId != '') {
  where = where + " and s.region_id = #{regionId}";
}
if (satelliteId != null && satelliteId != '') {
  where = where + " and s.satellite_id = #{satelliteId}";
}
if (maxCloudCover != null && maxCloudCover != '') {
  where = where + " and s.cloud_cover_pct <= #{maxCloudCover}";
}
if (from != null && from != '') {
  where = where + " and s.acquired_at >= cast(#{from} as timestamp)";
}
if (to != null && to != '') {
  where = where + " and s.acquired_at < cast(#{to} as timestamp) + interval '1 day'";
}
var total = db.agri_sat.selectInt("select count(1) from ods_satellite_scene s" + where);
var rows = db.agri_sat.select("select s.scene_id, s.region_id, r.prefecture, r.agro_zone, s.satellite_id, sat.satellite_name, sat.sensor, sat.resolution_m, s.acquired_at, s.cloud_cover_pct, s.sun_elevation, s.product_level, s.crs, s.bbox, s.file_uri from ods_satellite_scene s left join dim_region r on r.region_id = s.region_id left join dim_satellite sat on sat.satellite_id = s.satellite_id" + where + " order by s.acquired_at desc limit #{pageSize} offset #{offset}");
return { page: pageNo, size: pageSize, total: total, rows: rows };`
  },
  {
    group: 'agri_group',
    dir: 'agri',
    id: 'agri_crop_growth',
    name: 'crop_growth',
    path: '/crop-growth',
    description: '作物长势时间序列：NDVI 均值与 P10/P90 参考带，按区域（可选作物）返回',
    parameters: [
      param('region', '区域编码，必填，如 R001'),
      param('crop', '作物编码，可选，如 WHEAT')
    ],
    script: `if (region == null || region == '') {
  exit 400, 'region is required';
}
var where = " where g.region_id = #{region}";
if (crop != null && crop != '') {
  where = where + " and g.crop_code = #{crop}";
}
var series = db.agri_sat.select("select g.stat_date, g.region_id, r.province, r.prefecture, r.agro_zone, g.crop_code, c.crop_name, c.season, c.growth_days, g.growth_stage, g.ndvi_mean, g.ndvi_p10, g.ndvi_p90, g.sample_scenes from dws_crop_growth g left join dim_region r on r.region_id = g.region_id left join dim_crop c on c.crop_code = g.crop_code" + where + " order by g.stat_date asc");
var summary = db.agri_sat.selectOne("select min(g.stat_date) as first_date, max(g.stat_date) as last_date, count(1) as points, avg(g.ndvi_mean) as avg_ndvi from dws_crop_growth g" + where);
return { region: region, crop: crop, summary: summary, series: series };`
  },
  {
    group: 'agri_group',
    dir: 'agri',
    id: 'agri_health',
    name: 'health',
    path: '/health',
    description: '分区作物健康度：健康等级、面积、变化率与建议，并给出等级面积分布',
    parameters: [
      param('region', '区域编码，可选'),
      param('crop', '作物编码，可选'),
      param('reportDate', '报告日期 yyyy-MM-dd，可选')
    ],
    script: `var where = " where 1 = 1";
if (region != null && region != '') {
  where = where + " and h.region_id = #{region}";
}
if (crop != null && crop != '') {
  where = where + " and h.crop_code = #{crop}";
}
if (reportDate != null && reportDate != '') {
  where = where + " and h.report_date = cast(#{reportDate} as date)";
}
var rows = db.agri_sat.select("select h.report_date, h.region_id, r.province, r.prefecture, r.agro_zone, h.crop_code, c.crop_name, c.season, h.health_level, h.area_km2, h.change_rate_pct, h.suggestion from ads_crop_health h left join dim_region r on r.region_id = h.region_id left join dim_crop c on c.crop_code = h.crop_code" + where + " order by h.report_date desc, h.region_id, h.crop_code");
var distribution = db.agri_sat.select("select h.health_level, count(1) as records, sum(h.area_km2) as area_km2 from ads_crop_health h" + where + " group by h.health_level order by area_km2 desc");
return { rows: rows, distribution: distribution };`
  },
  {
    group: 'smoke_group',
    dir: 'smoke',
    id: 'smoke_select1',
    name: 'select1',
    path: '/select1',
    description: 'smoke select 1 on agri_sat (PG)',
    parameters: [],
    script: `return db.agri_sat.select('select 1 as n')`
  },
  {
    group: 'smoke_group',
    dir: 'smoke',
    id: 'smoke_agri_tables',
    name: 'agri_tables',
    path: '/tables',
    description: '列出 agri_sat 的 public 表与列数',
    parameters: [],
    script: `var limit = daMaxRows == null ? 1000 : daMaxRows;
var rows = db.agri_sat.select(\"\"\"
  select t.table_name,
         t.table_type,
         (select count(1) from information_schema.columns c
           where c.table_schema = t.table_schema and c.table_name = t.table_name) as column_count
  from information_schema.tables t
  where t.table_schema = 'public'
  order by t.table_name
  limit \"\"\" + limit);
return rows;`
  },
  {
    group: 'smoke_group',
    dir: 'smoke',
    id: 'smoke_agri_query',
    name: 'agri_query',
    path: '/query',
    description: '在 agri_sat 上执行只读 SELECT（服务端仍受 reject-dml 与 max-rows 约束）',
    parameters: [param('sql', 'SELECT 语句，缺省 select 1 as n')],
    script: `var q = sql;
if (q == null || q == '') {
  q = 'select 1 as n';
}
// only allow select for demo safety
if (!q.trim().toLowerCase().startsWith('select')) {
  exit 400, 'only SELECT allowed';
}
return db.agri_sat.select(q);`
  }
]

const render = (api) => {
  const head = {
    properties: {},
    id: api.id,
    script: null,
    groupId: api.group,
    name: api.name,
    createTime: NOW,
    updateTime: NOW,
    lock: null,
    createBy: 'admin',
    updateBy: 'admin',
    path: api.path,
    method: 'GET',
    parameters: api.parameters,
    options: [],
    requestBody: '',
    headers: [],
    paths: [],
    responseBody: null,
    description: api.description,
    requestBodyDefinition: null,
    responseBodyDefinition: null
  }
  return `${JSON.stringify(head, null, 2)}\n${SEP}\n${api.script}\n`
}

const writeBoth = (rel, content) => {
  for (const base of [LIVE, SEED]) {
    const target = path.join(base, rel)
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target, content, 'utf8')
  }
  console.log(`wrote ${rel}`)
}

const removeDirBoth = (rel) => {
  for (const base of [LIVE, SEED]) {
    const target = path.join(base, rel)
    if (fs.existsSync(target)) {
      fs.rmSync(target, { recursive: true, force: true })
      console.log(`removed ${target}`)
    }
  }
}

// the smoke group keeps its id/path but the store folder must exist for the new files
for (const g of GROUPS) {
  writeBoth(
    `${g.name}/group.json`,
    JSON.stringify({
      id: g.id,
      name: g.name,
      type: 'api',
      parentId: '0',
      path: g.path,
      createTime: NOW,
      updateTime: NOW,
      createBy: 'admin',
      updateBy: 'admin',
      paths: [],
      options: []
    })
  )
}

removeDirBoth('intel')
// the two MySQL-era smoke scripts are replaced by agri_tables / agri_query
for (const stale of ['smoke/osint_tables.ms', 'smoke/osint_query.ms']) {
  removeDirBoth(stale)
}

for (const api of APIS) {
  writeBoth(`${api.dir}/${api.name}.ms`, render(api))
}

console.log(`\n${APIS.length} APIs generated into ${LIVE} and ${SEED}`)
