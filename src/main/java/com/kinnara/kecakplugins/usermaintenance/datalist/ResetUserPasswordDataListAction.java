package com.kinnara.kecakplugins.usermaintenance.datalist;

import com.kinnara.kecakplugins.usermaintenance.utils.PasswordUtilMixin;
import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class ResetUserPasswordDataListAction extends DataListActionDefault implements PasswordUtilMixin {
    @Override
    public String getLinkLabel() {
        return getPropertyString("label");
    }

    @Override
    public String getHref() {
        return getPropertyString("href");
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    @Override
    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    @Override
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Please Confirm";
        }
        return confirm;
    }

    @Override
    public DataListActionResult executeAction(DataList dataList, String[] keys) {
        // only allow POST
        if (!isPostMethod()) {
            return null;
        }

        LogUtil.info(getClassName(), "executeAction");

        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        Arrays.stream(keys)
                .map(userDao::getUser)
                .filter(Objects::nonNull)
                .forEach(Try.onConsumer(u -> {
                    final String password = generateRandomPassword(6, true, true, true, false);
                    u.setPassword(password);
                    u.setConfirmPassword(password);

                    LogUtil.info(getClassName(), "Updating password for user [" + u.getId() + "] password [" + u.getPassword() + "]");

                    final User updatedPassword = generatePassword(u);
                    userDao.updateUser(updatedPassword);
                }));

        final DataListActionResult result = new DataListActionResult();
        result.setUrl("REFERER");
        result.setType(DataListActionResult.TYPE_REDIRECT);
        return result;
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
        return "Reset Password";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/ResetUserPasswordDataListAction.json");
    }

    protected boolean isPostMethod() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();

        return Optional.ofNullable(request)
                .map(HttpServletRequest::getMethod)
                .map("POST"::equalsIgnoreCase)
                .orElse(false);
    }
}
