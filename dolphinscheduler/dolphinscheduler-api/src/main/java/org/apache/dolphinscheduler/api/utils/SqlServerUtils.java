package org.apache.dolphinscheduler.api.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

public class SqlServerUtils {
    private static final Pattern SQL_KEYWORD_PATTERN = Pattern.compile("^(desc|key|order|group|select|from|where|having|by|and|or|not|as|on|join|inner|outer|left|right|union|intersect|except|create|drop|alter|insert|update|delete|table|column|index|primary|foreign|references|constraint|default|check|unique|clustered|nonclustered|with|option|values|set|into|all|any|some|exists|between|in|like|is|null|case|when|then|else|end|begin|try|catch|throw|goto|return|break|continue|declare|exec|execute|open|fetch|close|deallocate|print|raiserror|begin|commit|rollback|transaction|save|checkpoint|backup|restore|dbcc|kill|shutdown|reconfigure|configure|sp_|xp_|fn_|sys|information_schema|all|any|between|cross|except|foreign|group|having|in|inner|intersect|into|is|join|like|not|null|on|or|order|outer|select|some|union|where|add|all|alter|analyze|and|as|asc|asensitive|before|between|bigint|binary|blob|both|by|call|cascade|case|change|char|character|check|collate|column|condition|connection|constraint|continue|convert|create|cross|current_date|current_time|current_timestamp|current_user|cursor|database|databases|day_hour|day_microsecond|day_minute|day_second|dec|decimal|declare|default|delayed|delete|desc|describe|deterministic|distinct|distinctrow|div|double|drop|dual|each|else|elseif|enclosed|escaped|exists|exit|explain|false|fetch|float|float4|float8|for|force|foreign|from|fulltext|goto|grant|group|having|high_priority|hour_microsecond|hour_minute|hour_second|if|ignore|in|index|infile|inner|inout|insensitive|insert|int|int1|integer|interval|into|is|iterate|join|key|keys|kill|leading|leave|left|like|limit|lines|load|localtime|localtimestamp|lock|long|longblob|longtext|loop|low_priority|match|mediumblob|mediumint|mediumtext|middleint|minute_microsecond|minute_second|mod|modifies|natural|not|no_write_to_binlog|NULL|numeric|on|optimize|option|optionally|or|order|out|outer|outfile|precision|primary|procedure|purge|raid0|read|reads|real|references|regexp|release|rename|repeat|replace|require|restrict|return|revoke|right|rlike|schema|schemas|second_microsecond|select|sensitive|separator|set|show|smallint|soname|spatial|specific|sql|sql_big_result|sql_calc_found_rows|sql_small_result|sqlexception|sqlstate|sqlwarning|ssl|starting|straight_join|table|terminated|then|to|trailing|trigger|true|undo|union|unique|unlock|unsigned|update|usage|use|using|utc_date|utc_time|utc_timestamp|values|varbinary|varchar|varcharacter|varying|when|where|while|with|write|x509|xml|year_month|zerofill|action|admin|after|aggregate|allocation|analyse|array|asensitive|at|authors|auto_increment|avg_row_length|avg|backward|bit|bool|boolean|both|cascade|cascaded|catalog_name|chain|characteristics|character_set_catalog|character_set_name|character_set_schema|class_origin|client|clobber|code|collation_catalog|collation_name|collation_schema|column_name|command_class|commit|committed|condition_number|connection|const|constraint_catalog|constraint_name|constraint_schema|constructor|contains|continue|convert|copy|corr|corresponding|cpu|cube|cume_dist|current|current_schema|cursor_name|cycle|data|database|date|datetime|day|deallocate|dec|decimal|default|deferrable|deferred|defined|definer|degree|delay_key_write|delayed|delete|delimiter|delimiters|dense_rank|deterministic|diagnostics|directory|disable|disallow|disconnect|dispatch|distinct|do|document|domain|double|drop|dual|dynamic|dynamic_function|dynamic_function_code|each|element|else|elseif|enable|enclosed|encoding|encrypted|end|end-exec|enum|equals|error|escape|escaped|every|except|exception|exchange|exclude|exclusive|exec|execute|existing|expansion|expire|explain|export|expression|extension|external|extract|false|fetch|fields|file|first|first_value|fixed|following|follows|for|force|format|found|free|freeze|from|full|function|fusion|general|generated|geometry|geometrycollection|get|global|go|goto|grant|grants|greatest|group|grouping|groups|handler|hash|header|having|high_priority|hold|host|hosts|hour|identifier|identity|if|ignore|import|in|index|indicator|infile|inout|insensitive|insert|insert_method|install|instead|instr|integer|int|intersect|interval|into|invoker|is|isnull|isolation|issuer|iterate|join|json|key|keys|kill|label|lag|language|last|last_value|lateral|leading|least|leave|left|length|less|level|like|limit|linear|lines|linestring|listagg|listen|load|local|localtime|localtimestamp|location|locator|lock|locked|log|logfile|logs|longblob|longtext|loop|low_priority|master|match|materialized|max_rows|master_ssl_verify_server_cert|max|mediumblob|mediumint|mediumtext|memory|merge|message_length|message_octet_length|message_text|migrate|min_rows|min|minute|mod|mode|modifies|modify|multilinestring|multipoint|multipolygon|multiset|mumps|name|names|national|natural|nchar|nclob|nested|new|next|no|none|not|notnull|nowait|nullif|null|number|numeric|object|octet_length|of|off|offset|oids|old|on|one|only|open|optimize|option|options|or|order|ordinality|others|out|outer|outfile|output|over|overlaps|overlay|owner|pack_keys|pad|parameter|parameter_mode|parameter_name|parameter_ordinal_position|parameter_specific_catalog|parameter_specific_name|parameter_specific_schema|partial|partition|pascal|password|path|percent_rank|percentile_cont|percentile_disc|placing|plan|plugin|plugins|port|position|preceding|precision|prepare|preserve|primary|prior|privileges|procedure|process|processlist|profile|profiles|proxy|public|purge|quarter|query|quick|raise|random|range|rank|read|reads|real|reassign|rebuild|record|recursive|reduce|references|regexp|regr_avgx|regr_avgy|regr_count|regr_intercept|regr_r2|regr_slope|regr_sxx|regr_sxy|regr_syy|reindex|relative|relaylog|release|rename|repeat|repeatable|replace|replication|require|reset|resignal|restore|restrict|result|return|returned_cardinality|returned_length|returned_octet_length|returned_sqlstate|returns|revoke|right|role|routines|row_count|row_format|row_number|row|rows|rule|savepoint|schedule|schema|schema_name|schemas|scope|scope_catalog|scope_name|scope_schema|scroll|search|second|section|security|select|self|sensitive|separator|sequence|serializable|server_name|session|session_user|set|share|shared|show|shutdown|signal|similar|simple|size|smallint|snapshot|some|soname|soundex|source|space|spatial|specific|specifictype|sql|sql_big_result|sql_cache|sql_calc_found_rows|sql_no_cache|sql_small_result|sql_state|sql_warning|sqlexception|sqlstate|sqlvolume|sqrt|ssl|standalone|start|starting|starts|statement|static|statistics|status|stddev_pop|stddev_samp|stdin|stdout|storage|straight_join|strict|string|striped|structure|style|subclass_origin|subject|substring|successful|sum|superuser|table_name|table|tables|tablespace|temporary|temptable|terminated|text|than|then|ties|time|timestamp|timezone_hour|timezone_minute|tinyblob|tinyint|tinytext|to|trailing|transaction|translate|translation|treat|trigger|triggers|trim|true|truncate|type|uescape|unbounded|uncommitted|undefined|under|undo|unencrypted|union|unique|unknown|unlisten|unlock|unsigned|until|update|usage|use|user|user_resources|using|utc_date|utc_time|utc_timestamp|vacuum|valid|validate|validator|value|values|var_pop|var_samp|varbinary|varchar|varcharacter|variable|variables|varying|verbose|version|view|virtual|visible|volatile|wait|when|where|while|width_bucket|window|with|within|without|work|wrapper|write|xml|xmlattributes|xmlbinary|xmlcast|xmlcomment|xmlconcat|xmldocument|xmlelement|xmlexists|xmlforest|xmliterate|xmlnamespaces|xmlparse|xmlpi|xmlquery|xmlroot|xmlschema|xmlserialize|xmltable|xmltext|xmlvalidate|year|zone)$", Pattern.CASE_INSENSITIVE);
    private static String server = "172.41.133.84";
    private static String database = "UFDATA_027_2025";
    private static String user = "sa";
    private static String password = "admin@123";
    private static String baseUrl = "jdbc:sqlserver://%s:%s;" +
            "databaseName=%s;" +
            "user=%s;" +
            "password=%s;" +
            "encrypt=false;" +
            "trustServerCertificate=true;" +
            "sslProtocol=TLSv1;" +
            "loginTimeout=30;" +
            "sendTimeAsDatetime=false;" +
            "sendStringParametersAsUnicode=true;";

