{
  "properties" : { },
  "id" : "smoke_osint_query",
  "script" : null,
  "groupId" : "smoke_group",
  "name" : "osint_query",
  "createTime" : 1729770000000,
  "updateTime" : 1792904981022,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : "admin",
  "path" : "/osint/query",
  "method" : "GET",
  "parameters" : [ {
    "name" : "sql",
    "value" : null,
    "description" : "only SELECT",
    "required" : false,
    "dataType" : "String",
    "type" : null,
    "defaultValue" : "select 1 as n",
    "validateType" : null,
    "error" : null,
    "expression" : null,
    "children" : null
  } ],
  "options" : [ ],
  "requestBody" : "",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : "{\n    \"code\": -1,\n    \"message\": \"绯荤粺鍐呴儴鍑虹幇閿欒\",\n    \"data\": null,\n    \"timestamp\": 1792904869919,\n    \"executeTime\": 10016\n}",
  "description" : "run SELECT on osint_mysql (demo)",
  "requestBodyDefinition" : null,
  "responseBodyDefinition" : null
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
return db.osint_mysql.select(q);