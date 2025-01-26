package com.kinnarastudio.kecakplugins.usermaintenance.utils;

import com.kinnarastudio.kecakplugins.usermaintenance.form.binder.UserDirectoryFormBinder;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.LinkButton;
import org.joget.apps.form.lib.PasswordField;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    protected static Form loadFormByFormDefId(final String formDefId, final FormData formData, String mode) {
        Form form = new Form();
        form.setProperty(FormUtil.PROPERTY_ID, formDefId);
        form.setLoadBinder(new UserDirectoryFormBinder());
        form.setStoreBinder(new UserDirectoryFormBinder() {{
            setCheckPassword(true);
        }});

        final Collection<Element> children = Stream.of("id")
                .filter(s -> "add".equalsIgnoreCase(mode))
                .map(s -> {
                    final Element element = new TextField();
                    element.setProperty(FormUtil.PROPERTY_ID, s);
                    element.setProperty(FormUtil.PROPERTY_LABEL, createLabel(s));
                    return element;
                })
                .collect(Collectors.toList());

        final Collection<Element> textFieldElements = Stream.of(
                        "username", "firstName", "lastName", "email", "telephone_number")
                .filter(s -> "edit".equals(mode) || !s.equals("username"))
                .map(s -> {
                    final Element element = new TextField();
                    element.setProperty(FormUtil.PROPERTY_ID, s);
                    element.setProperty(FormUtil.PROPERTY_LABEL, createLabel(s));

                    if(s.equals("username")) {
                        FormUtil.setReadOnlyProperty(element);
                    }

                    return element;
                })
                .collect(Collectors.toList());

        children.addAll(textFieldElements);

        final Collection<Element> passwordElements = Stream.of("password", "confirm_password")
                .map(s -> {
                    final Element element = new PasswordField();
                    element.setProperty(FormUtil.PROPERTY_ID, s);
                    element.setProperty(FormUtil.PROPERTY_LABEL, createLabel(s));
                    return element;
                })
                .collect(Collectors.toList());

        children.addAll(passwordElements);

        form.setChildren(children);

        FormUtil.executeLoadBinders(form, formData);

        return form;
    }

    public static Form viewDataForm(String formDefId, String submitButtonLabel, String cancelButtonLabel, FormData formData, String formUrl, String cancelUrl, String mode) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");

        if (formData == null) {
            formData = new FormData();
        }

        // get form
        Form form = loadFormByFormDefId(formDefId, formData, mode);

        // set action URL
        if(formUrl != null) {
            form.setProperty("url", formUrl);
        }

        if (submitButtonLabel != null) {
            if (submitButtonLabel.isEmpty()) {
                submitButtonLabel = ResourceBundleUtil.getMessage("general.method.label.submit");
            }
            Element submitButton = (Element) pluginManager.getPlugin(SubmitButton.class.getName());
            submitButton.setProperty(FormUtil.PROPERTY_ID, "submit");
            submitButton.setProperty("label", submitButtonLabel);
            form.addAction((FormAction) submitButton);
        }

        if (cancelButtonLabel != null) {
            if (cancelButtonLabel.isEmpty()) {
                cancelButtonLabel = ResourceBundleUtil.getMessage("general.method.label.cancel");
            }
            Element cancelButton = (Element) pluginManager.getPlugin(LinkButton.class.getName());
            cancelButton.setProperty(FormUtil.PROPERTY_ID, "cancel");
            cancelButton.setProperty("label", cancelButtonLabel);
            cancelButton.setProperty("url", cancelUrl);
            cancelButton.setProperty("target", "window");
            form.addAction((FormAction) cancelButton);
        }

        form = decorateFormActions(form);

        return form;
    }

    protected final static Form decorateFormActions(Form form) {
        if (form != null && form.getActions() != null) {
            // create new section for buttons
            final Section section = new Section();
            section.setProperty(FormUtil.PROPERTY_ID, "section-actions");

            final Collection<Element> sectionChildren = new ArrayList<Element>();
            section.setChildren(sectionChildren);

            Collection<Element> formChildren = form.getChildren();
            if (formChildren == null) {
                formChildren = new ArrayList<>();
            }
            formChildren.add(section);

            // add new horizontal column to section
            final Column column = new Column();
            column.setProperty("horizontal", "true");

            final Collection<Element> columnChildren = new ArrayList<>();
            column.setChildren(columnChildren);

            sectionChildren.add(column);

            // add actions to column
            for (FormAction formAction : form.getActions()) {
                if (formAction instanceof Element) {
                    columnChildren.add((Element) formAction);
                }
            }
        }
        return form;
    }

    public static String createLabel(String input) {
        return Arrays.stream(input.split("(?=\\p{Upper})|[^a-zA-Z0-9]"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
