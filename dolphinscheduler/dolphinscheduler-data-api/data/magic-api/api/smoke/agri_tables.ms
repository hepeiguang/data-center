{
  "properties": {},
  "id": "smoke_agri_tables",
  "script": null,
  "groupId": "smoke_group",
  "name": "agri_tables",
  "createTime": 1793881340291,
  "updateTime": 1793881340291,
  "lock": null,
  "createBy": "admin",
  "updateBy": "admin",
  "path": "/tables",
  "method": "GET",
  "parameters": [],
  "options": [],
  "requestBody": "",
  "headers": [],
  "paths": [],
  "responseBody": null,
  "description": "列出 agri_sat 的 public 表与列数",
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
var limit = daMaxRows == null ? 1000 : daMaxRows;
var rows = db.agri_sat.select("""
  select t.table_name,
         t.table_type,
         (select count(1) from information_schema.columns c
           where c.table_schema = t.table_schema and c.table_name = t.table_name) as column_count
  from information_schema.tables t
  where t.table_schema = 'public'
  order by t.table_name
  limit """ + limit);
return rows;
