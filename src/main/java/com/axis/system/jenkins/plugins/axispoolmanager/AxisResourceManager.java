package com.axis.system.jenkins.plugins.axispoolmanager;

import com.axis.system.jenkins.plugins.axispoolmanager.config.GlobalConfig;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckInException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckOutException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.TransientErrorException;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.RequestFields;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.RestCheckInAllResponseHandler;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.RestCheckOutResponseHandler;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.RestResponse;
import hudson.EnvVars;
import hudson.Plugin;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Plugin main entry point.
 * <p/>
 * TODO: Setup thread for building auto completion data on pool/product names.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class AxisResourceManager extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxisResourceManager.class);
    private static final int HTTP_TIMEOUT = 30;
    private transient List<ResourceGroup> checkedOutResources = new CopyOnWriteArrayList<ResourceGroup>();
    private GlobalConfig config;

    /**
     * Returns the instance of this class.
     * If {@link jenkins.model.Jenkins#getInstance()} isn't available
     * or the plugin class isn't registered null will be returned.
     *
     * @return the instance.
     */
    public static AxisResourceManager getInstance() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            return jenkins.getPlugin(AxisResourceManager.class);
        } else {
            LOGGER.error("Error, Jenkins could not be found, so no plugin!");
            return null;
        }
    }

    private RequestConfig getRequestConfig() {
        return RequestConfig.custom().setConnectTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_TIMEOUT)).build();
    }

    /**
     * Checks out all resources in a resource group.
     *
     * @param resourceGroup Resource Group
     * @param userReference The user owning the resource
     * @param envVars hudson.EnvVars
     * @return true if successful
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean checkOut(ResourceGroup resourceGroup, String userReference, int leaseTime, EnvVars envVars)
            throws URISyntaxException, IOException, InterruptedException {
        // TODO: We should probably bookkeep the RESTApi end points in a separate file.
        URIBuilder uriBuilder = new URIBuilder(getConfig().getRestApiURI() + "checkout_product");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
            uriBuilder.setParameters(resourceEntity.getURICheckOutParameters(envVars));
            uriBuilder.addParameter("max_termination_time", Integer.toString(leaseTime));
            try {
                uriBuilder.addParameters(getBasicURICheckoutParameters(resourceGroup.getBuild(), userReference));
            } catch (UnknownHostException e) {
                throw new CheckOutException("Could not figure out host name", e);
            }
            HttpGet req = new HttpGet(uriBuilder.build());
            req.addHeader("accept", "application/json");
            req.setConfig(getRequestConfig());
            RestResponse response;
            try {
                response = httpClient.execute(req, new RestCheckOutResponseHandler());
            } catch (IOException e) {
                e.printStackTrace();
                throw new TransientErrorException("Could not contact pool manager", e);
            }
            switch (response.getResultType()) {
                case SUCCESS:
                    resourceEntity.setManagerMetaData(response.getJSONData());
                    resourceEntity.setCheckedOut(true);
                    LOGGER.info("Successfully checked out resource: " + resourceEntity.toString());
                    break;
                case FATAL:
                    throw new CheckOutException("Could not check out resource: " + response.getMessage());
                case RETRY:
                    // Do not log to LOGGER here. It would spam jenkins log as this is quite common to end up here.
                    throw new TransientErrorException("Could not check out resource: " + response.getMessage());
                default:
                    LOGGER.error("Unexpected state during checkout: " + response.getResultType());
                    throw new CheckOutException("Unexpected state during check out!");
            }
        }
        checkedOutResources.add(resourceGroup);
        return true;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Axis Pool Manager plug-in");
        super.start();
        super.load();
        try {
            checkInAll();
        } catch (URISyntaxException e) {
            LOGGER.warn("Could not build URI for checking in all products", e);
        } catch (CheckInException e) {
            LOGGER.warn("Could not check in all products", e);
        } catch (UnknownHostException e) {
            LOGGER.warn("Could not fetch host name when checking in all products", e);
        } catch (TransientErrorException e) {
            LOGGER.warn("Transient error when checking in all products", e);
        }
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Axis Pool Manager plug-in");
        super.stop();
    }

    public List<NameValuePair> getBasicURICheckoutParameters(Run build, String userReference)
            throws IOException, InterruptedException {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair(RequestFields.SERVER_HOST, java.net.InetAddress.getLocalHost().getHostName()));
        param.add(new BasicNameValuePair(RequestFields.USER_REFERENCE, userReference));
        param.add(new BasicNameValuePair(RequestFields.CONFIGURATION_NAME, build.getParent().getFullName()));
        param.add(new BasicNameValuePair(RequestFields.CONFIGURATION_BUILD_NBR_NUMBER, build.getId()));
        return param;
    }

    public void checkInAll(Run build) throws URISyntaxException, CheckInException, TransientErrorException {
        for (ResourceGroup resourceGroup : checkedOutResources) {
            if (resourceGroup.getBuild().equals(build)) {
                checkInGroup(resourceGroup);
            }
        }
    }

    public List<NameValuePair> getCheckInAllURIParameters() throws UnknownHostException {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair(RequestFields.SERVER_HOST, java.net.InetAddress.getLocalHost().getHostName()));
        return param;
    }

    public void checkInGroup(ResourceGroup resourceGroup) throws URISyntaxException, CheckInException, TransientErrorException {
        URIBuilder uriBuilder = new URIBuilder(getConfig().getRestApiURI() + "checkin_product");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
            if (!resourceEntity.isCheckedOut()) {
                continue;
            }
            uriBuilder.setParameters(resourceEntity.getURICheckInParameters());
            HttpGet req = new HttpGet(uriBuilder.build());
            req.setConfig(getRequestConfig());
            req.addHeader("accept", "application/json");
            RestResponse response;
            try {
                response = httpClient.execute(req, new RestCheckOutResponseHandler());
            } catch (IOException e) {
                e.printStackTrace();
                throw new TransientErrorException("Could not contact pool manager", e);
            }
            switch (response.getResultType()) {
                case SUCCESS:
                    LOGGER.info("Successfully checked in resource: " + resourceEntity.toString());
                    resourceEntity.setCheckedOut(false);
                    break;
                case FATAL:
                    throw new CheckInException("Could not check in: " + response.getMessage());
                default:
                    throw new CheckInException("Unexpected state: " + response.getMessage());
            }
        }
        checkedOutResources.remove(resourceGroup);
    }

    public void checkInAll() throws URISyntaxException, CheckInException, UnknownHostException,
        TransientErrorException {
        URIBuilder uriBuilder = new URIBuilder(getConfig().getRestApiURI() + "checkin_all_products");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        uriBuilder.setParameters(getCheckInAllURIParameters());
        HttpGet req = new HttpGet(uriBuilder.build());
        req.setConfig(getRequestConfig());
        req.addHeader("accept", "application/json");
        RestResponse response;
        try {
            response = httpClient.execute(req, new RestCheckInAllResponseHandler());
        } catch (IOException e) {
            e.printStackTrace();
            throw new TransientErrorException("Could not contact pool manager", e);
        }
        switch (response.getResultType()) {
            case SUCCESS:
                LOGGER.info("Successfully checked in all resources");
                break;
            case FATAL:
                throw new CheckInException("Could not check in all resources: " + response.getMessage());
            default:
                throw new CheckInException("Unexpected state: " + response.getMessage());
        }
        checkedOutResources.clear();
    }

    public ResourceGroup checkInGroup(Run build, int resourceGroupId) throws CheckInException,
            TransientErrorException, URISyntaxException {
        for (ResourceGroup resourceGroup : checkedOutResources) {
            if (resourceGroup.getBuild().equals(build) && resourceGroup.getId() == resourceGroupId) {
                checkInGroup(resourceGroup);
                return resourceGroup;
            }
        }
        throw new CheckInException("Could not find resource group: " + resourceGroupId);
    }

    /**
     * Used by the {@link AxisPoolManagement/index.jelly} page.
     *
     * @return All currently checked out (or partly checked out) resources
     */
    public List<ResourceGroup> getCheckedOutResources() {
        return checkedOutResources;
    }

    public GlobalConfig getConfig() {
        if (config == null) {
            config = GlobalConfig.getDefaultConfig();
        }
        return config;
    }

    /**
     * Sets config and saved to disk. Will be restored on Jenkins start up.
     *
     * @param config The config
     * @throws IOException Could not save to disk
     */
    public void setConfig(GlobalConfig config) throws IOException {
        this.config = config;
        save();
    }
}
