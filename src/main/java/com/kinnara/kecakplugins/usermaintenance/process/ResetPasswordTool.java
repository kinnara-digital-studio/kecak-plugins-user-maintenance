package com.kinnara.kecakplugins.usermaintenance.process;

import com.kinnara.kecakplugins.usermaintenance.utils.PasswordUtilMixin;
import com.kinnarastudio.commons.Try;
import org.apache.commons.text.RandomStringGenerator;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

import java.util.Map;
import java.util.Optional;

public class ResetPasswordTool extends DefaultApplicationPlugin implements PasswordUtilMixin {
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
    public Object execute(Map props) {
        PluginManager pluginManager = (PluginManager) props.get("pluginManager");
        UserDao userDao = (UserDao) pluginManager.getBean("userDao");
        WorkflowManager workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager");
        WorkflowAssignment workflowAssignment = (WorkflowAssignment) props.get("workflowAssignment");
//        UserSecurity us = DirectoryUtil.getUserSecurity();

        String username = String.valueOf(props.get("username"));

        Optional.of(username)
                .map(userDao::getUser)
                .ifPresent(Try.onConsumer(u -> {
                    final String password = generateRandomPassword(getDigits(props), isNumeric(), isUpperCase(), isLowerCase(), isSpecialCharacters());

                    LogUtil.info(getClassName(), "Updating password for user ["+ u.getId() + "] with ["+password+"]");

                    u.setPassword(password);
                    u.setConfirmPassword(password);

                    updatePassword(u);

                    String varPassword = getWorkflowVariableForPassword();
                    if(!varPassword.isEmpty()) {
                        workflowManager.processVariable(workflowAssignment.getProcessId(), varPassword, password);
                    }
                }));


        return null;
    }

    @Override
    public String getLabel() {
        return "Reset Password Tool";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/ResetPasswordTool.json");
    }

    protected String generateRandomPassword(int digits, boolean numeric, boolean upperCase, boolean lowerCase, boolean specialCharacter) {
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

    protected int getDigits(Map props) {
        return Optional.of("digits")
                .map(props::get)
                .map(String::valueOf)
                .map(Try.onFunction(Integer::parseInt))
                .orElse(8);
    }
}
