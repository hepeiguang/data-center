import fs from 'fs'
import path from 'path'

const root = process.argv[2] || 'data/magic-api/api'
const SEP = '================================'
const walk = (dir) =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]
  )

for (const file of walk(root).filter((f) => f.endsWith('.ms'))) {
  const raw = fs.readFileSync(file, 'utf8')
  const idx = raw.indexOf(SEP)
  const body = idx >= 0 ? raw.slice(idx + SEP.length).replace(/^\r?\n/, '') : '(no separator)'
  let head = {}
  try {
    head = JSON.parse(idx >= 0 ? raw.slice(0, idx) : raw)
  } catch (err) {
    head = { path: '(header parse failed)' }
  }
  console.log(`===== ${file}  [${head.method || '?'} ${head.path || '?'}] =====`)
  console.log(body.trimEnd())
  console.log()
}
