package com.kinnara.kecakplugins.usermaintenance.datalist;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.directory.dao.UserDao;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserDirectoryDataListBinder extends DataListBinderDefault {
    @Override
    public DataListColumn[] getColumns() {
        return new DataListColumn[] {
                new DataListColumn("id", "ID", true),
                new DataListColumn("username", "Username", true),
                new DataListColumn("firstName", "First Name", true),
                new DataListColumn("lastName", "Last Name", true),
                new DataListColumn("email", "Email", true),
                new DataListColumn("telephone_number", "Telephone", true)
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
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(r -> {
                    final Map<String, Object> record = new HashMap<>();
                    record.put("id", r.getId());
                    record.put("username", r.getUsername());
                    record.put("firstName", r.getFirstName());
                    record.put("lastName", r.getLastName());
                    record.put("email", r.getEmail());
                    record.put("timeZone", r.getTimeZone());
                    record.put("telephone_number", r.getTelephoneNumber());
                    record.put("active", r.getActive());
                    record.put("dateCreated", r.getDateCreated());
                    record.put("dateModified", r.getDateModified());
                    record.put("createdBy", r.getCreatedBy());
                    record.put("modifiedBy", r.getModifiedBy());
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
        return getClass().getPackage().getImplementationVersion();
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/UserDirectoryDataListBinder.json");
    }

    protected boolean isHideAdminRole() {
        return "true".equalsIgnoreCase(getPropertyString("hideAdminRole"));
    }

    protected String filter(DataListFilterQueryObject[] filters) {
        return Optional.ofNullable(filters)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(DataListFilterQueryObject::getValues)
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("");

    }

    protected DataListFilterQueryObject getCriteria(Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        return Arrays.stream(filterQueryObjects)
                .collect(() -> {
                            final DataListFilterQueryObject collected = new DataListFilterQueryObject();
                            collected.setValues(new String[0]);
                            collected.setQuery("where 1 = 1");
                            collected.setOperator("");
                            return collected;
                        },
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
