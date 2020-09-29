package com.axis.system.jenkins.plugins.axispoolmanager.actions;

import com.axis.system.jenkins.plugins.axispoolmanager.AxisResourceManager;
import com.axis.system.jenkins.plugins.axispoolmanager.ResourceGroup;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;
import net.sf.json.JSONArray;

/**
 * Builds and adds an Environment Variable suitable for consumption of
 * python scripts:
 *
 * RESERVED_RESOURCES=[
    {"product": "Q1234", "ip_addr": "127.0.0.1", ...},
    {"product": "Q5678", "ip_addr": "127.0.0.1"},
    ...
 * ]
 *
 * @author Gustaf Lundh {@literal <gustaf.lundh@axis.com>} (C) Axis 2015
 */
public final class ResourceJsonEnvironmentAction implements EnvironmentContributingAction {
    /**
     * Key name for this parameter.
     */
    public static final String ENV_KEY = "RESERVED_RESOURCES";

    private transient Run run;

    public ResourceJsonEnvironmentAction(Run run) {
        this.run = run;
    }

    public String getEnvVal() {
        AxisResourceManager manager = AxisResourceManager.getInstance();
        JSONArray jsonArray = new JSONArray();
        for (ResourceGroup resourceGroup : manager.getCheckedOutResources()) {
            if (resourceGroup.getBuild().equals(run)) {
                for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
                    jsonArray.add(resourceEntity.getMetaData());
                }
            }
        }
        return jsonArray.toString();
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        env.putIfNotNull(ENV_KEY, getEnvVal());
    }

    @Override
    public String getIconFileName() {
     return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
