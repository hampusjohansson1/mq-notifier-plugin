package com.axis.system.jenkins.plugins.axispoolmanager.rest;

import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Parses the check in RESTApi response.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class RestCheckInResponseHandler implements ResponseHandler<RestResponse> {
    /**
     * Types of errors returned from the Management Pool Interface Rest Api.
     */
    public static class CheckInErrors {
        private static final String CHECK_IN_FAILED = "checkin_failed";
    }

    @Override
    public RestResponse handleResponse(
            final HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return new RestResponse(EntityUtils.toString(entity));
            } else {
                return RestResponse.fromError("No data returned from server", RestResponse.ResultType.FATAL);
            }
        } else if (status == HttpStatus.SC_NOT_FOUND) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String responseMsg = EntityUtils.toString(entity);
                String errorType = JSONObject.fromObject(responseMsg).getString(ResponseFields.ERROR);
                if (errorType.equals(CheckInErrors.CHECK_IN_FAILED)) {
                    return RestResponse.fromError(responseMsg, "There is no products matching the query",
                            RestResponse.ResultType.FATAL);
                }
            }
        }
        return RestResponse.fromError("Unexpected response from server", RestResponse.ResultType.FATAL);
    }
}
