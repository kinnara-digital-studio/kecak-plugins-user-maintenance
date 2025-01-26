package com.kinnarastudio.kecakplugins.usermaintenance.datalist.action;

import com.kinnarastudio.kecakplugins.usermaintenance.utils.PasswordUtilMixin;
import com.kinnarastudio.kecakplugins.usermaintenance.utils.StartProcessUtils;
import com.kinnarastudio.commons.Try;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResetUserPasswordDataListAction extends DataListActionDefault implements PasswordUtilMixin, StartProcessUtils {
    @Override
    public String getLinkLabel() {
        return getPropertyString("label");
    }

    @Override
    public String getHref() {
        return getPropertyString("href");
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    @Override
    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    @Override
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Please Confirm";
        }
        return confirm;
    }

    @Override
    public DataListActionResult executeAction(DataList dataList, String[] keys) {
        // only allow POST
        if (!isPostMethod()) {
            return null;
        }

        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");

        final DataListCollection<Map<String, String>> rows = dataList.getRows(Integer.MAX_VALUE, 0);
        rows.sort(Comparator.comparing(m -> m.get(dataList.getBinder().getPrimaryKeyColumnName())));

        Optional.ofNullable(keys)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(Objects::nonNull)
                .distinct()
                .map(userDao::getUser)
                .filter(Objects::nonNull)
                .forEach(Try.onConsumer(u -> {
                    final String password = generateRandomPassword(6, true, true, true, false);
                    u.setPassword(password);
                    u.setConfirmPassword(password);

                    LogUtil.info(getClassName(), "Updating password for user [" + u.getId() + "] password [" + u.getPassword() + "]");

                    final User updatedPassword = generatePassword(u);
                    userDao.updateUser(updatedPassword);

                    final Map<String, String> row = getRow(dataList, rows, u.getId());
                    final String processId = getPropertyString("processId");

                    if(executeToolAfterAction()) {
                        PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");
                        Map<String, Object> pluginProperties = (Map<String, Object>)getProperty("postActionTool");
                        if(pluginProperties != null) {
                            pluginManager.execute((String) pluginProperties.get("className"), (Map) pluginProperties.get("properties"));
                        }
                    }

                    if(startProcessAfterAction()) {
                        final Map<String, String> workflowVariables = getWorkflowVariables(row);
                        workflowVariables.put(getPropertyString("passwordVariable"), password);
                        startProcess(processId, workflowVariables);
                    }
                }));

        final DataListActionResult result = new DataListActionResult();
        result.setUrl(getRedirectUrl());
        result.setType(DataListActionResult.TYPE_REDIRECT);
        return result;
    }

    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "Reset Password";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/datalist/action/ResetUserPasswordDataListAction.json");
    }

    protected boolean isPostMethod() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();

        return Optional.ofNullable(request)
                .map(HttpServletRequest::getMethod)
                .map("POST"::equalsIgnoreCase)
                .orElse(false);
    }

    protected Map<String, String> getWorkflowVariables(Map<String, String> row) {
        return Optional.of("workflowVariables")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, Object>) o)
                .collect(Collectors.toMap(m -> AppUtil.processHashVariable(m.get("name").toString(), null, null, null), m -> {
                    String field = String.valueOf(m.get("field"));
                    String value = String.valueOf(m.get("value"));

                    if (field.isEmpty()) {
                        return AppUtil.processHashVariable(value, null, null, null);
                    } else {
                        return row.get(field);
                    }
                }));
    }

    @Nonnull
    protected Map<String, String> getRow(DataList dataList, DataListCollection rows, String key) {
        final String keyField = dataList.getBinder().getPrimaryKeyColumnName();
        return Optional.ofNullable(rows)
                .map(DataListCollection<Map<String, String>>::stream)
                .orElseGet(Stream::empty)
                .filter(m -> key.equals(m.get(keyField)))
                .findFirst()
                .orElseGet(HashMap::new);
    }

    protected String getRedirectUrl() {
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        return Optional.ofNullable(request)
                .map(r -> r.getHeader("Referer"))

                // delete action parameter
                .map(s -> s.replaceAll("d-[0-9]+-ac=\\w+&?", ""))

                .orElse("REFERER");
    }

    protected boolean executeToolAfterAction() {
        return "true".equalsIgnoreCase(getPropertyString("executeToolAfterAction"));
    }

    protected boolean startProcessAfterAction() {
        return "true".equalsIgnoreCase(getPropertyString("startProcessAfterAction"));
    }
}
