package com.kinnarastudio.kecakplugins.usermaintenance.datalist.binder;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.directory.dao.UserDao;
import org.joget.plugin.base.PluginManager;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load list of user
 */
public class UserDirectoryDataListBinder extends DataListBinderDefault {
    @Override
    public DataListColumn[] getColumns() {
        return new DataListColumn[]{
                new DataListColumn("id", "ID", true),
                new DataListColumn("username", "Username", true),
                new DataListColumn("firstName", "First Name", true),
                new DataListColumn("lastName", "Last Name", true),
                new DataListColumn("email", "Email", true),
                new DataListColumn("phoneNumber", "Telephone", true)
        };
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return "id";
    }

    @Override
    public DataListCollection<Map<String, Object>> getData(DataList dataList, Map map, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final UserDao userDao = (UserDao) applicationContext.getBean("userDao");
        final DataListFilterQueryObject criteria = getCriteria(map, filterQueryObjects);
        return Optional.ofNullable(userDao.findUsers(criteria.getQuery(), criteria.getValues(), sort, desc, start, rows))
                .stream()
                .flatMap(Collection::stream)
                .map(r -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", r.getId());
                    record.put("username", r.getUsername());
                    record.put("firstName", r.getFirstName());
                    record.put("lastName", r.getLastName());
                    record.put("email", r.getEmail());
                    record.put("timeZone", r.getTimeZone());
                    record.put("phoneNumber", r.getTelephoneNumber());
                    record.put("active", r.getActive());
                    return record;
                })
                .collect(Collectors.toCollection(DataListCollection::new));
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] filterQueryObjects) {
        final UserDao userDao = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        final DataListFilterQueryObject criteria = getCriteria(map, filterQueryObjects);
        return Math.toIntExact(userDao.countUsers(criteria.getQuery(), criteria.getValues()));
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
        return "User Directory DataList Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    protected String filter(DataListFilterQueryObject[] filters) {
        return Optional.ofNullable(filters)
                .stream()
                .flatMap(Arrays::stream)
                .map(DataListFilterQueryObject::getValues)
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("");

    }

    protected DataListFilterQueryObject getCriteria(Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        return Arrays.stream(filterQueryObjects)
                .collect(() -> new DataListFilterQueryObject() {{
                            setValues(new String[0]);
                            setQuery("where 1 = 1");
                            setOperator("");
                        }},
                        (collected, item) -> {
                            final String query = String.join(" ", collected.getQuery(), item.getOperator(), item.getQuery());
                            collected.setQuery(query);

                            final String[] values = Stream.concat(Arrays.stream(collected.getValues()), Arrays.stream(item.getValues()))
                                    .toArray(String[]::new);
                            collected.setValues(values);
                        },
                        (f1, f2) -> {
                            final String query = String.join(" ", f1.getQuery(), f2.getOperator(), f2.getQuery());
                            f1.setQuery(query);

                            final String[] values = Stream.concat(Arrays.stream(f1.getValues()), Arrays.stream(f2.getValues()))
                                    .toArray(String[]::new);
                            f1.setValues(values);
                        });
    }
}
