package org.apache.dolphinscheduler.api.security.impl.htzh;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.api.security.impl.AbstractAuthenticator;
import org.apache.dolphinscheduler.api.security.impl.htzh.dto.HtzhSsoAuthInfo;
import org.apache.dolphinscheduler.api.security.impl.htzh.dto.HtzhSyncUserDeptDto;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.apache.dolphinscheduler.data.quality.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
@Slf4j
public class HtzhAuthenticator extends AbstractAuthenticator {


    public static final int TIME_OUT_MS = 10000;
    @Autowired
    private UserMapper  userMapper;

    @Autowired
    private HtzhAuthConfig htzhAuthConfig;

    @Override
    public User login(String userId, String password) {
//        DamUser damUser = damService.damLogin(userId, password);
//        return createUser(damUser);
        throw new RuntimeException("不支持用户名密码登录");
    }

    @Override
    public User tokenValidation(String token) {
        String url = htzhAuthConfig.getSsoAuthUrl(token, "dam-etl");
        //校验token是否有效，数据结构如下 {"token":"", "username":"超级管理员", "account":"admin", "userId": "1", "expiration": 86400, "loginStatus": true, "userAttrs": {"tenantId": "-1"}}
        String tokenResult = HttpUtil.createGet(url).timeout(TIME_OUT_MS).execute().body();
        HtzhSsoAuthInfo authInfo = JsonUtils.fromJson(tokenResult, HtzhSsoAuthInfo.class);
        if (!authInfo.valid()) {
            throw new RuntimeException("token无效");
        }
        return createUser(authInfo);
    }

    private User createUser(HtzhSsoAuthInfo authInfo) {
        User user = userMapper.queryByUserNameAccurately(authInfo.getAccount());
        if (user != null) {
            return user;
        }
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Basic " + authInfo.getToken());
        header.put("Content-Type", "application/x-www-form-urlencoded");
        String userUrl = htzhAuthConfig.getUserUrl(authInfo.getAccount());
        String tokenResult = HttpUtil.createGet(userUrl).addHeaders(header).timeout(TIME_OUT_MS).execute().body();
        HtzhSyncUserDeptDto dto = JsonUtils.fromJson(tokenResult, HtzhSyncUserDeptDto.class);
        if (!dto.isValid()) {
            log.error("Not found user from htzh system, url: {}", userUrl);
            throw new RuntimeException("未找到用户信息");
        }
        user = dto.getUserList().get(0).toUser();
        userMapper.insert(user);
        return user;
    }

}
