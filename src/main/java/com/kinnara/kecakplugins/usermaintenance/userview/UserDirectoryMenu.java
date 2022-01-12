package com.kinnara.kecakplugins.usermaintenance.userview;

import com.kinnara.kecakplugins.usermaintenance.datalist.UserDirectoryDataListBinder;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.lib.LinkButton;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormAction;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author aristo
 */
public class UserDirectoryMenu extends UserviewMenu {
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
//            setProperty("customHeader", getPropertyListCustomHeader());
            setProperty("customFooter", getPropertyString("list-customFooter"));
            viewList();
            return null;
//            return listFile;
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
        dataList.setColumns(getDataListColumns());
        dataList.setBinder(getDataListBinder());
        dataList.setActions(getDataListActions());
        dataList.setRowActions(getDataListRowActions());
        dataList.setFilters(getDataListFilters());

        return dataList;
    }

    protected DataListColumn[] getDataListColumns() {
        return new DataListColumn[] {
                getDataListColumn("id", "Username"),
                getDataListColumn("firstName", "First Name"),
                getDataListColumn("lastName", "Last Name"),
                getDataListColumn("email", "Email")
        };
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

    protected DataListAction[] getDataListRowActions() {
        // TODO
        return new DataListAction[0];
    }

    protected DataListColumn getDataListColumn(String columnId, String columnName) {
        return new DataListColumn(columnId, columnName, true);
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

            final DataListActionResult ac = dataList.getActionResult();
            if (ac != null) {
                if (ac.getMessage() != null && !ac.getMessage().isEmpty()) {
                    this.setAlertMessage(ac.getMessage());
                }
                if (ac.getType() != null && "REDIRECT".equals(ac.getType()) && ac.getUrl() != null && !ac.getUrl().isEmpty()) {
                    if ("REFERER".equals(ac.getUrl())) {
                        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                        if (request != null && request.getHeader("Referer") != null) {
                            this.setRedirectUrl(request.getHeader("Referer"));
                        } else {
                            this.setRedirectUrl("REFERER");
                        }
                    } else {
                        this.setRedirectUrl(ac.getUrl());
                    }
                }
            }
            this.setProperty("dataList", dataList);
        } catch (Exception ex) {
            StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            String message = ex.toString();
            message = message + "\r\n<pre class=\"stacktrace\">" + out.getBuffer() + "</pre>";
            this.setProperty("error", message);
        }
    }

    protected String handleForm(String formFile, String unauthorizedFile) {
        if ("submit".equals(getRequestParameterString("_action"))) {
            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
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
        String id = getRequestParameterString("id");
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        Form form = null;
        FormData formData = new FormData();
        form = this.retrieveDataForm(formData, id);
        if (form != null) {
            String formHtml = formService.retrieveFormHtml(form, formData);
            String formJson = formService.generateElementJson(form);
            this.setProperty("view", "formView");
            this.setProperty("formHtml", formHtml);
            this.setProperty("formJson", formJson);
        } else {
            this.setProperty("headerTitle", "Form Unavailable");
            this.setProperty("view", "assignmentFormUnavailable");
            this.setProperty("formHtml", "Form Unavailable");
        }
    }

    protected void submitForm() {
        String id = getRequestParameterString("id");
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        Form form;
        FormData formData = new FormData();
        form = retrieveDataForm(formData, id);
        if ((form = submitDataForm(formData, form)) != null) {
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

    protected Form retrieveDataForm(FormData formData, String primaryKeyValue) {
        Form form = null;
        ApplicationContext ac = AppUtil.getApplicationContext();
        AppService appService = (AppService) ac.getBean("appService");
        FormService formService = (FormService) ac.getBean("formService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formId = "";
        String mode = getRequestParameterString("_mode");
        if (isAddMode(mode)) {
            formId = getPropertyString("addFormId");
        } else if (isEditMode(mode)) {
            formId = getPropertyString("editFormId");
        }
        formData.setPrimaryKeyValue(primaryKeyValue);
        formData = formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());
        if (getPropertyString("keyName") != null && getPropertyString("keyName").trim().length() > 0 && getKey() != null) {
            formData.addRequestParameterValues("fk_" + getPropertyString("keyName"), new String[]{getKey()});
        }
        String formUrl = addParamToUrl(getUrl(), "_mode", mode) + "&_action=submit";
        if (primaryKeyValue != null) {
            try {
                formUrl = formUrl + "&id=" + URLEncoder.encode(primaryKeyValue, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                // empty catch block
            }
        }
        String submitLabel = "Save";
        String cancelLabel = null;
        if (isAddMode(mode)) {
            if (getPropertyString("add-saveButtonLabel") != null && getPropertyString("add-saveButtonLabel").trim().length() > 0) {
                submitLabel = getPropertyString("add-saveButtonLabel");
            }
            cancelLabel = "Cancel";
            if (getPropertyString("add-cancelButtonLabel") != null && getPropertyString("add-cancelButtonLabel").trim().length() > 0) {
                cancelLabel = getPropertyString("add-cancelButtonLabel");
            }
        } else if (isEditMode(mode)) {
            if (getPropertyString("edit-saveButtonLabel") != null && getPropertyString("edit-saveButtonLabel").trim().length() > 0) {
                submitLabel = getPropertyString("edit-saveButtonLabel");
            }
            cancelLabel = "Back";
            if (getPropertyString("edit-backButtonLabel") != null && getPropertyString("edit-backButtonLabel").trim().length() > 0) {
                cancelLabel = getPropertyString("edit-backButtonLabel");
            }
        }

        form = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), formId, null, submitLabel, cancelLabel, "window", formData, formUrl, getUrl());

        if (form != null) {
            String idValue;
            Element el = FormUtil.findElement("id", form, formData);
            if (el != null && (idValue = FormUtil.getElementPropertyValue(el, formData)) != null && !idValue.trim().isEmpty() && !"".equals(formData.getRequestParameter("_FORM_META_ORIGINAL_ID"))) {
                el.setProperty("readonly", "true");
            }

            Object[] moreActions = (Object[]) getProperty("edit-moreActions");
            if (isEditMode(mode) && moreActions != null && moreActions.length > 0) {
                ArrayList<FormAction> formActionList = new ArrayList<>();
                for (Object o : moreActions) {
                    HashMap action = (HashMap) o;
                    LinkButton button = new LinkButton();
                    button.setProperty("id", ("more_action_" + action.get("label").toString().replace(" ", "_")));
                    button.setProperty("label", action.get("label").toString());
                    String url = getRedirectUrl(action.get("href").toString(), form, formData, action.get("hrefParam").toString(), action.get("hrefColumn").toString());
                    button.setProperty("url", url);
                    button.setProperty("confirmation", action.get("confirmation").toString());
                    formActionList.add(button);
                }
                FormAction[] formActions = formActionList.toArray(new FormAction[0]);
//                form = decorateFormMoreActions(form, formActions);
            }
            if (getPropertyString("keyName") != null && getPropertyString("keyName").trim().length() > 0 && getKey() != null && (el = FormUtil.findElement((String) getPropertyString("keyName"), (Element) form, (FormData) formData)) != null) {
                FormUtil.setReadOnlyProperty(el);
            }
        }
        if (isEditMode(mode)) {
            Boolean readonly = "yes".equalsIgnoreCase(getPropertyString("edit-readonly"));
            Boolean readonlyLabel = "true".equalsIgnoreCase(getPropertyString("edit-readonlyLabel"));
            if (readonly || readonlyLabel) {
                FormUtil.setReadOnlyProperty(form, readonly, readonlyLabel);
            }
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
