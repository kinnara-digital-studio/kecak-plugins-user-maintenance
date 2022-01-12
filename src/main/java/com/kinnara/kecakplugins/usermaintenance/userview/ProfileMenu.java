package com.kinnara.kecakplugins.usermaintenance.userview;

import com.kinnara.kecakplugins.usermaintenance.form.UserDirectoryFormBinder;
import com.kinnara.kecakplugins.usermaintenance.utils.Utils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.SubmitButton;
import org.joget.apps.form.lib.TextField;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.kecak.apps.form.service.FormDataUtil;
import org.springframework.context.ApplicationContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class ProfileMenu extends UserviewMenu {

    @Override
    public String getLabel() {
        return "User Profile";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
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
    public String getCategory() {
        return "Kecak";
    }

    @Override
    public String getIcon() {
        return "/plugin/" + getClassName() + "/images/grid_icon.gif";
    }


    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDecoratedMenu() {
        return null;
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/profileMenu.json", null, true, "/messages/profileMenu");
    }

    @Override
    public String getRenderPage() {
        return null;
    }

    @Override
    public String getJspPage() {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        final PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");
        AppService appService = (AppService) applicationContext.getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        final Map<String, Object> dataModel = new HashMap<>();


        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        String currentUser = workflowManager.getWorkflowUserManager().getCurrentUsername();

        FormData formData = new FormData();
        formData.setPrimaryKeyValue(currentUser);

        String mode = "update";
        try {
            String formUrl = addParamToUrl(getUrl(), "_mode", mode) + "&_action=submit" + "&id=" + URLEncoder.encode(currentUser, "UTF-8");

//			Form form = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), "form_update_profile", null, "Save", "Cancel", "window", formData, formUrl, getUrl());

			Form form = Utils.viewDataForm(formData, "Save", "Back", getUrl());

            FormService formService = (FormService) applicationContext.getBean("formService");
            String formHtml = formService.generateElementHtml(form, formData);
            setProperty("formHtml", formHtml);

            String formJson = formService.generateElementJson(form);
            this.setProperty("formJson", formJson);

            setProperty("view", "formView");
        } catch (UnsupportedEncodingException e1) {
            LogUtil.error(this.getClassName(), e1, e1.getMessage());
        }
        return "userview/plugin/form.jsp";
    }

    protected String addParamToUrl(String url, String name, String value) {
        return StringUtil.addParamsToUrl(url, name, value);
    }

    protected <U, V extends U> U ifEmptyThen(V value, U ifEmpty) {
        return value == null || value.toString().isEmpty() ? ifEmpty : value;
    }

}
