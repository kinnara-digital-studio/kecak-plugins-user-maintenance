package com.kinnara.kecakplugins.usermaintenance.utils;

import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.HashSalt;
import org.joget.commons.util.PasswordGeneratorUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.dao.UserSaltDao;
import org.joget.directory.model.User;
import org.joget.directory.model.UserSalt;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface PasswordUtilMixin {
    default void updatePassword(@Nonnull User user) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final UserSecurity us = DirectoryUtil.getUserSecurity();
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final UserSaltDao userSaltDao = (UserSaltDao) applicationContext.getBean("userSaltDao");
        final UserSalt userSalt = Optional.of(user)
                .map(User::getUsername)
                .map(userSaltDao::getUserSaltByUserId)
                .orElseGet(Try.onSupplier(() -> generateSalt(user)));

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

    @Nonnull
    default UserSalt generateSalt(User user) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserSaltDao userSaltDao = (UserSaltDao) applicationContext.getBean("userSaltDao");

        final String currentUsername = WorkflowUtil.getCurrentUsername();
        final Date now = new Date();
        final UserSalt newUserSalt = new UserSalt();

        newUserSalt.setId(UUID.randomUUID().toString());
        newUserSalt.setUserId(user.getId());
        newUserSalt.setCreatedBy(currentUsername);
        newUserSalt.setModifiedBy(currentUsername);
        newUserSalt.setDateCreated(now);
        newUserSalt.setDateModified(now);

        final HashSalt hashSalt = PasswordGeneratorUtil.createNewHashWithSalt(user.getPassword());
        newUserSalt.setRandomSalt(hashSalt.getSalt());
        userSaltDao.addUserSalt(newUserSalt);

        return newUserSalt;
    }
}
