package com.kinnarastudio.kecakplugins.usermaintenance.form.binder;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.usermaintenance.utils.PasswordUtilMixin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.commons.util.SecurityUtil;
import org.joget.directory.dao.EmploymentDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.Employment;
import org.joget.directory.model.Organization;
import org.joget.directory.model.User;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author aristo
 * <p>
 * Store data into directory table <b>dir_user</b> and <b>dir_employment</b>.
 * <br/>
 * Build in properties are:
 * <ul>
 *     <li>Table <b>dir_user</b></li>
 *     <ul>
 *         <li>id</li>
 *         <li>username</li>
 *         <li>firstName</li>
 *         <li>lastName</li>
 *         <li>email</li>
 *         <li>telephoneNumber</li>
 *         <li>active</li>
 *         <li>locale</li>
 *         <li>telephone_number</li>
 *     </ul>
 *     <li>Table <b>dir_employment</b></li>
 *     <ul>
 *         <li>organizationId</li>
 *     </ul>
 * </ul>
 */
public class UserDirectoryFormBinder extends DefaultFormBinder implements FormLoadElementBinder,
        FormStoreElementBinder, FormDeleteBinder, PasswordUtilMixin {

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        return Optional.ofNullable(primaryKey)
                .map(userDao::getUserById)
                .map(user -> new FormRow() {{
                    setId(user.getId());
                    setProperty("username", Optional.ofNullable(user.getUsername()).orElse(""));
                    setProperty("firstName", Optional.ofNullable(user.getFirstName()).orElse(""));
                    setProperty("lastName", Optional.ofNullable(user.getLastName()).orElse(""));
                    setProperty("email", Optional.ofNullable(user.getEmail()).orElse(""));
                    setProperty("active", user.getActive() != 1 ? "false" : "true");
                    setProperty("locale", Optional.ofNullable(user.getLocale()).orElse(""));
                    setProperty("phoneNumber", Optional.ofNullable(user.getTelephoneNumber()).orElse(""));
                    setProperty("password", "");
                    setProperty("confirmPassword", "");
                }})
                .map(row -> new FormRowSet() {{
                    add(row);
                }})
                .orElse(null);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet originalRowSet, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        return Optional.ofNullable(originalRowSet)
                .stream()
                .flatMap(Collection::stream)
                .findFirst()
                .map(Try.onFunction(row -> {
                    final String primaryKey = Optional.of(row)
                            .map(FormRow::getId)
                            .orElseGet(formData::getPrimaryKeyValue);

                    row.setId(primaryKey);

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

                    final Optional<User> optUser = Optional.ofNullable(primaryKey)
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

                        if (plainPassword == null || plainPassword.isEmpty()) {
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

                    // create new user
                    else {
                        userDao.addUser(generatePassword(new User() {{
                            setId(row.getId());
                            setUsername(username);
                            setFirstName(firstName);
                            setLastName(lastName);
                            setEmail(email);
                            setActive(active);
                            setLocale(locale);
                            setTelephoneNumber(telephoneNumber);
                            setPassword(plainPassword);
                            setConfirmPassword(plainConfirmPassword);
                        }}));

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    }

                    final FormRowSet result = new FormRowSet() {{
                        add(row);
                    }};

                    return result;
                }))
                .orElse(originalRowSet);
    }

    @Override
    public String getName() {
        return getLabel();
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

    @Override
    public String getLabel() {
        return "User Directory Form Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Nonnull
    protected User setEmployment(@Nonnull User user, @Nonnull Organization organization) {
        final EmploymentDao employmentDao = (EmploymentDao) AppUtil.getApplicationContext().getBean("employmentDao");

        final Employment employment = Optional.of(user)
                .map(User::getEmployments)
                .stream()
                .flatMap(Collection<Employment>::stream)
                .findFirst()
                .orElseGet(() -> {
                    Employment newEmployment = new Employment();
                    newEmployment.setId(UUID.randomUUID().toString());
                    newEmployment.setUserId(user.getUsername());
                    newEmployment.setOrganizationId(organization.getId());

                    employmentDao.addEmployment(newEmployment);
                    return newEmployment;
                });

        employmentDao.assignUserToOrganization(employment.getUserId(), organization.getId());

        return user;
    }

    /**
     * Check password in {@link #store(Element, FormRowSet, FormData)}
     */
    public boolean checkPassword() {
//        return "true".equalsIgnoreCase(getPropertyString("checkPassword"));
        return true;
    }

    public void setCheckPassword(boolean checkPassword) {
        setProperty("checkPassword", "true");
    }

    @Override
    public void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        Optional.ofNullable(rowSet)
                .stream()
                .flatMap(FormRowSet::stream)
                .map(FormRow::getId)
                .forEach(userDao::deleteUser);
    }
}
