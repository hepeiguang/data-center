package org.apache.dolphinscheduler.api.service;

import java.util.List;
import java.util.Map;

public interface YunheziService {
    List<String> getVersionTypeList();

    Map<String, String> getParmeterMap(String name);
}
