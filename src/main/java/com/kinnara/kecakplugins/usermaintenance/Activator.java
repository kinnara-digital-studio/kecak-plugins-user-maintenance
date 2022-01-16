package com.kinnara.kecakplugins.usermaintenance;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnara.kecakplugins.usermaintenance.datalist.ResetUserPasswordDataListAction;
import com.kinnara.kecakplugins.usermaintenance.process.GeneratePasswordTool;
import com.kinnara.kecakplugins.usermaintenance.userview.ProfileMenu;
import com.kinnara.kecakplugins.usermaintenance.userview.UserDirectoryMenu;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(UserDirectoryMenu.class.getName(), new UserDirectoryMenu(), null));
        registrationList.add(context.registerService(ResetUserPasswordDataListAction.class.getName(), new ResetUserPasswordDataListAction(), null));
        registrationList.add(context.registerService(GeneratePasswordTool.class.getName(), new GeneratePasswordTool(), null));
        registrationList.add(context.registerService(ProfileMenu.class.getName(), new ProfileMenu(), null));

        // deprecated plugins
        registrationList.add(context.registerService(com.kinnara.kecakplugins.usermaintenance.ResetPasswordTool.class.getName(), new com.kinnara.kecakplugins.usermaintenance.ResetPasswordTool(), null));
	}

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}