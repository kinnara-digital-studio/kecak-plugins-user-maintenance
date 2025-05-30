package com.kinnarastudio.kecakplugins.usermaintenance.userview.menu;

import com.kinnarastudio.kecakplugins.usermaintenance.datalist.action.ResetUserPasswordDataListAction;
import com.kinnarastudio.kecakplugins.usermaintenance.datalist.binder.UserDirectoryDataListBinder;
import com.kinnarastudio.kecakplugins.usermaintenance.utils.Utils;
import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONMapper;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.HyperlinkDataListAction;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author aristo
 */
public class UserDirectoryMenu extends UserviewMenu  {
    @Nullable
    private DataList dataList = null;

    public final static String DATALIST_ID = "userDirectory";
    public final static String DATALIST_NAME = "User Directory";
    public final static String DATALIST_DESCRIPTION = "Manage users";

    @Override
    public String getCategory() {
        return "Kecak";
    }

    @Override
    public String getIcon() {
        return "/plugin/org.joget.apps.userview.lib.RunProcess/images/grid_icon.gif";
    }

    @Override
    public String getRenderPage() {
        return null;
    }

    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDecoratedMenu() {
        final DataList dataList = getDataList();
        boolean showRowCount = "true".equalsIgnoreCase(getPropertyString("rowCount"));
        if (showRowCount) {
            int rowCount = dataList.getTotal();
            String label = getPropertyString("label");
            if (label != null) {
                label = StringUtil.stripHtmlRelaxed(label);
            }
            return "<a href=\"" + getUrl() + "\" class=\"menu-link default\"><span>" + label + "</span> <span class='rowCount'>(" + rowCount + ")</span></a>";
        }
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
        final DataListBinder binder = getDataListBinder();
        final JSONArray jsonColumns = Optional.of(binder)
                .map(DataListBinder::getColumns)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(Try.onFunction(c -> {
                    final JSONObject jsonColumn = new JSONObject();
                    jsonColumn.put(FormUtil.PROPERTY_VALUE, c.getName());
                    jsonColumn.put(FormUtil.PROPERTY_LABEL, c.getLabel());
                    return jsonColumn;
                }))
                .collect(JSONCollectors.toJSONArray());

        try {
            final JSONArray jsonPropertiesUserDirectoryMenu = new JSONArray(AppUtil.readPluginResource(getClassName(), "/properties/userview/menu/UserDirectoryMenu.json", new String[]{jsonColumns.toString()}, true, null));
            final JSONArray jsonPropertiesResetUserPasswordDataListAction = new JSONArray(AppUtil.readPluginResource(getClassName(), "/properties/datalist/action/ResetUserPasswordDataListAction.json", null, true, null));
            return JSONMapper.concat(jsonPropertiesUserDirectoryMenu,
                    jsonPropertiesResetUserPasswordDataListAction).toString();
        } catch (JSONException e) {
            return AppUtil.readPluginResource(getClassName(), "/properties/userview/menu/UserDirectoryMenu.json", new String[]{jsonColumns.toString()}, true, null);
        }
    }

    @Override
    public String getJspPage() {
        return getJspPage("userview/plugin/datalist.jsp", "userview/plugin/form.jsp", "userview/plugin/unauthorized.jsp");
    }

