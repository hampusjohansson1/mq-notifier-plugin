package com.axis.system.jenkins.plugins.axispoolmanager.actions;

import hudson.EnvVars;
import hudson.model.*;

import java.util.List;

/**
 * We add environment variables by adding Parameter Actions to the build.
 * This action also provides a link to the build page to allow the user to see
 * all DUT check out meta data that was available during the build.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class AxisPoolParameterAction implements EnvironmentContributingAction {
    /**
     * The base url for this parameter action.
     */
    public static final String URL_PREFIX = "axis-pool-parameters";

    private final String displayName;
    private boolean exportEnvVars;
    private int resourceGroupId;
    private final String url;

    private final List<ParameterValue> parameters;

    public AxisPoolParameterAction(String displayName, List<ParameterValue> parameters, Run run, int resourceGroupId) {
        this.displayName = displayName;
        this.parameters = parameters;
        this.exportEnvVars = true;
        this.resourceGroupId = resourceGroupId;
        this.url = getUniqueUrl(run);
    }

    private String getUniqueUrl(Run run) {
        return URL_PREFIX + "-" + run.getActions(AxisPoolParameterAction.class).size();
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        if (exportEnvVars) {
            for (ParameterValue p : parameters) {
                if (p != null) {
                    p.buildEnvironment(build, env);
                }
            }
        }
    }

    /**
     * Allow .jelly to visualize data.
     * @return parameters
     */
    public List<ParameterValue> getParameters() {
        return parameters;
    }

    /**
     * Should this parameter action contribute environment variables?
     *
     * @param enabled
     */
    public void setExportEnvVars(boolean enabled) {
        exportEnvVars = enabled;
    }

    /**
     * Should this parameter action contribute environment varliables?
     *
     * @return Resource Group Id
     */
    public int getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Helper method to disable all environment variables for build.
     *
     * @param r the build
     */
    public static void disableEnvVars(Run r) {
        for (AxisPoolParameterAction action : r.getActions(AxisPoolParameterAction.class)) {
            action.setExportEnvVars(false);
        }
    }

    /**
     * Helper method to disable all environment variables with specified.
     * resource group id for build.
     *
     * @param r the build
     */
    public static void disableEnvVars(Run r, int resourceGroupId) {
        for (AxisPoolParameterAction action : r.getActions(AxisPoolParameterAction.class)) {
            if (action.getResourceGroupId() == resourceGroupId) {
                action.setExportEnvVars(false);
            }
        }
    }


    @Override
    public String getIconFileName() {
     return "/plugin/axis-pool-manager/images/icon.png";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getUrlName() {
        return url;
    }

}
