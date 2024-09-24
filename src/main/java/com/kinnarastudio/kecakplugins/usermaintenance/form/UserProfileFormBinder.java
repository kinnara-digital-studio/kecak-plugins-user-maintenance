package com.kinnarastudio.kecakplugins.usermaintenance.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.usermaintenance.exceptions.PasswordException;
import com.kinnarastudio.kecakplugins.usermaintenance.utils.PasswordUtilMixin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.directory.dao.OrganizationDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.Employment;
import org.joget.directory.model.User;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserProfileFormBinder extends DefaultFormBinder implements FormLoadElementBinder,
        FormStoreElementBinder, FormDataDeletableBinder, PasswordUtilMixin {

    @Override
    public String getFormId() {
        final Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_ID);
    }

    @Override
    public String getTableName() {
        return "dir_user";
    }

    @Override
    public FormRowSet load(Element element, String ignored, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        final String username = WorkflowUtil.getCurrentUsername();
        return Optional.ofNullable(username)
                .map(userDao::getUserById)
                .map(user -> {
                    final FormRow row = new FormRow();

                    row.setId(user.getId());
                    row.setProperty("username", Optional.ofNullable(user.getUsername()).orElse(""));
                    row.setProperty("firstName", Optional.ofNullable(user.getFirstName()).orElse(""));
                    row.setProperty("lastName", Optional.ofNullable(user.getLastName()).orElse(""));
                    row.setProperty("email", Optional.ofNullable(user.getEmail()).orElse(""));
                    row.setProperty("active", user.getActive() != 1 ? "false" : "true");
                    row.setProperty("locale", Optional.ofNullable(user.getLocale()).orElse(""));
                    row.setProperty("phoneNumber", Optional.ofNullable(user.getTelephoneNumber()).orElse(""));

                    final FormRowSet result = new FormRowSet();
                    result.add(row);
                    return result;
                })
                .orElseGet(FormRowSet::new);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        final Date now = new Date();

        if(WorkflowUtil.isCurrentUserAnonymous()) {
            return null;
        }

        return Optional.ofNullable(originalRowSet)
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .map(Try.onFunction(row -> {
                    final String currentUser = WorkflowUtil.getCurrentUsername();
                    row.setId(currentUser);

                    final String username = row.getProperty("username", row.getId());
                    final String firstName = row.getProperty("firstName");
                    final String lastName = row.getProperty("lastName");
                    final String email = row.getProperty("email");
                    final String strActive = row.getProperty("active", "true");
                    final int active = strActive.equals("true") || strActive.equals("active") || strActive.equals("1") ? 1 : 0;
                    final String locale = row.getProperty("locale");
                    final String telephoneNumber = row.getProperty("phoneNumber");
                    final String password = row.getProperty("password", "");
                    final String plainPassword = SecurityUtil.decrypt(password);
                    final String confirmPassword = row.getProperty("confirmPassword", "");
                    final String plainConfirmPassword = SecurityUtil.decrypt(confirmPassword);

                    final Optional<User> optUser = Optional.ofNullable(currentUser)
                            .filter(s -> !s.isEmpty())
                            .map(userDao::getUserById);

                    // update existing user
                    if (optUser.isPresent()) {
                        final User user = optUser.get();
                        user.setId(row.getId());
                        user.setUsername(username);
                        user.setFirstName(firstName);
                        user.setLastName(lastName);
                        user.setEmail(email);
                        user.setActive(active);
                        user.setLocale(locale);
                        user.setTelephoneNumber(telephoneNumber);
                        if(plainPassword == null || plainPassword.isEmpty()) {
                            // do not change password
                            user.setConfirmPassword(user.getPassword());
                            userDao.updateUser(user);
                        } else {
                            user.setPassword(plainPassword);
                            user.setConfirmPassword(plainConfirmPassword);
                            userDao.updateUser(generatePassword(user));
                        }

                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    }

                    final FormRowSet result = new FormRowSet();
                    result.add(row);
                    return result;
                }, (FormRow r, PasswordException e) -> {
                    formData.addFormError("password", e.getMessage());
                    formData.addFormError("confirmPassword", e.getMessage());
                    return null;
                }))
                .orElse(originalRowSet);
    }

    @Override
    public String getLabel() {
        return "User Profile Form Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
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
        return getClass().getPackage().getImplementationTitle();
    }
}
