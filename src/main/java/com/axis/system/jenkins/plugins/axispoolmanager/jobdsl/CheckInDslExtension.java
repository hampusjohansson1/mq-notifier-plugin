package com.axis.system.jenkins.plugins.axispoolmanager.jobdsl;

import com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckInBuilder;
import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

/**
 * {@link CheckInBuilder} DSL bindings.
 */
@Extension(optional = true)
public final class CheckInDslExtension extends ContextExtensionPoint {

    /***
     * Entry point for the CheckIn DSL bindings.
     * <p>
     * Checks in all previously checked out resources.
     *
     * @return {@link CheckInBuilder}
     */
    @DslExtensionMethod(context = StepContext.class)
    public Object resourceCheckInAll() {
        return new CheckInBuilder(CheckInBuilder.CheckInType.ALL, null);
    }

    /***
     * Entry point for the CheckIn DSL bindings.
     * <p>
     * Checks in the specified resource group.
     *
     * @param resourceGroupId The ID of the resource group to check in
     * @return {@link CheckInBuilder}
     */
    @DslExtensionMethod(context = StepContext.class)
    public Object resourceCheckIn(int resourceGroupId) {
        return new CheckInBuilder(CheckInBuilder.CheckInType.SINGLE, resourceGroupId);
    }
}
