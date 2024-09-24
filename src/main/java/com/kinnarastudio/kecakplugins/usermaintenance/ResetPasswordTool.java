package com.kinnarastudio.kecakplugins.usermaintenance;

import com.kinnarastudio.kecakplugins.usermaintenance.process.GeneratePasswordTool;

/**
 * @author aristo
 *
 * Deprecated, use @{@link GeneratePasswordTool}
 */
@Deprecated
public class ResetPasswordTool extends GeneratePasswordTool {
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
