//package org.apache.dolphinscheduler.plugin.task.yuenhezi;
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.dolphinscheduler.plugin.task.api.TaskException;
//import org.apache.dolphinscheduler.plugin.task.yuenhezi.dto.FieldInfoDto;
//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.fs.Path;
//import org.apache.orc.OrcFile;
//import org.apache.orc.Reader;
//import org.apache.orc.TypeDescription;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//public class FileHeaderParser {
//
//    /**
//     * 解析文件并返回字段信息
//     */
//    public static List<FieldInfoDto> parseFileHeader(String objectName, String fileName, String bucketName, MinioClient minioClient) {
//
//        return parseExcelHeadersFromMinio(objectName, fileName, bucketName, minioClient);
//    }
//
//    /**
//     * 从Minio解析Excel文件表头
//     */
//    private static List<FieldInfoDto> parseExcelHeadersFromMinio(String objectName, String fileName,
//                                                                 String bucketName, MinioClient minioClient) {
//        String extension = getFileExtension(objectName).toLowerCase();
////        log.error("File Header Parser: filePath: {}, fileName: {}, bucketName: {}, extension:{}", objectName, fileName, bucketName, extension);
//        try {
//            switch (extension) {
//                case "xlsx":
//                case "xls":
//                    // 使用流式方式解析Excel，避免内存问题
//                    return parseExcelHeadersFromStream(objectName, bucketName, 50, minioClient);
//                case "csv":
//                    return parseCsvHeader(objectName, bucketName, minioClient,50);
//                case "orc":
//                    return parseOrcHeader(objectName, bucketName, minioClient);
//                default:
////                throw new IllegalArgumentException("Unsupported file format: " + extension);
//                    return new ArrayList<>();
//            }
//
//        } catch (Exception e) {
//            log.error("从Minio解析Excel文件表头失败: {}", objectName, e);
////            throw new RuntimeException("从Minio解析Excel表头失败: " + objectName, e);
//        }
//        return new ArrayList<>();
//    }
//
//    /**
//     * 从输入流解析Excel表头
//     */
//    private static List<FieldInfoDto> parseExcelHeadersFromStream(String objectName, String bucketName, int maxColumns, MinioClient minioClient) {
//        List<FieldInfoDto> headers = new ArrayList<>();
//
//        try (InputStream stream = minioClient.getObject(
//                GetObjectArgs.builder()
//                        .bucket(bucketName)
//                        .object(objectName)
//                        .build())) {
//
//            try (Workbook workbook = new XSSFWorkbook(stream)) {
//                Sheet sheet = workbook.getSheetAt(0);
//                if (sheet == null) {
//                    log.warn("Excel文件没有工作表");
//                    return generateDefaultHeaders(objectName, bucketName, 10);
//                }
//
//                Row headerRow = sheet.getRow(0);
//                if (headerRow == null) {
//                    log.warn("Excel文件第一行为空");
//                    return generateDefaultHeaders(objectName, bucketName, 10);
//                }
//
//                int columnCount = Math.min(headerRow.getLastCellNum(), maxColumns);
//
//                for (int i = 0; i < columnCount; i++) {
//                    Cell cell = headerRow.getCell(i);
//                    String headerValue = getCellValueAsString(cell);
//
//                    if (headerValue == null || headerValue.trim().isEmpty()) {
//                        headerValue = "column_" + (i + 1);
//                    }
//
//                    FieldInfoDto field = new FieldInfoDto();
//                    field.setColumnName(headerValue);
//                    field.setColumnType(getCellValueAsString(cell));
//                    field.setSortIndex(String.valueOf(i + 1));
//                    String md5Hash = generateMD5(objectName, bucketName, headerValue, i);
//                    field.setMd5Hash(md5Hash);
//
//                    headers.add(field);
//                }
//
//                log.debug("解析到Excel表头: {}", headers.stream()
//                        .map(FieldInfoDto::getColumnName)
//                        .collect(Collectors.toList()));
//
//            } catch (Exception e) {
//                log.error("解析Excel流失败", e);
//                throw new RuntimeException("解析Excel流失败", e);
//            }
//
//        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
//                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
//                 XmlParserException e) {
//            log.error("Failed to parse File Header ", e);
//            throw new RuntimeException("解析Excel流失败", e);
//        }
//
//
//        log.info("从Minio Excel文件 {} 解析到 {} 个表头字段", objectName, headers.size());
//        return headers;
//    }
//
//    /**
//     * 生成默认表头
//     */
//    private static List<FieldInfoDto> generateDefaultHeaders(String objectName, String bucketName, int count) {
//        List<FieldInfoDto> headers = new ArrayList<>();
//        for (int i = 0; i < count; i++) {
//            FieldInfoDto field = new FieldInfoDto();
//            field.setColumnName("field_" + (i + 1));
//            field.setColumnType("string");
//            field.setSortIndex(String.valueOf(i + 1));
//            field.setMd5Hash(generateMD5(objectName, bucketName, field.getColumnName(), i));
//            headers.add(field);
//        }
//        return headers;
//    }
//
//    private static String generateMD5(String objectName, String bucketName, String filedName, int index) {
//        String combinedString = objectName + bucketName + filedName + index;
//        return DigestUtils.md5Hex(combinedString);
//    }
//
//    /**
//     * 解析 CSV 文件头
//     */
//    private static List<FieldInfoDto> parseCsvHeader(String objectName, String bucketName, MinioClient minioClient,int maxColumns) throws IOException {
//        List<FieldInfoDto> fields = new ArrayList<>();
//        List<FieldInfoDto> headers = new ArrayList<>();
//        try (InputStream stream = minioClient.getObject(
//                GetObjectArgs.builder()
//                        .bucket(bucketName)
//                        .object(objectName)
//                        .build())) {
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
//
//                // 读取第一行尝试解析为CSV
//                String firstLine = reader.readLine();
//                if (firstLine == null || firstLine.trim().isEmpty()) {
//                    log.warn("CSV文件第一行为空");
//                    return generateDefaultHeaders(objectName, bucketName, 10);
//                }
//
//                // 尝试多种CSV格式
//                CSVFormat[] formats = {
//                        CSVFormat.DEFAULT.withHeader(), // 标准格式
//                        CSVFormat.EXCEL.withHeader(),   // Excel格式
//                        CSVFormat.RFC4180.withHeader(), // RFC4180格式
//                        CSVFormat.TDF.withHeader()      // 制表符分隔
//                };
//
//                CSVParser parser = null;
//                for (CSVFormat format : formats) {
//                    try {
//                        // 重新创建Reader来解析第一行
//                        StringReader lineReader = new StringReader(firstLine);
//                        parser = format.parse(lineReader);
//                        break;
//                    } catch (Exception e) {
//                        // 尝试下一种格式
//                        continue;
//                    }
//                }
//
//                if (parser == null) {
//                    // 如果所有标准格式都失败，使用自定义解析
//                    return parseCustomCsvHeaders(objectName, bucketName, firstLine, maxColumns);
//                }
//
//                // 获取解析到的表头
//                for (String headerName : parser.getHeaderNames()) {
//                    if (headers.size() >= maxColumns) {
//                        break;
//                    }
//
//                    String finalHeaderName = headerName;
//                    if (finalHeaderName == null || finalHeaderName.trim().isEmpty()) {
//                        finalHeaderName = "column_" + (headers.size() + 1);
//                    }
//
//                    FieldInfoDto field = new FieldInfoDto();
//                    field.setColumnName(finalHeaderName.trim());
//                    field.setColumnType("string"); // CSV默认类型为字符串
//                    field.setSortIndex(String.valueOf(headers.size() + 1));
//                    field.setMd5Hash(generateMD5(objectName, bucketName,finalHeaderName, headers.size()));
//
//                    headers.add(field);
//                }
//
//                log.info("从CSV文件解析到 {} 个表头字段", headers.size());
//            }
//        } catch (Exception e) {
//            log.error("解析CSV流失败", e);
//            throw new RuntimeException("解析CSV流失败", e);
//        }
//
//
//
//        return fields;
//    }
//
//    /**
//     * 自定义CSV表头解析（处理复杂情况）
//     */
//    private static List<FieldInfoDto> parseCustomCsvHeaders(String objectName, String bucketName,String firstLine, int maxColumns) {
//        List<FieldInfoDto> headers = new ArrayList<>();
//
//        try {
//            // 简单的CSV分割逻辑
//            String[] columns;
//
//            // 先尝试逗号分割
//            if (firstLine.contains(",")) {
//                columns = firstLine.split(",", maxColumns);
//            }
//            // 再尝试制表符分割
//            else if (firstLine.contains("\t")) {
//                columns = firstLine.split("\t", maxColumns);
//            }
//            // 最后尝试分号分割
//            else if (firstLine.contains(";")) {
//                columns = firstLine.split(";", maxColumns);
//            } else {
//                // 如果都没有分隔符，整个作为一列
//                columns = new String[]{firstLine};
//            }
//
//            for (int i = 0; i < columns.length && i < maxColumns; i++) {
//                String header = columns[i].trim();
//
//                // 清理可能的引号
//                if (header.startsWith("\"") && header.endsWith("\"")) {
//                    header = header.substring(1, header.length() - 1);
//                }
//                if (header.startsWith("'") && header.endsWith("'")) {
//                    header = header.substring(1, header.length() - 1);
//                }
//
//                if (header.isEmpty()) {
//                    header = "column_" + (i + 1);
//                }
//
//                FieldInfoDto field = new FieldInfoDto();
//                field.setColumnName(header);
//                field.setColumnType("string");
//                field.setSortIndex(String.valueOf(i + 1));
//                String md5Hash = generateMD5(objectName, bucketName, header, i);
//                field.setMd5Hash(md5Hash);
//
//                headers.add(field);
//            }
//
//            log.info("使用自定义解析从CSV获取 {} 个表头字段", headers.size());
//
//        } catch (Exception e) {
//            log.warn("自定义CSV解析失败，使用默认字段", e);
//            return generateDefaultHeaders(objectName, bucketName, 10);
//        }
//
//        return headers;
//    }
//
//    /**
//     * 解析 ORC 文件头
//     */
//    private static List<FieldInfoDto> parseOrcHeader(String objectName, String bucketName, MinioClient minioClient) throws IOException {
//        List<FieldInfoDto> fields = new ArrayList<>();
//
//        try (InputStream stream = minioClient.getObject(
//                GetObjectArgs.builder()
//                        .bucket(bucketName)
//                        .object(objectName)
//                        .build())) {
//            // 创建临时文件来处理ORC流
//            Path tempFilePath = null;
//            try {
//                // 创建临时文件
//                tempFilePath = createTempFileFromStream(stream, objectName);
//
//                // 使用临时文件路径创建ORC Reader
//                try (Reader reader = OrcFile.createReader(tempFilePath,
//                        OrcFile.readerOptions(new Configuration()))) {
//
//                    TypeDescription schema = reader.getSchema();
//                    List<String> fieldNames = schema.getFieldNames();
//                    List<TypeDescription> fieldTypes = schema.getChildren();
//
//                    for (int i = 0; i < fieldNames.size(); i++) {
//                        String fieldName = fieldNames.get(i);
//                        String fieldType = fieldTypes.get(i).getCategory().toString();
//                        String md5Hash = generateMD5(objectName, bucketName, fieldName, i);
//
//                        FieldInfoDto field = new FieldInfoDto();
//                        field.setColumnName(fieldName);
//                        field.setColumnType(fieldType);
//                        field.setSortIndex(String.valueOf(i + 1));
//                        field.setMd5Hash(md5Hash);
//
//                        fields.add(field);
//                    }
//                }
//
//                log.info("从ORC文件 {} 解析到 {} 个字段", objectName, fields.size());
//
//
//            } catch (Exception e) {
//                log.error("解析Orc流失败", e);
//                throw new RuntimeException("解析Excel流失败", e);
//            } finally {
//                // 清理临时文件
//                if (tempFilePath != null) {
//                    cleanupTempFile(tempFilePath);
//                }
//            }
//
//            return fields;
//
//        } catch (InsufficientDataException | ErrorResponseException | IOException |
//                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException
//                 | ServerException | XmlParserException | InternalException e) {
//            throw new TaskException("解析Orc流失败",e);
//        }
//    }
//
//        /**
//     * 从输入流创建临时文件
//     */
//    private static Path createTempFileFromStream(InputStream inputStream, String objectName) throws IOException {
//        // 创建临时文件
//        String tempDir = System.getProperty("java.io.tmpdir");
//        String tempFileName = "orc_temp_" + System.currentTimeMillis() + "_" +
//                objectName.replaceAll("[^a-zA-Z0-9]", "_");
//        File tempFile = new File(tempDir, tempFileName);
//
//        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                fos.write(buffer, 0, bytesRead);
//            }
//        }
//
//        return new Path(tempFile.getAbsolutePath());
//    }
//
//    /**
//     * 清理临时文件
//     */
//    private static void cleanupTempFile(Path tempFilePath) {
//        try {
//            File file = new File(tempFilePath.toString());
//            if (file.exists()) {
//                boolean deleted = file.delete();
//                if (!deleted) {
//                    log.warn("无法删除临时文件: {}", tempFilePath);
//                }
//            }
//        } catch (Exception e) {
//            log.warn("清理临时文件失败: {}", tempFilePath, e);
//        }
//    }
//
//
////    /**
////     * 从单元格推断数据类型
////     */
////    private static String inferDataTypeFromCell(Cell cell) {
////        if (cell == null) return "STRING";
////
////        switch (cell.getCellType()) {
////            case NUMERIC:
////                return DateUtil.isCellDateFormatted(cell) ? "TIMESTAMP" : "DOUBLE";
////            case BOOLEAN:
////                return "BOOLEAN";
////            case FORMULA:
////                return "STRING"; // 公式结果需要进一步处理
////            default:
////                return "STRING";
////        }
////    }
////
////    /**
////     * 从 CSV 样本值推断数据类型
////     */
////    private static String inferDataTypeFromCsvSample(CSVRecord record, int index) {
////        if (record == null || index >= record.size()) return "STRING";
////
////        String value = record.get(index);
////        if (value == null || value.isEmpty()) return "STRING";
////
////        try {
////            Double.parseDouble(value);
////            return "DOUBLE";
////        } catch (NumberFormatException e) {
////            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
////                return "BOOLEAN";
////            }
////        }
////
////        return "STRING";
////    }
//
//    /**
//     * 获取单元格值的字符串表示
//     */
//    private static String getCellValueAsString(Cell cell) {
//        if (cell == null) return "";
//
//        switch (cell.getCellType()) {
//            case STRING:
//                return "STRING";
//            case NUMERIC:
//                if (DateUtil.isCellDateFormatted(cell)) {
//                    return "date";
//                }
//                return "NUMERIC";
//            case BOOLEAN:
//                return "BOOLEAN";
//            case FORMULA:
//                return "FORMULA";
//            default:
//                return "";
//        }
//    }
//
//    /**
//     * 获取文件扩展名
//     */
//    private static String getFileExtension(String filePath) {
//        int lastDotIndex = filePath.lastIndexOf('.');
//        if (lastDotIndex > 0) {
//            return filePath.substring(lastDotIndex + 1);
//        }
//        return "";
//    }
//}
