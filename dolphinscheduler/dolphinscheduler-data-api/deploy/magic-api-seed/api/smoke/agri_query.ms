{
  "properties": {},
  "id": "smoke_agri_query",
  "script": null,
  "groupId": "smoke_group",
  "name": "agri_query",
  "createTime": 1793881340291,
  "updateTime": 1793881340291,
  "lock": null,
  "createBy": "admin",
  "updateBy": "admin",
  "path": "/query",
  "method": "GET",
  "parameters": [
    {
      "name": "sql",
      "value": null,
      "description": "SELECT 语句，缺省 select 1 as n",
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    }
  ],
  "options": [],
  "requestBody": "",
  "headers": [],
  "paths": [],
  "responseBody": null,
  "description": "在 agri_sat 上执行只读 SELECT（服务端仍受 reject-dml 与 max-rows 约束）",
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
var q = sql;
if (q == null || q == '') {
  q = 'select 1 as n';
}
// only allow select for demo safety
if (!q.trim().toLowerCase().startsWith('select')) {
  exit 400, 'only SELECT allowed';
}
return db.agri_sat.select(q);
