package com.axis.system.jenkins.plugins.axispoolmanager.builders;

import com.axis.system.jenkins.plugins.axispoolmanager.AxisResourceManager;
import com.axis.system.jenkins.plugins.axispoolmanager.ResourceGroup;
import com.axis.system.jenkins.plugins.axispoolmanager.actions.AxisPoolParameterAction;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckInException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.TransientErrorException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

import java.net.URISyntaxException;

/**
 * Build step for checking in a previously dispatched camera.
 *
 * @author Gustaf Lundh {@literal <gustaf.lundh@axis.com>} (C) Axis 2015
 */
public final class CheckInBuilder extends Builder {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckInBuilder.class);

    /**
     * Indicates whether we should check in all checked out resources for the build or just one resource group?
     */
    public enum CheckInType {
        /**
         * Check in a single resource group.
         */
        SINGLE,
        /**
         * Check in all previously checked out resources by this build.
         */
        ALL
    }

    private CheckInType checkInType;
    private int resourceGroupId;

    /**
     * DataBoundContructor matching fields in config.jelly.
     *
     * @param value           Type of check in.
     * @param resourceGroupId resource group Id. Integer instead of int, since it may be null
     *                        depending on CheckInType bounded {@literal <f:radioBlock>} in .jelly
     */
    @DataBoundConstructor
    public CheckInBuilder(CheckInType value, Integer resourceGroupId) {
        this.checkInType = value;
        this.resourceGroupId = resourceGroupId != null ? resourceGroupId : 1;
    }

    /**
     * Does the actual checking in of resources. Moved to a separate method in order to be
     * callable from a {@link com.axis.system.jenkins.plugins.axispoolmanager.pipeline.CheckInStep}
     * @param build The build that wants to check in the resource.
     * @param listener The task listener used to log progress and errors.
     * @return true if the check in was successful, false if not.
     */
    public boolean checkInResource(Run build, TaskListener listener) {
        AxisResourceManager axisResourceManager = AxisResourceManager.getInstance();
        int retries = axisResourceManager.getConfig().getMaxCheckoutRetries();
        while (retries-- > 0) {
            try {
                switch (getCheckInType()) {
                    case ALL:
                        listener.getLogger().println("Checking in all resources...");
                        axisResourceManager.checkInAll(build);
                        AxisPoolParameterAction.disableEnvVars(build);
                        listener.getLogger().println("Successfully checked in all resources.");
                        break;
                    case SINGLE:
                        listener.getLogger().println("Checking in ResourceGroup: " + getResourceGroupId());
                        ResourceGroup resourceGroup = axisResourceManager.checkInGroup(build, getResourceGroupId());
                        AxisPoolParameterAction.disableEnvVars(build, getResourceGroupId());
                        listener.getLogger().println("Successfully checked in the complete resource group: "
                                + resourceGroup.toString());
                        break;
                    default:
                        LOGGER.warn("Unknown checkInType: " + getCheckInType());
                }
                return true;
            } catch (URISyntaxException e) {
                listener.fatalError("Could not construct URI. Please check global configuration for AxisPoolManager: "
                        + e.getMessage());
                return false;
            } catch (CheckInException e) {
                listener.error(e.getMessage());
                return false;
            } catch (TransientErrorException e) {
                listener.getLogger().println(e.getMessage());
            }

            int retryTimer = axisResourceManager.getConfig().RETRY_MILLIS;
            listener.getLogger().println("Retrying in " + TimeUnit.MILLISECONDS.toSeconds(retryTimer)
                    + " seconds (" + retries + " retries left).");
            try {
                Thread.sleep(retryTimer);
            } catch (InterruptedException e) {
                listener.fatalError(e.getMessage());
                return false;
            }
        }
        listener.fatalError("Out of retries. Failed to check in resources. Failing build.");
        return false;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        return checkInResource(build, listener);
    }

    /**
     * Used for building / looking up all-or-nothing check out / check in groups.
     *
     * @return The resource group ID.
     */
    public Integer getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * @return type of check in
     */
    public CheckInType getCheckInType() {
        return checkInType;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Generic BuildStepDescriptor for the {@link CheckInBuilder}. Used for instancing the
     * build steps from configuration.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Support all build types
            return true;
        }

        public String getDisplayName() {
            return "Check In Resource";
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // Unwrap the radioBox
            return super.newInstance(req, formData.getJSONObject("checkInType"));
        }
    }
}
