// Seeds DataLayer classification/tags and assigns layer/sensitivity/certification to demo tables.
// Run: node scripts/seed-governance.mjs [baseUrl]
const BASE = process.argv[2] || 'http://127.0.0.1:12347'

async function post(path, body) {
  const response = await fetch(BASE + '/data-asset/governance/' + path, {
    method: 'POST',
    headers: { 'X-Debug-User': 'admin', 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  if (!response.ok && response.status !== 409) {
    console.error(path, response.status, text.slice(0, 160))
  }
  return response
}

await post('classification', { name: 'DataLayer', description: '数据分层（ODS/DWD/DWS/ADS/DIM）' })
for (const layer of ['ODS', 'DWD', 'DWS', 'ADS', 'DIM']) {
  await post('tag', { classification: 'DataLayer', name: layer, description: `${layer} 层` })
}

const assets = [
  ['dim_satellite', 'DIM', 'Internal', null, 'Tier4'],
  ['dim_region', 'DIM', 'Internal', null, 'Tier4'],
  ['dim_crop', 'DIM', 'Internal', null, 'Tier4'],
  ['ods_satellite_scene', 'ODS', 'Restricted', null, 'Tier3'],
  ['dwd_ndvi_tile', 'DWD', 'Internal', 'Silver', 'Tier2'],
  ['dws_crop_growth', 'DWS', 'Internal', 'Silver', 'Tier2'],
  ['ads_crop_health', 'ADS', 'Internal', 'Gold', 'Tier1'],
  ['v_region_crop_health', 'ADS', 'Internal', null, 'Tier2']
]

for (const [table, layer, level, cert, tier] of assets) {
  const fqn = `agri_satellite_pg.agri_sat.public.${table}`
  await post('asset/tag-set', { fqn, classification: 'DataLayer', tagFqn: `DataLayer.${layer}` })
  await post('asset/tag-set', { fqn, classification: 'AgriDataSensitivity', tagFqn: `AgriDataSensitivity.${level}` })
  if (cert) {
    await post('asset/tag-set', { fqn, classification: 'Certification', tagFqn: `Certification.${cert}` })
  }
  await post('asset/tag-set', { fqn, classification: 'Tier', tagFqn: `Tier.${tier}` })
}
console.log('governance seed done')
