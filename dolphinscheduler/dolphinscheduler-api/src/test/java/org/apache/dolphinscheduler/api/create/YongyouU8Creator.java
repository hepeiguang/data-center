package org.apache.dolphinscheduler.api.create;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.snowflake.client.jdbc.internal.google.api.client.json.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class YongyouU8Creator {
//        private static String damUrl = "http://172.41.133.91:12345/";
    private static String damUrl = "http://localhost:12345/";
    private static String projectCode = "161098458353920";
    private static String targetDatasourceId = "1";
    private static String sessionId = "40813dac-49dc-418d-a273-8e2dac65d078";

    private static ContentType TEXT_PLAIN_UTF_8 = ContentType.create("text/plain", "UTF-8");
    private static String damJdbcUrl = "jdbc:mysql://172.41.133.91:3306/dam?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useTimezone=true";
    private static String damUserName = "dam";
    private static String damPassword = "Dam!@0001";
    private static final Pattern SQL_KEYWORD_PATTERN = Pattern.compile("^(desc|key|order|group|select|from|where|having|by|and|or|not|as|on|join|inner|outer|left|right|union|intersect|except|create|drop|alter|insert|update|delete|table|column|index|primary|foreign|references|constraint|default|check|unique|clustered|nonclustered|with|option|values|set|into|all|any|some|exists|between|in|like|is|null|case|when|then|else|end|begin|try|catch|throw|goto|return|break|continue|declare|exec|execute|open|fetch|close|deallocate|print|raiserror|begin|commit|rollback|transaction|save|checkpoint|backup|restore|dbcc|kill|shutdown|reconfigure|configure|sp_|xp_|fn_|sys|information_schema|all|any|between|cross|except|foreign|group|having|in|inner|intersect|into|is|join|like|not|null|on|or|order|outer|select|some|union|where|add|all|alter|analyze|and|as|asc|asensitive|before|between|bigint|binary|blob|both|by|call|cascade|case|change|char|character|check|collate|column|condition|connection|constraint|continue|convert|create|cross|current_date|current_time|current_timestamp|current_user|cursor|database|databases|day_hour|day_microsecond|day_minute|day_second|dec|decimal|declare|default|delayed|delete|desc|describe|deterministic|distinct|distinctrow|div|double|drop|dual|each|else|elseif|enclosed|escaped|exists|exit|explain|false|fetch|float|float4|float8|for|force|foreign|from|fulltext|goto|grant|group|having|high_priority|hour_microsecond|hour_minute|hour_second|if|ignore|in|index|infile|inner|inout|insensitive|insert|int|int1|integer|interval|into|is|iterate|join|key|keys|kill|leading|leave|left|like|limit|lines|load|localtime|localtimestamp|lock|long|longblob|longtext|loop|low_priority|match|mediumblob|mediumint|mediumtext|middleint|minute_microsecond|minute_second|mod|modifies|natural|not|no_write_to_binlog|NULL|numeric|on|optimize|option|optionally|or|order|out|outer|outfile|precision|primary|procedure|purge|raid0|read|reads|real|references|regexp|release|rename|repeat|replace|require|restrict|return|revoke|right|rlike|schema|schemas|second_microsecond|select|sensitive|separator|set|show|smallint|soname|spatial|specific|sql|sql_big_result|sql_calc_found_rows|sql_small_result|sqlexception|sqlstate|sqlwarning|ssl|starting|straight_join|table|terminated|then|to|trailing|trigger|true|undo|union|unique|unlock|unsigned|update|usage|use|using|utc_date|utc_time|utc_timestamp|values|varbinary|varchar|varcharacter|varying|when|where|while|with|write|x509|xml|year_month|zerofill|action|admin|after|aggregate|allocation|analyse|array|asensitive|at|authors|auto_increment|avg_row_length|avg|backward|bit|bool|boolean|both|cascade|cascaded|catalog_name|chain|characteristics|character_set_catalog|character_set_name|character_set_schema|class_origin|client|clobber|code|collation_catalog|collation_name|collation_schema|column_name|command_class|commit|committed|condition_number|connection|const|constraint_catalog|constraint_name|constraint_schema|constructor|contains|continue|convert|copy|corr|corresponding|cpu|cube|cume_dist|current|current_schema|cursor_name|cycle|data|database|date|datetime|day|deallocate|dec|decimal|default|deferrable|deferred|defined|definer|degree|delay_key_write|delayed|delete|delimiter|delimiters|dense_rank|deterministic|diagnostics|directory|disable|disallow|disconnect|dispatch|distinct|do|document|domain|double|drop|dual|dynamic|dynamic_function|dynamic_function_code|each|element|else|elseif|enable|enclosed|encoding|encrypted|end|end-exec|enum|equals|error|escape|escaped|every|except|exception|exchange|exclude|exclusive|exec|execute|existing|expansion|expire|explain|export|expression|extension|external|extract|false|fetch|fields|file|first|first_value|fixed|following|follows|for|force|format|found|free|freeze|from|full|function|fusion|general|generated|geometry|geometrycollection|get|global|go|goto|grant|grants|greatest|group|grouping|groups|handler|hash|header|having|high_priority|hold|host|hosts|hour|identifier|identity|if|ignore|import|in|index|indicator|infile|inout|insensitive|insert|insert_method|install|instead|instr|integer|int|intersect|interval|into|invoker|is|isnull|isolation|issuer|iterate|join|json|key|keys|kill|label|lag|language|last|last_value|lateral|leading|least|leave|left|length|less|level|like|limit|linear|lines|linestring|listagg|listen|load|local|localtime|localtimestamp|location|locator|lock|locked|log|logfile|logs|longblob|longtext|loop|low_priority|master|match|materialized|max_rows|master_ssl_verify_server_cert|max|mediumblob|mediumint|mediumtext|memory|merge|message_length|message_octet_length|message_text|migrate|min_rows|min|minute|mod|mode|modifies|modify|multilinestring|multipoint|multipolygon|multiset|mumps|name|names|national|natural|nchar|nclob|nested|new|next|no|none|not|notnull|nowait|nullif|null|number|numeric|object|octet_length|of|off|offset|oids|old|on|one|only|open|optimize|option|options|or|order|ordinality|others|out|outer|outfile|output|over|overlaps|overlay|owner|pack_keys|pad|parameter|parameter_mode|parameter_name|parameter_ordinal_position|parameter_specific_catalog|parameter_specific_name|parameter_specific_schema|partial|partition|pascal|password|path|percent_rank|percentile_cont|percentile_disc|placing|plan|plugin|plugins|port|position|preceding|precision|prepare|preserve|primary|prior|privileges|procedure|process|processlist|profile|profiles|proxy|public|purge|quarter|query|quick|raise|random|range|rank|read|reads|real|reassign|rebuild|record|recursive|reduce|references|regexp|regr_avgx|regr_avgy|regr_count|regr_intercept|regr_r2|regr_slope|regr_sxx|regr_sxy|regr_syy|reindex|relative|relaylog|release|rename|repeat|repeatable|replace|replication|require|reset|resignal|restore|restrict|result|return|returned_cardinality|returned_length|returned_octet_length|returned_sqlstate|returns|revoke|right|role|routines|row_count|row_format|row_number|row|rows|rule|savepoint|schedule|schema|schema_name|schemas|scope|scope_catalog|scope_name|scope_schema|scroll|search|second|section|security|select|self|sensitive|separator|sequence|serializable|server_name|session|session_user|set|share|shared|show|shutdown|signal|similar|simple|size|smallint|snapshot|some|soname|soundex|source|space|spatial|specific|specifictype|sql|sql_big_result|sql_cache|sql_calc_found_rows|sql_no_cache|sql_small_result|sql_state|sql_warning|sqlexception|sqlstate|sqlvolume|sqrt|ssl|standalone|start|starting|starts|statement|static|statistics|status|stddev_pop|stddev_samp|stdin|stdout|storage|straight_join|strict|string|striped|structure|style|subclass_origin|subject|substring|successful|sum|superuser|table_name|table|tables|tablespace|temporary|temptable|terminated|text|than|then|ties|time|timestamp|timezone_hour|timezone_minute|tinyblob|tinyint|tinytext|to|trailing|transaction|translate|translation|treat|trigger|triggers|trim|true|truncate|type|uescape|unbounded|uncommitted|undefined|under|undo|unencrypted|union|unique|unknown|unlisten|unlock|unsigned|until|update|usage|use|user|user_resources|using|utc_date|utc_time|utc_timestamp|vacuum|valid|validate|validator|value|values|var_pop|var_samp|varbinary|varchar|varcharacter|variable|variables|varying|verbose|version|view|virtual|visible|volatile|wait|when|where|while|width_bucket|window|with|within|without|work|wrapper|write|xml|xmlattributes|xmlbinary|xmlcast|xmlcomment|xmlconcat|xmldocument|xmlelement|xmlexists|xmlforest|xmliterate|xmlnamespaces|xmlparse|xmlpi|xmlquery|xmlroot|xmlschema|xmlserialize|xmltable|xmltext|xmlvalidate|year|zone)$", Pattern.CASE_INSENSITIVE);
    private static String server = "172.41.133.84";
    private static String database = "UFDATA_027_2025";
    private static String user = "sa";
    private static String password = "admin@123";
    private static String yyDbBaseUrl = "jdbc:sqlserver://%s:%s;" +
            "databaseName=%s;" +
            "user=%s;" +
            "password=%s;" +
            "encrypt=false;" +
            "trustServerCertificate=true;" +
            "sslProtocol=TLSv1;" +
            "loginTimeout=30;" +
            "sendTimeAsDatetime=false;" +
            "sendStringParametersAsUnicode=true;";
    private static Set<String> failedTables = new HashSet<>();
    private static Map<String, Map<String, String>> dbTablePrimaryKeyMap = new HashMap<>();
    private static Map<String, String> caccstartendYearMap = new HashMap<>();
    // 存储不重要的表, 存储数据量大，但是不重要的表
    private static Map<String, List<String>> dbUnimportantTableMap = new HashMap<>();

    // 数量小，但是重要的表
    private static Map<String, List<String>> dbImportantTableMap = new HashMap<>();

    // 无用的表，排除的表
    private static Map<String, List<String>> dbUselesTableMap = new HashMap<>();

    private static List<String> excludeDbs = new ArrayList<>();
    private static List<String> includeDbs = new ArrayList<>();

    static {
        String ufSystem = "{\"CN_ModuleCatalog\":\"ID\",\"CN_SubModuleCatalog\":\"SubModuleKey\",\"CN_Version\":\"ID\",\"GL_BTrade\":\"ctrade_name,LocaleId\",\"GL_CodeCheckStatus\":\"sAccount\",\"GL_CodeClass\":\"\",\"GL_DPrecode\":\"\",\"OMI_RecordExtMark\":\"ID\",\"open_api_lsfx_versiontag\":\"c_token\",\"openapi_use\":\"c_token\",\"ProjectStandard\":\"ID\",\"UA_AALog_base\":\"\",\"UA_BackupPath\":\"cPlan_Id\",\"UA_Chart\":\"ID\",\"UA_Control\":\"\",\"UA_ControlEx\":\"cAuth_Id,cRepellent\",\"UA_Flow\":\"ID\",\"UA_Log\":\"\",\"UA_MenuRulePLugin\":\"Key\",\"UA_MutexAuthItem\":\"ID,cAuth_ID\",\"UA_NavigationNode_Field\":\"Name,Property\",\"UA_NavigationNode_FieldEx\":\"Name,Property,UserId\",\"UA_Password\":\"iMaxDay\",\"UA_PortalAutoExitExincludeTasks\":\"cAuth_Id\",\"UA_PortalLang\":\"cCaption\",\"UA_ReplStru\":\"\",\"UA_ReplStru_order\":\"cName\",\"UA_SupAuths\":\"\",\"ua_Task_Common\":\"cTaskId\",\"UA_ToNextYear_base\":\"iNo,cTblId,localeid\",\"UA_UpgradeBatch\":\"iVer,iIndex\",\"UA_Version\":\"iVer\",\"UFMenu_Business_Lang\":\"\"}";
        dbTablePrimaryKeyMap.put("UFSystem", JSONUtils.parseObject(ufSystem, new TypeReference<Map<String, String>>() {
        }));

        String ufData = "{\"ZHTJRD_WIN_4U22VFGCLLU6\":\"\",\"ZHTJRD_WIN_4U22VFGCLLU5\":\"\",\"ZHTJRD_WIN_4U22VFGCLLU2\":\"\",\"ZHTJRD_HUANGLI_PC0\":\"\",\"ZHTJFP_WIN_4U22VFGCLLU6\":\"\",\"ZHTJFP_WIN_4U22VFGCLLU5\":\"\",\"ZHTJFP_WIN_4U22VFGCLLU2\":\"\",\"ZHTJFP_HUANGLI_PC0\":\"\",\"ZHTJFP_4CB2KM6GIUBV9YA0\":\"\",\"AA_ColumnModifyForExtend\":\"cFld\",\"AA_ColumnSetExtend\":\"ckey,cUserId\",\"AA_ExtendDefineMakeVouch_base\":\"cMakeVouchID\",\"AA_ExtendDefineMakeVouch_Sub\":\"cMakeVouchID,cType\",\"AA_Medal\":\"cmedcode\",\"AA_Medal_Config\":\"autoid\",\"AA_PortalIndicatorView_Default\":\"ScenarioID,ViewID\",\"AA_VoucherTemplate_MF\":\"cfld,CardNum\",\"Ap_BillAge\":\"cNum,cFlag\",\"ap_closebill_bak\":\"iID\",\"ap_closebills_bak\":\"ID\",\"AP_Detail_bak\":\"Auto_ID\",\"ap_Detail_bak20190719\":\"Auto_ID\",\"ap_digset_base\":\"\",\"ap_vouch_bak\":\"cLink\",\"ap_vouchs_bak\":\"Auto_ID\",\"Ar_Detail_bak\":\"Auto_ID\",\"ARVouchRule\":\"ID\",\"AuditBizComponent\":\"cBizObjectID\",\"AuditBizEvents_Lang\":\"cBizEventId,Lang_ID\",\"AuditBizObjects_Lang\":\"cBizObjectId\",\"AuditBizPriParams_Lang\":\"cBizEventId,cParamName\",\"AuditObjectRevokeAuth\":\"RevokeAuthID\",\"AuditPlugins_Lang\":\"cPluginId\",\"bas_systemsettingnbr\":\"\",\"BatchTask\":\"cVoumenu_id,cvouchertype\",\"BatchTaskSub\":\"cVType\",\"BG_Archive_SYS\":\"guid\",\"BG_ColumnDatatypeContra\":\"\",\"Bombuttonset\":\"bombuttonflag\",\"BW_AlgoPlan_Lang\":\"PlanId\",\"Ca_AmoTypeName\":\"cAmoType\",\"Ca_Ex_RebuildIndex\":\"TabName\",\"Ca_ExcelInputMap_base\":\"sType,cFieldName\",\"ca_ExcelInputMap_lang\":\"sType,cFieldName\",\"Ca_JZ_YearBegin_Def\":\"TabName\",\"CA_ReInitStatus\":\"ItemName\",\"Ca_UpdateInvCodeToPartID\":\"TableName\",\"Ca_Visio\":\"cMapID\",\"Ca_Visio_Condition\":\"cMapID,cObjCode,cCondType\",\"Ca_Visio_object\":\"cMapID,cObjID\",\"Ca_Visio_Rel\":\"cMapID,cNextLineID\",\"cClassName_Lang\":\"cClassName\",\"CM_Balance\":\"cBalanceID\",\"CM_CMInterface\":\"Modifytamp\",\"CM_Contract_B\":\"GUID\",\"CM_Contract_C\":\"GUID\",\"CM_Contract_Caluse_C\":\"ID\",\"CM_Contract_Item_C\":\"ID\",\"CM_DD\":\"\",\"CM_ExecInterface\":\"cOrdersID\",\"CM_ExecInterface_bak\":\"cOrdersID\",\"CM_ExecuteBill\":\"cExecID\",\"CM_ExecuteBills\":\"cRowID\",\"CM_Kind\":\"AutoID\",\"CM_MS\":\"strFld\",\"CM_Task\":\"ID\",\"CM_TrackClueUser\":\"cClueCode,cUserName\",\"CM_TrackField\":\"cClueCode,cFieldID,cFieldName\",\"CM_TrackFieldUser\":\"\",\"CM_Vouchers\":\"CardNumber\",\"CN_Version\":\"Version\",\"Code_LogDetail\":\"iLogID,cModifyField\",\"Crm_MAComponentRefFields\":\"componentid,cRefCode\",\"CurrentStock\":\"AutoID\",\"currentstock_bak_20190720\":\"AutoID\",\"DelDutyClassCheck\":\"PK_DelDutyClassCheck\",\"DimensionInScenario\":\"DimensionCode\",\"EA_Action\":\"EAA_Name\",\"EAO_Node\":\"EAO_ID\",\"EAO_NoteML\":\"EAO_ID\",\"EAON_Template\":\"EAONT_ID\",\"EAONT_NoteML\":\"EAONT_ID\",\"EAS_Feedback_Message\":\"EAA_ID\",\"EB_ButtonAuth\":\"autoID\",\"EB_Company\":\"cCompanyCode\",\"EB_EBCompanyRelation\":\"cEBPlatForm,cCompanyCode,cCompany_Code\",\"eb_goldtax\":\"id\",\"eb_goldtaxfield\":\"id\",\"EB_IndexHomeNews\":\"autoID\",\"eb_splitcode\":\"identityID\",\"eb_voucheritems\":\"cardnum,cardsection,fieldname\",\"eb_vouchertemplates\":\"vt_cardnumber\",\"EB_WlbPlatChannels\":\"cEBPlatForm\",\"EB_WlbSPCode\":\"cCompanyCode,cCompany_Code\",\"ex_ApportionRule\":\"vouchtype,ChildField\",\"ex_bondedbilltype\":\"ibondedbilltype\",\"ex_btmvoucher\":\"cbtmvcardnum\",\"EX_BUSTACHE_DEMO\":\"CBUSTACHECODE\",\"ex_ciqinvtype\":\"iciqinvtype\",\"ex_idemandtype\":\"idemandtype\",\"EX_LinkedRelation\":\"SourceVouchType,QueryType,TargetVouchType\",\"EX_LinkedVouch\":\"vouchtype\",\"Ex_OrderDateField\":\"cfieldcode\",\"ex_process_menudefine\":\"menuid\",\"ex_vouch_authdefine\":\"vouchguid\",\"Exp_Func_Lang\":\"cFuncId\",\"Exp_FuncDes_Lang\":\"cFuncId\",\"Exp_FuncGroup_Lang\":\"cGroupId\",\"Exp_FuncReturn_Lang\":\"cFuncId\",\"Exp_Parameter_Lang\":\"cParamId\",\"Exp_ParamType_Lang\":\"cParamId\",\"fa_CheckConfig\":\"sID\",\"FA_LabelDesign\":\"cLabelCode\",\"FA_LabelType\":\"cLableTypeCode\",\"FB_Form_LayoutTemplate\":\"tStamp\",\"fc_OpColor\":\"Flag\",\"fc_UpgradeLog\":\"autoid\",\"Flt_QuickConditionSet\":\"FilterID,ItemName\",\"GL_accvouch_20190709\":\"i_id\",\"GL_accvouch_add\":\"i_id\",\"GL_fltDefineMeta_lang\":\"FilterID\",\"gl_mend_bak\":\"pubufts\",\"GL_MRColumnDic_Lang\":\"cKey,cField\",\"GL_MRColumnDic_Main\":\"cKey,cField\",\"GL_MRColumnDic_Set\":\"iReportID,iColPos\",\"GL_MRReport_Main\":\"iID\",\"GL_MRReportOption\":\"iReportID\",\"GL_MRShowItem_Main\":\"iReportID,cItemType\",\"GL_ProcedureDic\":\"cProcedureName\",\"GL_RepPrintTemplate_log\":\"cOperator\",\"GSP_UnionChangeInfo\":\"ID\",\"hr_bd_BusSet\":\"cBusinessID\",\"HR_BD_CalcRuleObjFldReg\":\"\",\"HR_BD_CalcRuleObjReg\":\"cTableCode\",\"HR_BD_DeptAuthSet_lang\":\"cfid,LocaleID\",\"hr_bd_mcitemassign\":\"PK_Item\",\"hr_bd_mctableassign\":\"PK_Table\",\"HR_BD_QueryParaItem\":\"iParaID\",\"HR_BD_QueryStatWay\":\"vName\",\"HR_BD_RuleAndTask\":\"TaskId\",\"hr_bd_rulecontact\":\"cRuleId,cItem\",\"HR_Bd_TableDataType\":\"TableName\",\"Hr_bd_VoucherTable\":\"id\",\"HR_CT010\":\"ccodeID\",\"hr_ct055\":\"ccodeID\",\"HR_CT803\":\"ccodeID\",\"HR_FieldControlRelation\":\"cCardNum,cFieldName \",\"HR_HI_EnterCarryField\":\"PK_HR_HI_EnterCarryField\",\"HR_PF_EnumsCalcRule\":\"tStamp\",\"hr_sys_function_Base\":\"cAuth_ID\",\"hr_sys_sub_Base\":\"cSub_id\",\"hr_sys_ValuesMapping\":\"pk_id\",\"hr_tm_ArithmeticControl\":\"ExecProc\",\"hr_tm_EastRiverBasicSet\":\"vPassword\",\"HR_TM_ICCardParameters\":\"cDataKey\",\"HR_TM_LossNewMachine\":\"cMachine\",\"hr_tm_OverTimeCardExcursion\":\"BeginCardAhead\",\"hr_tm_Paras\":\"nTypeCode\",\"hy_DZ_ECN_SELECT\":\"ufts\",\"HY_LinkDesc\":\"desc\",\"HY_NeedUpdLinkField\":\"\",\"HY_TM_BarCodeSource_base\":\"cSrcID,cSrcName\",\"HYKCFile_Config_base\":\"FrmCaption\",\"HYKCFile_Items_base\":\"DisName\",\"IA_EstimateType\":\"\",\"IA_HisInventory\":\"\",\"IA_HisOption\":\"ID\",\"IA_HisWarehouse\":\"\",\"IA_PerdealInventory\":\"cinvcode\",\"Ia_PerDealWhDep\":\"cWhDepCode\",\"IA_PZEmptyAccountHUANGLI_PC0\":\"cVouType\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU2\":\"cVouType\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU3\":\"cinvcode\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU4\":\"cVouType\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU5\":\"cinvcode\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU6\":\"cinvcode\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU7\":\"cVouType\",\"IA_PZSelectBillListWIN_4U22VFGCLLU6\":\"AutoID\",\"IA_PZSelectBills4CB2KM6GIUBV9YA0_temp\":\"PSVSID\",\"IA_PZSelectBillsWIN_4U22VFGCLLU6\":\"AutoID\",\"IA_RepFilterCondition\":\"AutoID\",\"IA_Restore_Data\":\"AutoID\",\"IA_ST_UnAccountVouch01_bak20190716\":\"IDUN\",\"IA_ST_UnAccountVouch11\":\"IDSUN\",\"IA_ST_UnAccountVouch11_bak20190716\":\"IDSUN\",\"IM_BusType\":\"iimbustype\",\"IM_LinkedRelation\":\"LinkedSP\",\"im_paymoneystate\":\"ipaymoneystate\",\"IM_ReferGroupBy\":\"referkey,fname\",\"IM_ReferGroupBy_lang\":\"referkey,fname\",\"IM_VouchInfo\":\"vouchtype\",\"JSMX1_WIN_4U22VFGCLLU6\":\"cVenCode,发票标识\",\"KC_Ascii_Config\":\"ID\",\"KC_BarCodeFlowProject\":\"cprojectcode\",\"KC_VoucherPkMap\":\"cVouchType\",\"KC_WireMenu_base\":\"ID\",\"LogoutTable_Base\":\"cEnTableName\",\"lotnumberPrefabricate_base\":\"cdatasrccaption\",\"lotnumberPrefabricate_lang\":\"cdatasrccaption\",\"MFScriptSettings\":\"cScriptName\",\"mom_Datasource\":\"cMetCode\",\"mom_Dimension\":\"cDimCode\",\"mom_Metric\":\"cMetCode\",\"mom_MetricDimension\":\"cDimCode,cMetCode\",\"mps_lstdeldata\":\"DocDId\",\"NB_AreaCode_City\":\"AREA_CODE\",\"NB_AreaCode_province\":\"AREA_CODE\",\"NB_BankExt\":\"BankName,Version\",\"NB_BankNameConfig\":\"cBankName\",\"NB_BankProfile\":\"cUniqueID\",\"NB_BankProfileRef\":\"\",\"NB_InputConfig\":\"cKey\",\"NB_Task\":\"ID\",\"NE_AirPortCity\":\"ID\",\"NE_CountryCity\":\"ID\",\"NE_Extend\":\"DocType\",\"NE_OperationType_PrintTemplate\":\"ID\",\"NE_PzSet_ZYTREE\":\"ID\",\"NE_ThirdTicketSource\":\"TicketSource\",\"NotifyObjectName_lang\":\"cBizObjectId\",\"Pe_GetDataOpt\":\"cOptID\",\"Pe_GetDataOptDetails\":\"cSelID\",\"Pe_SysInfo\":\"autoID\",\"Pe_Szdetail\":\"AutoID\",\"Pe_SzItem\":\"AutoID\",\"Pe_Version\":\"AutoID\",\"PFFCGoToVoucherConfig\":\"condition\",\"PFFCSubSysConfig\":\"autoid\",\"PL_CO_SPVouchType\":\"cVouType,cBusType\",\"pl_co_YearBegin_Def\":\"TabName\",\"Pm_VouchType_Base\":\"cTypeCode\",\"PrintPolicy_vch_Detail\":\"\",\"PurBillVouch_bak1\":\"PBVID\",\"PurBillVouchs_bak1\":\"ID\",\"PurSettleVouch_bak\":\"PSVID\",\"PurSettleVouchs_bak\":\"ID\",\"QM_RANDORPROJECTS\":\"autoid\",\"qmchecktype_base\":\"CCHECKTYPECODE\",\"rb_Current_TabTree\":\"TabID\",\"rb_Localize\":\"TextKey,CultureCode\",\"rb_Monitoring\":\"ID\",\"rb_UserRoles\":\"UserID\",\"rb_Versions\":\"\",\"RefDefine\":\"cID\",\"RefDefines\":\"cID,cReplaceStr\",\"REFERARCHIVEMAPPING\":\"\",\"referarchives_base\":\"\",\"referarchives_local\":\"\",\"REFERBIZFIELDS\":\"REFERFIELDID\",\"REFERFIELDS\":\"ID\",\"referfilterfields\":\"ID\",\"REFERRETURNFIELDS\":\"ID\",\"RP_CurrencyValue\":\"i_id\",\"RP_NoteType_base\":\"cTypeCode\",\"RP_SysSet\":\"ID\",\"RP_TableField\":\"cTableName,cFieldName\",\"Rpt_CustomEvents_Base\":\"ReportName,ActionClass,Caption\",\"Rpt_FldDEF_Base_Bak\":\"\",\"Rpt_Glbdef_Base_backup_by_CA\":\"ID,LocaleID\",\"rpt_glbdef_base_bak\":\"ID,LocaleID\",\"Rpt_PrintSetting\":\"ID\",\"SA_GoldTaxField\":\"id,cField\",\"SA_GoToVoucherConfig\":\"autoid\",\"SA_MenuConfig\":\"autoid\",\"SA_OrderAscendConfig\":\"cRptName,iTabIndex\",\"SA_PopMenuConfig\":\"autoid\",\"SA_PrintOption\":\"cmenuid\",\"SA_RowColChange\":\"autoid\",\"SA_UnVerifyCheck\":\"autoid\",\"SaleBillVouch\":\"SBVID\",\"SCM_CheckTrackbUsed\":\"autoid\",\"SCM_ClearStock\":\"autoid\",\"ScmAscendInfo_lang\":\"cVouchtypeCode\",\"ScmAscendLinkInfo\":\"cVouchtypeCode,CNextVouchtypeCode\",\"ScmBusType_lang\":\"cVouchtypeCode\",\"ScmTypeTran\":\"id\",\"SCMVouchRelation\":\"autoid\",\"scmvouchrelationForLinkID\":\"clinkdetailtbl\",\"scmVouchRelInfo\":\"clinkdetailtbl\",\"SNPrefabricate_base\":\"cdatasrccaption\",\"SNPrefabricate_lang\":\"cdatasrccaption\",\"ST_DefineSet\":\"id\",\"ST_VMIUsedRule\":\"AutoID\",\"Table_EngineParam\":\"\",\"Table_Template_CustomCondition\":\"cBizObjectId,Variable\",\"Table_Template_Lang\":\"ID\",\"Table_TemplateKind_Lang\":\"KindID\",\"TaskExtendFieldDef\":\"TaskTypeID,ExtendFieldName\",\"TaskProcessorDef\":\"ProcessorName\",\"TaskTypeDef\":\"TaskTypeID\",\"TaskTypeDef_Lang\":\"TaskTypeID\",\"tc_backfill_setting\":\"attr_name\",\"tc_copyvouch\":\"cKey\",\"tc_copyvouch_d\":\"cKey,cFld\",\"tc_obj_rel_profile\":\"\",\"tc_object_stage_option\":\"\",\"tc_procInvok_log\":\"\",\"tc_stage_path_option\":\"\",\"tc_tmp_duplication\":\"code\",\"tc_ufrefresh\":\"\",\"TI_InvoiceAge\":\"cNum\",\"TI_InvoiceImportSet\":\"autoid\",\"tmpIAJZ\":\"ID\",\"TSS_20241021003322_HR_BD_RPT_FILE_2347\":\"iRptCode\",\"TSS_20241021003322_hr_hi_person_805\":\"cPsn_Num\",\"TSS_HR_BD_RPT_FILE\":\"iRptCode\",\"tss_level\":\"name\",\"U8Doc_Templates\":\"cDocCode,cVersion\",\"U8LoginProcess\":\"cProcessId,cTaskId\",\"UA_Account_Ex\":\"iSysID\",\"UA_Account_sub_Ex\":\"\",\"UA_AccountDatabase_Ex\":\"cAcc_Id\",\"UA_Chart\":\"ID\",\"UA_Chart_Sys\":\"ID\",\"ua_desktop_bak\":\"cID\",\"UA_DesktopPage\":\"cPageID\",\"UA_DesktopPage_Bak\":\"cPageID\",\"UA_Flow\":\"ID\",\"UA_Flow_Sys\":\"ID\",\"UA_Group_Ex\":\"cGroup_Id\",\"UA_HoldAuth_Ex\":\"\",\"UA_Identity_Ex\":\"cVouchType\",\"ua_idt_EX\":\"ufts\",\"UA_Log_Ex\":\"\",\"UA_Modules\":\"ModuleID\",\"UA_ModulesSet\":\"ModuleID,cUserID\",\"UA_Operation\":\"CreatedOn\",\"UA_Password_Ex\":\"iMaxDay\",\"UA_Period_Ex\":\"\",\"UA_PortalLang\":\"\",\"UA_PortalLang_Bak\":\"\",\"UA_PortalLogo_EX\":\"cId\",\"UA_Role_Ex\":\"cGroup_Id,cUser_Id\",\"UA_Scenario_bak\":\"cID\",\"UA_Scenario_WorkCategory_Bak\":\"ID\",\"UA_Scenario_WorkItem_Bak\":\"ID\",\"UA_ScenarioUser\":\"cScenarioID\",\"UA_ScenarioView_Bak\":\"cViewID,cScenarioID\",\"UA_ScenarioView_Flowlayout_Bak\":\"cInstanceID,cScenarioID\",\"UA_ToNextYear_base\":\"cTblName\",\"Ua_U8LoginProcess_EX\":\"procKey\",\"UA_User_Ex\":\"cUser_Id\",\"UA_UserRelation_ex\":\"cUser_ID\",\"UA_WorkCenterSolutions_Bak\":\"SolutionID\",\"UA_WorkItems_Bak\":\"WorkItemID\",\"UA_WorkSolutionDetail_Bak\":\"AUTOID\",\"UA_WorkTask\":\"TaskID\",\"UAP_VoucherLayout\":\"LayoutID,localid\",\"UFFA_Model_AccInformation_Init\":\"cSysID,cName\",\"UFFA_Model_fa_AssetGroup_Init\":\"sID\",\"UFFA_Model_fa_AssetTypes_Init\":\"sID\",\"UFFA_Model_fa_CardOfPUColSet_Init\":\"sIDKey\",\"UFFA_Model_fa_Departments_Init\":\"sNum\",\"UFFA_Model_fa_Depreciations_Init\":\"sID\",\"UFFA_Model_fa_Items_Init\":\"sID\",\"UFFA_Model_fa_ItemsOfModel_Init\":\"\",\"UFFA_Model_fa_ItemsOfQuery_Init\":\"\",\"UFFA_Model_fa_Models_Init\":\"sID\",\"UFFA_Model_fa_Objects_Init\":\"\",\"UFFA_Model_fa_Origins_Init\":\"sID\",\"UFFA_Model_fa_Status_Init\":\"sID\",\"UFFA_Model_fa_VtsObject_Init\":\"Id\",\"UFHeronNewRepSet\":\"name\",\"UFMenu_Business_Lang\":\"MenuId\",\"UFOperation_Business_Lang\":\"MenuId,LocaleId,UserID\",\"UFResIDToResString\":\"\",\"UFSUB_fitemss00\":\"I_id\",\"UFSUB_fitemss00class\":\"i_id\",\"UFTmpTable_10455976_5273032774046742210\":\"baseid\",\"UFTmpTable_18960648_5073786593268413634\":\"baseid\",\"UFTmpTable_20768102_4845152704238299066\":\"baseid\",\"UFTmpTable_25386371_5244050225645158643\":\"baseid\",\"UFTmpTable_25679319_5116222280184318806\":\"cNo\",\"UFTmpTable_29839867_4811302975503029952\":\"baseid\",\"UFTmpTable_31851111_4974751764607795621\":\"baseid\",\"UFTmpTable_33208444_4622598960392282983\":\"baseid\",\"UFTmpTable_4766884_4626017505396346494\":\"baseid\",\"UFTmpTable_6256172_5137592960889844285\":\"baseid\",\"UFTmpTable_7852878_5221512932527731635\":\"baseid\",\"UpdateAccountInfo\":\"BizObjID\",\"UserDefineDLL\":\"id\",\"UTU_Authorization\":\"cID\",\"UTU_Broadcast\":\"uidID\",\"UTU_Broadcast_User\":\"uidID\",\"UTU_Discussion\":\"uidID\",\"UTU_DiscussionFile\":\"uidID\",\"UTU_DiscussionMember\":\"uidID\",\"UTU_DiscussionMessage\":\"uidID\",\"UTU_DiscussionMessage_Image\":\"uidID\",\"UTU_MessageCenter\":\"iID\",\"UTU_MessageCenter_Attach\":\"iMessageID\",\"UTU_MessageCenter_U8ApproveTask\":\"uidID\",\"UTU_MessageCenter_User\":\"iMessageID\",\"UTU_NormalMessage\":\"uidID\",\"UTU_NormalMessage_Image\":\"cName\",\"UTU_Statistics_ChatID\":\"cUserID,cChatID\",\"UTU_SystemMessage\":\"uidID\",\"UTU_SystemMessage_User\":\"uidID\",\"UTU_User\":\"cUserID\",\"UTU_UserConfig\":\"cUserID\",\"VariationDeleteKeyFld\":\"BillType,cFldName\",\"VouchAPPMappingTable\":\"cBizObjectID\",\"VoucherExtendArcType\":\"ctype\",\"VoucherExtendInfo\":\"cardnumber,cextendtype\",\"VoucherExtendType\":\"\",\"voucheritems_base_U8upgrade_backup\":\"AutoId\",\"voucheritems_prn_base_U8upgrade_backup\":\"AutoId\",\"VoucherNumber_Bak10\":\"CardNumber\",\"VoucherNumber_Lang\":\"CardNumber,AppName\",\"voucherprefabricate_lang\":\"ufts\",\"WA_SDS_Set\":\"\",\"AllItemName\":\"citemcode\",\"GL_bfreqInfo\":\"Inid\",\"GL_RepPrintTemplate\":\"ufts\",\"hr_hi_workflowupdatelog\":\"id\",\"UA_DumpTrace\":\"Item\",\"UFTmpTable_11990674_4746430355863300831\":\"baseid\",\"UFTmpTable_40660411_5607909958865532904\":\"baseid\",\"WF_Engine\":\"ServerName\",\"UFTmpTable_2009718_4995360335203273248\":\"baseid\",\"UFTmpTable_22148959_4639919342175548490\":\"I_id\",\"UFTmpTable_31890594_5647413420197978645\":\"baseid\",\"fc_CombinationSet\":\"iNum\",\"hr_sys_params_v1100_bak\":\"paraKey\",\"IA_OtherAcountSet\":\"ExciseTaxHead\",\"mxzmonth2\":\"i_id\",\"mxzmonth3\":\"i_id\",\"mxzmonth4\":\"i_id\",\"NB_Code\":\"cUnitID\",\"NE_AreaType_Mapping\":\"OldPositionCode\",\"NE_voucheritems_base_bak\":\"AutoId\",\"NE_voucheritems_lang_bak\":\"guid\",\"NE_voucheritems_prn_base_bak\":\"AutoId\",\"NE_voucheritems_prn_lang_bak\":\"guid\",\"pl_CO_InitPost\":\"\",\"WF_ApplicationList\":\"AppName\",\"GL_CashItemDataSource\":\"iID\",\"UA_Auth_base_Ex\":\"cAuth_Id\",\"UA_Auth_lang_Ex\":\"localeid,cAuth_ID\",\"Vendor_Log\":\"HistoryId\",\"mxzmonth5\":\"i_id\",\"mxzmonth6\":\"i_id\",\"mxzmonth8\":\"i_id\",\"Customer_Log\":\"HistoryId\",\"newcode\":\"i_id\",\"UFTmpTable_36314309_4843295305356275695\":\"baseid\",\"UFTmpTable_7645121_5429293770076137056\":\"I_id\",\"UFTmpTable_13198334_5009154841650742616\":\"I_id\",\"UFTmpTable_32101308_5647597676108152209\":\"baseid\",\"UFTmpTable_32718465_5618864207565321679\":\"baseid\",\"UFTmpTable_33875820_5571767601625628353\":\"I_id\",\"mxzmonth10\":\"i_id\",\"GL_VouchDraft\":\"ccode\",\"TSS_20150130183504_HR_BD_RPT_FILE_8438\":\"iRptCode\",\"TSS_20150130183504_hr_hi_person_1262\":\"cPsn_Num\",\"UFTmpTable_15041804_5590687117576251080\":\"baseid\",\"UFTmpTable_42115440_4957843374075739158\":\"baseid\",\"UFTmpTable_11889477_4959363111433678822\":\"baseid\",\"UFTmpTable_12753101_5080021127641250436\":\"I_id\",\"UFTmpTable_21185302_5491386559431674318\":\"baseid\",\"UFTmpTable_27751266_4934083014632379348\":\"baseid\",\"UFTmpTable_19141045_4691186641269624455\":\"baseid\",\"UFTmpTable_34235110_4733858372266159294\":\"I_id\",\"UFTmpTable_11396936_4618142097794818542\":\"baseid\",\"UFTmpTable_20385557_5488610716193767116\":\"I_id\",\"UFTmpTable_40467043_4809893906734045994\":\"I_id\",\"UFTmpTable_40744457_5304084666879592572\":\"baseid\",\"UFTmpTable_1920142_5284041159812210761\":\"baseid\",\"UFTmpTable_28143180_4710914120011663712\":\"baseid\",\"UFTmpTable_28836035_5060528630643024688\":\"I_id\",\"UFTmpTable_20202573_4777551787041021838\":\"baseid\",\"UFTmpTable_24425258_5266266580272185227\":\"I_id\",\"UFTmpTable_25222422_5669765927449095131\":\"baseid\",\"UFTmpTable_30749755_5237343360035027386\":\"baseid\",\"UFTmpTable_3509642_4682262829906223018\":\"baseid\",\"UFTmpTable_39356639_5571726716840632041\":\"I_id\",\"TSS_20160427105859_HR_BD_RPT_FILE_2162\":\"iRptCode\",\"TSS_20160427105859_hr_hi_person_1018\":\"cPsn_Num\",\"TSS_20160427162258_HR_BD_RPT_FILE_6482\":\"iRptCode\",\"TSS_20160427162258_hr_hi_person_6695\":\"cPsn_Num\",\"TSS_20160427163709_HR_BD_RPT_FILE_7801\":\"iRptCode\",\"TSS_20160427163709_hr_hi_person_7810\":\"cPsn_Num\",\"UFTmpTable_17377591_5450377689154379203\":\"baseid\",\"UFTmpTable_17583143_5178215158709224693\":\"baseid\",\"UFTmpTable_31063988_5175668944175598531\":\"baseid\",\"UFTmpTable_17510751_5745115605814638805\":\"baseid\",\"UFTmpTable_2926298_4929893332729770994\":\"baseid\",\"UFTmpTable_33534594_4752628839744464246\":\"baseid\",\"BSP_Favorites_Lang\":\"cLocalID\",\"UFTmpTable_40133664_5045743814405795255\":\"baseid\",\"UFTmpTable_42423680_5723129419171797511\":\"baseid\",\"UFTmpTable_7075912_5570899835926460216\":\"baseid\",\"UFTmpTable_7549615_5016996687040306680\":\"baseid\",\"UFTmpTable_8096950_5545470263210748464\":\"baseid\",\"UFTmpTable_25875717_5252448436798611562\":\"baseid\",\"UFTmpTable_43095312_5151007735480681295\":\"baseid\",\"UFTmpTable_8506733_5489850224004843454\":\"baseid\",\"hr_hi_person_bak\":\"cPsn_Num\",\"IA_ST_UnAccountVouch01\":\"IDUN\",\"UFTmpTable_19429238_4894115248961819575\":\"baseid\",\"UFTmpTable_34545729_5547024637381243733\":\"baseid\",\"UFTmpTable_42794562_4785787819047155152\":\"I_id\",\"UFTmpTable_4972767_4888526430806454042\":\"baseid\",\"UFTmpTable_16826935_5140728930361618755\":\"baseid\",\"UFTmpTable_23698087_5341315209193074426\":\"baseid\",\"UFTmpTable_25586123_5030939123843017540\":\"baseid\",\"UFTmpTable_27153032_5317579899627517040\":\"baseid\",\"UFTmpTable_3591408_5397218394405461031\":\"baseid\",\"UFTmpTable_37847109_4903327181422721218\":\"baseid\",\"UFTmpTable_4523295_5746131914556995195\":\"baseid\",\"IA_PerDealCloseCheck\":\"cVouType,cInvCode,cResultKey\",\"IA_PerDealWhDep\":\"cWhDepCode\",\"IA_PZEmptyAccountTHINKCENTRE_PC0\":\"cVouType\",\"OrgData_Templog\":\"\",\"TSS_20200106112358_HR_BD_RPT_FILE_5336\":\"iRptCode\",\"TSS_20200106112358_hr_hi_person_3403\":\"cPsn_Num\",\"UFTmpTable_1564689_5662046225165374058\":\"baseid\",\"UFTmpTable_21022216_4867939782263287291\":\"baseid\",\"UFTmpTable_29113808_5579324619404681754\":\"baseid\",\"UFTmpTable_38855969_4859015022023759754\":\"baseid\",\"UFTmpTable_9026678_5736135686755388004\":\"baseid\",\"UFTmpTable_14607186_5050573169322434714\":\"baseid\",\"UFTmpTable_34697452_4652866374841386783\":\"baseid\",\"IA_PZEmptyAccountWIN_4U22VFGCLLU8\":\"cVouType\",\"TSS_20220531182155_HR_BD_RPT_FILE_1144\":\"iRptCode\",\"TSS_20220531182155_hr_hi_person_9815\":\"cPsn_Num\",\"UFTmpTable_13953081_5216434493805133046\":\"baseid\",\"UFTmpTable_16693202_4665802513931718297\":\"baseid\",\"UFTmpTable_17760517_5384250200185998049\":\"baseid\",\"UFTmpTable_21028944_5662947583297377346\":\"baseid\",\"UFTmpTable_23303172_4675144580394428083\":\"cNo\",\"UFTmpTable_23872203_4951003360502537861\":\"baseid\",\"UFTmpTable_24452368_4661617030826698327\":\"baseid\",\"UFTmpTable_26800857_5715037538958312415\":\"baseid\",\"UFTmpTable_32347381_5148793736701507526\":\"baseid\",\"UFTmpTable_36213941_5172572502759315816\":\"baseid\",\"UFTmpTable_37287611_5687999593324988112\":\"baseid\",\"UFTmpTable_4081412_4997200252323481417\":\"baseid\",\"UFTmpTable_45768094_4971185377973698261\":\"baseid\",\"UFTmpTable_15285027_5436297084328932461\":\"baseid\",\"UFTmpTable_21578533_5362094058996736553\":\"baseid\",\"UFTmpTable_34000808_4614576452805151039\":\"baseid\",\"UFTmpTable_41506473_5707505589831107513\":\"baseid\",\"UFTmpTable_640569_4848549768217370992\":\"baseid\",\"UFTmpTable_10312541_4724627749180783396\":\"baseid\",\"UFTmpTable_17879829_4643703710797835761\":\"cNo\",\"UFTmpTable_18414731_4612227043328215090\":\"I_id\",\"UFTmpTable_18918910_4793971226507307448\":\"baseid\",\"UFTmpTable_28088475_4918832449123658905\":\"baseid\",\"UFTmpTable_32304065_5495825209689401843\":\"baseid\",\"UFTmpTable_4082488_5523247485150603590\":\"baseid\",\"UFTmpTable_41960300_4891296777404204949\":\"baseid\",\"UFTmpTable_44745246_4714842937888296274\":\"baseid\",\"UFTmpTable_45597108_4795201627411687142\":\"baseid\",\"UFTmpTable_4676529_4776293401887577027\":\"baseid\",\"UFTmpTable_9048094_4727465510726253698\":\"baseid\",\"IA_ST_UnAccountVouch08\":\"IDSUN\",\"UFTmpTable_10408839_5202208068003155267\":\"baseid\",\"UFTmpTable_11091452_5732002143324891201\":\"cNo\",\"UFTmpTable_1143850_4853768468635729884\":\"I_id\",\"UFTmpTable_16524416_4837998161968588707\":\"baseid\",\"UFTmpTable_2758011_5269759221684498626\":\"baseid\",\"UFTmpTable_29676157_5392117799751026918\":\"baseid\",\"UFTmpTable_33485472_5004633922183855639\":\"baseid\",\"UFTmpTable_3999845_5400395347833367260\":\"baseid\",\"UFTmpTable_40917243_5106038067505044365\":\"baseid\",\"UFTmpTable_42830666_5024400064715807014\":\"baseid\",\"UFTmpTable_44648900_4657307892211682301\":\"baseid\",\"UFTmpTable_542346_4843050275253648657\":\"baseid\",\"UFTmpTable_11701060_5137891018117691018\":\"baseid\",\"UFTmpTable_16104707_5178703920547622677\":\"baseid\",\"UFTmpTable_23314723_5401149268559240727\":\"I_id\",\"UFTmpTable_28787690_4853912183547021229\":\"baseid\",\"UFTmpTable_30460355_4788227392134842649\":\"baseid\",\"UFTmpTable_33123356_5056476441133972745\":\"baseid\",\"UFTmpTable_41758395_5199085623073984923\":\"baseid\",\"UFTmpTable_45339870_5472132719051698244\":\"baseid\",\"UFTmpTable_4778144_5492401891332174719\":\"baseid\",\"UFTmpTable_6702217_5562976690674303267\":\"baseid\",\"UFTmpTable_9157988_5007543843697623921\":\"cNo\",\"UFTmpTable_34984111_5067724717646780869\":\"baseid\",\"UFTmpTable_40271661_4723038584435899201\":\"baseid\",\"UFTmpTable_6162121_5365065915987347771\":\"I_id\",\"UFTmpTable_22583014_5213320885663501013\":\"baseid\",\"UFTmpTable_36551961_5672009348341046241\":\"baseid\",\"UFTmpTable_45271576_4736226744808548933\":\"I_id\",\"UFTmpTable_18238264_5074306766584578779\":\"baseid\",\"UFTmpTable_24175729_5739080196600006276\":\"I_id\",\"UFTmpTable_25285789_5307490651861959329\":\"baseid\",\"UFTmpTable_36538542_4853359199471469766\":\"baseid\",\"UFTmpTable_36944668_5295470851910855253\":\"baseid\",\"UFTmpTable_43741012_5247856002645966393\":\"baseid\",\"UFTmpTable_4881084_5059645187466329305\":\"baseid\",\"UFTmpTable_25878501_4901054754741073731\":\"baseid\",\"UFTmpTable_39727847_5646933212961020075\":\"I_id\",\"UFTmpTable_536185_5306738372133561161\":\"baseid\",\"UFTmpTable_33076871_5090712442949175733\":\"baseid\",\"UFTmpTable_36766321_5499190348460897993\":\"baseid\",\"UFTmpTable_38428250_5557019145346523289\":\"I_id\",\"UFTmpTable_4088415_5563094831708994645\":\"baseid\"}";
        dbTablePrimaryKeyMap.put("UFDATA", JSONUtils.parseObject(ufData, new TypeReference<Map<String, String>>() {
        }));

        String ufMeta = "{\"Table\":\"OID\",\"View\":\"OID\",\"flt_DefineMeta_Cust\":\"FilterID\",\"UAP_PublishGridLayout\":\"GridLayoutID,LocaleID\",\"UAP_PublishLayout\":\"LayoutID\",\"UAP_Languages\":\"textid,localid,BusinessFunctionId\",\"DP_ChannelProcessor\":\"ProcessorID\",\"UAP_ExtendViewModels\":\"modeltype\",\"UAP_Entity\":\"id\",\"DP_MetaDataType\":\"MetaDataType\",\"UAP_EntityRelationDetails\":\"id\",\"UAP_ReportView_Lang_FactoryBak\":\"ViewID,LocaleID\",\"Field\":\"OID\",\"MERP_App\":\"app\",\"Merp_Module\":\"cAppID,cModuleID\",\"UAP_VoucherGridLayout_Backup\":\"GridLayoutID\",\"UAP_EntityRelation\":\"id\",\"UAP_ViewModelPlugins\":\"applicationid\",\"BD_TimeStamp\":\"TableName\",\"UAP_Report_Limit\":\"ReportID\",\"AA_CustomerButton\":\"cButtonID\",\"MetaEnumDef\":\"MetaID,LanguageID\",\"UAP_VoucherLayout_Backup\":\"LayoutID\",\"AA_BusObject_base\":\"cBusObId,cBusObName,langid\",\"MERP_ConditionPlugins\":\"cKey,cVarient\",\"UAP_ExtendHugeEntity\":\"ReportID,tablename\",\"UAP_VoucherGridLayout\":\"GridLayoutID\",\"TraceResult\":\"cDate\",\"DP_DataTransformConfig\":\"ID\",\"BD_CacheScripts\":\"timestamp\",\"Merp_RelatedObj\":\"cVoucherType\",\"Uap_Upgrade\":\"CardNumber\",\"EAI_AuthId\":\"Name\",\"AA_BusObject_General\":\"pubufts\",\"UAP_ReportView_FactoryBak\":\"ID\",\"AA_Autocomplete_Field\":\"cTableName,cFieldName\",\"MERP_DynamicRefer\":\"ReferKey\",\"ViewField\":\"OID\"}";
        dbTablePrimaryKeyMap.put("UFMeta", JSONUtils.parseObject(ufMeta, new TypeReference<Map<String, String>>() {
        }));

        String ufDataUnimportantTable = "hr_sys_itemdict_Base,vouchers_lang,code,NB_AreaCode_City,VoucherCustomRelation,UFResIDToResString,,tc_menu_lang,REFERFIELDS,referfilterfields,HR_BD_RPT_COND2,SA_FieldExch,,AA_ColumnDicCache_Detail,tc_enum_str,Rpt_GlbDEF_Base,GL_DigestDisp,VoucherExtendType,AuditBizPriParams,HR_CT004,tc_user_rel_condition,QmChartProperty,IB_MsgType,ua_idt_EX,CN_NoteBase,tc_entity_relation_attr,SA_VoucherCellCheck,rb_Localize,vouchertemplates_base,SA_ButtonState,vouchertemplates_lang,SA_ReferFillConfig,VoucherPrefabricate,voucherprefabricate_lang,SA_VoucherListFieldConfig,UTU_Statistics_ChatID,fd_fields,AA_VouchersDigestDetail_lang,AA_VouchersDigestDetail_base,CM_DD,AuditBizObjectAuth,AA_ProcedureDic,NE_AirPortCity,NE_CountryCity,AccInformation,UA_Log_Ex,SA_ToolBarConfig,HR_CT007,Flt_QuickConditionSet,BG_Archive_SYS,AA_ColumnAuthMapping,REFERRETURNFIELDS,UFOperation_Business_Lang,,voucherfiexdlabel_lang,EX_DocumentDataSourceItemCaption,EX_DocumentDataSourceItem,AA_Enum,tc_ufrefresh,dd_attribute,dd_str_resource,CN_NoteRelateExt,AA_ColumnSet_base,VQD_Dictionary,VQD_CaptionLang,Rpt_ItmDEF_Base,dd_dict_str,Rpt_FltDEF_Base,eb_splitcode,AA_ColumnDic_base,UA_HoldAuth_Ex,Rpt_FldDEF_Base_Bak,voucheritems_prn_lang,voucheritems_lang,voucheritems_prn_base,voucheritems_base,voucheritems_base_U8upgrade_backup,voucheritems_prn_base_U8upgrade_backup,Rpt_FldDEF_Base,VQD_DataItem,GSP_VOUCHERREFS,hr_bd_columnField_base,VoucherCustomRelation,AA_ColumnSet_base,NE_CountryCity,AA_ColumnDicCache_Detail,GL_RepPrintTemplate,NE_AirPortCity,AA_ProcedureDic,Hr_bd_VoucherItem,AuditBizObjectAuth,CM_DD,fd_fields,UTU_Statistics_ChatID,AA_VouchersDigestDetail_base,AA_BusObject_base,SA_VoucherListFieldConfig,voucherprefabricate_lang,SA_ReferFillConfig,vouchertemplates_lang,SA_ButtonState,vouchertemplates_base,fa_ItemsOfModel,SA_VoucherCellCheck,tc_entity_relation_attr,CN_NoteBase,ua_idt_EX,IB_MsgType,ap_digset_base,VoucherExtendType,QmChartProperty,HR_CT004,AuditBizPriParams,GL_DigestDisp,GL_mybooktype,HR_CT117,variationbillfield,AuditBizEvents,SA_AuthConfig,SA_VoucherFieldConfig,tc_enum,AuditBizEntities,AA_ColumnSetExtend,tc_query_profile_str,vouchers_lang,AuditBizEvents_Lang,PDM_BusinessMeta,tc_entity_relation,HR_BD_QueryIncludeSubCls,HR_BD_QueryClsAndSubClsRel,REFERARCHIVEMAPPING,tc_operation,tc_user_rel_profile,UA_PortalLang,UA_PortalLang_Bak,Rpt_GRPDEF_Base,AuditBizPriParams_Lang,PFFCVoucherFieldConfig,UFOperation_Business_Lang,AuditBizObjectDataAuth,tc_profile_trans,CM_TrackField,CM_TrackFieldUser,PDM_BusinessItems,AA_TableDic_Lang,VariationDeleteKeyFld,hr_sys_setdict_Base,VariationBillOptions,CollectorConfigItem,AuditEventPlugins,VQD_Rpt_ReportViewUFTS,PFFCReferFillConfig,EB_EBCompanyRelation,AuditMsgTypeConfig,UTU_NormalMessage_Image,GL_PrnXmlFormat,HR_BD_RPT_CONDITEM,NE_PzSet_ZYItem_lang,NE_PzSet_ZYItem,tc_user_list_report,scmVouchRelInfo,VQD_ListColumnDec,tc_menu,U8LangResource,VoucherStatusRuleContrapose,IB_EndPoint,HR_BD_RPT_SECTION,UFFA_Model_fa_ItemsOfModel_Init,IB_AppTag,UTU_DiscussionMember,HR_CT018,HR_BD_DefaultOrderFields,AA_DefineExtendTables_Filter,IB_AppTag_MsgType,NB_regions,AuditBizPredicate,SA_GoldTaxField,VoucherNumber,Table_Template_CustomCondition,dd_object_option,dd_object,hr_tm_RestDay,CA_Error_Hint,tc_query_profile,Hr_BD_Dts_PrjDetail,HR_BD_CalcRuleObjReg,UFFA_Model_AccInformation_Init,U8TaskNotifyType,NE_PzSet_ZYItem_Bak,Crm_Lang_Resource,AuditBizObjects_Lang,GSP_UnionChangeInfo,sfc_opaccountcfg,tc_user_list_profile,IN_Rule_ResultTpl,rpt_FetchObject,QM_RANDORPROJECTS,VoucherStatusRules,HR_CT006,UA_Caption,SA_CheckBeforeSave,VoucherNumber_Bak10,VoucherNumber_Lang,printEx,Table_Template_Lang,HR_BD_CalcRuleObjFldReg,AA_FormButtonsMenus_Default,EB_Switch_Api,UserDef_Base,UserDef_Lang,VQD_DataTable,tc_edit_card_format,UA_Operation";
        dbUnimportantTableMap.put("UFDATA", Arrays.asList(ufDataUnimportantTable.split(",")));

        String ufDataImportantTableMapTable = "Ven_Contact,VendorBank,SA_OrderTraceItem_Base,SA_OrderTraceQuery_Base,SA_OrderTraceQuery_Lang,SCM_ImportDefaultValSet,CM_Trans,fitemss98,HR_CT251,hr_hi_jobInfo,hr_hi_person,IN_Reconcile,fitemss98class,HR_BD_Alert,HR_BD_AlertItem,CW_ProfPlan";
        dbImportantTableMap.put("UFDATA", Arrays.asList(ufDataImportantTableMapTable.split(",")));

        String ufDataUselesTable = "UFTmpTable_11990674_4746430355863300831,VoucherPrintSettingExtension,WA_GZSectiontblset,WF_Engine,CN_Version,crp_resourcevs,dsign,EA_Flow,EB_IndexHomeNews,EB_Switch_AsyncApiSetting,eb_vouchertemplates,ex_process,fa_AssetGroup,fa_AssetTypes,FA_LabelDesign,FA_LabelType,fc_OpColor,fc_UpgradeLog,FD_Option,FM_SysSetting_base,foreigncurrency,HR_BD_AnalyItem,hr_bd_BusSet,HR_BD_RuleAndTask,HR_CT031,HR_CT320,HR_PF_Alert,HR_PF_AuthSchema,HR_SyncData,hr_tm_AutoVoucherID,hr_tm_DutyPeriod,hr_tm_EastRiverBasicSet,HR_TM_ICCardParameters,HR_TM_LossNewMachine,hr_tm_OverTimeCardExcursion,hr_tm_Paras,hr_tm_PersonParameters,hr_tm_RestType,hr_tm_Rule,hr_tm_RulesForShift,hr_tm_SignCardType,HR_WA_StdCat,hy_DZ_ECN_SELECT,hy_parameter,IB_MsgFilter,KC_BarCodeFlowProject,KC_PickingBusType,KC_PickingComparison,KC_PickingStategy,mom_parameter,MOMClusterSrvReg,MOMSrvReg,mps_ssr_para,NE_OperationType,NE_OperationType_PrintTemplate,NE_PzSet_QT,NE_PzSet_QT_Bak,NE_RestType,NeedLogActions,NotifyObjectName_lang,Pe_Version,PeriodDef,PeriodDef_Lang,pl_co_MaterialItem,PM_InfoSet,PP_RMRPmain,pp_ropoptma,QMRANDORCHECK,rb_Roles,rb_Solutions,Reason,Reg_ChannelProcessor,Reg_ChannelProcessorType,RP_SysSet,tc_activity_cost_d,tc_beforetake,tc_biz_dept,tc_desktop_report,tc_edit_card_format_p,tc_inventory_d,tc_invoice,tc_invoice_d,tc_locale,tc_mail_info,tc_mng_dept,tc_price_book,tc_privilege3,tc_privilege4,tc_queue,tc_receipt,tc_receipt_d,tc_reception,tc_score_regulation,tc_skill_group,tc_sms_info,U8LangDefine,UA_Account_Ex,UA_AccountDatabase_Ex,UA_Password_Ex,UA_PortalLogo_EX,UA_ScenarioUser,UA_UserRelation_ex,UAP_TablePKIntMaxValue,UFFA_Model_fa_AssetGroup_Init,UFFA_Model_fa_AssetTypes_Init,UFFA_Model_fa_Departments_Init,UFHeronNewRepSet,UFSUB_dsign,UFSUB_fitem,UFSUB_fitemss00class,UTU_Broadcast,VariationRecordBeginPoint,EAS_Feedback_Message,ex_allow,ex_boolean,ex_ciqinvtype,ex_comrule,ex_configstatus,ex_farecontroltype,ex_paymentstyle,ex_ststatus,ex_tradeprocessmode,FB_Version,fitem,fitemss97class,GC_Route,GL_mcodeused,hr_bd_ColumnModel_base,HR_BD_QueryLogicSign,HR_BD_Roster,hr_bd_utable,HR_CT014,HR_CT023,HR_CT038,HR_CT041,HR_CT046,HR_CT047,HR_CT052,HR_CT080,HR_CT081,HR_CT1001,HR_CT106,HR_CT154,HR_CT158,HR_CT203,HR_CT204,HR_CT205,HR_CT207,HR_CT208,hr_ct237,HR_CT256_Base,HR_CT257_Base,HR_CT259_Base,HR_CT261_Base,hr_ct265,HR_CT275,HR_CT276,HR_CT302,HR_CT310,hr_ct321,HR_CT323,HR_CT324,HR_CT330,HR_CT331,HR_CT332,HR_CT335,HR_CT370_base,hr_ct376,hr_ct403,hr_ct404,hr_ct407,hr_ct408,HR_CT409,HR_CT410,HR_CT411,HR_CT800,HR_PF_Alert_OLD,HR_PF_EnumsCheckMainbody,HR_PF_EnumsFormulaCheckMainbody,HR_PF_EnumsFormulaType,HR_PF_EnumsGetIntegerManner,HR_PF_EnumsKeyType,HR_PF_EnumsMsgTemplateType,HR_PF_EnumsOperationGetValueManner,HR_PF_EnumsOpRelation,HR_PF_EnumsPeriodDimensionality,HR_PF_EnumsPlanState,HR_PF_EnumsSchemeState,HR_PF_EnumsTemplateType,HR_PF_EnumsType,HR_PF_EnumsValidateRule,HR_PF_SchemaClass,hr_rt_dtsNetStation,hr_tm_DutyBasic,hr_tm_DutyType,hr_tm_ErrandType,HR_TM_ManHourSystem,HR_TM_SignCardReason,hr_wm_project_base,IB_AppSys,IB_Entities,im_paymentstyle,IN_Param_Module,mps_lrplock,mps_parameter,NB_MaxNo,NE_AuditType,PFFCVoucherListConfig,rb_Portals,rb_UserRoles,rb_Users,REFERBIZFIELDS,RP_ModelType,RP_Vouchers_ExSet,Rpt_SelfDefineRptCallBackObj,SA_LockSourceVouchConfig,SCM_CheckTrackbUsed,tc_cti_config,tc_login_history,tc_stage_path_action,tc_user_attr,tc_user_xattr,UA_IncomeExpenditureItem,UA_PortalOptionItem,UTU_DiscussionMessage_Image,VendorClass,WF_ApplicationList,AA_Medal_Config,AA_Picture,AA_RequirementClass,Account,Ap_CancelNo,AutoSetTableInf,AutoSetTableInf_Lang,bas_calendar,bas_updlog,BG_AuthRelation,BG_FactColumnDisplay,HR_CT200,HR_CT201,HR_CT202,HR_CT206,HR_CT209,HR_CT211,hr_ct236,HR_CT260_Base,HR_CT264_Base,hr_ct266,HR_CT271,HR_CT272,HR_CT333,HR_CT350,HR_CT398,HR_CT416,hr_ct419,HR_CT801,Hr_CT802,HR_HB_MTColumnDicEx,hr_ht_HtType,hr_ht_NewCtrtType,HR_PF_EnumsAffairType,HR_PF_EnumsCalcProcess,HR_PF_EnumsCheckScoreWay,HR_PF_EnumsDataModel,HR_PF_EnumsDisclaimDisposeManner,HR_PF_EnumsMatchWay,hr_pf_GradeMode_OLD,hr_pf_NumMode_OLD,HR_PF_TargetClass,hr_ss_SubTableConf,HR_TM_DateType,hr_tm_OverTimeType,IM_BusType,im_paymoneystate,MOM_MsgTypeCategories,NB_InputConfig,NB_PayUnitClass,NE_OperationType_Lang,NE_ThirdTicketSource,Pe_GetDataOpt,Pe_SysInfo,PFFCGoToVoucherConfig,PFFCReferVoucherConfig,pl_Co_ErrorPos,rb_Announcements,rel_user_privilege,SA_SalePlanOption,tc_backfill_setting,tc_online_lead_tmpl,tc_score_regulation_other,tc_search_profile,U8TaskSourceNet,UA_DumpTrace,UA_MenuAssocChart,UA_PortalOption,UpdateAccountInfo,UTU_SystemMessage,AA_ColumnsDisplayRules,AA_Init_Lang,BG_Version,HR_CT152,HR_CT157,HR_CT159,HR_CT160,HR_CT000,HR_CT010,HR_CT029,HR_CT049,HR_CT053,HR_CT054,hr_ct055,HR_CT059,HR_CT064,HR_CT065,HR_CT066,HR_CT067,HR_CT068,HR_CT083,HR_CT086,HR_CT127,CA_MenuDef,crp_parameter,crp_planproject,dd_object_table_def,dd_sys_lang,EA_Action,EAI_UserSetting,EntityClass,ex_bustype,ex_drawbackstyle,ex_massunit,Ex_OrderDateField,ex_portattr,ex_vouchstype,GL_ProcedureDic,HR_Bd_TableDataType,tc_privilege,tc_user_desktop,tc_user_menu,hr_dm_feeitems,HR_PF_AuthSchemaDistributeTable,HR_PF_EnumsVoucherState,HR_PF_GradeManner,hr_wm_formula,Hy_Mutex,IB_Event_Plugin,IN_Param_Group,NE_EffectItem_Default_Base,NE_Vehicle,Pe_Szdetail,HR_CT016,HR_CT021,HR_CT035,HR_CT036,HR_CT051,HR_CT062,HR_CT063,HR_CT076,HR_CT082,hr_ct099,HR_CT122,HR_CT126,HR_CT155,HR_CT253,hr_ct262,HR_CT263_Base,HR_CT270,HR_CT273,HR_CT274,SCM_SysTableTS,tc_queue_rule,UA_Identity_Ex,UA_MyWorkCategory,UA_ViewCategory,UFFA_Model_fa_Models_Init,WA_FilterExpList,AA_BizProcedureDic,hr_bd_tableclass_Base,HR_CT009,HR_CT024,HR_CT027,HR_CT033,HR_CT045,HR_CT050,HR_CT079,HR_CT088,HR_CT112,HR_CT113,HR_CT125,HR_CT153,HR_CT255_Base,HR_CT303,HR_CT375_base,hr_ct406,HR_CT418,hr_om_depStaffRosterSet,HR_PF_EnumsColumnType,HR_PF_EnumsWorkStep,HR_TM_Arithmetic_base,HR_TM_Arithmetic_lang,hr_tm_TimeUnit,hr_wa_adjusttype,hr_wm_boonset,hr_wm_typeset,hr_ct402,hr_ct405,hr_dm_DormType,HR_PF_EnumsCalcRule,HR_PF_EnumsConditionStyle,HR_PF_EnumsDefaultPeriodType,HR_PF_EnumsFormulaPeriodType,HR_PF_EnumsIncludeContent,HR_PF_EnumsPeriodType,HR_PF_EnumsCaculateType,HR_PF_EnumsState,WA_PRAssignProject,BarCodeType,GrpArchCode,hr_bd_columnobject_base,HR_CT119,HR_CT120,HR_TM_AlertSource,IN_ModuleOrder,TaskProcessorDef,tc_folder,tc_mail_template_tmpl,UA_PortalOptionDefaultItem,EX_BUSTACHE_DEMO,EA_Stream_Log,EAO_SubClassML,WF_AccountCloseInfo,WF_ActiveMessage,WF_LogType,WF_PDVariable,Bombuttonset,BW_Algo,BW_InvClassAlgo,BW_AlgoClass,BW_AlgoPlan,BW_AlgoPlan_Lang,Ca_Visio,cw_webconfig,HR_CT277,HR_CT420,rb_EcommerceMerchants,SA_HeadOnEdit,SCM_SKUTransactionRule,BW_PeriodType,HY_LinkDesc,mom_Datasource,mom_Dimension,mom_Metric,SCM_ClearStock,AA_ColumnModifyForExtend,BW_ComputeBy,cw_menuInfo,EB_Switch_ExCoSetting,rb_Versions,RP_PrintTable,HR_HB_MTColumnDic,HR_HB_MTColumnLang,HR_HB_MTColumnSet,HR_HB_MTColumnSetLang,ST_DefineSet,voucheritems_base_U8upgrade_backup,voucheritems_prn_base_U8upgrade_backup,NE_PzSet_ZYItem_Bak,hr_sys_itemdict_Base_His,GSP_UnionChangeInfo,VoucherNumber_Bak10,Upgrade_EAONTReference,Crm_BaseCode_860_OLD,Upgrade_EAObjectInfo,PFFCToolBarConfig,IB_MsgType,IB_AppTag,IB_AppTag_MsgType,IB_AppType";
        dbUselesTableMap.put("UFDATA", Arrays.asList(ufDataUselesTable.split(",")));

        String caccMapStr = "{\"004_2009\": \"2009\", \"004_2010\": \"2010\", \"004_2011\": \"2011\", \"004_2012\": \"2012\", \"004_2013\": \"2013\", \"004_2014\": \"2015\", \"005_2010\": \"2010\", \"005_2011\": \"2011\", \"005_2012\": \"2012\", \"005_2013\": \"2013\", \"005_2014\": \"2015\", \"006_2013\": \"2013\", \"006_2014\": \"2015\", \"007_2015\": \"2015\"}";
        caccstartendYearMap = JSONUtils.parseObject(caccMapStr, new TypeReference<Map<String, String>>() {
        });

//        String excludeDbStr = "UFDATA_002_2016,UFDATA_003_2016,UFDATA_004_2009,UFDATA_004_2010,UFDATA_004_2011,UFDATA_004_2012,UFDATA_004_2013,UFDATA_004_2014,UFDATA_004_2016,UFDATA_005_2010,UFDATA_005_2011,UFDATA_005_2012,UFDATA_005_2013,UFDATA_005_2014,UFDATA_005_2016,UFDATA_006_2013,UFDATA_006_2014,UFDATA_006_2016,UFDATA_007_2015,UFDATA_007_2016,UFDATA_008_2016,UFDATA_009_2016,UFDATA_010_2020,UFDATA_011_2017,UFDATA_012_2017,UFDATA_013_2021,UFDATA_015_2019";
        String excludeDbStr = "UFDATA_016_2019,UFDATA_002_2016";
        excludeDbs.addAll(Arrays.asList(excludeDbStr.split(",")));

        String includeDbStr = "";
//        includeDbs.addAll(Arrays.asList(includeDbStr.split(",")));

    }


    public static void main(String[] args) {
//        processUFDATADb();
        processUFSystemDb();
//        processUFMetaDb();
    }

    private static void processUTUDb() {
        String dbType = "UTU";
        List<DataSource> dataSources = queryDataSourceByName(dbType);
        String sqlTaskContent = "";
        String incrementDatetime = "start_datetime_yyyyMMddHHmmss";
        String globalParams = "[{\"prop\":\"" + incrementDatetime + "\",\"value\":\"[yyyy-MM-dd 00:00:00-1]\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}]";
        String preCustomSql = "DELETE FROM ${tableName} ";
        String primaryKeyJson = "";
        for (DataSource dataSource : dataSources) {
            if (excludeDbs.contains(dataSource.getDatabase())) {
                System.out.println("Ignore create db task, db detail: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
                continue;
            }
            System.out.println("数据库信息: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
            String connectionUrl = String.format(yyDbBaseUrl, server, 1433, dataSource.getDatabase(), user, password);
            List<TableCt> notEmptyTables;
            Set<String> incrementFields;
            List<String> noPrimarkeyTableList;
            try (Connection conn = DriverManager.getConnection(connectionUrl);) {
                notEmptyTables = getNotEmptyTableList(dbType, conn);
                noPrimarkeyTableList = getNoPrimaryKeyTableList(conn);
                incrementFields = getIncrementFields(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Map<String, String> tablePrimaryKeyMap = getTableUniqueKeyMapWithNotEmptyTables(noPrimarkeyTableList, connectionUrl, "UFSystem");
            createProcess("", dataSource.getId(), notEmptyTables.stream().map(TableCt::getTableName).collect(Collectors.toList()),
                    tablePrimaryKeyMap, dataSource.getDatabase(), "ods_cwu8_ufsystem", sqlTaskContent,
                    globalParams, preCustomSql, incrementFields, primaryKeyJson, 30, incrementDatetime);
            System.out.println("dbTablePrimaryKeyMap: \r\n" + JSONUtils.toJsonString(dbTablePrimaryKeyMap.get("UFSystem")));
        }
    }
    private static void processUFSystemDb() {
        String dbType = "UFSystem";
        List<DataSource> dataSources = queryDataSourceByName(dbType);
        String sqlTaskContent = "";
        String incrementDatetime = "start_datetime_yyyyMMddHHmmss";
        String globalParams = "[{\"prop\":\"" + incrementDatetime + "\",\"value\":\"[yyyy-MM-dd 00:00:00-1]\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}]";
        String preCustomSql = "DELETE FROM ${tableName} ";
        String primaryKeyJson = "";
        for (DataSource dataSource : dataSources) {
            if (excludeDbs.contains(dataSource.getDatabase())) {
                System.out.println("Ignore create db task, db detail: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
                continue;
            }
            System.out.println("数据库信息: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
            String connectionUrl = String.format(yyDbBaseUrl, server, 1433, dataSource.getDatabase(), user, password);
            List<TableCt> notEmptyTables;
            Set<String> incrementFields;
            List<String> noPrimarkeyTableList;
            try (Connection conn = DriverManager.getConnection(connectionUrl);) {
                notEmptyTables = getNotEmptyTableList(dbType, conn);
                noPrimarkeyTableList = getNoPrimaryKeyTableList(conn);
                incrementFields = getIncrementFields(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Map<String, String> tablePrimaryKeyMap = getTableUniqueKeyMapWithNotEmptyTables(noPrimarkeyTableList, connectionUrl, "UFSystem");
            createProcess("", dataSource.getId(), notEmptyTables.stream().map(TableCt::getTableName).collect(Collectors.toList()),
                    tablePrimaryKeyMap, dataSource.getDatabase(), "ods_cwu8_ufsystem", sqlTaskContent,
                    globalParams, preCustomSql, incrementFields, primaryKeyJson, 30, incrementDatetime);
            System.out.println("dbTablePrimaryKeyMap: \r\n" + JSONUtils.toJsonString(dbTablePrimaryKeyMap.get("UFSystem")));
        }
    }

    private static void processUFMetaDb() {
        String dbType = "UFMeta";
        List<DataSource> dataSources = queryDataSourceByName(dbType + "_");
        String sqlTaskContent = "SELECT '${data_cacc_id}' AS data_cacc_id, * FROM dule";
        String incrementDatetime = "start_datetime_yyyyMMddHHmmss";
        String globalParams = "[{\"prop\":\"" + incrementDatetime + "\",\"value\":\"1970-01-01 00:00:00\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}," +
                "{\"prop\":\"data_cacc_id\",\"value\":\"%s\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}]";
        String preCustomSql = "DELETE FROM ${tableName} WHERE data_cacc_id='${data_cacc_id}'";
        String primaryKeyJson = "[\"data_cacc_id\", \"data_year_start\", \"data_year_end\"]";
        for (DataSource dataSource : dataSources) {
            if (excludeDbs.contains(dataSource.getDatabase())) {
                System.out.println("Ignore create db task, db detail: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
                continue;
            }
            System.out.println("数据库信息: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
            String connectionUrl = String.format(yyDbBaseUrl, server, 1433, dataSource.getDatabase(), user, password);
            List<TableCt> notEmptyTables;
            Set<String> incrementFields;
            List<String> noPrimarkeyTableList;
            try (Connection conn = DriverManager.getConnection(connectionUrl);) {
                notEmptyTables = getNotEmptyTableList(dbType, conn);
                noPrimarkeyTableList = getNoPrimaryKeyTableList(conn);
                incrementFields = getIncrementFields(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Map<String, String> tablePrimaryKeyMap = getTableUniqueKeyMapWithNotEmptyTables(noPrimarkeyTableList, connectionUrl, "UFMeta");
            createProcess("活跃表", dataSource.getId(), notEmptyTables.stream().map(TableCt::getTableName).collect(Collectors.toList()),
                    tablePrimaryKeyMap, dataSource.getDatabase(), "ods_cwu8_ufmeta", sqlTaskContent,
                    globalParams, preCustomSql, incrementFields, primaryKeyJson, 30, incrementDatetime);
            System.out.println("dbTablePrimaryKeyMap: \r\n" + JSONUtils.toJsonString(dbTablePrimaryKeyMap.get("UFMeta")));
        }

    }

    private static void processUFDATADb() {
        String dbType = "UFDATA";
        List<DataSource> dataSources = queryDataSourceByName(dbType + "_");
        int minCt = 50;
        String sqlTaskContent = "SELECT '${data_cacc_id}' AS data_cacc_id, ${data_year_start} AS data_year_start, CAST(${data_year_end} AS INTEGER) AS data_year_end, * FROM dule";
        String incrementDatetime = "start_datetime_yyyyMMddHHmmss";
        String globalParams = "[{\"prop\":\"" + incrementDatetime + "\",\"value\":\"1970-01-01 00:00:00\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}," +
                "{\"prop\":\"data_cacc_id\",\"value\":\"%s\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}," +
                "{\"prop\":\"data_year_start\",\"value\":\"%s\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}," +
                "{\"prop\":\"data_year_end\",\"value\":\"%s\",\"direct\":\"OUT\",\"type\":\"VARCHAR\"}]";
        String preCustomSql = "DELETE FROM ${tableName} WHERE data_cacc_id='${data_cacc_id}' AND data_year_start=${data_year_start} AND data_year_end=${data_year_end}";
        String preCustomSqlIsNull = "DELETE FROM ${tableName} WHERE data_cacc_id='${data_cacc_id}' AND data_year_start=${data_year_start} AND data_year_end IS NULL";
        String primaryKeyJson = "[\"data_cacc_id\", \"data_year_start\", \"data_year_end\"]";

        for (DataSource dataSource : dataSources) {
            if (excludeDbs.contains(dataSource.getDatabase())) {
                System.out.println("Ignore create db task, db detail: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
                continue;
            }
            if (!includeDbs.isEmpty() && !includeDbs.contains(dataSource.getDatabase())) {
                continue;
            }
            System.out.println("数据库信息: " + dataSource.getId() + " " + dataSource.getName() + " " + dataSource.getDatabase());
            String connectionUrl = String.format(yyDbBaseUrl, server, 1433, dataSource.getDatabase(), user, password);
            String dbArr[] = dataSource.getDatabase().split("_");
            List<TableCt> notEmptyTables;
            Set<String> incrementFields;
            List<String> noPrimarkeyTableList;
            List<String> dynamicTables = new ArrayList<>();
            List<String> staticTables = new ArrayList<>();
            String endYear = caccstartendYearMap.get(dbArr[1] + "_" + dbArr[2]);
            String currentPreCustomSql = StringUtils.isEmpty(endYear) ? preCustomSqlIsNull : preCustomSql;
            String currentGlobalParams = String.format(globalParams, dbArr[1], dbArr[2], caccstartendYearMap.get(dbArr[1] + "_" + dbArr[2]));
            try (Connection conn = DriverManager.getConnection(connectionUrl);) {
                notEmptyTables = getNotEmptyTableList(dbType, conn);
                noPrimarkeyTableList = getNoPrimaryKeyTableList(conn);
                incrementFields = getIncrementFields(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Map<String, String> tablePrimaryKeyMap = getTableUniqueKeyMapWithNotEmptyTables(noPrimarkeyTableList, connectionUrl, "UFDATA");
            classifyTables(notEmptyTables, dynamicTables, staticTables, minCt, "UFDATA");
            if (!dynamicTables.isEmpty()) {
                createProcess("活跃表", dataSource.getId(), dynamicTables, tablePrimaryKeyMap,
                        dataSource.getDatabase(), "ods_cwu8_ufdata", sqlTaskContent,
                        currentGlobalParams, currentPreCustomSql,
                        incrementFields, primaryKeyJson, 80, incrementDatetime);
            }
            if (!staticTables.isEmpty()) {
                createProcess("静态表", dataSource.getId(), staticTables, tablePrimaryKeyMap,
                        dataSource.getDatabase(), "ods_cwu8_ufdata", sqlTaskContent,
                        currentGlobalParams, currentPreCustomSql, incrementFields,
                        primaryKeyJson, 100, incrementDatetime);
            }
            System.out.println("dbTablePrimaryKeyMap: \r\n" + JSONUtils.toJsonString(dbTablePrimaryKeyMap.get("UFDATA")));
        }
    }

    private static void createProcess(String type, int sourceDatasourceId, List<String> tables,
                                      Map<String, String> tablePrimaryKeyMap, String database, String targetTablePrefix,
                                      String sqlTaskContent, String globalParams, String preCustomSql,
                                      Set<String> incrementFields, String primaryKeyJson, int serialTaskNum, String incrementDatetime) {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(damUrl + "/dolphinscheduler/projects/" + projectCode + "/batch-create-definition/create-by-datasource-tables");

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setCharset(StandardCharsets.UTF_8); // 设置字符编码
            String name = type + "-" + database + "数据库";
            // 添加表单参数
            entityBuilder.addPart("sourceDatasourceId", new StringBody(String.valueOf(sourceDatasourceId), TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("targetDatasourceId", new StringBody(targetDatasourceId, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("serialTaskNum", new StringBody(String.valueOf(serialTaskNum), TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("etlTechnology", new StringBody("SEATUNNEL", TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("deployMode", new StringBody("cluster", TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("name", new StringBody(name, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("targetTablePrefix", new StringBody(targetTablePrefix, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("enableIncrement", new StringBody("true", TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("incrementFieldJson", new StringBody(JSONUtils.toJsonString(incrementFields), TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("incrementParam", new StringBody(incrementDatetime, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("filterType", new StringBody("include", TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("filterContentJson", new StringBody(JSONUtils.toJsonString(tables), TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("primaryKeyJson", new StringBody(primaryKeyJson, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("sqlTaskContent", new StringBody(sqlTaskContent, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("globalParams", new StringBody(globalParams, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("preCustomSql", new StringBody(preCustomSql, TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("dataSaveModel", new StringBody("APPEND_DATA", TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("tableUniqueKeyJson", new StringBody(JSONUtils.toJsonString(tablePrimaryKeyMap), TEXT_PLAIN_UTF_8));
            entityBuilder.addPart("isSplitProcess", new StringBody("false", TEXT_PLAIN_UTF_8));

            httpPost.setEntity(entityBuilder.build());

            httpPost.addHeader("sessionid", sessionId);
            // 执行请求
            HttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity());

            System.out.println("API Response: " + responseBody);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<String> getIncrementFields(Connection conn) throws SQLException {
        String sql = "SELECT \n" +
                "    SCHEMA_NAME(t.schema_id) AS [SchemaName],\n" +
                "    t.name AS [TableName],\n" +
                "    c.name AS [ColumnName],\n" +
                "    TYPE_NAME(c.user_type_id) AS [DataType]\n" +
                "FROM sys.tables t\n" +
                "INNER JOIN sys.columns c ON t.object_id = c.object_id \n" +
                "WHERE TYPE_NAME(c.user_type_id) = 'timestamp'\n" +
                "    AND t.name IN (\n" +
                "      SELECT t.name\n" +
                "      FROM sys.tables t\n" +
                "      INNER JOIN sys.partitions p ON t.object_id = p.object_id\n" +
                "      WHERE p.index_id IN (0, 1)\n" +
                "          AND t.type = 'U'\n" +
                "          AND t.is_ms_shipped = 0\n" +
                "      GROUP BY t.name\n" +
                "      HAVING SUM(p.rows) > 50\n" +
                "    )\n" +
                "ORDER BY t.name, c.name";
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                columns.add(rs.getString("ColumnName"));
            }
        }
        return columns;
    }

    private static Map<String, String> getTableUniqueKeyMapWithNotEmptyTables(List<String> noPrimaryKeyTableList,
                                                                              String jdbcUrl, String database) {
        Map<String, String> tablePrimaryKeyMap = new HashMap<>();
        failedTables.clear(); // 清空之前的失败记录
//        System.out.println(JSONUtils.toJsonString(dbTablePrimaryKeyMap.get("UFDATA")));
        for (String tableName : noPrimaryKeyTableList) {
            if (dbTablePrimaryKeyMap.containsKey(database) && dbTablePrimaryKeyMap.get(database).containsKey(tableName)) {
                tablePrimaryKeyMap.put(tableName, dbTablePrimaryKeyMap.get(database).get(tableName));
                continue;
            }
            try (Connection conn = DriverManager.getConnection(jdbcUrl);) {
                String uniqueKey = getUniqueKey(conn, database, tableName);
                tablePrimaryKeyMap.put(tableName, uniqueKey);
                System.out.println(JSONUtils.toJsonString(dbTablePrimaryKeyMap.get(database)));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return tablePrimaryKeyMap;
    }

    private static Map<String, String> getTableUniqueKeyMapWithNotEmptyTables(List<String> noPrimaryKeyTableList,
                                                                              Connection conn, String database) {
        Map<String, String> tablePrimaryKeyMap = new HashMap<>();
        failedTables.clear(); // 清空之前的失败记录

        for (String tableName : noPrimaryKeyTableList) {
            String uniqueKey = getUniqueKey(conn, database, tableName);
            tablePrimaryKeyMap.put(tableName, uniqueKey);
        }

        return tablePrimaryKeyMap;
    }

    private static String getUniqueKey(Connection conn, String database, String tableName) {
        if (dbTablePrimaryKeyMap.containsKey(database) && dbTablePrimaryKeyMap.get(database).containsKey(tableName)) {
            return dbTablePrimaryKeyMap.get(database).get(tableName);
        }
        try {
            // 获取表的列信息
            List<String> columnNames = getTableColumns(conn, tableName);

            if (columnNames.isEmpty()) {
                return "";
            }

            // 尝试找出能保证数据唯一的列组合
            String uniqueColumns = findUniqueColumns(conn, tableName, columnNames);

            if (uniqueColumns != null && !uniqueColumns.isEmpty()) {
                dbTablePrimaryKeyMap.get(database).put(tableName, uniqueColumns);
                return uniqueColumns;
            } else {
                dbTablePrimaryKeyMap.get(database).put(tableName, "");
                return "";
            }
        } catch (SQLException e) {
            System.err.println("处理表 " + tableName + " 时发生错误: " + e.getMessage());
            failedTables.add(tableName); // 记录失败的表
            // 继续处理下一个表
        }
        return "";
    }

    /**
     * 获取表的所有列名
     */
    private static List<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (supportedType(rs.getString("DATA_TYPE"))) {
                        columnNames.add(rs.getString("COLUMN_NAME"));
                    }
                }
            }
        }

        return columnNames;
    }

    private static boolean supportedType(String dataType) {
        switch (dataType) {
            case "int":
            case "bigint":
            case "nvarchar":
            case "varchar":
            case "uniqueidentifier":
            case "timestamp":
                return true;
        }
        return false;
    }

    /**
     * 尝试找出能保证数据唯一的列组合
     */
    private static String findUniqueColumns(Connection conn, String tableName, List<String> columnNames) throws SQLException {
        // 尝试不同的列组合来找到唯一性约束
        // 从单列开始，逐步增加列数

        // 首先尝试单列唯一性
        for (String column : columnNames) {
            if (isColumnUnique(conn, tableName, column)) {
                return column; // 找到单列唯一性，直接返回
            }
        }

        // 如果单列不唯一，尝试两列组合
        for (int i = 0; i < columnNames.size(); i++) {
            for (int j = i + 1; j < columnNames.size(); j++) {
                String col1 = columnNames.get(i);
                String col2 = columnNames.get(j);
                String combinedColumns = col1 + "," + col2;

                if (isColumnsUnique(conn, tableName, Arrays.asList(col1, col2))) {
                    return combinedColumns;
                }
            }
        }

        // 如果两列组合还不够，可以继续尝试更多列的组合
        // 为了效率考虑，这里只尝试到3列组合
        for (int i = 0; i < columnNames.size(); i++) {
            for (int j = i + 1; j < columnNames.size(); j++) {
                for (int k = j + 1; k < columnNames.size(); k++) {
                    String col1 = columnNames.get(i);
                    String col2 = columnNames.get(j);
                    String col3 = columnNames.get(k);
                    String combinedColumns = col1 + "," + col2 + "," + col3;

                    if (isColumnsUnique(conn, tableName, Arrays.asList(col1, col2, col3))) {
                        return combinedColumns;
                    }
                }
            }
        }

        // 如果没有找到唯一列组合，返回空字符串
        return "";
    }

    /**
     * 检查单列是否唯一
     */
    private static boolean isColumnUnique(Connection conn, String tableName, String columnName) throws SQLException {
        // 检查数据类型是否支持 GROUP BY
        if (!isColumnGroupable(conn, tableName, columnName)) {
            return false; // 如果列类型不支持分组，直接返回 false
        }

        // 转义 SQL 关键字
        String escapedColumnName = escapeSqlKeyword(columnName);
        String sql = "SELECT top 3 " + escapedColumnName + ", COUNT(1) AS ct FROM " + tableName +
                " GROUP BY " + escapedColumnName + " ORDER BY ct DESC";
        System.out.println("Find primary key, sql: " + sql);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long count = rs.getLong("ct");
                // 如果最大计数大于1，说明该列不是唯一的
                return count == 1;
            }
        }

        return true; // 如果没有数据，认为是唯一的
    }

    /**
     * 检查多列组合是否唯一
     */
    private static boolean isColumnsUnique(Connection conn, String tableName, List<String> columnNames) throws SQLException {
        // 检查所有列是否都支持 GROUP BY
        for (String columnName : columnNames) {
            if (!isColumnGroupable(conn, tableName, columnName)) {
                return false; // 如果有任何一列不支持分组，返回 false
            }
        }

        // 转义 SQL 关键字
        List<String> escapedColumns = new ArrayList<>();
        for (String columnName : columnNames) {
            escapedColumns.add(escapeSqlKeyword(columnName));
        }
        String columnsStr = String.join(", ", escapedColumns);
        String sql = "SELECT " + columnsStr + ", COUNT(1) AS ct FROM " + tableName +
                " GROUP BY " + columnsStr + " ORDER BY ct DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                long count = rs.getLong("ct");
                // 如果最大计数大于1，说明该列组合不是唯一的
                return count == 1;
            }
        }

        return true; // 如果没有数据，认为是唯一的
    }

    // 检查列是否支持 GROUP BY（即不是 text、ntext、image 等类型）
    private static boolean isColumnGroupable(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            pstmt.setString(2, columnName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dataType = rs.getString("DATA_TYPE").toLowerCase();
                    // 检查是否为不支持 GROUP BY 的数据类型
                    return !Arrays.asList("text", "ntext", "image", "xml", "varbinary", "binary").contains(dataType);
                }
            }
        }
        return false; // 如果无法获取数据类型，认为不支持
    }

    // 转义 SQL 关键字
    private static String escapeSqlKeyword(String columnName) {
        if (SQL_KEYWORD_PATTERN.matcher(columnName).matches()) {
            return "[" + columnName + "]"; // 使用方括号转义 SQL 关键字
        }
        return columnName;
    }


    private static List<String> getNoPrimaryKeyTableList(Connection conn) {
        String sql = "SELECT \n" +
                "    SCHEMA_NAME(t.schema_id) AS [SchemaName],\n" +
                "    t.name AS [TableName],\n" +
                "    t.create_date AS [CreateDate],\n" +
                "    t.modify_date AS [LastModified],\n" +
                "    p.rows AS [RowCount],\n" +
                "    CAST(ROUND(SUM(a.total_pages) * 8.0 / 1024, 2) AS DECIMAL(10,2)) AS [TotalSize_MB]\n" +
                "FROM sys.tables t\n" +
                "INNER JOIN sys.indexes i ON t.object_id = i.object_id\n" +
                "INNER JOIN sys.partitions p ON i.object_id = p.object_id AND i.index_id = p.index_id\n" +
                "INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id\n" +
                "WHERE t.is_ms_shipped = 0\n" +
                "    AND i.index_id <= 1\n" +
                "    AND NOT EXISTS (\n" +
                "        SELECT 1 \n" +
                "        FROM sys.indexes i2 \n" +
                "        WHERE i2.object_id = t.object_id \n" +
                "            AND i2.is_primary_key = 1\n" +
                "    )\n" +
                "    AND t.name IN (\n" +
                "      SELECT t.name\n" +
                "      FROM sys.tables t\n" +
                "      INNER JOIN sys.partitions p ON t.object_id = p.object_id\n" +
                "      WHERE p.index_id IN (0, 1)\n" +
                "          AND t.type = 'U'\n" +
                "          AND t.is_ms_shipped = 0\n" +
                "      --     AND t.name IN ()\n" +
                "      GROUP BY t.name\n" +
                "      HAVING SUM(p.rows) > 0\n" +
                "    )\n" +
                "GROUP BY SCHEMA_NAME(t.schema_id), t.name, t.create_date, t.modify_date, p.rows\n" +
                "ORDER BY [SchemaName], [TableName];";
        List<String> tableList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
//                String schemaName = rs.getString("SchemaName");
//                int rowCount = rs.getInt("RowCount");
//                System.out.println(schemaName + "." + tableName + ": " + rowCount);
//                String tableName = rs.getString("TableName");
                tableList.add(rs.getString("TableName"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tableList;
    }

    private static void classifyTables(List<TableCt> notEmptyTables, List<String> dynamicTables,
                                       List<String> staticTables, int minCt, String db) {
        for (TableCt tableCt : notEmptyTables) {
            if (tableCt.getRowCt() > minCt) {
                // 数据量大，是不重要的表
                if (dbUnimportantTableMap.get(db).contains(tableCt.getTableName())) {
                    staticTables.add(tableCt.getTableName());
                } else {
                    dynamicTables.add(tableCt.getTableName());
                }
            } else {
                // 数据量小，是重要的表
                if (dbImportantTableMap.get(db).contains(tableCt.getTableName())) {
                    dynamicTables.add(tableCt.getTableName());
                } else {
                    staticTables.add(tableCt.getTableName());
                }
            }
        }

    }


    private static List<TableCt> getNotEmptyTableList(String dbType, Connection conn) {
        String sql = "SELECT \n" +
                "    SCHEMA_NAME(t.schema_id) AS [SchemaName],\n" +
                "    t.name AS [TableName],\n" +
                "    SUM(p.rows) AS [RowCount]\n" +
                "FROM sys.tables t\n" +
                "INNER JOIN sys.partitions p ON t.object_id = p.object_id\n" +
                "WHERE p.index_id IN (0, 1) \n" +
                "    AND t.type = 'U' \n" +
                "    AND t.is_ms_shipped = 0 \n" +
                "--     AND t.name IN ()\n" +
                "GROUP BY t.schema_id, t.name\n" +
                "HAVING SUM(p.rows) > 0\n" +
                "ORDER BY [RowCount] DESC, [SchemaName], [TableName];\n";
        List<TableCt> tableList = new ArrayList<>();
        List<String> uselessTables = dbUselesTableMap.get(dbType);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String tableName = rs.getString("TableName");
                // 过滤无用的表
                if (uselessTables != null && uselessTables.contains(tableName)) {
                    continue;
                }
                int rowCount = rs.getInt("RowCount");
                tableList.add(new TableCt(tableName, rowCount));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return tableList;
    }

    public static List<DataSource> queryDataSourceByName(String name) {
        List<DataSource> results = new ArrayList<>();
        String sql = "SELECT * FROM t_ds_datasource WHERE name LIKE ? ORDER BY name asc";

        try (Connection conn = DriverManager.getConnection(damJdbcUrl, damUserName, damPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 设置模糊查询参数
            pstmt.setString(1, "%" + name + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // 简单返回name字段，可根据需要返回其他字段
                    String database = parseDatabaseNameFromJson(rs.getString("connection_params"));
                    results.add(new DataSource(rs.getInt("id"), rs.getString("name"), database, "dbo"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    private static String parseDatabaseNameFromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        Map<String, String> params = JSONUtils.parseObject(jsonStr, Map.class);
        if (params != null && params.containsKey("database")) {
            return params.get("database");
        }
        return null;
    }


}


@Data
@AllArgsConstructor
class TableCt {
    private String tableName;
    private int rowCt;
}

@Data
class DataSource {
    private int id;
    private String name;
    private String database;
    private String schema;

    public DataSource(int id, String name, String database, String schema) {
        this.id = id;
        this.name = name;
        this.database = database;
        this.schema = schema;
    }
}