    // 添加失败表记录功能
    private static Set<String> failedTables = new HashSet<>();

    private static Map<String, Map<String, String>> dbTablePrimaryKeyMap = new HashMap<>();

    static {
        String ufSystem = "{\"OMI_RecordExtMark\":\"ID\",\"openapi_use\":\"c_token\",\"UA_Version\":\"iVer\",\"UA_MenuRulePLugin\":\"Key\",\"UA_MutexAuthItem\":\"ID,cAuth_ID\",\"UA_PortalAutoExitExincludeTasks\":\"cAuth_Id\",\"ua_Task_Common\":\"iLogId\",\"CN_SubModuleCatalog\":\"SubModuleKey\",\"UA_Password\":\"iMinLen\",\"UA_NavigationNode_Field\":\"Name,Property\",\"UA_NavigationNode_FieldEx\":\"Name,Property,UserId\",\"UA_Flow\":\"ID\",\"GL_CodeCheckStatus\":\"sAccount\",\"open_api_lsfx_versiontag\":\"tag\",\"CN_ModuleCatalog\":\"ID\",\"UA_PortalLang\":\"cCaption\",\"GL_BTrade\":\"itrade_id,LocaleId\",\"UA_AALog_base\":\"cLog_Txt,dInTime\",\"UA_BackupPath\":\"cPlan_Id\",\"CN_Version\":\"ID\",\"UA_ControlEx\":\"cAuth_Id,cRepellent\",\"GL_DPrecode\":\"itrade_id,ccode,LocaleId\",\"UA_UpgradeBatch\":\"iVer,iIndex\",\"ProjectStandard\":\"ID\",\"UA_ToNextYear_base\":\"iNo,cTblId,localeid\",\"UA_Chart\":\"ID\",\"UA_Log\":\"iLogId\",\"UA_ReplStru_order\":\"cName\"}";
        dbTablePrimaryKeyMap.put("UFSystem", JSONUtils.parseObject(ufSystem, new TypeReference<Map<String, String>>() {
        }));
        String ufData = "{\"Rpt_FldDEF_Base_Bak\":\"\",\"referarchives_local\":\"ID,localid\",\"referarchives_base\":\"\",\"REFERARCHIVEMAPPING\":\"REFERFIELDID,MASKVALUE,ID\",\"NB_BankProfileRef\":\"RefType,BankCode,RefValue\",\"HY_NeedUpdLinkField\":\"HY,subid,cardnum,field\",\"hr_hi_workflowupdatelog\":\"id\",\"HR_BD_CalcRuleObjFldReg\":\"cTableCode,cItemCode\",\"GL_RepPrintTemplate\":\"cDocName,dSynDate\",\"GL_bfreqInfo\":\"Inid\",\"Code_LogDetail\":\"iLogID\",\"CM_TrackFieldUser\":\"cClueCode,cFieldID\",\"CM_DD\":\"strTbl,strFld,lngStatus\",\"ap_digset_base\":\"localeid\",\"UA_Modules\":\"ModuleID\",\"fc_UpgradeLog\":\"autoid\",\"KC_Ascii_Config\":\"ID\",\"EAON_Template\":\"EAONT_ID\",\"UFMenu_Business_Lang\":\"MenuId\",\"ex_vouch_authdefine\":\"vouchguid\",\"IM_BusType\":\"iimbustype\",\"UA_UserRelation_ex\":\"cUser_ID\",\"UTU_MessageCenter_User\":\"iMessageID\",\"Ca_Visio\":\"cMapID\",\"IM_LinkedRelation\":\"LinkedSP\",\"tc_procInvok_log\":\"log_id\",\"Exp_FuncDes_Lang\":\"cFuncId\",\"hr_sys_sub_Base\":\"cSub_id\",\"UTU_MessageCenter_U8ApproveTask\":\"uidID\",\"UFTmpTable_38428250_5557019145346523289\":\"I_id\",\"tc_obj_rel_profile\":\"obj_type,rel_obj_type\",\"GL_RepPrintTemplate_log\":\"cOperator\",\"VoucherExtendArcType\":\"ctype\",\"TaskTypeDef\":\"TaskTypeID\",\"EB_IndexHomeNews\":\"autoID\",\"HR_BD_DeptAuthSet_lang\":\"cfid,LocaleID\",\"hr_sys_function_Base\":\"cAuth_ID\",\"NB_BankNameConfig\":\"cBankName\",\"ST_DefineSet\":\"id\",\"SA_GoldTaxField\":\"id,cField\",\"UFFA_Model_fa_VtsObject_Init\":\"Id\",\"SA_OrderAscendConfig\":\"cRptName,iTabIndex\",\"AA_PortalIndicatorView_Default\":\"ScenarioID,ViewID\",\"Exp_FuncReturn_Lang\":\"cFuncId\",\"HY_TM_BarCodeSource_base\":\"cSrcID,cSrcName\",\"QM_RANDORPROJECTS\":\"autoid\",\"UTU_MessageCenter\":\"iID\",\"CN_Version\":\"Version\",\"UFFA_Model_fa_ItemsOfQuery_Init\":\"iQuerysType,sName\",\"Rpt_CustomEvents_Base\":\"ReportName,ActionClass,Caption\",\"UTU_SystemMessage_User\":\"uidID\",\"UA_User_Ex\":\"cUser_Id\",\"UA_Identity_Ex\":\"cVouchType\",\"fc_OpColor\":\"Flag\",\"tc_object_stage_option\":\"stage_id,obj_type\",\"Hr_bd_VoucherTable\":\"id\",\"BG_Archive_SYS\":\"guid\",\"Ca_UpdateInvCodeToPartID\":\"TableName\",\"GL_ProcedureDic\":\"cProcedureName\",\"Pe_Version\":\"AutoID\",\"EX_LinkedRelation\":\"SourceVouchType,QueryType,TargetVouchType\",\"ScmAscendLinkInfo\":\"cVouchtypeCode,CNextVouchtypeCode\",\"eb_goldtax\":\"id\",\"SNPrefabricate_lang\":\"cdatasrccaption\",\"tc_backfill_setting\":\"id\",\"IM_VouchInfo\":\"vouchtype\",\"NE_Extend\":\"DocType\",\"EX_LinkedVouch\":\"vouchtype\",\"UA_Password_Ex\":\"iMinLen\",\"CM_MS\":\"strFld\",\"HR_TM_LossNewMachine\":\"cMachine\",\"fa_CheckConfig\":\"sID\",\"UA_Scenario_bak\":\"cID\",\"NE_CountryCity\":\"ID\",\"HR_BD_QueryParaItem\":\"iParaID\",\"NB_InputConfig\":\"cKey\",\"UA_Flow_Sys\":\"ID\",\"SCMVouchRelation\":\"autoid\",\"Ca_JZ_YearBegin_Def\":\"TabName\",\"WA_SDS_Set\":\"yMin_level,yMax_level\",\"Table_TemplateKind_Lang\":\"KindID\",\"CM_ExecInterface\":\"cContractID\",\"HR_HI_EnterCarryField\":\"PK_HR_HI_EnterCarryField\",\"REFERBIZFIELDS\":\"REFERFIELDID\",\"UA_ModulesSet\":\"ModuleID,cUserID\",\"AA_ExtendDefineMakeVouch_base\":\"cMakeVouchID\",\"BG_ColumnDatatypeContra\":\"cU8DataType\",\"ex_idemandtype\":\"idemandtype\",\"hr_tm_OverTimeCardExcursion\":\"BeginCardAhead\",\"AuditPlugins_Lang\":\"cPluginId\",\"HR_BD_CalcRuleObjReg\":\"cTableCode\",\"UA_Chart\":\"ID\",\"ST_VMIUsedRule\":\"AutoID\",\"UFFA_Model_fa_Origins_Init\":\"sID\",\"UFFA_Model_fa_Objects_Init\":\"iObjectNum\",\"Pe_SzItem\":\"AutoID\",\"scmVouchRelInfo\":\"clinkdetailtbl\",\"NE_PzSet_ZYTREE\":\"ID\",\"AA_Medal_Config\":\"autoid\",\"UA_ScenarioView_Flowlayout_Bak\":\"cInstanceID,cScenarioID\",\"UA_Operation\":\"CreatedOn\",\"hr_ct055\":\"ccodeID\",\"SCM_ClearStock\":\"autoid\",\"rpt_glbdef_base_bak\":\"ID,LocaleID\",\"UFOperation_Business_Lang\":\"MenuId,LocaleId,UserID\",\"ex_bondedbilltype\":\"ibondedbilltype\",\"EB_EBCompanyRelation\":\"cEBPlatForm,cCompanyCode,cCompany_Code\",\"GL_MRReportOption\":\"iReportID\",\"RP_TableField\":\"cTableName,cFieldName\",\"UFFA_Model_fa_AssetGroup_Init\":\"sID\",\"UFFA_Model_fa_AssetTypes_Init\":\"sID\",\"rb_UserRoles\":\"UserID\",\"Ex_OrderDateField\":\"cfieldcode\",\"Pm_VouchType_Base\":\"cTypeCode\",\"bas_systemsettingnbr\":\"Type\",\"BatchTask\":\"cVoumenu_id,cvouchertype\",\"UFSUB_fitemss00\":\"I_id\",\"AuditBizObjects_Lang\":\"cBizObjectId\",\"AA_ColumnModifyForExtend\":\"cFld\",\"Table_Template_Lang\":\"ID\",\"AuditBizEvents_Lang\":\"cBizEventId\",\"EB_WlbSPCode\":\"cCompanyCode,cCompany_Code\",\"U8LoginProcess\":\"dInTime\",\"UA_Log_Ex\":\"dInTime\",\"RP_CurrencyValue\":\"i_id\",\"UA_Account_Ex\":\"iSysID\",\"FA_LabelType\":\"cLableTypeCode\",\"EB_WlbPlatChannels\":\"cEBPlatForm\",\"HR_BD_QueryStatWay\":\"vName\",\"UA_Account_sub_Ex\":\"iYear,cSub_Id\",\"voucherprefabricate_lang\":\"ufts\",\"IM_ReferGroupBy_lang\":\"referkey,fname\",\"TaskProcessorDef\":\"ProcessorName\",\"Ap_BillAge\":\"cNum\",\"SA_UnVerifyCheck\":\"autoid\",\"TI_InvoiceImportSet\":\"autoid\",\"eb_goldtaxfield\":\"id\",\"Bombuttonset\":\"bombuttonflag\",\"UA_DesktopPage_Bak\":\"cPageID\",\"EAONT_NoteML\":\"EAONT_ID\",\"UA_AccountDatabase_Ex\":\"cAcc_Id\",\"UFFA_Model_fa_CardOfPUColSet_Init\":\"sIDKey\",\"VariationDeleteKeyFld\":\"BillType,cFldName\",\"hr_bd_rulecontact\":\"cRuleId,cItem\",\"PFFCGoToVoucherConfig\":\"condition\",\"Pe_SysInfo\":\"autoID\",\"hr_sys_ValuesMapping\":\"pk_id\",\"cClassName_Lang\":\"cClassName\",\"lotnumberPrefabricate_base\":\"cdatasrccaption\",\"AA_Medal\":\"cmedcode\",\"SA_GoToVoucherConfig\":\"autoid\",\"UTU_Broadcast\":\"uidID\",\"EB_ButtonAuth\":\"autoID\",\"mom_MetricDimension\":\"cDimCode,cMetCode\",\"ua_idt_EX\":\"ufts\",\"eb_splitcode\":\"identityID\",\"tc_copyvouch_d\":\"cKey,cFld\",\"Pe_GetDataOptDetails\":\"cSelID\",\"rb_Current_TabTree\":\"TabID\",\"UTU_Broadcast_User\":\"uidID\",\"HR_Bd_TableDataType\":\"TableName\",\"hr_bd_mcitemassign\":\"PK_Item\",\"CM_TrackClueUser\":\"cClueCode\",\"KC_WireMenu_base\":\"ID\",\"NB_AreaCode_province\":\"AREA_CODE\",\"CM_Contract_B\":\"GUID\",\"HR_FieldControlRelation\":\"cCardNum,cFieldName \",\"AuditBizComponent\":\"cBizObjectID\",\"TI_InvoiceAge\":\"cNum\",\"BW_AlgoPlan_Lang\":\"PlanId\",\"REFERRETURNFIELDS\":\"ID\",\"tc_tmp_duplication\":\"obj_type\",\"hr_tm_Paras\":\"nTypeCode\",\"ex_btmvoucher\":\"cbtmvcardnum\",\"ca_ExcelInputMap_lang\":\"sType,cFieldName\",\"UTU_Authorization\":\"cID\",\"UA_Group_Ex\":\"cGroup_Id\",\"Exp_Func_Lang\":\"cFuncId\",\"AuditBizPriParams_Lang\":\"cBizEventId,cParamName\",\"RP_NoteType_base\":\"cTypeCode\",\"ex_ciqinvtype\":\"iciqinvtype\",\"rb_Versions\":\"Version,ReleaseDate\",\"Exp_ParamType_Lang\":\"cParamId\",\"UA_DesktopPage\":\"cPageID\",\"Rpt_Glbdef_Base_backup_by_CA\":\"ID,LocaleID\",\"UA_ScenarioUser\":\"cScenarioID\",\"AA_ColumnSetExtend\":\"ckey,cUserId\",\"Ua_U8LoginProcess_EX\":\"procKey\",\"eb_vouchertemplates\":\"vt_cardnumber\",\"UTU_MessageCenter_Attach\":\"iMessageID\",\"SA_MenuConfig\":\"autoid\",\"UserDefineDLL\":\"id\",\"ua_desktop_bak\":\"cID\",\"UA_Scenario_WorkItem_Bak\":\"ID\",\"mom_Datasource\":\"cMetCode\",\"ex_process_menudefine\":\"menuid\",\"NE_ThirdTicketSource\":\"TicketSource\",\"DelDutyClassCheck\":\"PK_DelDutyClassCheck\",\"UA_WorkItems_Bak\":\"WorkItemID\",\"hr_bd_mctableassign\":\"PK_Table\",\"SA_RowColChange\":\"autoid\",\"Ca_ExcelInputMap_base\":\"sType,cFieldName\",\"Ca_Visio_object\":\"cMapID,cObjID\",\"UTU_SystemMessage\":\"uidID\",\"UA_Scenario_WorkCategory_Bak\":\"ID\",\"UAP_VoucherLayout\":\"LayoutID,localid\",\"tc_copyvouch\":\"cKey\",\"NB_BankExt\":\"BankName,Version\",\"UA_PortalLogo_EX\":\"cId\",\"eb_voucheritems\":\"cardnum,cardsection,fieldname\",\"EAO_NoteML\":\"EAO_ID\",\"UA_WorkSolutionDetail_Bak\":\"AUTOID\",\"UA_HoldAuth_Ex\":\"cUser_Id,cAuth_Id\",\"qmchecktype_base\":\"CCHECKTYPECODE\",\"Ca_AmoTypeName\":\"cAmoType\",\"UA_WorkTask\":\"TaskID\",\"BatchTaskSub\":\"cVType\",\"UFFA_Model_AccInformation_Init\":\"cSysID,cName\",\"RefDefine\":\"cID\",\"im_paymoneystate\":\"ipaymoneystate\",\"lotnumberPrefabricate_lang\":\"cdatasrccaption\",\"UTU_User\":\"cUserID\",\"UA_WorkCenterSolutions_Bak\":\"SolutionID\",\"UTU_DiscussionMember\":\"uidID\",\"LogoutTable_Base\":\"cEnTableName\",\"CM_Task\":\"ID\",\"Crm_MAComponentRefFields\":\"componentid,cRefCode\",\"UFHeronNewRepSet\":\"name\",\"DimensionInScenario\":\"DimensionCode\",\"UFTmpTable_23975469_4671674514149184997\":\"baseid\",\"UTU_Statistics_ChatID\":\"cUserID,cChatID\",\"TaskTypeDef_Lang\":\"TaskTypeID\",\"HR_PF_EnumsCalcRule\":\"tStamp\",\"EB_Company\":\"cCompanyCode\",\"UFFA_Model_fa_Depreciations_Init\":\"sID\",\"UFFA_Model_fa_ItemsOfModel_Init\":\"sModelID,iSheetID,sItemID\",\"KC_BarCodeFlowProject\":\"cprojectcode\",\"tc_stage_path_option\":\"path_id\",\"UFFA_Model_fa_Status_Init\":\"sID\",\"SA_PopMenuConfig\":\"autoid\",\"NE_OperationType_PrintTemplate\":\"ID\",\"SA_PrintOption\":\"cmenuid\",\"CM_Kind\":\"AutoID\",\"SNPrefabricate_base\":\"cdatasrccaption\",\"ex_ApportionRule\":\"vouchtype,ChildField\",\"PL_CO_SPVouchType\":\"cVouType,cBusType\",\"Exp_Parameter_Lang\":\"cParamId\",\"HYKCFile_Items_base\":\"DisName\",\"Pe_GetDataOpt\":\"cOptID\",\"NB_Task\":\"ID\",\"Pe_Szdetail\":\"AutoID\",\"HY_LinkDesc\":\"desc\",\"U8Doc_Templates\":\"cDocCode,cVersion\",\"UA_ToNextYear_base\":\"cTblName\",\"UA_Period_Ex\":\"iId\",\"Flt_QuickConditionSet\":\"FilterID,ItemName\",\"hr_tm_ArithmeticControl\":\"ExecProc\",\"Ca_Ex_RebuildIndex\":\"TabName\",\"ScmAscendInfo_lang\":\"cVouchtypeCode\",\"GL_MRColumnDic_Set\":\"iReportID,iColPos\",\"GL_MRColumnDic_Lang\":\"cKey,cField\",\"PFFCSubSysConfig\":\"autoid\",\"VoucherExtendInfo\":\"cardnumber,cextendtype\",\"mom_Dimension\":\"cDimCode\",\"scmvouchrelationForLinkID\":\"clinkdetailtbl\",\"UFFA_Model_fa_Items_Init\":\"sID\",\"UFFA_Model_fa_Departments_Init\":\"sNum\",\"ScmTypeTran\":\"id\",\"UA_Flow\":\"ID\",\"GSP_UnionChangeInfo\":\"ID\",\"UTU_NormalMessage\":\"uidID\",\"GL_MRReport_Main\":\"iID\",\"voucheritems_prn_base_U8upgrade_backup\":\"AutoId\",\"CA_ReInitStatus\":\"ItemName\",\"SCM_CheckTrackbUsed\":\"autoid\",\"CM_TrackField\":\"cClueCode,cFieldID,cFieldName\",\"UpdateAccountInfo\":\"BizObjID\",\"bas_updlog\":\"DataType\",\"RefDefines\":\"cID,cReplaceStr\",\"UFFA_Model_fa_Models_Init\":\"sID\",\"FA_LabelDesign\":\"cLabelCode\",\"UTU_NormalMessage_Image\":\"cName\",\"hr_bd_BusSet\":\"cBusinessID\",\"RP_SysSet\":\"ID\",\"AA_ExtendDefineMakeVouch_Sub\":\"cMakeVouchID,cType\",\"AA_VoucherTemplate_MF\":\"cfld,CardNum\",\"UFTmpTable_4872786_5254787061058531514\":\"baseid\",\"VoucherNumber_Bak10\":\"CardNumber\",\"VoucherNumber_Lang\":\"CardNumber,AppName\",\"HR_CT803\":\"ccodeID\",\"EX_BUSTACHE_DEMO\":\"CBUSTACHECODE\",\"ScmBusType_lang\":\"cVouchtypeCode\",\"HR_BD_RuleAndTask\":\"TaskId\",\"IM_ReferGroupBy\":\"referkey,fname\",\"AuditObjectRevokeAuth\":\"RevokeAuthID\",\"rb_Monitoring\":\"ID\",\"KC_VoucherPkMap\":\"cVouchType\",\"UA_Chart_Sys\":\"ID\",\"pl_co_YearBegin_Def\":\"TabName\",\"CM_Vouchers\":\"CardNumber\",\"GL_MRColumnDic_Main\":\"cKey,cField\",\"UA_ScenarioView_Bak\":\"cViewID,cScenarioID\",\"FB_Form_LayoutTemplate\":\"tStamp\",\"UFTmpTable_4088415_5563094831708994645\":\"baseid\",\"UTU_DiscussionFile\":\"uidID\",\"UTU_UserConfig\":\"cUserID\",\"UFSUB_fitemss00class\":\"i_id\",\"GL_MRShowItem_Main\":\"iReportID,cItemType\",\"NB_AreaCode_City\":\"AREA_CODE\",\"UTU_DiscussionMessage\":\"uidID\",\"TaskExtendFieldDef\":\"TaskTypeID,ExtendFieldName\",\"HR_CT010\":\"ccodeID\",\"REFERFIELDS\":\"ID\",\"AA_PinYin\":\"letter\",\"Table_Template_CustomCondition\":\"cBizObjectId,Variable\",\"VouchAPPMappingTable\":\"cBizObjectID\",\"rb_Localize\":\"TextKey,CultureCode\",\"MFScriptSettings\":\"cScriptName\",\"referfilterfields\":\"ID\",\"UTU_Discussion\":\"uidID\",\"EAS_Feedback_Message\":\"EASF_ID\",\"GL_fltDefineMeta_lang\":\"FilterID\",\"HR_TM_ICCardParameters\":\"cDataKey\",\"mom_Metric\":\"cMetCode\",\"voucheritems_base_U8upgrade_backup\":\"AutoId\",\"NotifyObjectName_lang\":\"cBizObjectId\",\"hy_DZ_ECN_SELECT\":\"IsSelect\",\"UA_Role_Ex\":\"cGroup_Id,cUser_Id\",\"hr_tm_EastRiverBasicSet\":\"bIsID\",\"Ca_Visio_Condition\":\"cMapID,cObjCode,cCondType\",\"Exp_FuncGroup_Lang\":\"cGroupId\",\"NE_AirPortCity\":\"ID\",\"EAO_Node\":\"EAO_ID\",\"EA_Action\":\"EAA_ID\",\"HYKCFile_Config_base\":\"FrmCaption\",\"NB_BankProfile\":\"cUniqueID\",\"Ca_Visio_Rel\":\"cMapID,cNextLineID\",\"UTU_DiscussionMessage_Image\":\"uidID\"}";
        dbTablePrimaryKeyMap.put("UFDATA", JSONUtils.parseObject(ufData, new TypeReference<Map<String, String>>() {
        }));
        String ufMeta = "{\"Table\":\"OID\",\"View\":\"OID\",\"flt_DefineMeta_Cust\":\"FilterID\",\"UAP_PublishGridLayout\":\"GridLayoutID,LocaleID\",\"UAP_PublishLayout\":\"LayoutID\",\"UAP_Languages\":\"textid,localid,BusinessFunctionId\",\"DP_ChannelProcessor\":\"ProcessorID\",\"UAP_ExtendViewModels\":\"modeltype\",\"UAP_Entity\":\"id\",\"DP_MetaDataType\":\"MetaDataType\",\"UAP_EntityRelationDetails\":\"id\",\"UAP_ReportView_Lang_FactoryBak\":\"ViewID,LocaleID\",\"Field\":\"OID\",\"MERP_App\":\"app\",\"Merp_Module\":\"cAppID,cModuleID\",\"UAP_VoucherGridLayout_Backup\":\"GridLayoutID\",\"UAP_EntityRelation\":\"id\",\"UAP_ViewModelPlugins\":\"applicationid\",\"BD_TimeStamp\":\"TableName\",\"UAP_Report_Limit\":\"ReportID\",\"AA_CustomerButton\":\"cButtonID\",\"MetaEnumDef\":\"MetaID,LanguageID\",\"UAP_VoucherLayout_Backup\":\"LayoutID\",\"AA_BusObject_base\":\"cBusObId,cBusObName,langid\",\"MERP_ConditionPlugins\":\"cKey,cVarient\",\"UAP_ExtendHugeEntity\":\"ReportID,tablename\",\"UAP_VoucherGridLayout\":\"GridLayoutID\",\"TraceResult\":\"cDate\",\"DP_DataTransformConfig\":\"ID\",\"BD_CacheScripts\":\"timestamp\",\"Merp_RelatedObj\":\"cVoucherType\",\"Uap_Upgrade\":\"CardNumber\",\"EAI_AuthId\":\"Name\",\"AA_BusObject_General\":\"pubufts\",\"UAP_ReportView_FactoryBak\":\"ID\",\"AA_Autocomplete_Field\":\"cTableName,cFieldName\",\"MERP_DynamicRefer\":\"ReferKey\",\"ViewField\":\"OID\"}";
        dbTablePrimaryKeyMap.put("UFMeta", JSONUtils.parseObject(ufMeta, new TypeReference<Map<String, String>>() {
        }));
    }


