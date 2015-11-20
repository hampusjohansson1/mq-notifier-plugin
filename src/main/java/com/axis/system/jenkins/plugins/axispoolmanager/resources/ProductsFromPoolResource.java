package com.axis.system.jenkins.plugins.axispoolmanager.resources;

import com.axis.system.jenkins.plugins.axispoolmanager.rest.ResponseFields;
import hudson.Extension;
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

    // TODO: May need some changes in the management interface
    private final int numberOfProducts;

    @DataBoundConstructor
    public ProductsFromPoolResource(String poolName, String productName, int numberOfProducts) {
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

    public int getNumberOfProducts() {
        return numberOfProducts;
    }

    @Override
    public ResourceEntity getCopy() {
        return new ProductsFromPoolResource(poolName, productName, numberOfProducts);
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getSimpleName() + ": [Pool name: %s, Product name: %s, Number of products: %d]",
                getPoolName(), getProductName(), getNumberOfProducts());
    }

    @Override
    public List<NameValuePair> getURICheckOutParameters() {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair("product", getProductName()));
        param.add(new BasicNameValuePair("pool", getPoolName()));
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


