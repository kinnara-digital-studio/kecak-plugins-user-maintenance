package com.kinnara.kecakplugins.usermaintenance.form;

import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.HashSalt;
import org.joget.commons.util.PasswordGeneratorUtil;
import org.joget.commons.util.PasswordSalt;
import org.joget.commons.util.SecurityUtil;
import org.joget.directory.dao.*;
import org.joget.directory.model.Employment;
import org.joget.directory.model.Organization;
import org.joget.directory.model.User;
import org.joget.directory.model.UserSalt;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

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
public class UserDirectoryFormBinder extends DefaultFormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder {
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
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        return Optional.ofNullable(primaryKey)
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
                    row.setProperty("telephone_number", Optional.ofNullable(user.getTelephoneNumber()).orElse(""));

                    Optional.of(user)
                            .map(User::getEmployments)
                            .map(Collection<Employment>::stream)
                            .orElseGet(Stream::empty)
                            .findFirst()
                            .map(Employment::getOrganizationId)
                            .ifPresent(s -> row.setProperty("organizationId", s));

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
        final RoleDao roleDao = (RoleDao) applicationContext.getBean("roleDao");
        final OrganizationDao organizationDao = (OrganizationDao) applicationContext.getBean("organizationDao");

        final Date now = new Date();
        final String currentUser = WorkflowUtil.getCurrentUsername();

        return Optional.ofNullable(originalRowSet)
                .map(FormRowSet::stream)
                .orElseGet(Stream::empty)
                .findFirst()
                .map(Try.onFunction(row -> {
                    final String primaryKey = Optional.of(row)
                            .map(FormRow::getId)
                            .orElseGet(formData::getPrimaryKeyValue);

                    row.setId(primaryKey);

                    final Optional<User> optUser = Optional.ofNullable(primaryKey)
                            .filter(s -> !s.isEmpty())
                            .map(userDao::getUserById);

                    final Optional<Organization> optOrganization = Optional.ofNullable(row.getProperty("organizationId"))
                            .map(organizationDao::getOrganization);

                    final String active = row.getProperty("active", "true");
                    final String password = row.getProperty("password", "");
                    final String confirmPassword = row.getProperty("confirm_password", "");

                    if (optUser.isPresent()) {
                        final User user = optUser.get();
                        user.setId(row.getId());
                        user.setUsername(row.getProperty("username", row.getId()));
                        user.setFirstName(row.getProperty("firstName"));
                        user.setLastName(row.getProperty("lastName"));
                        user.setEmail(row.getProperty("email"));
                        user.setActive("true".equals(active) || "active".equals(active) || "1".equals(active) ? 1 : 0);
                        user.setLocale(row.getProperty("locale"));
                        user.setTelephoneNumber(row.getProperty("telephone_number"));
                        user.setDateModified(row.getDateModified());
                        user.setModifiedBy(row.getModifiedBy());

                        updatePassword(user, password, confirmPassword);

                        optOrganization.ifPresent(o -> setEmployment(user, o));

                        userDao.updateUser(user);

                        row.setDateModified(now);
                        row.setModifiedBy(currentUser);
                    } else {
                        final User user = new User();
                        user.setId(row.getId());
                        user.setUsername(row.getProperty("username", row.getId()));
                        user.setFirstName(row.getProperty("firstName"));
                        user.setLastName(row.getProperty("lastName"));
                        user.setEmail(row.getProperty("email"));
                        user.setActive(active.equals("true") || active.equals("active") || active.equals("1") ? 1 : 0);
                        user.setLocale(row.getProperty("locale"));
                        user.setTelephoneNumber(row.getProperty("telephone_number"));

                        updatePassword(user, password, confirmPassword);

                        user.setDateCreated(now);
                        user.setCreatedBy(currentUser);
                        userDao.addUser(user);

                        Optional.of("roleId")
                                .map(s -> row.getProperty(s, WorkflowUtil.ROLE_USER))
                                .map(roleDao::getRole)
                                .map(Collections::singleton)
                                .ifPresent(user::setRoles);

                        optOrganization.ifPresent(o -> setEmployment(user, o));

                        row.setDateCreated(now);
                        row.setCreatedBy(currentUser);
                    }

                    final FormRowSet result = new FormRowSet();
                    result.add(row);
                    return result;
                })).orElse(originalRowSet);
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
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
                .map(Collection<Employment>::stream)
                .orElseGet(Stream::empty)
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

    protected void updatePassword(User user, String password, String confirmPassword) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserSaltDao userSaltDao = (UserSaltDao) applicationContext.getBean("userSaltDao");
        final UserSecurity us = DirectoryUtil.getUserSecurity();
        final String plainPassword = SecurityUtil.decrypt(password);
        final String plainConfirmPassword = SecurityUtil.decrypt(confirmPassword);

        if (!plainPassword.isEmpty() && plainPassword.equals(plainConfirmPassword)) {
            if (us != null) {
                user.setPassword(us.encryptPassword(user.getUsername(), password));
            } else {
                final Optional<UserSalt> optOurrentUserSalt = Optional.of(user)
                        .map(User::getId)
                        .map(userSaltDao::getUserSaltByUserId);

                // update password
                if (optOurrentUserSalt.isPresent()) {
                    optOurrentUserSalt
                            .map(UserSalt::getRandomSalt)
                            .map(s -> new PasswordSalt(s, plainPassword))
                            .map(Try.onFunction(PasswordGeneratorUtil::hashPassword))
                            .ifPresent(user::setPassword);
                }

                // create new password
                else {
                    optOurrentUserSalt.orElseGet(Try.onSupplier(() -> {
                        final HashSalt hashSalt = PasswordGeneratorUtil.createNewHashWithSalt(plainPassword);

                        final UserSalt userSalt = new UserSalt();
                        userSalt.setUserId(user.getId());
                        userSalt.setId(UUID.randomUUID().toString());
                        userSalt.setRandomSalt(hashSalt.getSalt());

                        final Date date = new Date();
                        userSalt.setDateCreated(date);
                        userSalt.setDateModified(date);

                        final String currentUser = WorkflowUtil.getCurrentUsername();
                        userSalt.setCreatedBy(currentUser);
                        userSalt.setModifiedBy(currentUser);

                        userSaltDao.addUserSalt(userSalt);

                        return userSalt;
                    }));
                }
            }
        }
    }
}
