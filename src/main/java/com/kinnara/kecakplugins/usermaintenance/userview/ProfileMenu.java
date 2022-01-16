package com.kinnara.kecakplugins.usermaintenance.userview;

import com.kinnara.kecakplugins.usermaintenance.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.kecak.apps.userview.model.AceUserviewMenu;
import org.kecak.apps.userview.model.BootstrapUserviewTheme;
import org.springframework.context.ApplicationContext;


public class ProfileMenu extends UserviewMenu implements AceUserviewMenu {

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
    	AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appId = appDef.getId();
        String appVersion = appDef.getVersion().toString();
        Object[] arguments = new Object[]{appId, appVersion};
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/profileMenu.json", arguments, true, "/messages/profileMenu");
        return json;
    }

    @Override
    public String getRenderPage() {
        return null;
    }

    @Override
    public String getJspPage() {
    	return getJspPage("userview/plugin/form.jsp", "userview/plugin/unauthorized.jsp");
    }

    private String getJspPage(String jspFile, String unauthorizedJspFile) {
    	if ("submit".equals(getRequestParameterString("_action"))) {
            // only allow POST
            final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
                return unauthorizedJspFile;
            }

            // submit form
            try {
				submitForm();
			} catch (UnsupportedEncodingException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
			}
        } else {
            try {
				displayForm();
			} catch (UnsupportedEncodingException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
			}

        }

        return jspFile;
	}

	private void displayForm() throws UnsupportedEncodingException {
		final ApplicationContext applicationContext = AppUtil.getApplicationContext();

        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        String currentUser = workflowManager.getWorkflowUserManager().getCurrentUsername();

        FormData formData = new FormData();
        formData.setPrimaryKeyValue(currentUser);
		String mode = "edit";
		
        String formUrl = addParamToUrl(getUrl(), "_mode", mode) + "&_action=submit" + "&id=" + URLEncoder.encode(currentUser, "UTF-8");

		Form form = Utils.viewDataForm("userProfile", "Submit", "Cancel", formData, formUrl, getUrl(), mode);

		FormService formService = (FormService) applicationContext.getBean("formService");

		String formHtml = formService.generateElementHtml(form, formData);
		String formJson = formService.generateElementJson(form);
		
		this.setProperty("view", "formView");
		this.setProperty("appDef", AppUtil.getCurrentAppDefinition());
		this.setProperty("formHtml", formHtml);
		this.setProperty("formJson", formJson);
	}

	private void submitForm() throws UnsupportedEncodingException {
		ApplicationContext applicationContext = AppUtil.getApplicationContext();
		WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        String currentUser = workflowManager.getWorkflowUserManager().getCurrentUsername();
        
		FormData formData = new FormData();
		formData.setPrimaryKeyValue(currentUser);
		String mode = "edit";
		
        String formUrl = addParamToUrl(getUrl(), "_mode", mode) + "&_action=submit" + "&id=" + URLEncoder.encode(currentUser, "UTF-8");

		Form form = Utils.viewDataForm("userProfile", "Submit", "Cancel", formData, formUrl, getUrl(), mode);
		form = submitDataForm(formData, form);
		
		if(form != null) {
            // generate form HTML
            final FormService formService = (FormService) applicationContext.getBean("formService");
            final String formHtml = formService.generateElementHtml(form, formData);
            final String formJson = formService.generateElementJson(form);

            setProperty(REDIRECT_URL_PROPERTY, "");
            setProperty(REDIRECT_PARENT_PROPERTY, "false");
            setProperty("view", "formView");
            setProperty("stay", formData.getStay());
            setProperty("submitted", Boolean.TRUE);
            setProperty("formHtml", formHtml);
            setProperty("formJson", formJson);
            setProperty("redirectUrlAfterComplete", "");
        }
	}

	protected Form submitDataForm(FormData formData, Form form) {
		ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        formData = formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());
        formData = formService.executeFormActions(form, formData);

        setProperty("submitted", Boolean.TRUE);
        setProperty("redirectUrlAfterComplete", getPropertyString("redirectUrlAfterComplete"));

        return form;
	}

	protected String addParamToUrl(String url, String name, String value) {
        return StringUtil.addParamsToUrl(url, name, value);
    }

    protected <U, V extends U> U ifEmptyThen(V value, U ifEmpty) {
        return value == null || value.toString().isEmpty() ? ifEmpty : value;
    }

    @Override
    public String getAceJspPage(BootstrapUserviewTheme bootstrapUserviewTheme) {
        return getJspPage(bootstrapUserviewTheme.getFormJsp(), bootstrapUserviewTheme.getUnauthorizedJsp());
    }

    @Override
    public String getAceRenderPage() {
        return getRenderPage();
    }

    @Override
    public String getAceDecoratedMenu() {
        return getDecoratedMenu();
    }
}
