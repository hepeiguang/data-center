package org.apache.dolphinscheduler.api.security.impl.dam;

import org.apache.dolphinscheduler.api.security.impl.AbstractAuthenticator;
import org.apache.dolphinscheduler.api.service.UsersService;
import org.apache.dolphinscheduler.dao.entity.User;
import org.springframework.beans.factory.annotation.Autowired;

public class DamAuthenticator  extends AbstractAuthenticator {


    @Autowired
    private UsersService usersService;
    @Autowired
    DamService damService;

    @Override
    public User login(String userId, String password) {
        DamUser damUser = damService.damLogin(userId, password);
        return createUser(damUser);
    }

    @Override
    public User tokenValidation(String token) {
        DamUser damUser = damService.tokenValidation(token);
        return createUser(damUser);
    }

    private User createUser(DamUser damUser) {
        User user = null;
        if (damUser != null) {
            //check if user exist
            user = usersService.getUserByUserName(damUser.getUsername());
            if (user == null) {
                user = usersService.createUser(damService.getUserType(damUser.getUsername()), damUser.getUsername(), damUser.getEmail());
            }
        }
        return user;
    }

}
