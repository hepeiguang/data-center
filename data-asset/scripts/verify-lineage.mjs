// Verifies the manual-lineage / key-mapping / row-lineage endpoints against a running data-asset service.
// Usage: node scripts/verify-lineage.mjs [baseUrl]
const base = process.argv[2] || 'http://127.0.0.1:12347';
const headers = { 'Content-Type': 'application/json', 'X-Debug-User': 'admin' };

const call = async (method, path, body) => {
  const res = await fetch(base + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch { json = text.slice(0, 200); }
  return { status: res.status, body: json };
};

const ODS = 'agri_satellite_pg.agri_sat.public.ods_satellite_scene';
const DWD = 'agri_satellite_pg.agri_sat.public.dwd_ndvi_tile';
const ADS = 'agri_satellite_pg.agri_sat.public.ads_crop_health';
const DIM = 'agri_satellite_pg.agri_sat.public.dim_region';

const steps = [
  ['table lineage', () => call('GET', `/data-asset/lineage/table?fqn=${DWD}&upstreamDepth=2&downstreamDepth=2`)],
  ['create manual edge', () => call('POST', '/data-asset/lineage/manual', {
    fromFqn: DIM,
    toFqn: ODS,
    sqlQuery: 'select ... from ods_satellite_scene s join dim_region d on s.region_id = d.region_id',
    description: 'verify script',
    columns: [{ from: 'region_id', to: 'region_id' }]
  })],
  ['reject unknown column', () => call('POST', '/data-asset/lineage/manual', {
    fromFqn: DIM, toFqn: ODS, columns: [{ from: 'no_such_col', to: 'region_id' }]
  })],
  ['reject self edge', () => call('POST', '/data-asset/lineage/manual', { fromFqn: DWD, toFqn: DWD })],
  ['reject unknown table', () => call('POST', '/data-asset/lineage/manual', {
    fromFqn: 'agri_satellite_pg.agri_sat.public.not_a_table', toFqn: ODS
  })],
  ['register key mapping', () => call('POST', `/data-asset/lineage/keys?targetFqn=${ODS}&keyColumn=region_id&sourceFqn=${DIM}&sourceKeyColumn=region_id`)],
  ['list key mappings', () => call('GET', `/data-asset/lineage/keys?targetFqn=${ODS}`)],
  ['row switches', () => call('GET', '/data-asset/lineage/row-switch')],
  ['resolve row (3 hops)', () => call('GET', `/data-asset/lineage/row/resolve?fqn=${ADS}&key=region_id=R001&key=crop_code=COTTON&depth=3`)],
  ['resolve row (unmapped key)', () => call('GET', `/data-asset/lineage/row/resolve?fqn=${ADS}&key=health_level=%E8%89%AF%E5%A5%BD&depth=2`)],
  ['resolve row (disabled table)', () => call('GET', `/data-asset/lineage/row/resolve?fqn=${DIM}&key=region_id=R001&depth=2`)],
  ['delete manual edge', () => call('DELETE', `/data-asset/lineage/manual?fromFqn=${DIM}&toFqn=${ODS}`)],
  ['lineage after delete', () => call('GET', `/data-asset/lineage/table?fqn=${ODS}&upstreamDepth=1&downstreamDepth=1`)]
];

for (const [name, run] of steps) {
  try {
    const { status, body } = await run();
    const summary = Array.isArray(body)
      ? `list(${body.length}) ${JSON.stringify(body[0] || {}).slice(0, 160)}`
      : JSON.stringify(body).slice(0, 320);
    console.log(`${status}  ${name}: ${summary}`);
  } catch (e) {
    console.log(`ERR   ${name}: ${e.message}`);
  }
}

// clean up the mapping registered by this run
const listed = await call('GET', `/data-asset/lineage/keys?targetFqn=${ODS}`);
for (const row of Array.isArray(listed.body) ? listed.body : []) {
  if (row.source_fqn === DIM) {
    const res = await call('DELETE', `/data-asset/lineage/keys/${row.id}`);
    console.log(`${res.status}  cleanup key ${row.id}`);
  }
}
