package com.kinnara.kecakplugins.usermaintenance.utils;

import com.kinnara.kecakplugins.usermaintenance.form.UserDirectoryFormBinder;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.LinkButton;
import org.joget.apps.form.lib.SubmitButton;
import org.joget.apps.form.lib.TextField;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class Utils {
    public static Form viewDataForm(FormData formData, String submitLabel, String cancelLabel, String cancelUrl) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");

        Form form = new Form();
        form.setProperty("id", "form_update_profile");
        form.setLoadBinder(new UserDirectoryFormBinder());
        form.setStoreBinder(new UserDirectoryFormBinder());

        final Collection<String> defaultFields = Arrays.asList(
                "firstName", "lastName", "email", "telephoneNumber", "password", "confirmPassword", "oldPassword");

        final Collection<Element> children = defaultFields.stream()
                .map(s -> {
                    final TextField textField = new TextField();
                    textField.setProperty(FormUtil.PROPERTY_ID, s);
                    textField.setProperty(FormUtil.PROPERTY_LABEL, s);
                    return textField;
                })
                .collect(Collectors.toList());

        {
            final Section sectionActions = new Section();
            sectionActions.setProperty(FormUtil.PROPERTY_ID, "section-actions");
            sectionActions.setProperty(FormUtil.PROPERTY_LABEL, "section-actions");

            final Collection<Element> sectionButtons = new ArrayList<>();
            {
                final Element submitButton = (Element) pluginManager.getPlugin(SubmitButton.class.getName());
                submitButton.setProperty(FormUtil.PROPERTY_ID, "submit");
                submitButton.setProperty(FormUtil.PROPERTY_LABEL, submitLabel);
                sectionButtons.add(submitButton);

                form.addAction((FormAction) submitButton, formData);
            }

            {
                final Element cancelButton = (Element) pluginManager.getPlugin(LinkButton.class.getName());
                cancelButton.setProperty(FormUtil.PROPERTY_ID, "cancel");
                cancelButton.setProperty("label", cancelLabel);
                cancelButton.setProperty("url", cancelUrl);
                sectionButtons.add(cancelButton);

                form.addAction((FormAction) cancelButton, formData);
            }

            sectionActions.setChildren(sectionButtons);

            children.add(sectionActions);
        }

        form.setChildren(children);

        return form;
    }
}
