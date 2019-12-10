package com.axis.system.jenkins.plugins.axispoolmanager.mq;

import com.axis.system.jenkins.plugins.axispoolmanager.ResourceGroup;
import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import com.axis.system.jenkins.plugins.axispoolmanager.rest.ResponseFields;
import com.sonymobile.jenkins.plugins.mq.mqnotifier.MQConnection;
import hudson.Util;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Helper class for sending messages to rabbit-mq by facilitating the mq-notifier plugin.
 */
public class MqHelper {
    /**
     * All methods are static. Hide the constructor.
     */
    protected MqHelper() { }

    /**
     * Is mq-notifier plugin installed?
     *
     * @return true if installed
     */
    public static boolean isMqNotifierInstalled() {
        return Jenkins.getInstance().getPlugin("mq-notifier") != null;
    }

    /**
     * Publishes a checkout message using the mq-notifier plugin.
     *
     * @param resourceGroup The resource group
     * @param queueTime ms for the checkout to get accepted
     */
    public static void publishSuccessfulCheckOut(ResourceGroup resourceGroup, long queueTime) {
        for (ResourceEntity resourceEntity : resourceGroup.getResourceEntities()) {
            JSONObject response = resourceEntity.getManagerMetaData();
            String resourceId = response.getString(ResponseFields.REFERENCE);
            JSONArray hosts = response.getJSONArray(ResponseFields.HOSTS);
            for (Object o : hosts) {
                JSONObject host = (JSONObject) o;
                JSONObject json = new JSONObject();
                json.element("build_job_name", resourceGroup.getBuild().getParent().getFullName())
                        .element("build_number", resourceGroup.getBuild().number)
                        .element("build_url", resourceGroup.getBuild().getAbsoluteUrl())
                        .element("client", Util.getHostName())
                        .element("dut_ip_address", host.getString("ip_addr"))
                        .element("dut_mac_address", host.getString("mac_addr"))
                        .element(
                                "dut_name",
                                getDutName(
                                        host.getString("project"),
                                        host.getString("product"),
                                        host.getString("identifier")))
                        .element("jenkins_master_fqdn", Util.getHostName())
                        .element("location", host.getString("shelf"))
                        .element("pool_name", host.getString("pool"))
                        .element("product_name", host.getString("product"))
                        .element("project", host.getString("project"))
                        .element("queue_time", queueTime)
                        .element("resource_id", resourceId)
                        .element("task", "checkout")
                        .element("type", "dut_event");
                MQConnection.getInstance().publish(json);
            }
        }
    }

    /**
     * Formats the dut name according to rules setup in the backend.
     *
     * @param project project
     * @param product product
     * @param identifier identifier
     * @return the formatted dut name
     */
    public static String getDutName(String project, String product, String identifier) {
        return String.format("%s-%s-%s", project, product, identifier);
    }
}
