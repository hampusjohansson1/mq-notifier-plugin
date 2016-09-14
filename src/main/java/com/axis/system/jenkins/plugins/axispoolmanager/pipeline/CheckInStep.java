package com.axis.system.jenkins.plugins.axispoolmanager.pipeline;

import com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckInBuilder;
import com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckInBuilder.CheckInType;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import com.google.inject.Inject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Pipeline step implementation of the {@link CheckInBuilder}.
 *
 * @author Tomas Westling <tomas.westling@axis.com> (C) Axis 2016
 */
public class CheckInStep extends AbstractStepImpl {
    private final int resourceGroupId;
    private final String checkInType;

    /**
     * Databound Constructor matching the fields in config.groovy.
     * @param checkInType the type of checkin represented as a String.
     * @param resourceGroupId the GroupId for this resource.
     */
    @DataBoundConstructor
    public CheckInStep(String checkInType, int resourceGroupId) {
        this.checkInType = checkInType;
        this.resourceGroupId = resourceGroupId;
    }

    /**
     * Getter for the resource group id, needed for pipeline snippet generator.
     * @return the resource group Id
     */
    public final int getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Getter for the check in type, needed for pipeline snippet generator.
     * @return the check in type, as a String.
     */
    public final String getCheckInType() {
        return checkInType;
    }

    /**
     * Step descriptor for the CheckInStep, used to instantiate CheckInSteps from configuration.
     */
    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        /**
         * Standard constructor.
         */
        public DescriptorImpl() {
            super(CheckInStepExecution.class);
        }

        @Override
        public final String getFunctionName() {
            return "checkInResources";
        }

        @Override
        public final String getDisplayName() {
            return "Check in Axis resources to a pool";
        }
    }

    /**
     * Step Execution for the CheckInStep. Passes on the parameters to a CheckInBuilder
     * that does the actual work.
     */
    public static class CheckInStepExecution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject
        private transient CheckInStep step;

        @Override
        protected final Boolean run() throws Exception {
            CheckInType type;
            if ("single".equals(step.checkInType.toLowerCase())) {
                type = CheckInType.SINGLE;
            } else if ("all".equals(step.checkInType.toLowerCase())) {
                type = CheckInType.ALL;
            } else {
                throw new IllegalArgumentException("Wrong check in type, should be single or all, was: "
                        + step.checkInType);
            }
            CheckInBuilder builder = new CheckInBuilder(type, step.resourceGroupId);
            return builder.checkInResource(build, listener);
        }
    }
}
