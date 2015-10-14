package com.axis.system.jenkins.plugins.axispoolmanager.jobdsl;

import com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckOutBuilder;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ExactProductResource;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ProductsFromPoolResource;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import hudson.Extension;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link CheckOutBuilder} DSL bindings.
 */
@Extension(optional = true)
public final class CheckOutDslExtension extends ContextExtensionPoint {

    /***
     * Entry point for the CheckOut DSL bindings.
     *
     * @return {@link CheckOutBuilder} populated with Resource Entities {@link ResourceEntity}
     */
    @DslExtensionMethod(context = StepContext.class)
    public Object resourceCheckOut(int resourceGroup, Runnable closure) {
        CheckOutDslContext context = new CheckOutDslContext();
        executeInContext(closure, context);
        return new CheckOutBuilder(resourceGroup, context.resourceEntities);
    }

    /**
     * Building Resource Entities to use by {@link CheckOutBuilder}.
     */
    final class CheckOutDslContext implements Context {
        List resourceEntities = new ArrayList<ResourceEntity>();
        /**
         * DSL Mapping for {@link ExactProductResource}.
         */
        public void exactProduct(String ipAddress, String macAddress) {
            resourceEntities.add(new ExactProductResource(ipAddress, macAddress));
        }

        /**
         * DSL Mapping for {@link ProductsFromPoolResource}.
         */
        public void productsFromPool(String poolName, String productName, int numberOfProducts) {
            resourceEntities.add(new ProductsFromPoolResource(poolName, productName, numberOfProducts));
        }

        /**
         * DSL Mapping for {@link ProductsFromPoolResource} that defaults to 1 resource check out.
         */
        public void productsFromPool(String poolName, String productName) {
            resourceEntities.add(new ProductsFromPoolResource(poolName, productName, 1));
        }
    }
}