    protected String getJspPage(String dataListFile, String formFile, String unauthorizedFile) {
        // handle form
        if (isAddMode() || isEditMode()) {
            final String mode = isAddMode() ? "add" : "edit";
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
        return Optional.ofNullable(dataList)
                .orElseGet(() -> {
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
                    dataList.setSelectionType("single");
                    dataList.setCheckboxPosition("no"); // disable checkbox

                    if (getPropertyString(Userview.USERVIEW_KEY_NAME) != null && getPropertyString(Userview.USERVIEW_KEY_NAME).trim().length() > 0) {
                        dataList.addBinderProperty(Userview.USERVIEW_KEY_NAME, getPropertyString(Userview.USERVIEW_KEY_NAME));
                    }
                    if (getKey() != null && getKey().trim().length() > 0) {
                        dataList.addBinderProperty(Userview.USERVIEW_KEY_VALUE, getKey());
                    }

                    return this.dataList = dataList;
                });
    }

    protected DataListBinder getDataListBinder() {
        return new UserDirectoryDataListBinder();
    }

    protected DataListFilter[] getDataListFilters() {
        return Stream.of("username", "firstName", "lastName", "email")
                .map(s -> {
                    final DataListFilter filterByUsername = new DataListFilter();
                    filterByUsername.setName(s);
                    filterByUsername.setLabel(Utils.createLabel(s));
                    filterByUsername.setOperator("and");
                    return filterByUsername;
                })
                .toArray(DataListFilter[]::new);
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

        // edit button
        dataListActionList.add(getEditRowDataListAction());

        // reset password button
        if (getPropertyShowResetPasswordButton()) {
            dataListActionList.add(getResetUserPasswordDataListAction());
        }

        return dataListActionList.toArray(new DataListAction[0]);
    }

    protected DataListAction getAddRecordDataListAction() {
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListAction action = (DataListAction) pluginManager.getPlugin(HyperlinkDataListAction.class.getName());

        final String url = getUrl().replaceAll("\\?.+", "");
        action.setProperty("id", "USER_DIR_MENU_ADD_USER");
        action.setProperty("label", !getPropertyString("list-addLinkLabel").isEmpty() ? getPropertyString("list-addLinkLabel") : "Add");
        action.setProperty("href", addParamToUrl(url, "_mode", "add"));
        action.setProperty("hrefParam", "");
        action.setProperty("hrefColumn", "");
        action.setProperty("visible", "true");

        return action;
    }

    /**
     * Generate "Edit" row button
     *
     * @return
     */
    protected DataListAction getEditRowDataListAction() {
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListAction action = (DataListAction) pluginManager.getPlugin(HyperlinkDataListAction.class.getName());

        final String url = getUrl().replaceAll("\\?.+", "");
        action.setProperty("label", !getPropertyString("list-editLinkLabel").isEmpty() ? getPropertyString("list-editLinkLabel") : "Edit");
        action.setProperty("href", addParamToUrl(url, "_mode", "edit"));
        action.setProperty("hrefParam", "id");
        action.setProperty("hrefColumn", "id");

        return action;
    }

    protected DataListAction getResetUserPasswordDataListAction() {
        final PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        final DataListAction action = (DataListAction) pluginManager.getPlugin(ResetUserPasswordDataListAction.class.getName());

        action.setProperty("id", "USER_DIR_MENU_RESET_PASSWORD");
        action.setProperty("label", getPropertyResetPasswordActionLabel());
        action.setProperty("confirmation", getPropertyResetPasswordConfirmation());
        action.setProperty("processId", getProperty("processId"));
        action.setProperty("workflowVariables", getProperty("workflowVariables"));
        action.setProperty("passwordVariable", getProperty("passwordVariable"));

        return action;
    }

    protected boolean isAddMode() {
        return "add".equalsIgnoreCase(getRequestParameterString("_mode"));
    }

    protected boolean isEditMode() {
        return "edit".equals(getRequestParameterString("_mode")) && getPrimaryKey() != null;
    }

    protected boolean isResetMode() {
        return "reset".equalsIgnoreCase(getRequestParameterString("_mode"));
    }

    protected String getPrimaryKey() {
        return getRequestParameterString("id");
    }

    protected void viewList() {
        try {
            final DataList dataList = getDataList();

            // set current datalist
            setProperty("dataList", dataList);

            final Optional<DataListActionResult> optActionResult = Optional.of(dataList).map(DataList::getActionResult);
            optActionResult.map(DataListActionResult::getMessage).ifPresent(this::setAlertMessage);

            final Optional<String> optType = optActionResult.map(DataListActionResult::getType).filter("REDIRECT"::equalsIgnoreCase);
            final Optional<String> optUrl = optActionResult.map(DataListActionResult::getUrl).filter(s -> !s.isEmpty());

            if (optType.isPresent() && optUrl.isPresent()) {
                final String url = optUrl.get();
                if ("REFERER".equalsIgnoreCase(url)) {
                    final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                    if (request != null && request.getHeader("Referer") != null) {
                        setRedirectUrl(request.getHeader("Referer"));
                    } else {
                        setRedirectUrl("REFERER");
                    }
                } else {
                    setRedirectUrl(url);
                }
            }

        } catch (Exception ex) {
            final StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            String message = ex.toString();
            message = message + "\r\n<pre class=\"stacktrace\">" + out.getBuffer() + "</pre>";
            setProperty("error", message);
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
        final Form form = Utils.viewDataForm("displayUser-" + getPropertyString("id"), "Submit", "Back", formData, formUrl, url, mode);

        final String formHtml = formService.retrieveFormHtml(form, formData);
        final String formJson = formService.generateElementJson(form);

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

        final Form form = submitDataForm(formData, Utils.viewDataForm("submitUser-" + getPropertyString("id"), "Submit", "Back", formData, "", "", ""));
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
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    protected String getPropertyListCustomHeader() {
        return AppUtil.processHashVariable(getPropertyString("list-customHeader"), null, null, null);
    }

    protected String getPropertyListCustomFooter() {
        return AppUtil.processHashVariable(getPropertyString("list-customFooter"), null, null, null);
    }

    protected boolean getPropertyShowResetPasswordButton() {
        return "true".equalsIgnoreCase(getPropertyString("allowResetPasswordButton"));
    }

    protected String getPropertyResetPasswordActionLabel() {
        return getPropertyString("resetPasswordActionLabel");
    }

    protected String getPropertyResetPasswordConfirmation() {
        return getPropertyString("resetPasswordConfirmation");
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
