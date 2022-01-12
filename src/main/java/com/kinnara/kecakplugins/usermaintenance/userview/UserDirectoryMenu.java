package com.kinnara.kecakplugins.usermaintenance.userview;

import com.kinnara.kecakplugins.usermaintenance.datalist.UserDirectoryDataListBinder;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
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
        return getLabel() + getVersion();
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
        return getPropertyString("label");
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
            setProperty("customHeader", getPropertyListCustomHeader());
            setProperty("customFooter", getPropertyString("list-customFooter"));
            viewList();
            return listFile;
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

    }

    protected DataListAction[] getDataListActions() {

    }

    protected DataListAction[] getDataListRowActions() {

    }

    protected DataListColumn getDataListColumn(String columnId, String columnName) {

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
            this.displayForm();
        }
        return formFile;
    }

    protected void submitForm() {
        String id = getRequestParameterString("id");
        ApplicationContext ac = AppUtil.getApplicationContext();
        FormService formService = (FormService) ac.getBean("formService");
        Form form;
        FormData formData = new FormData();
        form = this.retrieveDataForm(formData, id);
        if ((form = this.submitDataForm(formData, form)) != null) {
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
}
