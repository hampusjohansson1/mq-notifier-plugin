package com.axis.system.jenkins.plugins.axispoolmanager.rest;

/**
 * Important response data fields referenced from code.
 *
 * @author Gustaf Lundh {@literal <gustaf.lundh@axis.com>} (C) Axis 2015
 */
public final class ResponseFields {
    /**
     * Reference ID used for check ins.
     */
    public static final String REFERENCE = "reference";
    /**
     * Used for grouping unique DUTs in exported environment variables.
     * See {@link com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckOutBuilder}
     * See {@link com.axis.system.jenkins.plugins.axispoolmanager.actions.AxisPoolParameterAction}
     */
    public static final String IDENTIFIER = "id";
    /**
     * The property that contains the error type and message.
     */
    public static final String ERROR = "error";
    /**
     * Message returned from the pool management Rest Api.
     */
    public static final String MESSAGE = "message";
    /**
     * List of hosts (duts) returned from the pool management Rest Api.
     */
    public static final String HOSTS = "hosts";
    /**
     * The product name.
     */
    public static final String PRODUCT = "product";

    private ResponseFields() { }
}
