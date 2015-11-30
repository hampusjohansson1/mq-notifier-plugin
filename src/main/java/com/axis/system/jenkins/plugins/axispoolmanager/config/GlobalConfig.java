package com.axis.system.jenkins.plugins.axispoolmanager.config;

import net.sf.json.JSONObject;

/**
 * Main container of the global configuration settings.
 * Values modified in runtime through {@link "AxisPoolManagement/index.jelly"}
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class GlobalConfig {
    /**
     * The management server providing the RESTApi end points.
     */
    public static final String DEFAULT_RESTAPI_URI = "http://staginglab.staging.rnd.axis.com/pool/";
    /**
     * Default maximum timeout in hours.
     */
    public static final int DEFAULT_MAXIMUM_TIMEOUT = 12;
    /**
     * Default check out retries for non fatal check outs.
     */
    public static final int DEFAULT_CHECK_OUT_RETRIES = 10;

    /**
     * Default time between check outs. In milliseconds.
     */
    public static final int RETRY_MILLIS = 60 * 1000;

    private String restApiURI;
    private int maximumTimeout;
    private int maxCheckoutRetries;

    /**
     * Empty constructor.
     */
    public GlobalConfig() {

    }

    /**
     * Create a new GlobalConfig from form posted JSON data through.
     *
     * {@link "AxisPoolManagement/index.jelly"}
     *
     * @param jsonObject The form data
     * @return Parsed form data as GlobalConfig
     */
    public static GlobalConfig fromJSON(JSONObject jsonObject) {
        GlobalConfig config = new GlobalConfig();
        config.setRestApiURI(jsonObject.getString("restApiURI"));
        config.setMaximumTimeout(jsonObject.getInt("maximumTimeout"));
        config.setMaxCheckoutRetries(jsonObject.getInt("maxCheckOutRetries"));
        return config;
    }

    /**
     * Setup a default config. Used when no config can be deserialized from disk.
     *
     * @return GlobalConfig populated with default values
     */
    public static GlobalConfig getDefaultConfig() {
        GlobalConfig config = new GlobalConfig();
        config.setRestApiURI(DEFAULT_RESTAPI_URI);
        config.setMaximumTimeout(DEFAULT_MAXIMUM_TIMEOUT);
        config.setMaxCheckoutRetries(DEFAULT_CHECK_OUT_RETRIES);
        return config;
    }

    /**
     * @return The maximum number of check out retries.
     * @see {@link com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckOutBuilder}
     */
    public int getMaxCheckoutRetries() {
        return maxCheckoutRetries;
    }

    /**
     * Sets maximum number of checkout retries.
     *
     * @param maxCheckoutRetries
     */
    public void setMaxCheckoutRetries(int maxCheckoutRetries) {
        this.maxCheckoutRetries = maxCheckoutRetries;
    }

    /**
     * Returns the RESTApi used by {@link com.axis.system.jenkins.plugins.axispoolmanager.AxisResourceManager}.
     *
     * @return URI for REST Api end points
     */
    public String getRestApiURI() {
        return restApiURI;
    }

    /**
     * Sets RESTApi URI used by {@link com.axis.system.jenkins.plugins.axispoolmanager.AxisResourceManager}.
     *
     * @param restApiURI The Complete REST Api URI.
     */
    public void setRestApiURI(String restApiURI) {
        if (!restApiURI.endsWith("/")) {
            restApiURI += '/';
        }
        this.restApiURI = restApiURI;
    }

    /**
     * Retrieves Max time (hours) that the pool manager will hold the checked out resource.
     *
     * @return maximum timeout in hours
     */
    public int getMaximumTimeout() {
        return maximumTimeout;
    }

    /**
     * Sets Max time (hours) that the pool manager will hold the checked out resource.
     *
     * @param maximumTimeout Maximum timeout in hours
     */
    public void setMaximumTimeout(int maximumTimeout) {
        this.maximumTimeout = maximumTimeout;
    }
}
