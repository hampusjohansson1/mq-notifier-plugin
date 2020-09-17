package com.axis.system.jenkins.plugins.axispoolmanager.resources;

import com.axis.system.jenkins.plugins.axispoolmanager.rest.ResponseFields;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckOutException;
import hudson.Extension;
import hudson.EnvVars;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a checkout of a specific product from a pool.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 * @see <a href="http://staginglab.staging.rnd.axis.com/pool/">staginglab.staging.rnd.axis.com</a>
 */
public final class ProductsFromPoolResource extends ResourceEntity {
    private final String poolName;
    private final String productName;

    private final String numberOfProducts;

    @DataBoundConstructor
    public ProductsFromPoolResource(String poolName, String productName, String numberOfProducts) {
        this.poolName = poolName;
        this.productName = productName;
        this.numberOfProducts = numberOfProducts;
    }

    public String getPoolName() {
        return poolName;
    }

    public String getProductName() {
        return productName;
    }

    public String getNumberOfProducts() {
        return numberOfProducts;
    }

    @Override
    public ResourceEntity getCopy() {
        return new ProductsFromPoolResource(poolName, productName, numberOfProducts);
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getSimpleName() + ": [Pool name: %s, Product name: %s, Number of products: %s, Correlation ID: %s]",
                getPoolName(), getProductName(), getNumberOfProducts(), getCorrelationID());
    }

    @Override
    public List<NameValuePair> getURICheckOutParameters(EnvVars envVars) throws CheckOutException {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair("product", envVars.expand(getProductName())));
        param.add(new BasicNameValuePair("pool", envVars.expand(getPoolName())));
        String number = envVars.expand(getNumberOfProducts());
        try {
            Integer.parseInt(number);
        } catch (NumberFormatException e) {
            throw new CheckOutException("Number products to checkout is not a number.");
        }
        param.add(new BasicNameValuePair("number", number));
        return param;
    }

    @Override
    public List<NameValuePair> getURICheckInParameters() {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair(ResponseFields.REFERENCE, getManagerMetaData().getString(ResponseFields.REFERENCE)));
        return param;
    }

    /**
     * Extends from a {@link ResourceEntityDescriptor}. Used for instancing the
     * ResourceEntities from config.jelly form posts.
     */
    @Extension
    public static final class ProductFromPoolResourceDescriptor extends ResourceEntityDescriptor {

        @Override
        public String getDisplayName() {
            return "Check Out Products from Pool";
        }
    }

}


