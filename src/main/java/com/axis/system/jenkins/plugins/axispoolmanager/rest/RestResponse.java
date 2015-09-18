package com.axis.system.jenkins.plugins.axispoolmanager.rest;

import net.sf.json.JSONObject;

/**
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class RestResponse {

    /**
     * The type of response we want to propagate to the builders.
     *
     * @see com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckInBuilder
     * @see com.axis.system.jenkins.plugins.axispoolmanager.builders.CheckOutBuilder
     */
    public enum ResultType {
        /**
         * Successful REST Operation.
         */
        SUCCESS,
        /**
         * Fatal error. Do not retry.
         */
        FATAL,
        /**
         * No products available at the moment.
         */
        RETRY
    }

    private final String response;
    private final String message;
    private final ResultType result;

    public RestResponse(String response, String message, ResultType result) {
        this.response = response;
        this.message = message;
        this.result = result;
    }

    public RestResponse(String response) {
        this(response, "", ResultType.SUCCESS);
    }

    public static RestResponse fromError(String message, ResultType result) {
        return new RestResponse("", message, result);
    }

    public static RestResponse fromError(String response, String message, ResultType result) {
        return new RestResponse(response, message, result);
    }

    public JSONObject getJSONData() {
        return JSONObject.fromObject(response);
    }

    public String getResponse() {
        return response;
    }

    public String getMessage() {
        return message;
    }

    public ResultType getResultType() {
        return result;
    }
}
