package org.apache.dolphinscheduler.api.service.impl;

import org.apache.dolphinscheduler.api.service.YunheziService;
import org.apache.dolphinscheduler.spi.enums.YunheziVersionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class YunheziServiceImpl implements YunheziService {
    @Override
    public List<String> getVersionTypeList() {
        return YunheziVersionType.getAllTypeIds();
    }

    @Override
    public Map<String, String> getParmeterMap(String name) {
        return YunheziVersionType.getByName(name).getGetDirListParamMap();
    }
}
