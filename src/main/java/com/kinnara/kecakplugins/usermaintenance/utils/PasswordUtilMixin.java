package com.kinnara.kecakplugins.usermaintenance.utils;

import com.kinnarastudio.commons.Try;
import org.apache.commons.text.RandomStringGenerator;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.*;
import org.joget.directory.dao.UserDao;
import org.joget.directory.dao.UserSaltDao;
import org.joget.directory.model.User;
import org.joget.directory.model.UserSalt;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface PasswordUtilMixin {
    default User updatePassword(final User user) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserSaltDao userSaltDao = (UserSaltDao) applicationContext.getBean("userSaltDao");
        final UserSecurity us = DirectoryUtil.getUserSecurity();
        final String plainPassword = SecurityUtil.decrypt(user.getPassword());
        final String plainConfirmPassword = SecurityUtil.decrypt(user.getConfirmPassword());

        if (!plainPassword.isEmpty() && plainPassword.equals(plainConfirmPassword)) {
            final String encryptedPassword;
            if (us != null) {
                encryptedPassword = us.encryptPassword(user.getUsername(), plainPassword);

            } else {
                final UserSalt userSalt = Optional.of(user)
                        .map(User::getId)
                        .map(userSaltDao::getUserSaltByUserId)
                        .orElseGet(Try.onSupplier(() -> {
                            // generate new user salt and store to database
                            final UserSalt newUserSalt = generateSalt(user);
                            userSaltDao.addUserSalt(newUserSalt);
                            return newUserSalt;
                        }));

                final String randomSalt = userSalt.getRandomSalt();
                final PasswordSalt passwordSalt = new PasswordSalt(randomSalt, plainPassword);
                encryptedPassword = PasswordGeneratorUtil.hashPassword(passwordSalt);
            }

            user.setPassword(encryptedPassword);
        }

        return user;
    }

    @Nonnull
    default UserSalt generateSalt(final User user) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final UserSalt userSalt = new UserSalt();
        userSalt.setId(UUID.randomUUID().toString());
        userSalt.setUserId(user.getId());

        final String password = user.getPassword();
        final HashSalt hashSalt = PasswordGeneratorUtil.createNewHashWithSalt(password);
        userSalt.setRandomSalt(hashSalt.getSalt());

        final Date date = new Date();
        userSalt.setDateCreated(date);
        userSalt.setDateModified(date);

        final String currentUser = WorkflowUtil.getCurrentUsername();
        userSalt.setCreatedBy(currentUser);
        userSalt.setModifiedBy(currentUser);

        return userSalt;
    }

    /**
     * Generate Random Password
     *
     * @param digits
     * @param numeric
     * @param upperCase
     * @param lowerCase
     * @param specialCharacter
     * @return
     */
    default String generateRandomPassword(int digits, boolean numeric, boolean upperCase, boolean lowerCase, boolean specialCharacter) {
        StringBuilder sb = new StringBuilder();
        if(numeric) {
            sb.append("0123456789");
        }

        if(upperCase) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        if(lowerCase) {
            sb.append("abcdefhhijklmnopqrstuvwxyz");
        }

        if(specialCharacter) {
            sb.append("[]|\\@#%^&*()-_=+");
        }

        RandomStringGenerator pwdGenerator = new RandomStringGenerator.Builder()
                .selectFrom(sb.toString().toCharArray())
                .build();

        return pwdGenerator.generate(digits);
    }
}
