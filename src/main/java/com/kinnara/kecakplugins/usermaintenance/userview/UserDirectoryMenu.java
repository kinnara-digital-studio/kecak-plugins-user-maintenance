package com.kinnara.kecakplugins.usermaintenance.userview;

import com.kinnara.kecakplugins.usermaintenance.datalist.UserDirectoryDataListBinder;
import com.kinnara.kecakplugins.usermaintenance.form.UserDirectoryFormBinder;
import com.kinnara.kecakplugins.usermaintenance.utils.Utils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.HyperlinkDataListAction;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.lib.LinkButton;
import org.joget.apps.form.lib.SubmitButton;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.kecak.apps.userview.model.AceUserviewMenu;
import org.kecak.apps.userview.model.BootstrapUserviewTheme;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aristo
 */
public class UserDirectoryMenu extends UserviewMenu implements AceUserviewMenu {
    public final static String DATALIST_ID = "userDirectory";
    public final static String DATALIST_NAME = "User Directory";
    public final static String DATALIST_DESCRIPTION = "Manage users";

    @Override
    public String getCategory() {
        return "Kecak";
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public String getRenderPage() {
        final DataList dataList = getDataList();
        String menuItem = null;
        boolean showRowCount = Boolean.valueOf(getPropertyString("rowCount"));
        if (showRowCount) {
            int rowCount = dataList.getTotal();
            String label = getPropertyString("label");
            if (label != null) {
                label = StringUtil.stripHtmlRelaxed(label);
            }
            menuItem = "<a href=\"" + getUrl() + "\" class=\"menu-link default\"><span>" + label + "</span> <span class='rowCount'>(" + rowCount + ")</span></a>";
        }
        return menuItem;
    }

    @Override
    public boolean isHomePageSupported() {
        return false;
    }

    @Override
    public String getDecoratedMenu() {
        return null;
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
        return "User Directory";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/UserDirectoryMenu.json");
    }

    @Override
    public String getJspPage() {
        return getJspPage("userview/plugin/datalist.jsp", "userview/plugin/form.jsp", "userview/plugin/unauthorized.jsp");
    }

    @Override
    public String getAceJspPage(BootstrapUserviewTheme bootstrapUserviewTheme) {
        return getJspPage(bootstrapUserviewTheme.getDataListJsp(), bootstrapUserviewTheme.getFormJsp(), bootstrapUserviewTheme.getUnauthorizedJsp());
    }

    @Override
    public String getAceRenderPage() {
        return getRenderPage();
    }

    @Override
    public String getAceDecoratedMenu() {
        return getDecoratedMenu();
    }

    protected String getJspPage(String dataListFile, String formFile, String unauthorizedFile) {
        final String mode = getRequestParameterString("_mode");

        // handle form
        if (isAddMode(mode) || isEditMode(mode)) {
            setProperty("customHeader", getPropertyString(mode + "-customHeader"));
            setProperty("customFooter", getPropertyString(mode + "-customFooter"));
            setProperty("messageShowAfterComplete", getPropertyString(mode + "-messageShowAfterComplete"));
            return handleForm(formFile, unauthorizedFile);
        }

        // handle list
        else {
            setProperty("customHeader", getPropertyListCustomHeader());
            setProperty("customFooter", getPropertyListCustomFooter());
            viewList();
            return dataListFile;
        }
    }

    @Nonnull
    protected DataList getDataList() {
        final DataList dataList = new DataList();

        dataList.setId(DATALIST_ID);
        dataList.setName(DATALIST_NAME);
        dataList.setDescription(DATALIST_DESCRIPTION);
        dataList.setDefaultPageSize(getPropertyPageSize());
        dataList.setShowPageSizeSelector(true);
        dataList.setDefaultOrder(getPropertyOrder());
        dataList.setDefaultSortColumn(getPropertyOrderBy());

        {
            final DataListBinder binder = getDataListBinder();
            dataList.setBinder(binder);
            dataList.setColumns(binder.getColumns());
        }

        dataList.setActions(getDataListActions());
        dataList.setRowActions(getDataListRowActions());
        dataList.setFilters(getDataListFilters());

        return dataList;
    }

    protected DataListBinder getDataListBinder() {
        final UserDirectoryDataListBinder dataListBinder = new UserDirectoryDataListBinder();
        return dataListBinder;
    }

    protected DataListFilter[] getDataListFilters() {
        // TODO
        return new DataListFilter[0];
    }

    protected DataListAction[] getDataListActions() {
        // TODO
        return new DataListAction[0];
    }

    /**
     * Get DataList row actions
     *
     * @return
     */
    protected DataListAction[] getDataListRowActions() {
        final List<DataListAction> dataListActionList = new ArrayList<>();
        dataListActionList.add(getEditRowDataListAction());
        return dataListActionList.toArray(new DataListAction[0]);
    }

    /**
     * Generate "Edit" row button
     *
     * @return
     */
    protected DataListAction getEditRowDataListAction() {
        final PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListAction action = (DataListAction)pluginManager.getPlugin(HyperlinkDataListAction.class.getName());

        action.setProperty("label", getPropertyString("list-editLinkLabel") != null && getPropertyString("list-editLinkLabel").trim().length() > 0 ? getPropertyString("list-editLinkLabel") : "Edit");
        action.setProperty("href", addParamToUrl(getUrl(), "_mode", "edit"));
        action.setProperty("hrefParam", "id");
        action.setProperty("hrefColumn", "id");

        return action;
    }

    protected boolean isAddMode(String mode) {
        return "add".equals(mode);
    }

    protected boolean isEditMode(String mode) {
        return "edit".equals(mode);
    }

    protected void viewList() {
        try {
            final DataList dataList = getDataList();
//            dataList.setActionPosition(getPropertyString("buttonPosition"));
//            dataList.setSelectionType(getPropertyString("selectionType"));
//            dataList.setCheckboxPosition(getPropertyString("checkboxPosition"));

            // set current datalist
            setProperty("dataList", dataList);

            final DataListActionResult actionResult = dataList.getActionResult();
            if (actionResult == null) {
                return;
            }

            if (actionResult.getMessage() != null && !actionResult.getMessage().isEmpty()) {
                this.setAlertMessage(actionResult.getMessage());
            }

            if (actionResult.getType() != null && "REDIRECT".equals(actionResult.getType()) && actionResult.getUrl() != null && !actionResult.getUrl().isEmpty()) {
                if ("REFERER".equals(actionResult.getUrl())) {
                    HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                    if (request != null && request.getHeader("Referer") != null) {
                        this.setRedirectUrl(request.getHeader("Referer"));
                    } else {
                        this.setRedirectUrl("REFERER");
                    }
                } else {
                    this.setRedirectUrl(actionResult.getUrl());
                }
            }

        } catch (Exception ex) {
            final StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            String message = ex.toString();
            message = message + "\r\n<pre class=\"stacktrace\">" + out.getBuffer() + "</pre>";
            this.setProperty("error", message);
        }
    }

    protected String handleForm(String formFile, String unauthorizedFile) {
        if ("submit".equals(getRequestParameterString("_action"))) {
            final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
                return unauthorizedFile;
            }
            submitForm();
        } else {
            displayForm();
        }
        return formFile;
    }

