{
  "properties": {},
  "id": "agri_health",
  "script": null,
  "groupId": "agri_group",
  "name": "health",
  "createTime": 1793881340291,
  "updateTime": 1793881340291,
  "lock": null,
  "createBy": "admin",
  "updateBy": "admin",
  "path": "/health",
  "method": "GET",
  "parameters": [
    {
      "name": "region",
      "value": null,
      "description": "区域编码，可选",
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
      "name": "crop",
      "value": null,
      "description": "作物编码，可选",
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
      "name": "reportDate",
      "value": null,
      "description": "报告日期 yyyy-MM-dd，可选",
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
  "description": "分区作物健康度：健康等级、面积、变化率与建议，并给出等级面积分布",
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
var where = " where 1 = 1";
if (region != null && region != '') {
  where = where + " and h.region_id = #{region}";
}
if (crop != null && crop != '') {
  where = where + " and h.crop_code = #{crop}";
}
if (reportDate != null && reportDate != '') {
  where = where + " and h.report_date = cast(#{reportDate} as date)";
}
var rows = db.agri_sat.select("select h.report_date, h.region_id, r.province, r.prefecture, r.agro_zone, h.crop_code, c.crop_name, c.season, h.health_level, h.area_km2, h.change_rate_pct, h.suggestion from ads_crop_health h left join dim_region r on r.region_id = h.region_id left join dim_crop c on c.crop_code = h.crop_code" + where + " order by h.report_date desc, h.region_id, h.crop_code");
var distribution = db.agri_sat.select("select h.health_level, count(1) as records, sum(h.area_km2) as area_km2 from ads_crop_health h" + where + " group by h.health_level order by area_km2 desc");
return { rows: rows, distribution: distribution };
