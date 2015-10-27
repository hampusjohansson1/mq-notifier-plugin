package com.axis.system.jenkins.plugins.axispoolmanager;

import com.axis.system.jenkins.plugins.axispoolmanager.config.GlobalConfig;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckInException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckOutException;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.RestCheckOutResponseHandler;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.RestResponse;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import jenkins.model.Jenkins;
import org.apache.http.NameValuePair;
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

/**
 * Plugin main entry point.
 * <p/>
 * TODO: Setup thread for building auto completion data on pool/product names.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class AxisResourceManager extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxisResourceManager.class);
    private List<ResourceGroup> checkedOutResources = new CopyOnWriteArrayList<ResourceGroup>();
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

    public boolean checkOut(ResourceGroup resourceGroup, String userReference)
            throws URISyntaxException, IOException, InterruptedException {
        // TODO: We should probably bookkeep the RESTApi end points in a separate file.
        URIBuilder uriBuilder = new URIBuilder(getConfig().getRestApiURI() + "checkout_product");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
            uriBuilder.setParameters(resourceEntity.getURICheckOutParameters());
            try {
                uriBuilder.addParameters(getBasicURICheckoutParameters(resourceGroup.getBuild(), userReference));
            } catch (UnknownHostException e) {
                throw new CheckOutException("Could not figure out host name", e);
            }
            HttpGet req = new HttpGet(uriBuilder.build());
            req.addHeader("accept", "application/json");
            RestResponse response;
            try {
                response = httpClient.execute(req, new RestCheckOutResponseHandler());
            } catch (IOException e) {
                e.printStackTrace();
                throw new CheckOutException("Could not contact pool manager", e);
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
                    return false;
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
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Axis Pool Manager plug-in");
        super.stop();
    }

    public List<NameValuePair> getBasicURICheckoutParameters(AbstractBuild build, String userReference)
            throws IOException, InterruptedException {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair("server_host", java.net.InetAddress.getLocalHost().getHostName()));
        param.add(new BasicNameValuePair("user_reference", userReference));
        param.add(new BasicNameValuePair("configuration_name", build.getProject().getFullName()));
        param.add(new BasicNameValuePair("configuration_build_nbr_number", build.getId()));
        param.add(new BasicNameValuePair("max_termination_time", String.valueOf(getConfig().getMaximumTimeout())));
        return param;
    }

    public void checkInAll(AbstractBuild build) throws URISyntaxException, CheckInException {
        for (ResourceGroup resourceGroup : checkedOutResources) {
            if (resourceGroup.getBuild().equals(build)) {
                checkInGroup(resourceGroup);
            }
        }
    }

    public void checkInGroup(ResourceGroup resourceGroup) throws URISyntaxException, CheckInException {
        URIBuilder uriBuilder = new URIBuilder(getConfig().getRestApiURI() + "checkin_product");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
            if (!resourceEntity.isCheckedOut()) {
                continue;
            }
            uriBuilder.setParameters(resourceEntity.getURICheckInParameters());
            HttpGet req = new HttpGet(uriBuilder.build());
            req.addHeader("accept", "application/json");
            try {
                RestResponse response = httpClient.execute(req, new RestCheckOutResponseHandler());
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
            } catch (IOException e) {
                e.printStackTrace();
                throw new CheckInException("Could not contact pool manager", e);
            }
        }
        checkedOutResources.remove(resourceGroup);
    }

    public ResourceGroup checkInGroup(AbstractBuild build, int resourceGroupId) throws CheckInException, URISyntaxException {
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