    private static List<String> includeTableList = Arrays.asList("AA_Agreement", "AA_ArchOptionSet", "AA_ArchiveTable", "AA_Archive_Base", "AA_Archive_Lang", "AA_AuthClassDic_Base", "AA_AuthClassDic_Lang", "AA_AuthClassSet", "AA_AuthDimen_Type", "AA_Bank", "AA_BarCodePrefixRules", "AA_BizProcedureDic", "AA_BusObject_base", "AA_ColumnAuthMapping", "AA_ColumnDicCache_Detail", "AA_ColumnDicCache_Main", "AA_ColumnDic_Extend", "AA_ColumnDic_base", "AA_ColumnDic_inherit", "AA_ColumnSetExtend", "AA_ColumnSet_base", "AA_ColumnsDisplayRules", "AA_CustomerButton", "AA_DefineExtendTables", "AA_DefineExtendTables_Filter", "AA_DefineExtendTables_Lang", "AA_DefineExtendTables_Voucher", "AA_DefineExtendUpdateView", "AA_Enum", "AA_ExtendDefineMakeVouch_Sub", "AA_ExtendDefineMakeVouch_base", "AA_FormButtonAuths", "AA_FormButtonMenus_base", "AA_FormButtonMenus_lang", "AA_FormButtons_base", "AA_FormButtons_lang", "AA_FormCommonMenus_base", "AA_FormCommonMenus_lang", "AA_Forms_base", "AA_Forms_lang", "AA_HoldAuth", "AA_IndustryPlugins", "AA_InitDepend", "AA_InitItemTable", "AA_InitItem_Base", "AA_InitItem_Lang", "AA_Init_Base", "AA_Init_Lang", "AA_KpiLib_Lang", "AA_KpiLib_base", "AA_Picture", "AA_PinYin", "AA_Plugins", "AA_Plugins_Lang", "AA_ProcedureDic", "AA_ProductDic_Lang", "AA_ProxyProcedureDic", "AA_RefDesign", "AA_RequirementClass", "AA_RowAuthDic_base", "AA_TableDic_Lang", "AA_Timestamp", "AA_VouchersDigestDetail_base", "AP_OppCodeSet", "AccInformation", "AccInformation_Year", "Ap_BillAge", "Ap_CancelNo", "Ap_CloseBill", "Ap_CloseBills", "Ap_Detail", "Ap_InputCode", "Ap_SStyleCode", "Ap_Vouch", "Ap_Vouchs", "AuditBizComponent", "AuditBizEntities", "AuditBizEvents", "AuditBizEvents_Lang", "AuditBizObjectAuth", "AuditBizObjectDataAuth", "AuditBizObjectFlows", "AuditBizObjects", "AuditBizObjects_Lang", "AuditBizPredicate", "AuditBizPriParams", "AuditBizPriParams_Lang", "AuditEventPlugins", "AuditMsgTypeConfig", "AuditObjectCategory", "AuditObjectRevokeAuth", "AuditPlugins", "AuditPlugins_Lang", "AuditRouteRules", "AutoSetEntityObj", "AutoSetFieldInf", "AutoSetFieldInf_Lang", "AutoSetTableInf", "AutoSetTableInf_Lang", "BD_BusinessObjects", "BD_BusinessObjects_sec", "BD_CacheScripts", "BD_Catagory_Base", "BD_Entities", "BD_Entities_sec", "BD_EntityRelations", "BD_PhysicsTables", "BD_PhysicsTables_sec", "BD_RecentList", "BD_TimeStamp", "Bank", "BarCodeType", "BatchTask", "BatchTaskSub", "BusSenObject", "CA_CBSys", "CA_ErrorPos", "CA_Error_Hint", "CA_MenuDef", "CA_ReInitStatus", "CA_RptFld", "CA_RptSys", "CM_AlertType", "CM_Clause", "CM_ClauseClass", "CM_Contract_B", "CM_Contract_Item_B", "CM_Contract_Main", "CM_DD", "CM_ExecInterface", "CM_GradeDef", "CM_Kind", "CM_List", "CM_MS", "CM_SystemSet", "CM_Task", "CM_TrackClueSet", "CM_TrackClueUser", "CM_TrackField", "CM_TrackFieldUser", "CM_TrackReportSet", "CM_Trans", "CM_Type", "CM_TypeClass", "CM_TypeControl", "CM_UserContractSet", "CM_VoucherGrp", "CM_Vouchers", "CN_AcctParam", "CN_BaseFieldPro", "CN_DefPrinterParam", "CN_LevelClass", "CN_NARelate", "CN_NNRelate", "CN_NSRelate", "CN_NoteAcctRule", "CN_NoteBase", "CN_NoteImage", "CN_NoteLevel", "CN_NoteLinkage", "CN_NoteManageState", "CN_NoteRelateExt", "CN_NoteType", "CN_Purpose", "CN_Version", "CW_ProfPlan", "Ca_AmoTypeName", "Ca_ExcelInputMap_base", "Ca_JZ_YearBegin_Def", "Ca_UpdateInvCodeToPartID", "CollectorConfig", "CollectorConfigItem", "CollectorReference", "CollectorReferenceItem", "ColumnSet", "ComputationGroup", "ComputationUnit", "Crm_Contact", "Crm_Contact_extradefine", "CusDeliverAdd", "Customer", "CustomerBank", "CustomerClass", "DistrictClass", "EAONT_NoteML", "EAON_Template", "EAO_Node", "EAO_NoteML", "EAP_Constraint_Template", "EAS_Feedback_Message", "EA_Action", "EA_Flow", "EA_Object", "EntityClass", "Entity_Attribute", "Entity_Name", "Entity_Relation", "Entity_Table", "Exchange", "Exp_FuncDes_Lang", "Exp_FuncGroup_Lang", "Exp_FuncReturn_Lang", "Exp_Func_Lang", "Exp_Function", "Exp_ParamType_Lang", "Exp_Parameter", "Exp_Parameter_Lang", "FM_BillType", "FM_EnumList", "FM_SysSetting_base", "GL_AccMultiAss", "GL_BTrade", "GL_CashTable", "GL_CodeCheckStatus", "GL_CodeClass", "GL_CodeRemark", "GL_DPrecode", "GL_DigestDisp", "GL_Item", "GL_ItemStructure", "GL_LTAccSet", "GL_LTKmSet", "GL_MRColumnDic_Lang", "GL_MRColumnDic_Main", "GL_MRColumnDic_Set", "GL_MRReportOption", "GL_MRReport_Main", "GL_MRShowItem_Main", "GL_NewReport_Base", "GL_NewReport_Lang", "GL_PrnXmlFormat", "GL_ProcedureDic", "GL_VoucherOption", "GL_YearState", "GL_accass", "GL_accsum", "GL_accvouch", "GL_bdigest", "GL_fltDefineMeta_Base", "GL_fltDefineMeta_lang", "GL_mend", "GL_modelpreplace", "GL_mpostcond", "GL_mpostcond1", "GL_msysname", "GL_mybooktype", "GL_myoutput", "GradeDef_Base", "GradeDef_Lang", "GrpArchCode", "HB_QueueInfo", "HB_TaskInfo", "HY_QPPDM_SyncVersion", "HY_QPPDM_TblEAImport", "HY_QPPDM_TblPDM", "IA_DifRateDef", "IA_HeadSet", "IA_OppHead", "IA_QcFormat", "IA_tblRCsetM", "IB_AppSys", "IB_AppTag", "IB_AppTag_MsgType", "IB_AppType", "IB_EndPoint", "IB_Entities", "IB_Event_Plugin", "IB_MsgFilter", "IB_MsgType", "InvFldDesc", "Inventory", "InventoryClass", "InventoryRelatedClear", "Inventory_Sub", "Inventory_extradefine", "ItemSourceType", "Languages", "MERP_ActionTypeMaping", "MERP_App", "MERP_AppAbility", "MERP_Auth_Info", "MERP_Auth_Info_Bak", "MERP_CacheScripts", "MERP_ColumnDic", "MERP_ColumnDic_lan", "MERP_ColumnSet", "MERP_ConditionPlugins", "MERP_Conditions", "MERP_Conditions_lan", "MERP_Customer_Info", "MERP_EditSession", "MERP_Group", "MERP_Group_lang", "MERP_LoginAppToU8AppMapping", "MERP_Oper_Log", "MERP_Oper_Log_Bak", "MERP_SessionItem", "MERP_SessionResult", "MERP_Template", "MERP_TemplateApp", "MERP_TemplateSub", "MERP_Terminal_App", "MERP_Terminal_Mgr", "MERP_Terminal_Mgr_Bak", "MOM_MsgTypeCategories", "MR_FM", "Merp_ComponentMappingRelation", "Merp_ComponentServiceMapping", "Merp_Module", "Merp_Module_UserDefined", "MultTemplateVoucherConfig", "PDM_BusinessItems", "PDM_BusinessMeta", "PDM_PDMBusinessMeta", "PM_ActQuaInput", "PM_InfoBillItems", "PM_InfoSet", "PM_SettingInf", "PO_Podetails", "PO_Pomain", "Person", "Pm_RecordIns", "Pm_RecordOuts", "Pm_VouchType_Base", "PurchaseType", "Rd_Style", "Reason", "RefDefine", "RefDefines", "RefDesign_Base", "RefDesign_Lang", "RefTableDic", "RelationDesignLayout", "Rpt_CustomEvents_Base", "Rpt_FldDEF_Base", "Rpt_FldDEF_Base_Bak", "Rpt_FltDEF_Base", "Rpt_Folder_Base", "Rpt_GRPDEF_Base", "Rpt_GlbDEF_Base", "Rpt_ItmDEF_Base", "Rpt_RelDEF", "Rpt_SelfDefineRptCallBackObj", "SA_AuthConfig", "SA_ButtonState", "SA_CheckBeforeSave", "SA_CheckListBeforeSave", "SA_FieldExch", "SA_GoToVoucherConfig", "SA_GoldTax", "SA_GoldTaxField", "SA_GridRowColChange", "SA_LockSourceVouchConfig", "SA_MenuConfig", "SA_OrderAscendConfig", "SA_OrderTraceItem_Base", "SA_OrderTraceQuery_Base", "SA_OrderTraceQuery_Lang", "SA_PopMenuConfig", "SA_PriceDef", "SA_PriceSequence", "SA_ReferFillConfig", "SA_ReferVoucherConfig", "SA_RowColChange", "SA_SalePlanOption", "SA_Setting_CusProperty", "SA_ToolBarConfig", "SA_UnVerifyCheck", "SA_VoucherCellCheck", "SA_VoucherFieldConfig", "SA_VoucherListConfig", "SA_VoucherListFieldConfig", "SCMVouchRelation", "SCM_ImportDefaultValSet", "SCM_SysTableTS", "SNPrefabricate_base", "SNPrefabricate_lang", "SQLFrameworkLibrary", "SQLLibrary", "ST_SNDefine_base", "ST_SetInformationDic", "ST_VMIUsedRule", "SaleType", "SenderAccount", "SettleStyle", "SynchLock", "Table_EngineParam", "Table_Template", "Table_TemplateKind", "Table_TemplateKind_Lang", "Table_Template_CustomCondition", "Table_Template_Lang", "TaskExtendFieldDef", "TaskProcessorDef", "TaskTypeDef", "TaskTypeDef_Lang", "U8ArchivePlugin_Account", "U8ArchivePlugin_Category", "U8ArchivePlugin_Register", "U8CleanConfig", "U8Doc_Templates", "U8LangDefine", "U8LangResource", "U8TaskConfig", "U8TaskFactory", "U8TaskLog", "U8TaskNotifyConfig", "U8TaskNotifyType", "U8TaskSource", "U8TaskSourceNet", "UAP_Auth", "UAP_BusinessFunctionAndCatalogMappings", "UAP_BusinessFunctionCatalogs", "UAP_CustomGridLayout", "UAP_Entity", "UAP_EntityRelation", "UAP_EntityRelationDetails", "UAP_GeneralList", "UAP_JointQuery", "UAP_JointQuery_Condition", "UAP_JointQuery_Description", "UAP_Languages", "UAP_ListMeta", "UAP_MaxPrimeryKey", "UAP_OldVoucher", "UAP_PortalViewBehavior", "UAP_PortalViews", "UAP_PortalViewsMeta", "UAP_PublishGridLayout", "UAP_PublishLayout", "UAP_Report", "UAP_ReportFontColor", "UAP_ReportFontColor_Lang", "UAP_ReportHaveRead", "UAP_ReportMyReport", "UAP_ReportPublish", "UAP_ReportPublish_Lang", "UAP_ReportReceivers", "UAP_ReportStaticRpt", "UAP_ReportUserView", "UAP_ReportView", "UAP_ReportViewExtend", "UAP_ReportView_AuthId", "UAP_ReportView_DefaultBak", "UAP_ReportView_FactoryBak", "UAP_ReportView_Lang", "UAP_ReportView_Lang_DefaultBak", "UAP_ReportView_Lang_FactoryBak", "UAP_Report_ExpandRule", "UAP_Report_ExpandUserDefine", "UAP_Report_ExtendGetSql", "UAP_Report_Lang", "UAP_Report_Limit", "UAP_Report_ReportCatalogRelation", "UAP_Report_ReportCenterCatalog", "UAP_Report_ReportCenterCatalog_Lang", "UAP_Report_SelfAction", "UAP_Report_StaticReport", "UAP_TablePKIntMaxValue", "UAP_Tasks", "UAP_TemplateTransHistory", "UAP_UserRole", "UAP_ViewModelPlugins", "UAP_VoucherConvertRule", "UAP_VoucherCustomize", "UAP_VoucherGridLayout", "UAP_VoucherGridLayout_Backup", "UAP_VoucherItems_Update", "UAP_VoucherLayout", "UAP_VoucherLayout_Backup", "UAP_VoucherMeta", "UAP_VoucherStruct", "UAP_VoucherTransformRules", "UAP_VoucherTransformRulesMeta", "UAP_VoucherTypes", "UAP_VoucherTypes_lang", "UAP_projects", "UA_AALog_base", "UA_Account", "UA_AccountDatabase", "UA_Account_sub", "UA_AuthRelation", "UA_Auth_base", "UA_Auth_lang", "UA_BackupLog", "UA_BackupPath", "UA_BackupPlan", "UA_BackupPlans", "UA_Caption", "UA_Control", "UA_ControlEx", "UA_DataDumpTable", "UA_DataDump_Base", "UA_DataDump_Lang", "UA_FlowItems", "UA_FlowLines", "UA_Group", "UA_GroupAuth", "UA_HelpView_MessageTypes", "UA_HelpView_Panel_DataSource", "UA_HelpView_Panel_ListeningMsg", "UA_HelpView_Panels", "UA_HelpView_Personal_QuickTask", "UA_HelpView_QuickTask", "UA_HelpView_UserDelete_QuickTask", "UA_HoldAuth", "UA_Identity", "UA_Information", "UA_Log", "UA_MTCLangResource", "UA_Menu", "UA_MenuOther", "UA_MenuRulePLugin", "UA_MenuUAPArchives", "UA_Menurule", "UA_MessageCfg", "UA_MessageType", "UA_MessageTypeCfg", "UA_MetaUpgradeMap", "UA_MutexAuth", "UA_MutexAuthItem", "UA_NavigationNode", "UA_OAMapU8", "UA_Password", "UA_Period", "UA_ReplStru_order", "UA_ReportCenterrule", "UA_Role", "UA_SupAuths", "UA_TaskLog", "UA_Task_Special", "UA_ToNextYear_base", "UA_UpgradeBatch", "UA_Upgrade_base", "UA_User", "UA_UserExtendAuth", "UA_UserPwdLog", "UA_UserRelation", "UA_Version", "UFMenu_Business_Lang", "UFSUB_dsign", "UFSUB_fitem", "UFSUB_fitemss00", "UFSUB_fitemss00class", "UFSUB_fitemstructure", "UFSUB_gl_bautotran", "UTU_Broadcast", "UTU_Broadcast_User", "UTU_Config", "UTU_Discussion", "UTU_DiscussionFile", "UTU_DiscussionMember", "UTU_DiscussionMessage", "UTU_DiscussionMessage_Image", "UTU_Log", "UTU_MessageCenter", "UTU_MessageCenter_Attach", "UTU_MessageCenter_U8ApproveTask", "UTU_MessageCenter_User", "UTU_NormalMessage", "UTU_NormalMessage_Image", "UTU_PublicGroupCache", "UTU_SystemMessage", "UTU_SystemMessage_User", "UTU_User", "UTU_UserConfig", "Ua_U8LoginProcess", "UapVersion", "Uap_ActiveImage", "Uap_VoucherRelation", "Uap_Vouchers", "UpdateAccountInfo", "Upgrade_EAONTReference", "Upgrade_EAObjectInfo", "UserDefDataEngineConfig", "UserDef_Base", "UserDef_Lang", "UserDefineDLL", "UserDevelop", "VariationRecordBeginPoint", "Ven_Contact", "Vendor", "VendorBank", "VendorClass", "VouchType", "VouchTypeDic_Lang", "VoucherContrapose", "VoucherCustomRelation", "VoucherCustomerInfo", "VoucherExtendArcType", "VoucherExtendInfo", "VoucherExtendType", "VoucherHistory", "VoucherNumber", "VoucherNumber_Bak10", "VoucherNumber_Lang", "VoucherPlugins", "VoucherPrefabricate", "VoucherPrefabricateRef", "VoucherPrintSettingExtension", "VoucherStatusDetails", "VoucherStatusRuleContrapose", "VoucherStatusRuleDetails", "VoucherStatusRules", "VouchersExInfo", "WF_ApplicationList", "Warehouse", "ap_digset_base", "ap_vouchtype_base", "bas_calendar", "bas_part", "cClassName_Lang", "ca_ExcelInputMap_lang", "ca_amoct_temp_for_weicha", "code", "dd_dict_str", "dd_object", "dd_object_option", "dd_object_table_def", "dd_sys_lang", "dsign", "fitem", "fitemss00", "fitemss00class", "fitemss98class", "fitemstructure", "fitemstrumode", "flt_Solution", "fm_systarget_base", "foreigncurrency", "lotnumberPrefabricate_base", "lotnumberPrefabricate_lang", "printEx", "rpt_FetchObject", "rpt_funPere_Base", "rpt_fundef_Base", "rpt_glbdef_base_bak", "sa_invoicecustomers", "scmVouchRelInfo", "sys_besortseqdef", "sys_entity", "sys_entityreference", "tc_background_task", "tc_card_format", "tc_data_object", "tc_desktop_report", "tc_edit_card_format", "tc_entity_relation", "tc_entity_relation_attr", "tc_enum", "tc_enum_str", "tc_file_resource", "tc_locale", "tc_login_history", "tc_menu", "tc_menu_lang", "tc_obj_rel_profile", "tc_object_stage_option", "tc_prec_u82crm", "tc_profile", "tc_profile_trans", "tc_query_profile", "tc_query_profile_str", "tc_search_profile", "tc_setting", "tc_stage_path_option", "tc_type", "tc_user_attr", "tc_user_list_profile", "tc_user_list_report", "tc_user_mattr", "tc_user_qry_profile", "tc_user_rel_condition", "tc_user_rel_profile", "tc_user_xattr", "tsys_crm_nextid", "ua_Task_Common", "ua_auth_base", "ua_idt", "ua_mlTables", "ua_subsys_base", "uap_ReportPublishSaveAs", "variationbilltable", "variationbilltype", "voucheritems_base", "voucheritems_lang", "voucheritems_prn_base", "voucheritems_prn_lang", "voucherprefabricate_lang", "vouchers_base", "vouchers_lang", "vouchertemplates_base", "vouchertemplates_lang");
//    private static  List<String> includeTableList = Arrays.asList("HR_CT007", "HR_CT004", "HR_CT117", "AA_Agreement");


