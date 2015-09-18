package com.axis.system.jenkins.plugins.axispoolmanager.actions;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

import java.util.List;

/**
 * We add environment variables by adding Parameter Actions to the build.
 * This action also provides a link to the build page to allow the user to see
 * all DUT check out meta data that was available during the build.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class AxisPoolParameterAction extends ParametersAction {
    private final String displayName;

    public AxisPoolParameterAction(String displayName, List<ParameterValue> parameters) {
        super(parameters);
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getUrlName() {
        return "axis-pool-parameters";
    }

}
