package com.kinnarastudio.kecakplugins.usermaintenance.form.validator;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.PluginManager;

import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class ConfirmationPasswordValidator extends FormValidator {
    public final static String LABEL = "Confirmation Password Validator";

    @Override
    public boolean validate(Element element, FormData formData, String[] values) {
        final String elementId = element.getPropertyString("id");

        final Form form = FormUtil.findRootForm(element);
        final String passwordField = getPasswordField();
        final Element passwordElement = FormUtil.findElement(getPasswordField(), form, formData);
        if (passwordElement == null) {
            formData.addFormError(elementId, "Element [" + passwordField + "] cannot be found");
            return false;
        }

        final String password = FormUtil.getRequestParameter(passwordElement, formData);
        final String plainPassword = SecurityUtil.decrypt(password);

        final String confirmPassword = Arrays.stream(values).findFirst().orElse("");
        final String plainConfirmPassword = SecurityUtil.decrypt(confirmPassword);
        if (!plainPassword.equals(plainConfirmPassword)) {
            final String errorMessage = getErrorMessage();
            formData.addFormError(elementId, errorMessage);
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
        return AppUtil.readPluginResource(getClassName(), "/properties/form/validator/ConfirmationPasswordValidator.json");
    }

    protected String getPasswordField() {
        return Optional.of(getPropertyString("passwordField"))
                .filter(Predicate.not(String::isEmpty))
                .orElse("password");
    }

    protected String getErrorMessage() {
        return "Unmatched password";
    }
}
