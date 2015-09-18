package com.axis.system.jenkins.plugins.axispoolmanager.builders;

import com.axis.system.jenkins.plugins.axispoolmanager.AxisResourceManager;
import com.axis.system.jenkins.plugins.axispoolmanager.ResourceGroup;
import com.axis.system.jenkins.plugins.axispoolmanager.actions.AxisPoolParameterAction;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckInException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckOutException;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.ResponseFields;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.StringParameterValue;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
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

    private static long retryTimer = TimeUnit.MINUTES.toMillis(1);
    private final int resourceGroupId;
    private final List<ResourceEntity> resources;

    /**
     * DataBoundContructor matching fields in config.jelly.
     *
     * @param resourceGroupId The resource group id
     * @param resources       Resource entries from .jelly HetrogenList
     */
    @DataBoundConstructor
    public CheckOutBuilder(int resourceGroupId, List<ResourceEntity> resources) {
        this.resourceGroupId = resourceGroupId;
        this.resources = resources;
    }

    /**
     * Used for building / looking up all-or-nothing check out / check in groups.
     *
     * @return The resource group Id.
     */
    public Integer getResourceGroupId() {
        return resourceGroupId;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        AxisResourceManager axisResourceManager = AxisResourceManager.getInstance();
        if (axisResourceManager == null) {
            listener.fatalError("Could not fetch " + AxisResourceManager.class.getName() + " instance.");
            return false;
        }

        ResourceGroup resourceGroup = new ResourceGroup(build, getResourceGroupId(), getResourceEntities());
        int retries = axisResourceManager.getConfig().getMaxCheckoutRetries();
        try {
            listener.getLogger().println("Checking out " + resourceGroup.toString());
            String buildTag = build.getEnvironment(listener).get("BUILD_TAG", UNKNOWN_USER_REFERENCE);
            while (!axisResourceManager.checkOut(resourceGroup, buildTag) && retries-- > 0) {
                // Some entities may have been checked out. Let's try to check them in.
                // TODO: This can be removed when we have a real multi checkout transaction support in the DUT manager
                axisResourceManager.checkInGroup(resourceGroup);
                listener.getLogger().println("The specified resources are currently not available. Retrying in "
                        + TimeUnit.MILLISECONDS.toSeconds(retryTimer) + " seconds (" + retries + " retries left).");
                Thread.sleep(retryTimer);
            }
            if (retries <= 0) {
                listener.fatalError("Out of retries. Failed to check out all resources. Failing build.");
                return false;
            } else {
                // A successful check-out. Add DUT information to environment variables.
                ArrayList parameters = new ArrayList<StringParameterValue>();
                for (ResourceEntity resourceEntity : resources) {
                    String resourceId = resourceEntity.getManagerMetaData().getString(ResponseFields.IDENTIFIER);
                    for (Map.Entry<String, Object> entry
                            : (Set<Map.Entry<String, Object>>) resourceEntity.getManagerMetaData().entrySet()) {
                        String envKey = getEnvKeyFormat(resourceId, entry.getKey());
                        String envValue = entry.getValue().toString();
                        parameters.add(new StringParameterValue(envKey, envValue));
                    }
                }
                // We expose the data as environment variables through the use of a ParameterAction. This
                // also means the user gets easy access to the DUTs meta data for every build.
                // TODO: Do not expose environment variables after check in.
                build.addAction(new AxisPoolParameterAction("Axis DUT Data [" + resourceGroupId + "]",
                        parameters));
                listener.getLogger().println("Successfully checked out the complete resource group: "
                        + resourceGroup.toString());
            }
        } catch (URISyntaxException e) {
            listener.fatalError("Could not construct URI. Please check global configuration for Pool Manager RESTApi URI: "
                    + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            listener.fatalError(e.getMessage());
            return false;
        } catch (CheckOutException e) {
            listener.fatalError("Fatal error while checking out DUTs", e.getMessage());
            return false;
        } catch (CheckInException e) {
            listener.fatalError("Fatal error while checking in DUTs: ", e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private String getEnvKeyFormat(String id, String key) {
        return "DUT_" + id + "_" + key.toUpperCase();
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