    protected void displayForm() {
        final String id = getRequestParameterString("id");
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormService formService = (FormService) applicationContext.getBean("formService");

        final FormData formData = new FormData();
        formData.setPrimaryKeyValue(id);

        final Form form = Utils.viewDataForm(formData, "Save", "Back", getUrl());
        String formHtml = formService.retrieveFormHtml(form, formData);
        String formJson = formService.generateElementJson(form);
        this.setProperty("view", "formView");
        this.setProperty("formHtml", formHtml);
        this.setProperty("formJson", formJson);
    }

    protected void submitForm() {
        String id = getRequestParameterString("id");
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");

        final FormData formData = new FormData();
        formData.setPrimaryKeyValue(id);

        final Form form = submitDataForm(formData, retrieveDataForm(formData));
        if (form != null) {
            String formHtml;
            Map errors = formData.getFormErrors();
            int errorCount = 0;
            if (!formData.getStay() && (errors == null || errors.isEmpty())) {
                String mode = getRequestParameterString("_mode");
                String redirectUrl = getPropertyString("redirectUrlAfterComplete");
                this.setRedirectUrl(redirectUrl);
                this.setAlertMessage(getPropertyString(mode + "-messageShowAfterComplete"));
                formHtml = formService.generateElementHtml(form, formData);
            } else {
                formHtml = formService.generateElementErrorHtml(form, formData);
                errorCount = errors.size();
            }
            if (formData.getStay()) {
                this.setAlertMessage("");
                this.setRedirectUrl("");
            }

            String formJson = formService.generateElementJson(form);
            setProperty("view", "formView");
            setProperty("stay", formData.getStay());
            setProperty("submitted", Boolean.TRUE);
            setProperty("errorCount", errorCount);
            setProperty("formHtml", formHtml);
            setProperty("formJson", formJson);
        } else {
            setProperty("headerTitle", "Form Unavailable");
            setProperty("view", "assignmentFormUnavailable");
            setProperty("formHtml", "Form Unavailable");
        }
    }


    /**
     * Get property "orderBy"
     *
     * @return
     */
    protected String getPropertyOrderBy() {
        return getPropertyString("orderBy");
    }

    /**
     * Get property "order"
     *
     * @return
     */
    protected String getPropertyOrder() {
        return getPropertyString("order");
    }

    /**
     * Get property "pageSize"
     *
     * @return
     */
    protected int getPropertyPageSize() {
        try {
            return Integer.parseInt(getPropertyString("pageSize"));
        }catch (NumberFormatException e) {
            return 10;
        }
    }