    public static void main(String[] args) throws SQLException {
        String connectionUrl = String.format(baseUrl, server, 1433, database, user, password);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(connectionUrl);
            List<String> notEmptyTableList = getNotEmptyTableList(conn);
            System.out.println("需要同步的表个数: " + notEmptyTableList.size() + ", 列表：");
            Collections.sort(notEmptyTableList);
            System.out.println(JSONUtils.toJsonString(notEmptyTableList));
            List<String> noPrimarkeyTableList = getNoPrimaryKeyTableList(conn);
//            List<String> noPrimarkeyTableList = JSONUtils.toList("[\"hr_tm_ArithmeticControl\",\"hr_tm_Paras\",\"ex_btmvoucher\",\"fc_UpgradeLog\",\"scmVouchRelInfo\",\"ex_ciqinvtype\",\"rb_Versions\",\"ex_vouch_authdefine\",\"mom_Dimension\",\"hr_ct055\",\"referarchives_base\",\"referarchives_local\",\"rpt_glbdef_base_bak\",\"ex_bondedbilltype\",\"tc_procInvok_log\",\"hr_sys_sub_Base\",\"scmvouchrelationForLinkID\",\"eb_vouchertemplates\",\"tc_obj_rel_profile\",\"ua_desktop_bak\",\"mom_Datasource\",\"ex_process_menudefine\",\"rb_UserRoles\",\"voucheritems_prn_base_U8upgrade_backup\",\"hr_sys_function_Base\",\"hr_bd_mctableassign\",\"hr_bd_BusSet\",\"tc_copyvouch\",\"eb_voucheritems\",\"voucherprefabricate_lang\",\"fc_OpColor\",\"rb_Monitoring\",\"tc_object_stage_option\",\"tc_ufrefresh\",\"pl_co_YearBegin_Def\",\"qmchecktype_base\",\"tc_backfill_setting\",\"hr_bd_rulecontact\",\"im_paymoneystate\",\"hr_sys_ValuesMapping\",\"lotnumberPrefabricate_lang\",\"lotnumberPrefabricate_base\",\"fa_CheckConfig\",\"rb_Localize\",\"referfilterfields\",\"mom_MetricDimension\",\"ua_idt_EX\",\"tc_copyvouch_d\",\"tc_stage_path_option\",\"mom_Metric\",\"rb_Current_TabTree\",\"voucheritems_base_U8upgrade_backup\",\"ex_ApportionRule\",\"hr_bd_mcitemassign\",\"hy_DZ_ECN_SELECT\",\"hr_tm_EastRiverBasicSet\",\"ex_idemandtype\",\"hr_tm_OverTimeCardExcursion\",\"tc_tmp_duplication\"]", String.class);
            System.out.println("主键为空需要处理的表,个数： " + noPrimarkeyTableList.size() + ", 列表：");
            Collections.sort(noPrimarkeyTableList);
            System.out.println(JSONUtils.toJsonString(noPrimarkeyTableList));
            Map<String, String> tablePrimaryKeyMap = getTableUniqueKeyMapWithNotEmptyTables(noPrimarkeyTableList, conn, database.split("_")[0]);
            System.out.println("成功处理的表， 个数: " + tablePrimaryKeyMap.size() + ", 列表：");
            System.out.println(JSONUtils.toJsonString(tablePrimaryKeyMap));
            System.out.println("未能处理的表， 个数：" + failedTables.size() + ", 列表：");
            System.out.println(JSONUtils.toJsonString(getFailedTables()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getUrl(BaseConnectionParam connParam) {
        return String.format(baseUrl, connParam.getHost(), connParam.getPort(), connParam.getDatabase(), connParam.getUser(), connParam.getPassword());
    }

    public static List<String> getNotEmptyTableList(BaseConnectionParam connParam, List<String> includeTableNames) {
        try (Connection conn = DriverManager.getConnection(getUrl(connParam))) {
            List<String> notEmptyTableList = getNotEmptyTableList(conn);
            if (includeTableNames != null && !includeTableNames.isEmpty()) {
                notEmptyTableList.retainAll(includeTableNames);
            }
            return notEmptyTableList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getNoPrimaryKeyTableList(BaseConnectionParam connParam) {
        try (Connection conn = DriverManager.getConnection(getUrl(connParam))) {
            return getNoPrimaryKeyTableList(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getTableUniqueKeyMapWithNotEmptyTables(BaseConnectionParam connParam, List<String> includeTableNames) {
            List<String> notEmptyTableList = getNotEmptyTableList(connParam, includeTableNames);
        try (Connection conn = DriverManager.getConnection(getUrl(connParam))) {
            List<String> noPrimarkeyTableList = getNoPrimaryKeyTableList(conn);
            noPrimarkeyTableList.retainAll(notEmptyTableList);
            String dbKey = connParam.getDatabase().split("_")[0];
            Map<String, String> tablePrimaryKeyMap = getTableUniqueKeyMapWithNotEmptyTables(noPrimarkeyTableList, conn, dbKey);
//            if (tablePrimaryKeyMap.size() > dbTablePrimaryKeyMap.get(dbKey).size()) {
//                for (Map.Entry<String, String> entry : dbTablePrimaryKeyMap.get(dbKey).entrySet()) {
//                    if (!tablePrimaryKeyMap.containsKey(entry.getKey())) {
//                        tablePrimaryKeyMap.put(entry.getKey(), entry.getValue());
//                    }
//                }
//                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//                System.out.println(tablePrimaryKeyMap);
//                System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//            }
            return tablePrimaryKeyMap;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> getFailedTables() {
        return failedTables;
    }

    private static Map<String, String> getTableUniqueKeyMapWithNotEmptyTables(List<String> noPrimaryKeyTableList,
                                                                              Connection conn, String database) {
        Map<String, String> tablePrimaryKeyMap = new HashMap<>();
        failedTables.clear(); // 清空之前的失败记录

        for (String tableName : noPrimaryKeyTableList) {
            if (dbTablePrimaryKeyMap.containsKey(database) && dbTablePrimaryKeyMap.get(database).containsKey(tableName)) {
                tablePrimaryKeyMap.put(tableName, dbTablePrimaryKeyMap.get(database).get(tableName));
                continue;
            }
            try {
                // 获取表的列信息
                List<String> columnNames = getTableColumns(conn, tableName);

                if (columnNames.isEmpty()) {
                    continue; // 如果没有列，跳过该表
                }

                // 尝试找出能保证数据唯一的列组合
                String uniqueColumns = findUniqueColumns(conn, tableName, columnNames);

                if (uniqueColumns != null && !uniqueColumns.isEmpty()) {
                    tablePrimaryKeyMap.put(tableName, uniqueColumns);
                    dbTablePrimaryKeyMap.get(database).put(tableName, uniqueColumns);
                } else {
                    dbTablePrimaryKeyMap.get(database).put(tableName, "");
                }
            } catch (SQLException e) {
                System.err.println("处理表 " + tableName + " 时发生错误: " + e.getMessage());
                failedTables.add(tableName); // 记录失败的表
                // 继续处理下一个表
            }
        }

        return tablePrimaryKeyMap;
    }

    /**
     * 获取表的所有列名
     */
    private static List<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME"));
                }
            }
        }

        return columnNames;
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
                String schemaName = rs.getString("SchemaName");
                String tableName = rs.getString("TableName");
                int rowCount = rs.getInt("RowCount");
//                System.out.println(schemaName + "." + tableName + ": " + rowCount);
                tableList.add(tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tableList;
    }

    private static List<String> getNotEmptyTableList(Connection conn) {
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
        List<String> tableList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String schemaName = rs.getString("SchemaName");
                String tableName = rs.getString("TableName");
                int rowCount = rs.getInt("RowCount");
//                System.out.println(schemaName + "." + tableName + ": " + rowCount);
                tableList.add(tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return tableList;
    }

}
