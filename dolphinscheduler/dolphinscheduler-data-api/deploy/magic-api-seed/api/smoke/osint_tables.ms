{
  "properties" : { },
  "id" : "smoke_osint_tables",
  "script" : null,
  "groupId" : "smoke_group",
  "name" : "osint_tables",
  "createTime" : 1729770000000,
  "updateTime" : 1792904993876,
  "lock" : null,
  "createBy" : "admin",
  "updateBy" : "admin",
  "path" : "/osint/tables",
  "method" : "GET",
  "parameters" : [ ],
  "options" : [ ],
  "requestBody" : "",
  "headers" : [ ],
  "paths" : [ ],
  "responseBody" : "{\n    \"code\": -1,\n    \"message\": \"绯荤粺鍐呴儴鍑虹幇閿欒\",\n    \"data\": null,\n    \"timestamp\": 1792904842238,\n    \"executeTime\": 10010\n}",
  "description" : "list tables in osint via osint_mysql",
  "requestBodyDefinition" : null,
  "responseBodyDefinition" : {
    "name" : "",
    "value" : "",
    "description" : "",
    "required" : false,
    "dataType" : "Object",
    "type" : null,
    "defaultValue" : null,
    "validateType" : "",
    "error" : "",
    "expression" : "",
    "children" : [ {
      "name" : "code",
      "value" : "-1",
      "description" : "",
      "required" : false,
      "dataType" : "Integer",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    }, {
      "name" : "message",
      "value" : "绯荤粺鍐呴儴鍑虹幇閿欒",
      "description" : "",
      "required" : false,
      "dataType" : "String",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    }, {
      "name" : "data",
      "value" : "null",
      "description" : "",
      "required" : false,
      "dataType" : "Object",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    }, {
      "name" : "timestamp",
      "value" : "1792904842238",
      "description" : "",
      "required" : false,
      "dataType" : "Long",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    }, {
      "name" : "executeTime",
      "value" : "10010",
      "description" : "",
      "required" : false,
      "dataType" : "Integer",
      "type" : null,
      "defaultValue" : null,
      "validateType" : "",
      "error" : "",
      "expression" : "",
      "children" : [ ]
    } ]
  }
}
================================
var limit = daMaxRows == null ? 1000 : daMaxRows;
var rows = db.osint_mysql.select("""
  select table_name as tableName
  from information_schema.tables
  where table_schema = database()
  order by table_name
  limit """ + limit);
return rows;