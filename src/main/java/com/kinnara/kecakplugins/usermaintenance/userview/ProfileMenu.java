package com.kinnara.kecakplugins.usermaintenance.userview;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.kinnara.kecakplugins.usermaintenance.form.UserDirectoryFormBinder;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.DefaultFormBinder;
import org.joget.apps.form.lib.TextField;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.kecak.apps.form.service.FormDataUtil;
import org.springframework.context.ApplicationContext;

public class ProfileMenu extends UserviewMenu{
	
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
        final Map<String, Object> dataModel = new HashMap<>();

        
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        String currentUser = workflowManager.getWorkflowUserManager().getCurrentUsername();
        
        Form form = new Form();
        form.setProperty("id", "form_update_profile");
        form.setLoadBinder(new UserDirectoryFormBinder());
        form.setStoreBinder(new UserDirectoryFormBinder());
        
        final FormData formData = new FormData();
        formData.setPrimaryKeyValue(currentUser);
        
        final Collection<String> defaultFields = Arrays.asList(
        		FormUtil.PROPERTY_ORG_ID,
                FormUtil.PROPERTY_DATE_CREATED, 
                FormUtil.PROPERTY_DATE_MODIFIED,
                FormUtil.PROPERTY_CREATED_BY, 
                FormUtil.PROPERTY_MODIFIED_BY,
                FormUtil.PROPERTY_DELETED,
                "firstName","lastName","email","telephoneNumber","password","confirmPassword","oldPassword");
        
        final Collection<Element> children = defaultFields.stream()
        		.map(s -> {
        			 final TextField textField = new TextField();
                     textField.setProperty(FormUtil.PROPERTY_ID, s);
                     textField.setProperty(FormUtil.PROPERTY_LABEL, s);
                     return textField;
        		})
        		.collect(Collectors.toList());
        
        FormDataUtil.elementStream(form, formData)
        .filter(e -> "section-actions".equals(e.getPropertyString("id")))
        .findFirst()
        .ifPresent(children::add);

        form.setChildren(children);
        
        final FormService formService = (FormService) applicationContext.getBean("formService");
        
        Form profile = formService.loadFormData(form, formData);
        
        final String mode = getRequestParameterString("_mode");
        
        LogUtil.info(getClassName(), "[CUR USER] "+profile.getPropertyString("firstName"));

        String formHtml = formService.generateElementHtml(form, formData);
        setProperty("formHtml", formHtml);
        
        String formJson = formService.generateElementJson(form);
        this.setProperty("formJson", formJson);
        
        setProperty("view", "formView");
        
        return "userview/plugin/form.jsp";
    }
	
	private void submitForm() {
		
	}

	protected <U, V extends U> U ifEmptyThen(V value, U ifEmpty) {
        return value == null || value.toString().isEmpty() ? ifEmpty : value;
    }

}
