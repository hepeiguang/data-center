package org.apache.dolphinscheduler.dao.mapper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.apache.dolphinscheduler.dao.entity.Resources;
import org.apache.dolphinscheduler.spi.enums.ResourceType;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ResourcesMapper {
    //保存资源
    Integer saveResource(Resources resource);

    //通过完整的资源名找到数据库中的数据
    Resources getResourceByFullName(@Param("fullName") String fullName);

    //通过完整的资源名找到数据库中的数据  不能等于自己
    List<Resources> getAllResourceList(@Param("fullName") String fullName);

    //查询所有资源
    List<Resources> getResourceList();


    //通过部分中间资源名找到数据库中的数据  不能等于自己
    List<Resources> getResourceListLikePartName(String partName);

    //修改资源
    Integer fixResourceByFullName(@Param("fullName")  String fullName,
                                  @Param("fileName") String fileName,
                                  @Param("updateTime") Date updateTime,
                                  @Param("oldFullName") String oldFullName
    );

    //删除资源
    Integer delResourceByFullName(String fullName);


    //分页查询
    List<Resources> getResourceListPaging(
            @Param("searchVal") String trimmedSearchVal,
            @Param("fullName") String fullName,
            //@Param("resTenantCode") String resTenantCode,
            @Param("type") ResourceType type,
            @Param("customType") String customType,
            @Param("level") Integer level
            //@Param("parentPath") String parentPath
    );




}
