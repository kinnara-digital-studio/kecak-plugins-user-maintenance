package com.kinnarastudio.kecakplugins.usermaintenance.form.validator;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class PasswordValidator extends FormValidator {
    public final static String LABEL = "Password Validator";

    @Override
    public boolean validate(Element element, FormData formData, String[] values) {
        final String elementId = element.getPropertyString("id");

        final String password = Arrays.stream(values)
                .findFirst()
                .orElse("");

        final String policyMessage = getPolicyMessage();

        boolean isRequired = isRequired();
        if (!password.isEmpty()) {
            int minimumLength = getMinimumPasswordLength();
            if (password.length() < minimumLength) {
                formData.addFormError(elementId, String.format("Password requires at least %d characters", minimumLength));
                return false;
            }

            if (needsUpperCase() && !contains(password, "[A-Z]")) {
                formData.addFormError(elementId, "Password requires UPPER CASEs");
                return false;
            }

            if (needsLowerCase() && !contains(password, "[a-z]")) {
                formData.addFormError(elementId, "Password requires lower cases");
                return false;
            }

            if (needsNumerics() && !contains(password, "[0-9]")) {
                formData.addFormError(elementId, "Password requires numerics");
                return false;
            }

            if (needsSpecialCharacters() && !contains(password, "[!@#\\$%\\^&\\*]")) {
                formData.addFormError(elementId, "Password requires special characters");
                return false;
            }
        } else if (isRequired) {
            formData.addFormError(elementId, "Password cannot be empty");
            return false;
        }

        return true;
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
        return AppUtil.readPluginResource(getClassName(), "/properties/form/validator/PasswordValidator.json");
    }

    protected String getPolicy() {
        return getPropertyString("policy");
    }

    protected boolean needsNumerics() {
        return getPolicy().contains("numbers");
    }

    protected boolean needsSpecialCharacters() {
        return getPolicy().contains("specials");
    }

    protected boolean needsUpperCase() {
        return getPolicy().contains("uppers");
    }

    protected boolean needsLowerCase() {
        return getPolicy().contains("lowers");
    }

    protected boolean contains(String value, String regex) {
        return Pattern.compile(regex).matcher(value).find();
    }

    protected String getPolicyMessage() {
        return "";
    }

    protected int getMinimumPasswordLength() {
        try {
            return Integer.parseInt(getPropertyString("minimumLength"));
        } catch (NumberFormatException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return 6;
        }
    }

    @Override
    public String getElementDecoration() {
        final StringBuilder sb = new StringBuilder();
        if (isRequired()) sb.append("*");
        if (needsUpperCase()) sb.append("A");
        if (needsLowerCase()) sb.append("a");
        if (needsNumerics()) sb.append("0");
        if (needsSpecialCharacters()) sb.append("@");

        return sb.toString();
    }

    protected boolean isRequired() {
        return "true".equalsIgnoreCase(getPropertyString("required"));
    }
}
