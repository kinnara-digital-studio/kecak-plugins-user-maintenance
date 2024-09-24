package com.kinnarastudio.kecakplugins.usermaintenance.process;

import com.kinnarastudio.kecakplugins.usermaintenance.utils.PasswordUtilMixin;
import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * @author aristo
 *
 * Generate random password
 *
 */
public class GeneratePasswordTool extends DefaultApplicationPlugin implements PasswordUtilMixin {


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
    public Object execute(Map props) {
        final PluginManager pluginManager = (PluginManager) props.get("pluginManager");
        final UserDao userDao = (UserDao) pluginManager.getBean("userDao");
        final WorkflowManager workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager");
        final WorkflowAssignment workflowAssignment = (WorkflowAssignment) props.get("workflowAssignment");

        Optional.of("username")
                .map(props::get)
                .map(String::valueOf)
                .map(userDao::getUser)
                .ifPresent(Try.onConsumer(u -> {
                    final String password = generateRandomPassword(getDigits(), isNumeric(), isUpperCase(), isLowerCase(), isSpecialCharacters());
                    u.setPassword(password);
                    u.setConfirmPassword(password);

                    if("true".equalsIgnoreCase(getPropertyString("debug"))) {
                        LogUtil.info(getClassName(), "Updating password for user [" + u.getId() + "] password [" + u.getPassword() + "]");
                    }

                    final User updatedPassword = generatePassword(u);
                    userDao.updateUser(updatedPassword);

                    final String varPassword = getWorkflowVariableForPassword();
                    if(!varPassword.isEmpty()) {
                        workflowManager.processVariable(workflowAssignment.getProcessId(), varPassword, password);
                    }
                }));

        return null;
    }

    @Override
    public String getLabel() {
        return "Generate Password Tool";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/GeneratePasswordTool.json");
    }

    protected String getWorkflowVariableForPassword() {
        return getPropertyString("varPassword");
    }

    protected boolean isLowerCase() {
        return getPropertyString("passwordRules").contains("lower");
    }

    protected boolean isUpperCase() {
        return getPropertyString("passwordRules").contains("upper");
    }

    protected boolean isNumeric() {
        return getPropertyString("passwordRules").contains("numeric");
    }

    protected boolean isSpecialCharacters() {
        return getPropertyString("passwordRules").contains("special");
    }

    protected int getDigits() {
        return Optional.of("digits")
                .map(this::getPropertyString)
                .map(String::valueOf)
                .map(Try.onFunction(Integer::parseInt))
                .orElse(8);
    }
}
