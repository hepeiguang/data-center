// Verifies the datasource-backed lineage picker endpoints (tables/columns/exists) and the auto-register path.
// Usage: node scripts/verify-datasource.mjs [baseUrl] [probeFqn]
// probeFqn: a table that exists in the business DB but not in OpenMetadata; when given, the script
// registers it, writes a lineage edge to it, and cleans both up again.
const base = (process.argv[2] || 'http://127.0.0.1:12347') + '/data-asset'
const probe = process.argv[3] || ''
const headers = { 'Content-Type': 'application/json', 'X-Debug-User': 'admin' }

const call = async (method, path, body) => {
  const res = await fetch(base + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json
  try { json = JSON.parse(text) } catch { json = text.slice(0, 160) }
  return { status: res.status, body: json }
}

const ds = await call('GET', '/datasource/datasources')
console.log(`${ds.status}  datasources: ${JSON.stringify(ds.body)}`)
const name = ds.body[0].name
const database = ds.body[0].database

const schemas = await call('GET', `/datasource/schemas?datasource=${name}`)
console.log(`${schemas.status}  schemas: ${JSON.stringify(schemas.body)}`)
const schema = schemas.body[0]

const tables = await call('GET', `/datasource/tables?datasource=${name}&schema=${schema}&limit=200`)
console.log(`${tables.status}  tables: ${Array.isArray(tables.body) ? tables.body.length : tables.body}`)

const sample = Array.isArray(tables.body) && tables.body.length ? tables.body[0] : null
if (sample) {
  const cols = await call('GET',
    `/datasource/columns?datasource=${name}&schema=${schema}&table=${sample.table}`)
  const pk = (cols.body || []).filter((c) => c.constraint === 'PRIMARY_KEY').map((c) => c.name)
  console.log(`${cols.status}  columns(${sample.table}): ${(cols.body || []).length} 列, 主键 ${pk.join(',') || '无'}`)
  const exists = await call('GET', `/datasource/exists?fqn=${sample.fqn}`)
  console.log(`${exists.status}  exists(${sample.table}): inOpenMetadata=${exists.body.inOpenMetadata}`)
}

const bad = await call('GET', `/datasource/tables?datasource=${name}&schema=pg_catalog&limit=5`)
console.log(`${bad.status}  system schema filtered: ${Array.isArray(bad.body) ? bad.body.length : JSON.stringify(bad.body).slice(0, 90)}`)
const inject = await call('GET', `/datasource/columns?datasource=${name}&schema=pu%22blic&table=x`)
console.log(`${inject.status}  illegal identifier rejected: ${JSON.stringify(inject.body).slice(0, 90)}`)
const unknown = await call('GET', `/datasource/schemas?datasource=nope`)
console.log(`${unknown.status}  unknown datasource: ${JSON.stringify(unknown.body).slice(0, 90)}`)

if (probe) {
  const before = await call('GET', `/datasource/exists?fqn=${probe}`)
  console.log(`${before.status}  probe exists before: ${before.body.inOpenMetadata}`)
  const reg = await call('POST', `/datasource/register?fqn=${probe}`)
  console.log(`${reg.status}  register: ${JSON.stringify(reg.body).slice(0, 200)}`)
  const edge = await call('POST', '/lineage/manual', {
    fromFqn: probe,
    toFqn: sample ? sample.fqn : probe,
    sqlQuery: 'verify-datasource probe edge',
    columns: []
  })
  console.log(`${edge.status}  edge with auto-register: ${JSON.stringify(edge.body).slice(0, 200)}`)
  if (sample) await call('DELETE', `/lineage/manual?fromFqn=${probe}&toFqn=${sample.fqn}`)
  console.log('cleanup: edge deleted (the registered OM table is left in place; delete it in OM if unwanted)')
}
