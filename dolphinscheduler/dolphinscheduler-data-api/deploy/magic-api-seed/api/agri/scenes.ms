{
  "properties": {},
  "id": "agri_scenes",
  "script": null,
  "groupId": "agri_group",
  "name": "scenes",
  "createTime": 1793881340291,
  "updateTime": 1793881340291,
  "lock": null,
  "createBy": "admin",
  "updateBy": "admin",
  "path": "/scenes",
  "method": "GET",
  "parameters": [
    {
      "name": "page",
      "value": null,
      "description": "页码，从 1 开始",
      "required": false,
      "dataType": "Integer",
      "type": null,
      "defaultValue": "1",
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "size",
      "value": null,
      "description": "每页条数，上限 200",
      "required": false,
      "dataType": "Integer",
      "type": null,
      "defaultValue": "20",
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "regionId",
      "value": null,
      "description": "区域编码，如 R001",
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "satelliteId",
      "value": null,
      "description": "卫星编码，如 SAT01",
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "maxCloudCover",
      "value": null,
      "description": "最大云量百分比",
      "required": false,
      "dataType": "Integer",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "from",
      "value": null,
      "description": "acquisition 起始日期 yyyy-MM-dd",
      "required": false,
      "dataType": "String",
      "type": null,
      "defaultValue": null,
      "validateType": null,
      "error": null,
      "expression": null,
      "children": null
    },
    {
      "name": "to",
      "value": null,
      "description": "acquisition 截止日期 yyyy-MM-dd",
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
  "description": "卫星场景清单：按区域/卫星/云量/时间筛选，返回分页结果与影像地址",
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
var pageNo = page == null || page == '' ? 1 : page.asInt();
var pageSize = size == null || size == '' ? 20 : size.asInt();
if (pageNo < 1) {
  pageNo = 1;
}
if (pageSize < 1) {
  pageSize = 20;
}
if (pageSize > 200) {
  pageSize = 200;
}
var offset = (pageNo - 1) * pageSize;
var where = " where 1 = 1";
if (regionId != null && regionId != '') {
  where = where + " and s.region_id = #{regionId}";
}
if (satelliteId != null && satelliteId != '') {
  where = where + " and s.satellite_id = #{satelliteId}";
}
if (maxCloudCover != null && maxCloudCover != '') {
  where = where + " and s.cloud_cover_pct <= #{maxCloudCover}";
}
if (from != null && from != '') {
  where = where + " and s.acquired_at >= cast(#{from} as timestamp)";
}
if (to != null && to != '') {
  where = where + " and s.acquired_at < cast(#{to} as timestamp) + interval '1 day'";
}
var total = db.agri_sat.selectInt("select count(1) from ods_satellite_scene s" + where);
var rows = db.agri_sat.select("select s.scene_id, s.region_id, r.prefecture, r.agro_zone, s.satellite_id, sat.satellite_name, sat.sensor, sat.resolution_m, s.acquired_at, s.cloud_cover_pct, s.sun_elevation, s.product_level, s.crs, s.bbox, s.file_uri from ods_satellite_scene s left join dim_region r on r.region_id = s.region_id left join dim_satellite sat on sat.satellite_id = s.satellite_id" + where + " order by s.acquired_at desc limit #{pageSize} offset #{offset}");
return { page: pageNo, size: pageSize, total: total, rows: rows };
