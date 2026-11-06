#!/usr/bin/env bash
# Seeds the demo whitelist entries (all sample tables) and approves them.
# Usage: bash scripts/seed-demo.sh [base-url]
set -euo pipefail

BASE="${1:-http://127.0.0.1:12347}"
USER_HEADER="X-Debug-User: admin"
APPROVER_HEADER="X-Debug-User: owner"

tables=(ods_satellite_scene dwd_ndvi_tile dim_satellite dim_region dim_crop dws_crop_growth ads_crop_health v_region_crop_health)

existing=$(curl -s -H "$USER_HEADER" "$BASE/data-asset/browse/entries")

for table in "${tables[@]}"; do
  if printf '%s' "$existing" | node -e '
    let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{
      const t=process.argv[1];
      try {
        const rows=JSON.parse(s);
        const hit=rows.some(r=>r.status==="ACTIVE" && (r.tables||[]).includes(t));
        process.exit(hit?0:1);
      } catch(e){ process.exit(1) }
    })' "$table"; then
    echo "skip $table (active entry exists)"
    continue
  fi

  body=$(node -e '
    const t=process.argv[1];
    console.log(JSON.stringify({
      datasource:"agri_satellite_pg", database:"agri_sat", schema:"public",
      tables:[t], subjects:{users:["admin"],roles:[],projects:[]},
      pageSize:100, maxRows:5000, exportEnabled:false
    }))' "$table")
  id=$(curl -s -X POST "$BASE/data-asset/browse/entries" -H "$USER_HEADER" \
        -H "Content-Type: application/json" -d "$body" \
      | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{console.log(JSON.parse(s).id)}catch(e){console.log("")}})')
  if [ -n "$id" ]; then
    curl -s -X POST "$BASE/data-asset/browse/entries/$id/approve" -H "$APPROVER_HEADER" >/dev/null
    echo "created+approved $table ($id)"
  else
    echo "failed $table" >&2
  fi
done
