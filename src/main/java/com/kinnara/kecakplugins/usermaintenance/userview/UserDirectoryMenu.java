package com.kinnara.kecakplugins.usermaintenance.userview;

import com.kinnara.kecakplugins.usermaintenance.datalist.UserDirectoryDataListBinder;
import com.kinnara.kecakplugins.usermaintenance.utils.Utils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.HyperlinkDataListAction;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.kecak.apps.userview.model.AceUserviewMenu;
import org.kecak.apps.userview.model.BootstrapUserviewTheme;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        return null;
    }

    @Override
    public boolean isHomePageSupported() {
        return false;
    }

    @Override
    public String getDecoratedMenu() {
        final DataList dataList = getDataList();
        String menuItem = null;
        boolean showRowCount = "true".equalsIgnoreCase(getPropertyString("rowCount"));
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
        final String mode = isAddMode() ? "add" : "edit";

        // handle form
        if (isAddMode() || isEditMode()) {
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

        dataList.setRowActions(getDataListRowActions());
        dataList.setActions(getDataListActions());
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
        final List<DataListAction> dataListActionList = new ArrayList<>();
        dataListActionList.add(getAddRecordDataListAction());
        return dataListActionList.toArray(new DataListAction[0]);
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

    protected DataListAction getAddRecordDataListAction() {
        final PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListAction action = (DataListAction)pluginManager.getPlugin(HyperlinkDataListAction.class.getName());

        final String url = getUrl();
        action.setProperty("label", !getPropertyString("list-addLinkLabel").isEmpty() ? getPropertyString("list-addLinkLabel") : "Add");
        action.setProperty("href", addParamToUrl(url, "_mode", "edit"));
        action.setProperty("hrefParam", "");
        action.setProperty("hrefColumn", "");

        return action;
    }

    /**
     * Generate "Edit" row button
     *
     * @return
     */
    protected DataListAction getEditRowDataListAction() {
        final PluginManager pluginManager = (PluginManager)AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListAction action = (DataListAction)pluginManager.getPlugin(HyperlinkDataListAction.class.getName());

        final String url = getUrl();
        action.setProperty("label", !getPropertyString("list-editLinkLabel").isEmpty() ? getPropertyString("list-editLinkLabel") : "Edit");
        action.setProperty("href", addParamToUrl(url, "_mode", "edit"));
        action.setProperty("hrefParam", "id");
        action.setProperty("hrefColumn", "id");

        return action;
    }

    protected boolean isAddMode() {
        return "edit".equals(getRequestParameterString("_mode")) && getPrimaryKey() == null;
    }

    protected boolean isEditMode() {
        return "edit".equals(getRequestParameterString("_mode")) && getPrimaryKey() != null;
    }

    protected String getPrimaryKey() {
        return getRequestParameterString("id");
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
            final String method = Optional.ofNullable(request).map(HttpServletRequest::getMethod).orElse("");
            if (!"POST".equalsIgnoreCase(method)) {
                return unauthorizedFile;
            }

            submitForm();
        } else {
            displayForm();
        }
        return formFile;
    }

    protected void displayForm() {
        final String id = getPrimaryKey();
        final String mode = isAddMode() ? "add" : "edit";
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormService formService = (FormService) applicationContext.getBean("formService");

        FormData formData = new FormData();
        formData.setPrimaryKeyValue(id);
        formData = formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());

        final String url = getUrl();
        final String formUrl = addParamToUrl(addParamToUrl(addParamToUrl(url, "_action", "submit"), "_mode", mode), "id", id);
        final Form form = Utils.viewDataForm("displayUser-" + getPropertyString("id"), "Submit",  "Back", formData, formUrl, url, mode);

        String formHtml = formService.retrieveFormHtml(form, formData);
        String formJson = formService.generateElementJson(form);
        setProperty("view", "formView");
        setProperty("formHtml", formHtml);
        setProperty("formJson", formJson);
    }

    protected void submitForm() {
        final String id = getPrimaryKey();
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormService formService = (FormService) applicationContext.getBean("formService");

        final FormData formData = new FormData();
        formData.setPrimaryKeyValue(id);

        final Form form = submitDataForm(formData, Utils.viewDataForm("submitUser-" + getPropertyString("id"), "Submit",  "Back", formData, "", "", ""));
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

    protected Form submitDataForm(FormData formData, Form form) {
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        formService.retrieveFormDataFromRequestMap(formData, getRequestParameters());
        formService.executeFormActions(form, formData);
        setProperty("submitted", Boolean.TRUE);
        setProperty("redirectUrlAfterComplete", getUrl());
        return form;
    }

    protected String addParamToUrl(String url, String name, String value) {
        return StringUtil.addParamsToUrl(url, name, value);
    }
}
