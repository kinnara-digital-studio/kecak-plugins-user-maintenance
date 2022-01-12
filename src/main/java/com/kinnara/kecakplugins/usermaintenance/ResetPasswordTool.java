package com.kinnara.kecakplugins.usermaintenance;

/**
 * @author aristo
 *
 * Deprecated, use @{@link com.kinnara.kecakplugins.usermaintenance.process.ResetPasswordTool}
 */
@Deprecated
public class ResetPasswordTool extends com.kinnara.kecakplugins.usermaintenance.process.ResetPasswordTool {
    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getName() {
        return "(Deprecated) - " + super.getName();
    }

    @Override
    public String getLabel() {
        return "(Deprecated) - " + super.getLabel();
    }
}
