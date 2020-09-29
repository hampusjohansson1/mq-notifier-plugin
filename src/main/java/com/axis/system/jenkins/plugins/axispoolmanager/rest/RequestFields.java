package com.axis.system.jenkins.plugins.axispoolmanager.rest;

/**
 * Important request GET-parameters referenced from code.
 *
 * @author Gustaf Lundh {@literal <gustaf.lundh@axis.com>} (C) Axis 2015
 */
public final class RequestFields {
    /**
     * Host ID. Used when checking in all products leased by this jenkins master.
     */
    public static final String SERVER_HOST = "server_host";
    /**
     * User that owns the build. Currently mapped to the BUILD_TAG.
     * See {@link com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckOutBuilder}
     */
    public static final String USER_REFERENCE = "user_reference";
    /**
     * Build that owns the camera.
     */
    public static final String CONFIGURATION_NAME = "configuration_name";
    /**
     * Build ID.
     */
    public static final String CONFIGURATION_BUILD_NBR_NUMBER = "configuration_build_nbr_number";

    private RequestFields() { }
}
