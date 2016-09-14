package com.axis.system.jenkins.plugins.axispoolmanager.pipeline;

import com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckOutBuilder;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ExactProductResource;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ProductsFromPoolResource;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import com.google.inject.Inject;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Pipeline step implementation of the {@link CheckOutBuilder}.
 *
 * @author Tomas Westling <tomas.westling@axis.com> (C) Axis 2016
 */
public class CheckOutStep extends AbstractStepImpl {
    private final int resourceGroupId;
    //TODO the pipeline implementation only takes a single resourceEntity per step.
    //If we see the need to support multiple, like the CheckOutBuilder, this needs to be implemented.
    private final List<String> resource;
    private final int leaseTime;

    /**
     * Databound Constructor matching the fields in config.groovy.
     * @param resourceGroupId the GroupId for this resource.
     * @param leaseTime the leaseTime in hours.
     * @param resource the resource itself, represented as a list of Strings.
     */
    @DataBoundConstructor
    public CheckOutStep(int resourceGroupId, int leaseTime, List<String> resource) {
        this.resourceGroupId = resourceGroupId;
        this.resource = resource;
        this.leaseTime = leaseTime;
    }

    /**
     * Getter for the resource group id, needed for pipeline snippet generator.
     * @return the resource group id
     */
    public final int getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Getter for resource, needed for pipeline snippet generator.
     * @return the resource, as a list of strings
     */
    public final List<String> getResource() {
        return resource;
    }

    /**
     * Getter for the lease time, needed for pipeline snippet generator.
     * @return the lease time
     */
    public final int getLeaseTime() {
        return leaseTime;
    }

    /**
     * Step descriptor for the CheckOutStep, used to instantiate CheckOutSteps from configuration.
     */
    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        /**
         * Standard constructor.
         */
        public DescriptorImpl() {
            super(CheckOutStepExecution.class);
        }

        @Override
        public final String getFunctionName() {
            return "checkOutResources";
        }

        @Override
        public final String getDisplayName() {
            return "Check out Axis resources from a pool";
        }

        @Override
        public final Step newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            int resourceGroupId = formData.getInt("resourceGroupId");
            int leaseTime = formData.getInt("leaseTime");
            String resourcesString = formData.getString("resource");
            List<String> resources = new ArrayList<String>();
            for (String line : resourcesString.split("\r?\n")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    resources.add(line);
                }
            }
            return new CheckOutStep(resourceGroupId, leaseTime, resources);
        }
    }

    /**
     * Step Execution for the CheckOutStep. Passes on the parameters to a CheckOutBuilder
     * that does the actual work.
     */
    public static class CheckOutStepExecution extends AbstractSynchronousNonBlockingStepExecution<String> {

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject
        private transient CheckOutStep step;

        @Override
        protected final String run() throws Exception {
            List<ResourceEntity> resourceEntitylist = new LinkedList<ResourceEntity>();
            if (step.resource.size() == 1) {
                    resourceEntitylist.add(new ExactProductResource(step.resource.get(0)));
                } else if (step.resource.size() == 3) {
                    resourceEntitylist.add(new ProductsFromPoolResource(step.resource.get(0),
                            step.resource.get(1), step.resource.get(2)));
                } else {
                    throw new IllegalArgumentException("Illegal number of parameters for the resource, should be"
                            + " 1 or 3, but was " + step.resource.size());
            }
            CheckOutBuilder builder = new CheckOutBuilder(step.resourceGroupId, resourceEntitylist, step.leaseTime);
            return builder.checkOutResource(build, listener);
        }
    }
}
