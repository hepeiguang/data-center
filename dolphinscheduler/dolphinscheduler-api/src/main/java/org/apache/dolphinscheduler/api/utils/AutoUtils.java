package org.apache.dolphinscheduler.api.utils;

import com.facebook.presto.jdbc.internal.jackson.databind.exc.InvalidFormatException;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.dto.auto.ApiExcelDto;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AutoUtils {
    /**
     * 解析Excel文件，提取API信息
     * @param excelFile Excel文件
     * @return API信息列表
     * @throws IOException IO异常
     * @throws InvalidFormatException Excel格式异常
     */
    public static List<ApiExcelDto> parseExcelFile(MultipartFile excelFile) throws IOException, InvalidFormatException {
        List<ApiExcelDto> apiList = new ArrayList<>();
        // 创建Excel工作簿
        Workbook workbook = WorkbookFactory.create(excelFile.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
        // 遍历行（跳过标题行，从第二行开始）
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            ApiExcelDto apiDto = new ApiExcelDto();
            // 序号 (第1列)
            Cell seqCell = row.getCell(0);
            apiDto.setSeq(seqCell != null ? getCellValue(seqCell) : "");
            // 父模块 (第2列)
            Cell parentModuleCell = row.getCell(1);
            apiDto.setParentModule(parentModuleCell != null ? getCellValue(parentModuleCell) : "");
            // 模块 (第3列)
            Cell moduleCell = row.getCell(2);
            apiDto.setModule(moduleCell != null ? getCellValue(moduleCell) : "");
            // 功能 (第4列)
            Cell functionCell = row.getCell(3);
            apiDto.setFunction(functionCell != null ? getCellValue(functionCell) : "");
            // 名称 (第5列)
            Cell apiNameCell = row.getCell(4);
            apiDto.setApiName(apiNameCell != null ? getCellValue(apiNameCell) : "");
            // 请求地址 (第6列)
            Cell urlCell = row.getCell(5);
            apiDto.setUrl(urlCell != null ? getCellValue(urlCell) : "");
            // 请求方式 (第7列)
            Cell methodCell = row.getCell(6);
            apiDto.setMethod(methodCell != null ? getCellValue(methodCell) : "");
            // 内容获取 (第8列)
            Cell json_contentFetchCell = row.getCell(7);
            apiDto.setJson_contentField(json_contentFetchCell != null ? getCellValue(json_contentFetchCell) : "");
            // 请求参数 (第9列)
            Cell paramsCell = row.getCell(8);
            apiDto.setParams(paramsCell != null ? getCellValue(paramsCell) : "");
            // 分页参数 (第10列)
            Cell paginationParamsCell = row.getCell(9);
            apiDto.setPaginationParams(paginationParamsCell != null ? getCellValue(paginationParamsCell) : "");
            // 表名称 (第11列)
            Cell tableNameCell = row.getCell(10);
            apiDto.setTableName(tableNameCell != null ? getCellValue(tableNameCell) : "");
            // 主键列 (第12列)
            Cell primaryKeyCell = row.getCell(11);
            apiDto.setPrimaryKey(primaryKeyCell != null ? getCellValue(primaryKeyCell) : "");
            // 返回结果 (第13列)
            Cell resultCell = row.getCell(12);
            apiDto.setResult(resultCell != null ? getCellValue(resultCell) : "");
            // 返回结果 (第14列)
            Cell dependCell = row.getCell(13);
            apiDto.setDepend(dependCell != null ? getCellValue(dependCell) : "");
            // 返回结果 (第15列)
            Cell OtherParamsCell = row.getCell(14);
            apiDto.setOtherParams(OtherParamsCell != null ? getCellValue(OtherParamsCell) : "");
            // 返回结果 (第16列)
            Cell schemaCell = row.getCell(15);
            apiDto.setSchema(schemaCell != null ? getCellValue(schemaCell) : "");

            apiList.add(apiDto);
        }
        workbook.close();
        return apiList;
    }

    /**
     * 获取Excel单元格的值，处理不同类型的单元格
     * @param cell 单元格
     * @return 单元格的值
     */
    public static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cell.getDateCellValue());
                } else {
                    // 处理数字类型，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getStringCellValue());
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    /**
     * 验证API配置的完整性
     * @param apiConfigs API配置列表
     * @throws ServiceException 配置不完整时抛出异常
     */
    public static void validateApiConfigs(List<ApiExcelDto> apiConfigs) {
        for (ApiExcelDto apiDto : apiConfigs) {
            if (
//                    StringUtils.isEmpty(apiDto.getParentModule()) ||
//                    StringUtils.isEmpty(apiDto.getModule()) ||
//                    StringUtils.isEmpty(apiDto.getFunction()) ||
                    StringUtils.isEmpty(apiDto.getApiName()) ||
                            StringUtils.isEmpty(apiDto.getUrl()) ||
                            StringUtils.isEmpty(apiDto.getMethod()) ||
                            StringUtils.isEmpty(apiDto.getJson_contentField()) ||
                            StringUtils.isEmpty(apiDto.getTableName())
//                            StringUtils.isEmpty(apiDto.getPrimaryKey())
            ) {
                throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS,"excel row " + apiDto.getSeq() + "/" + apiDto.getApiName() +" is not corret");
            }
        }
    }
}
