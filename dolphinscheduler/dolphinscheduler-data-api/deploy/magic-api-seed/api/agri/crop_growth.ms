{
  "properties": {},
  "id": "agri_crop_growth",
  "script": null,
  "groupId": "agri_group",
  "name": "crop_growth",
  "createTime": 1793881340291,
  "updateTime": 1793881340291,
  "lock": null,
  "createBy": "admin",
  "updateBy": "admin",
  "path": "/crop-growth",
  "method": "GET",
  "parameters": [
    {
      "name": "region",
      "value": null,
      "description": "区域编码，必填，如 R001",
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
      "description": "作物编码，可选，如 WHEAT",
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
  "description": "作物长势时间序列：NDVI 均值与 P10/P90 参考带，按区域（可选作物）返回",
  "requestBodyDefinition": null,
  "responseBodyDefinition": null
}
================================
if (region == null || region == '') {
  exit 400, 'region is required';
}
var where = " where g.region_id = #{region}";
if (crop != null && crop != '') {
  where = where + " and g.crop_code = #{crop}";
}
var series = db.agri_sat.select("select g.stat_date, g.region_id, r.province, r.prefecture, r.agro_zone, g.crop_code, c.crop_name, c.season, c.growth_days, g.growth_stage, g.ndvi_mean, g.ndvi_p10, g.ndvi_p90, g.sample_scenes from dws_crop_growth g left join dim_region r on r.region_id = g.region_id left join dim_crop c on c.crop_code = g.crop_code" + where + " order by g.stat_date asc");
var summary = db.agri_sat.selectOne("select min(g.stat_date) as first_date, max(g.stat_date) as last_date, count(1) as points, avg(g.ndvi_mean) as avg_ndvi from dws_crop_growth g" + where);
return { region: region, crop: crop, summary: summary, series: series };
