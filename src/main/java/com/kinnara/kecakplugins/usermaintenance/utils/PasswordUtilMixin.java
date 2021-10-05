package com.kinnara.kecakplugins.usermaintenance.utils;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.HashSalt;
import org.joget.commons.util.PasswordGeneratorUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.dao.UserSaltDao;
import org.joget.directory.model.User;
import org.joget.directory.model.UserSalt;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.springframework.context.ApplicationContext;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface PasswordUtilMixin {
    default void updatePassword(User user) throws NoSuchAlgorithmException, InvalidKeySpecException {
        UserSecurity us = DirectoryUtil.getUserSecurity();
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        UserSaltDao userSaltDao = (UserSaltDao) applicationContext.getBean("userSaltDao");
        UserSalt userSalt = userSaltDao.getUserSaltByUserId(user.getUsername());

        if (user.getPassword() != null && user.getConfirmPassword() != null && user.getPassword().length() > 0 && user.getPassword().equals(user.getConfirmPassword())) {
            if (us != null) {
                user.setPassword(us.encryptPassword(user.getUsername(), user.getPassword()));
            } else {
                //currentUser.setPassword(StringUtil.md5Base16(user.getPassword()));
                HashSalt hashSalt = PasswordGeneratorUtil.createNewHashWithSalt(user.getPassword());
                userSalt.setRandomSalt(hashSalt.getSalt());

                user.setPassword(hashSalt.getHash());
            }
            user.setConfirmPassword(user.getPassword());
        }

        userDao.updateUser(user);
        userSaltDao.updateUserSalt(userSalt);
        if (us != null) {
            us.updateUserProfilePostProcessing(user);
        }
    }
}
