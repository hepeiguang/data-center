// End-to-end check of the PostgreSQL-backed sample APIs through the data gateway.
// NOTE: it logs into DolphinScheduler, which replaces any sessionId held in a browser
// (the DS session is single-slot per login) - re-login in the UI afterwards.
const DS = process.env.DS_BASE || 'http://127.0.0.1:12345/dolphinscheduler'
const API = process.env.API_BASE || 'http://127.0.0.1:12346'

const login = async () => {
  const body = new URLSearchParams({
    userName: process.env.DS_USER || 'admin',
    userPassword: process.env.DS_PASS || 'dolphinscheduler123'
  })
  const r = await fetch(`${DS}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  }).then((x) => x.json())
  if (!r.success) throw new Error(`login failed: ${JSON.stringify(r)}`)
  return r.data.sessionId
}

const summarize = (json) => {
  if (json == null || typeof json !== 'object') return String(json).slice(0, 120)
  if (Array.isArray(json)) return `array rows=${json.length}`
  if (typeof json.total === 'number' && Array.isArray(json.rows)) {
    return `total=${json.total} rows=${json.rows.length} sample=${JSON.stringify(json.rows[0] || {}).slice(0, 150)}`
  }
  if (Array.isArray(json.rows)) {
    const dist = Array.isArray(json.distribution) ? ` distribution=${json.distribution.length}` : ''
    return `rows=${json.rows.length}${dist} sample=${JSON.stringify(json.rows[0] || {}).slice(0, 150)}`
  }
  if (Array.isArray(json.series)) {
    return `points=${json.series.length} summary=${JSON.stringify(json.summary)}`
  }
  return JSON.stringify(json).slice(0, 180)
}

const main = async () => {
  const sid = await login()
  const magic = { 'magic-token': sid }
  const gw = { token: sid }

  const reloaded = await fetch(`${API}/magic/web/reload`, { method: 'GET', headers: magic }).then((r) => r.json())
  console.log('reload resources:', JSON.stringify(reloaded).slice(0, 80))

  const tree = await fetch(`${API}/magic/web/resource`, { method: 'POST', headers: magic }).then((r) => r.json())
  const apiRoot = tree.data && tree.data.api ? tree.data.api : tree.api
  const groups = (apiRoot && apiRoot.children ? apiRoot.children : []).map(
    (g) => `${g.node.name}(${(g.children || []).length})`
  )
  console.log('magic api groups:', groups.join(' '))

  const sources = await fetch(`${API}/data-api/datasources`, { headers: gw }).then((r) => r.json())
  console.log('data-api datasources:', JSON.stringify(sources.keys || sources))
  const ping = await fetch(`${API}/data-api/datasources/agri_sat/ping`, { headers: gw }).then((r) => r.json())
  console.log('ping agri_sat:', JSON.stringify(ping).slice(0, 140))

  const calls = [
    ['smoke select1', '/data-api/gateway/smoke/select1'],
    ['smoke tables', '/data-api/gateway/smoke/tables'],
    [
      'smoke query',
      `/data-api/gateway/smoke/query?sql=${encodeURIComponent('select count(1) as scenes from ods_satellite_scene')}`
    ],
    ['agri scenes', '/data-api/gateway/agri/scenes?size=3'],
    ['agri scenes filtered', '/data-api/gateway/agri/scenes?maxCloudCover=20'],
    ['agri crop-growth', '/data-api/gateway/agri/crop-growth?region=R001'],
    ['agri crop-growth crop', '/data-api/gateway/agri/crop-growth?region=R001&crop=CORN'],
    ['agri health', '/data-api/gateway/agri/health'],
    ['agri health region', '/data-api/gateway/agri/health?region=R002'],
    ['EXPECT-400 crop-growth missing region', '/data-api/gateway/agri/crop-growth'],
    ['EXPECT-400 reject non-select', `/data-api/gateway/smoke/query?sql=${encodeURIComponent('delete from dim_crop')}`]
  ]

  let failures = 0
  for (const [label, url] of calls) {
    const r = await fetch(`${API}${url}`, { headers: gw })
    const text = await r.text()
    let parsed
    try {
      parsed = JSON.parse(text)
    } catch (err) {
      parsed = text
    }
    // the two guard cases must answer 400; anything else must be a magic success envelope
    if (label.startsWith('EXPECT-400')) {
      const okNegative = r.status === 400 && parsed && parsed.code === 400
      console.log(`${okNegative ? 'ok(400)' : 'FAIL '} [${label}] http=${r.status} body=${JSON.stringify(parsed).slice(0, 130)}`)
      if (!okNegative) failures += 1
      continue
    }
    const code = parsed && typeof parsed === 'object' && 'code' in parsed ? parsed.code : r.status
    const bad = r.status >= 400 || (typeof code === 'number' && code !== 1)
    if (bad) failures += 1
    console.log(`${bad ? 'FAIL' : 'ok   '} [${label}] http=${r.status} -> ${summarize(parsed)}${bad ? ' body=' + JSON.stringify(parsed).slice(0, 200) : ''}`)
  }

  console.log(`\n${failures === 0 ? 'ALL GREEN' : failures + ' FAILURE(S)'}`)
  process.exit(failures === 0 ? 0 : 1)
}

main().catch((e) => {
  console.error('ERROR:', e.message)
  process.exit(1)
})
