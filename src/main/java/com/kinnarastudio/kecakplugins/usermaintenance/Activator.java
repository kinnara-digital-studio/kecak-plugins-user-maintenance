package com.kinnarastudio.kecakplugins.usermaintenance;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.usermaintenance.datalist.ResetUserPasswordDataListAction;
import com.kinnarastudio.kecakplugins.usermaintenance.datalist.UserDirectoryDataListBinder;
import com.kinnarastudio.kecakplugins.usermaintenance.form.UserDirectoryFormBinder;
import com.kinnarastudio.kecakplugins.usermaintenance.form.UserProfileFormBinder;
import com.kinnarastudio.kecakplugins.usermaintenance.process.GeneratePasswordTool;
import com.kinnarastudio.kecakplugins.usermaintenance.userview.ProfileMenu;
import com.kinnarastudio.kecakplugins.usermaintenance.userview.UserDirectoryMenu;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here

        // userview menu
        registrationList.add(context.registerService(UserDirectoryMenu.class.getName(), new UserDirectoryMenu(), null));
        registrationList.add(context.registerService(ProfileMenu.class.getName(), new ProfileMenu(), null));

        registrationList.add(context.registerService(GeneratePasswordTool.class.getName(), new GeneratePasswordTool(), null));

        // form binder
        registrationList.add(context.registerService(UserDirectoryFormBinder.class.getName(), new UserDirectoryFormBinder(), null));
        registrationList.add(context.registerService(UserProfileFormBinder.class.getName(), new UserProfileFormBinder(), null));

        // datalist binder
        registrationList.add(context.registerService(UserDirectoryDataListBinder.class.getName(), new UserDirectoryDataListBinder(), null));

        // datalist action
        registrationList.add(context.registerService(ResetUserPasswordDataListAction.class.getName(), new ResetUserPasswordDataListAction(), null));

        // deprecated plugins
        registrationList.add(context.registerService(ResetPasswordTool.class.getName(), new ResetPasswordTool(), null));
	}

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}