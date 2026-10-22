package org.apache.dolphinscheduler.plugin.task.api.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

//
//import dm.jdbc.util.StringUtil;
//import org.apache.commons.io.FilenameUtils;
////import org.geotools.coverage.grid.GridCoverage2D;
////import org.geotools.coverage.processing.CoverageProcessor;
////import org.geotools.factory.Hints;
////import org.geotools.gce.geotiff.GeoTiffReader;
////import org.geotools.gce.geotiff.GeoTiffWriter;
////import org.geotools.referencing.CRS;
////import org.opengis.coverage.Coverage;
////import org.opengis.parameter.ParameterValueGroup;
////import org.opengis.referencing.FactoryException;
////import org.opengis.referencing.NoSuchAuthorityCodeException;
////import org.opengis.referencing.crs.CoordinateReferenceSystem;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.annotation.Nonnull;
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
@Slf4j
@UtilityClass
public class TiffCoverUtil {

//   public void getTiff(String a,String b,String c){
//       System.out.println(a);
//       System.out.println(b);
//       System.out.println(c);
//    }
//
//        private static final Logger logger = LoggerFactory.getLogger(TiffCoverUtil.class);
//
//        static {// 实测 这段静态代码块没什么作用！
//            // 当设置为true时，GeoTools会严格检查坐标变换的数学合理性（例如是否保角、是否保持距离等），可能导致某些不满足条件的变换被拒绝。
//            // 通常用于需要高精度地理计算的场景（如工程测绘），但可能增加计算失败的风险。
//            System.setProperty("org.geotools.referencing.criteria", "true");
//            // 重置GeoTools的CRS缓存和配置。
//            CRS.reset("all");
//        }
//
//        /**
//         * 将tiff格式地理坐标转换为指定坐标系
//         * @param inputPath 文件读取路径
//         * @param outputPath 文件输出路径
//         * @param coverEPSG 转换后的EPSG代码
//         *                  CGCS2000代码为：EPSG:4490
//         *                  WGS84代码为：EPSG:4326
//         * @return GridCoverage2D
//         */
//        public static GridCoverage2D tiffCovert(String inputPath, String outputPath, String coverEPSG) {
//            if(StringUtil.isEmpty(inputPath)||StringUtil.isEmpty(outputPath)||StringUtil.isEmpty(coverEPSG)){
//                throw new IllegalArgumentException("文件读取路径不允许为空，请检查！");
//            }
//            if(!FilenameUtils.getExtension(inputPath).equals("tif") && !FilenameUtils.getExtension(outputPath).equals("tiff")){
//                throw new IllegalArgumentException("文件格式不正确，请检查！");
//            }
//            GeoTiffReader reader = null;
//            GridCoverage2D reporjected = null;
//            try {
//                // 创建GridCoverage2DReader 对象
//                reader = new GeoTiffReader(new File(inputPath));
//                // 读取整个栅格数据
//                GridCoverage2D sourceCoverage = reader.read(null);
//                if(sourceCoverage == null){
//                    logger.error("无法读取TIFF文件，文件不存在！");
//                    throw new RuntimeException("无法读取TIFF文件，文件不存在！");
//                }
//                // 获取原始数据的坐标参考系统
//                CoordinateReferenceSystem sourceCRS = sourceCoverage.getCoordinateReferenceSystem();
//                logger.info(" 原始坐标参考系统 CRS: {}", CRS.toSRS(sourceCRS));
//                CoordinateReferenceSystem targetCRS = CRS.decode(coverEPSG);
//                // 检查原始CRS是否已经是所需转换的标准坐标
//                if(!CRS.toSRS(sourceCRS).equalsIgnoreCase(coverEPSG)) {
//                    // 1：重新投影 创建处理器并设置参数
//                    CoverageProcessor processor = CoverageProcessor.getInstance();
//                    ParameterValueGroup params = processor.getOperation("Resample").getParameters();
//                    params.parameter("Source").setValue(sourceCoverage);
//                    params.parameter("CoordinateReferenceSystem").setValue(targetCRS);
//                    Hints hints = new Hints();
//                    hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,Boolean.TRUE);
//                    hints.put(Hints.LENIENT_DATUM_SHIFT,Boolean.TRUE);
//                    // 重新投影
//                    Coverage Coverage = processor.doOperation(params,hints);
//                    reporjected = (GridCoverage2D)Coverage;
//                    // 输出文件
//                    String fileName = FilenameUtils.getBaseName(inputPath);
//                    // 创建输出文件夹
//                    Path path = Paths.get(outputPath);
//                    Files.createDirectories(path);
//                    GeoTiffWriter writer = new GeoTiffWriter(new File(outputPath,fileName+"_NEW.tif"));
//                    writer.write(reporjected,null);
//                    writer.dispose();
//                    // 输出转换后信息
//                    logger.info("源数据为 "+CRS.toSRS(sourceCRS)+"坐标系, 已经转换为 "+coverEPSG+" 标准坐标！");
//                } else {
//                    logger.info("原数据已经是 "+coverEPSG+" 坐标，无需转换！");
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            } catch (NoSuchAuthorityCodeException e) {
//                throw new RuntimeException(e);
//            } catch (FactoryException e) {
//                throw new RuntimeException(e);
//            } finally {
//                if(reader != null) { // 关闭GeoTiffReader释放资源
//                    reader.dispose();
//                }
//            }
//            return reporjected;
//        }
//
//    public static void main(String[] args) throws FactoryException, IOException {
//        tiffCovert("E:\\航天\\投影插件测试数据\\CGCS2000.tif","E:\\newTIFF","EPSG:4326");
//    }
//
}
