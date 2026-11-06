{
  "properties": {},
  "id": "smoke_select1",
  "script": null,
  "groupId": "smoke_group",
  "name": "select1",
  "createTime": 1793881340291,
  "updateTime": 1793881340291,
  "lock": null,
  "createBy": "admin",
  "updateBy": "admin",
  "path": "/select1",
  "method": "GET",
  "parameters": [],
  "options": [],
  "requestBody": "",
  "headers": [],
  "paths": [],
  "responseBody": null,
  "description": "smoke select 1 on agri_sat (PG)",
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
return db.agri_sat.select('select 1 as n')
