package com.kinnarastudio.kecakplugins.usermaintenance.utils;

import com.kinnarastudio.kecakplugins.usermaintenance.exceptions.PasswordException;
import org.apache.commons.text.RandomStringGenerator;
import org.joget.commons.util.StringUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;

public interface PasswordUtilMixin {
    default User generatePassword(final User user) throws PasswordException {
        final UserSecurity us = DirectoryUtil.getUserSecurity();

        if(user.getPassword() == null || user.getPassword().isEmpty()) {
            return user;
        } else if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new PasswordException("Password does not match");
        }

        if (us != null) {
            user.setPassword(us.encryptPassword(user.getUsername(), user.getPassword()));
        } else {
            user.setPassword(StringUtil.md5Base16(user.getPassword()));
        }
        user.setConfirmPassword(user.getPassword());
        return user;
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
        if (numeric) {
            sb.append("0123456789");
        }

        if (upperCase) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        if (lowerCase) {
            sb.append("abcdefhhijklmnopqrstuvwxyz");
        }

        if (specialCharacter) {
            sb.append("[]|\\@#%^&*()-_=+");
        }

        RandomStringGenerator pwdGenerator = new RandomStringGenerator.Builder()
                .selectFrom(sb.toString().toCharArray())
                .build();

        return pwdGenerator.generate(digits);
    }
}