    protected String getPropertyListCustomHeader() {
        return AppUtil.processHashVariable(getPropertyString("list-customHeader"), null, null, null);
    }

    protected String getPropertyListCustomFooter() {
        return AppUtil.processHashVariable(getPropertyString("list-customFooter"), null, null, null);
    }

    protected Form retrieveDataForm(final FormData formData) {
        final ApplicationContext ac = AppUtil.getApplicationContext();
        final AppService appService = (AppService) ac.getBean("appService");
        final FormService formService = (FormService) ac.getBean("formService");
        final AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        final PluginManager pluginManager = (PluginManager) ac.getBean("pluginManager");

        final String mode = getRequestParameterString("_mode");

        formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());

        if (getPropertyString("keyName") != null && getPropertyString("keyName").trim().length() > 0 && getKey() != null) {
            formData.addRequestParameterValues("fk_" + getPropertyString("keyName"), new String[]{getKey()});
        }

        String formUrl = addParamToUrl(getUrl(), "_mode", mode) + "&_action=submit";

        final String primaryKeyValue = formData.getPrimaryKeyValue();
        if (primaryKeyValue != null) {
            try {
                formUrl += ("&id=" + URLEncoder.encode(primaryKeyValue, "UTF-8"));
            } catch (UnsupportedEncodingException ignored) {
                formUrl += ("&id=" + primaryKeyValue);
            }
        }

        final String submitLabel = "Save";
        final String cancelLabel = isAddMode(mode) ? "Cancel" : "Back";

//        final Form form = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), formId, null, submitLabel, cancelLabel, "window", formData, formUrl, getUrl());
        final Form form = new Form();

        final UserDirectoryFormBinder formBinder = new UserDirectoryFormBinder();
        form.setLoadBinder(formBinder);
        form.setStoreBinder(formBinder);

        final Element submitButton = (Element) pluginManager.getPlugin(SubmitButton.class.getName());
        submitButton.setProperty(FormUtil.PROPERTY_ID, "submit");
        submitButton.setProperty(FormUtil.PROPERTY_LABEL,"Save");
        form.addAction((FormAction) submitButton, formData);

        String idValue;
        Element el = FormUtil.findElement("id", form, formData);
        if (el != null && (idValue = FormUtil.getElementPropertyValue(el, formData)) != null && !idValue.trim().isEmpty() && !"".equals(formData.getRequestParameter("_FORM_META_ORIGINAL_ID"))) {
            FormUtil.setReadOnlyProperty(el);
        }
        return form;
    }

    protected Form submitDataForm(FormData formData, Form form) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        formData = formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());
        formData = formService.executeFormActions(form, formData);
        this.setProperty("submitted", Boolean.TRUE);
        String redirectUrl = "";
        String mode = getRequestParameterString("_mode");
        String redirectType = getPropertyString(mode + "-afterSaved");
        if ("add".equals(redirectType)) {
            redirectUrl = addParamToUrl(getUrl(), "_mode", "add");
        } else if ("list".equals(redirectType)) {
            redirectUrl = getUrl();
        } else if ("edit".equals(redirectType)) {
            redirectUrl = getRedirectUrl(addParamToUrl(getUrl(), "_mode", "edit"), form, formData, "id", "id");
        } else if ("redirect".equals(redirectType)) {
            redirectUrl = getRedirectUrl(getPropertyString(mode + "-afterSavedRedirectUrl"), form, formData, getPropertyString(mode + "-afterSavedRedirectParamName"), getPropertyString(mode + "-afterSavedRedirectParamvalue"));
        } else if ("continueNext".equals(redirectType)) {
            redirectUrl = !getPropertyString("nextRecordUrl").isEmpty() ? getPropertyString("nextRecordUrl") : getUrl();
        }
        this.setProperty("redirectUrlAfterComplete", redirectUrl);
        return form;
    }

    protected String addParamToUrl(String url, String name, String value) {
        return StringUtil.addParamsToUrl(url, name, value);
    }

    protected String getRedirectUrl(String url, Form form, FormData formData, String paramName, String fieldName) {
        if (url != null && url.trim().length() > 0 && fieldName != null && fieldName.trim().length() > 0) {
            String value;
            Element element = FormUtil.findElement(fieldName, form, formData);
            String string = value = element != null ? FormUtil.getElementPropertyValue(element, formData) : "";
            if (fieldName.equalsIgnoreCase("id") && value.isEmpty()) {
                value = formData.getPrimaryKeyValue();
            }
            if (paramName != null && paramName.trim().length() > 0) {
                url = url.contains("?") ? url + "&" : url + "?";
                url = url + paramName + "=";
            } else if (!url.endsWith("/")) {
                url = url + "/";
            }
            try {
                url = url + (value != null ? URLEncoder.encode(value, "UTF-8") : null);
            } catch (UnsupportedEncodingException ex) {
                // empty catch block
            }
        }
        return url;
    }


}
