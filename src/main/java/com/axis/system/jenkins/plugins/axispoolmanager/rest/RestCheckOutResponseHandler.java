package com.axis.system.jenkins.plugins.axispoolmanager.rest;

import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Parses the check out RESTApi response.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class RestCheckOutResponseHandler implements ResponseHandler<RestResponse> {
    /**
     * Types of errors returned from the Management Pool Interface Rest Api.
     */
    public static class CheckOutErrors {
        private static final String NO_MATCH = "no_match";
        private static final String PRODUCT_FAILED = "product_failed";
        private static final String TRY_AGAIN = "try_again";
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
                JSONObject json = JSONObject.fromObject(responseMsg);

                String errorType = json.getString(ResponseFields.ERROR);
                String errorMessage = json.getString(ResponseFields.MESSAGE);

                RestResponse.ResultType resultType;
                if (errorType.equals(CheckOutErrors.NO_MATCH)) {
                    resultType = RestResponse.ResultType.FATAL;
                } else {
                    resultType = RestResponse.ResultType.RETRY;
                }
                return RestResponse.fromError(responseMsg, errorMessage, resultType);
            }
        }
        return RestResponse.fromError("Unexpected response from server", RestResponse.ResultType.FATAL);
    }
}
