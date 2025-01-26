package com.kinnarastudio.kecakplugins.usermaintenance.form.validator;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.commons.util.StringUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfilePasswordValidator extends FormValidator {
    public final static String LABEL = "Profile Password Validator";

    @Override
    public boolean validate(Element element, FormData formData, String[] values) {
        final String password = Arrays.stream(values)
                .findFirst()
                .orElse("");

        if(password.isEmpty()) {
            return true;
        }

        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final String username = formData.getPrimaryKeyValue();
        final Optional<User> optUser = Optional.of(username)
                .map(userDao::getUserById);

        if(optUser.isEmpty()) {
            return true;
        }

        final User user = optUser.get();

        final UserSecurity us = DirectoryUtil.getUserSecurity();
        if(us != null) {
            if(us.verifyPassword(user, password)) {
                return true;
            }
        }

        final String hashedPassword = StringUtil.md5Base16(password);
        if(hashedPassword.equals(user.getPassword())) {
            return true;
        }

        final String elementId = element.getPropertyString("id");
        formData.addFormError(elementId, "Invalid password");
        return false;
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
}
