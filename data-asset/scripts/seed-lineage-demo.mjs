// Seeds the demo lineage graph through the manual-lineage API (so the write path itself is exercised).
// Usage: node scripts/seed-lineage-demo.mjs [baseUrl]
const base = process.argv[2] || 'http://127.0.0.1:12347';
const headers = { 'Content-Type': 'application/json', 'X-Debug-User': 'admin' };
const P = 'agri_satellite_pg.agri_sat.public';

const post = async (path, body) => {
  const res = await fetch(base + path, { method: 'POST', headers, body: JSON.stringify(body) });
  const text = await res.text();
  return { status: res.status, body: text.slice(0, 200) };
};
const put = async (path) => {
  const res = await fetch(base + path, { method: 'PUT', headers });
  return { status: res.status, body: (await res.text()).slice(0, 200) };
};

const edges = [
  {
    fromFqn: `${P}.ods_satellite_scene`,
    toFqn: `${P}.dwd_ndvi_tile`,
    sqlQuery: 'insert into dwd_ndvi_tile (tile_id, scene_id, region_id, valid_pixel_ratio)\n'
      + 'select scene_id || \'_\' || tile_row || \'_\' || tile_col, scene_id, region_id, 1 - cloud_cover_pct\n'
      + '  from ods_satellite_scene',
    description: '瓦片切分：按景切分为网格瓦片',
    columns: [
      { from: 'scene_id', to: 'scene_id' },
      { from: 'region_id', to: 'region_id' },
      { from: 'cloud_cover_pct', to: 'valid_pixel_ratio' }
    ]
  },
  {
    fromFqn: `${P}.dwd_ndvi_tile`,
    toFqn: `${P}.dws_crop_growth`,
    sqlQuery: 'insert into dws_crop_growth\n'
      + 'select stat_date, region_id, crop_code, avg(ndvi_mean), count(distinct scene_id)\n'
      + '  from dwd_ndvi_tile group by stat_date, region_id, crop_code',
    description: '按区域/作物聚合日均值',
    columns: [
      { from: 'region_id', to: 'region_id' },
      { from: 'ndvi_mean', to: 'ndvi_mean', function: 'avg' },
      { from: 'scene_id', to: 'sample_scenes', function: 'count' }
    ]
  },
  {
    fromFqn: `${P}.dws_crop_growth`,
    toFqn: `${P}.ads_crop_health`,
    sqlQuery: 'insert into ads_crop_health\n'
      + 'select stat_date, region_id, crop_code, case when ndvi_mean > 0.6 then \'良好\' else \'轻度胁迫\' end\n'
      + '  from dws_crop_growth',
    description: 'NDVI 阈值分档（region_id 走登记键映射）',
    columns: [
      { from: 'crop_code', to: 'crop_code' },
      { from: 'ndvi_mean', to: 'health_level', function: 'case' }
    ]
  }
];

for (const edge of edges) {
  const r = await post('/data-asset/lineage/manual', edge);
  console.log(`${r.status}  edge ${edge.fromFqn.split('.').pop()} -> ${edge.toFqn.split('.').pop()} ${r.body}`);
}

// Column lineage maps the value-carrying columns; region_id is registered here to show the registry branch.
const registry = [
  ['ads_crop_health', 'region_id', 'dws_crop_growth', 'region_id'],
  ['ads_crop_health', 'report_date', 'dws_crop_growth', 'stat_date']
];
for (const [target, keyColumn, source, sourceKeyColumn] of registry) {
  const url = `/data-asset/lineage/keys?targetFqn=${P}.${target}&keyColumn=${keyColumn}`
    + `&sourceFqn=${P}.${source}&sourceKeyColumn=${sourceKeyColumn}`;
  const res = await fetch(base + url, { method: 'POST', headers });
  console.log(`${res.status}  key ${target}.${keyColumn} <- ${source}.${sourceKeyColumn}`);
}

for (const table of ['ods_satellite_scene', 'dwd_ndvi_tile', 'dws_crop_growth', 'ads_crop_health']) {
  const r = await put(`/data-asset/lineage/row-switch?tableFqn=${P}.${table}&enabled=true`);
  console.log(`${r.status}  row-switch ${table}`);
}
