package com.axis.system.jenkins.plugins.axispoolmanager.builders;

import com.axis.system.jenkins.plugins.axispoolmanager.AxisResourceManager;
import com.axis.system.jenkins.plugins.axispoolmanager.ResourceGroup;
import com.axis.system.jenkins.plugins.axispoolmanager.actions.AxisPoolParameterAction;
import com.axis.system.jenkins.plugins.axispoolmanager.actions.ResourceJsonEnvironmentAction;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckInException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckOutException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.TransientErrorException;
import com.axis.system.jenkins.plugins.axispoolmanager.mq.MqHelper;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.ResponseFields;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Build step for checking out DUTs from a lab. The ResourceEntities
 * in a group should only be checked out in as all-or-nothing.
 * <p/>
 * If a DUT in a group cannot be checked out, all other DUTs should
 * be immediately returned. This negotiation should be moved as a
 * transaction in the Pool Manager.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class CheckOutBuilder extends Builder {
    private static final String UNKNOWN_USER_REFERENCE = "jenkins-unknown";
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckOutBuilder.class);

    private final int resourceGroupId;
    private final List<ResourceEntity> resources;
    private final int leaseTime;

    /**
     * DataBoundContructor matching fields in config.jelly.
     *
     * @param resourceGroupId The resource group id
     * @param resources       Resource entries from .jelly HetrogenList
     */
    @DataBoundConstructor
    public CheckOutBuilder(int resourceGroupId, List<ResourceEntity> resources, int leaseTime) {
        this.resourceGroupId = resourceGroupId;
        this.resources = resources;
        this.leaseTime = leaseTime;
    }

    public CheckOutBuilder(int resourceGroupId, List<ResourceEntity> resources) {
        this.resourceGroupId = resourceGroupId;
        this.resources = resources;
        this.leaseTime = AxisResourceManager.getInstance().getConfig().getMaximumTimeout();
    }

    /**
     * Entities to check out. Can never be null.
     *
     * @return resourceEntities
     */
    public List<ResourceEntity> getResourceEntities() {
        if (resources == null) {
            return new LinkedList<ResourceEntity>();
        }
        return resources;
    }

    /**
     * Used for building / looking up all-or-nothing check out / check in groups.
     *
     * @return The resource group Id.
     */
    public Integer getResourceGroupId() {
        return resourceGroupId;
    }

    public Integer getLeaseTime() {
        return leaseTime;
    }

    /**
     * Does the actual checking out of resources. This method is callable from
     * {@link com.axis.system.jenkins.plugins.axispoolmanager.pipeline.CheckOutStep}
     * @param build The build that wants to check out the resource.
     * @param listener The task listener used to log progress and errors.
     * @return A string describing the checked out resource if the check out was successful, null if not.
     */
    public String checkOutResource(Run build, TaskListener listener) {
        AxisResourceManager axisResourceManager = AxisResourceManager.getInstance();
        if (axisResourceManager == null) {
            listener.fatalError("Could not fetch " + AxisResourceManager.class.getName() + " instance.");
            return null;
        }

        ResourceGroup resourceGroup = new ResourceGroup(build, getResourceGroupId(), getCopyOfResourceEntities());
        int retries = axisResourceManager.getConfig().getMaxCheckoutRetries();
        long startTime = System.currentTimeMillis();
        while (retries-- > 0) {
            try {
                // Some entities may have been checked out. Let's try to check them in.
                // TODO: This can be removed when we have a real multi checkout transaction support in the DUT manager
                axisResourceManager.checkInGroup(resourceGroup);

                listener.getLogger().println("Checking out " + resourceGroup.toString());
                String buildTag = build.getEnvironment(listener).get("BUILD_TAG", UNKNOWN_USER_REFERENCE);

                EnvVars envVars = build.getEnvironment(listener);
                if (axisResourceManager.checkOut(resourceGroup, buildTag, getLeaseTime(), envVars)) {

                    if (MqHelper.isMqNotifierInstalled()) {
                        MqHelper.publishSuccessfulCheckOut(resourceGroup, System.currentTimeMillis() - startTime);
                    }
                    List<ParameterValue> resourceParameters = createResourceParameters(resourceGroup);
                    // We expose the data as environment variables through the use of a ParameterAction. This
                    // also means the user gets easy access to the DUTs meta data for every build.
                    build.addAction(new AxisPoolParameterAction("Axis DUT Data [" + resourceGroupId + "]",
                            resourceParameters, build, resourceGroupId));
                    // JSONified action struct. We only need one which will be rebuilt when the environment is built.
                    ResourceJsonEnvironmentAction resourceAction = new ResourceJsonEnvironmentAction(build);
                    build.addOrReplaceAction(resourceAction);
                    listener.getLogger().println("Successfully checked out the complete resource group: "
                            + resourceGroup.toString());
                    return resourceAction.getEnvVal();
                }

            } catch (URISyntaxException e) {
                listener.fatalError("Could not construct URI. Please check global configuration for Pool Manager RESTApi URI: "
                        + e.getMessage());
                return null;
            } catch (InterruptedException e) {
                listener.fatalError(e.getMessage());
                return null;
            } catch (TransientErrorException e) {
                // Log the reason for the unsuccessful checkout and try again.
                listener.getLogger().println(e.getMessage());
            } catch (CheckOutException e) {
                // Log the reason for the unsuccessful checkout and quit
                listener.error(e.getMessage());
                return null;
            } catch (CheckInException e) {
                listener.fatalError(e.getMessage());
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            int retryTimer = axisResourceManager.getConfig().RETRY_MILLIS;
            listener.getLogger().println("Retrying in " + TimeUnit.MILLISECONDS.toSeconds(retryTimer)
                    + " seconds (" + retries + " retries left).");
            try {
                Thread.sleep(retryTimer);
            } catch (InterruptedException e) {
                listener.fatalError(e.getMessage());
                return null;
            }
        }
        listener.fatalError("Out of retries. Failed to check out all resources. Failing build.");
        return null;
    }

    private List<ParameterValue> createResourceParameters(ResourceGroup resourceGroup){
        int rollingId = 1;
        // A successful check-out. Add DUT information to environment variables.
        ArrayList parameters = new ArrayList<StringParameterValue>();
        for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
            String resourceId = resourceEntity.getManagerMetaData().getString(ResponseFields.REFERENCE);
            JSONArray hosts = resourceEntity.getManagerMetaData().getJSONArray(ResponseFields.HOSTS);
            for (Object o : hosts) {
                JSONObject host = (JSONObject) o;
                for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) host.entrySet()) {
                    String envKey  = "DUT_" + rollingId + "_" + entry.getKey().toUpperCase();
                    String envValue = entry.getValue().toString();
                    parameters.add(new StringParameterValue(envKey, envValue));
                }
                rollingId++;
            }
        }
        return parameters;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        return checkOutResource(build, listener) != null;
    }

    /**
     * Clone entities before check out. Can never be null.
     *
     * @return resourceEntities
     */
    private List<ResourceEntity> getCopyOfResourceEntities() {
        if (resources == null) {
            return new ArrayList<ResourceEntity>();
        }
        List<ResourceEntity> deepClone = new ArrayList(resources.size());
        for (ResourceEntity resourceEntity : resources) {
            deepClone.add(resourceEntity.getCopy());
        }
        return deepClone;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Generic BuildStepDescriptor for the {@link CheckOutBuilder}. Used for instancing the
     * build steps from configuration.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // We support everything!
            return true;
        }

        public String getDisplayName() {
            return "Check Out Resources";
        }

        public Integer getDefaultLeaseTime() {
            return AxisResourceManager.getInstance().getConfig().getMaximumTimeout();
        }

        @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public List<ResourceEntity.ResourceEntityDescriptor> getResourceDescriptors() {
            ExtensionList<ResourceEntity.ResourceEntityDescriptor> extensionList =
                    Jenkins.getInstance().getExtensionList(ResourceEntity.ResourceEntityDescriptor.class);
            return extensionList;
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // Overload not needed, but great for Jelly-debugging.
            return super.newInstance(req, formData);
        }
    }
}

