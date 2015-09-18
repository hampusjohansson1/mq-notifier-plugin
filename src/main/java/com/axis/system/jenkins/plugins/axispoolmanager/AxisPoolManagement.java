package com.axis.system.jenkins.plugins.axispoolmanager;

import com.axis.system.jenkins.plugins.axispoolmanager.config.GlobalConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Global configuration and status page. Only used from {@link AxisPoolManagement/index.jelly}
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
@Extension
public final class AxisPoolManagement extends ManagementLink implements Describable<AxisPoolManagement> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxisResourceManager.class);

    @Override
    public String getIconFileName() {
        return "/plugin/axis-pool-manager/images/icon.png";
    }

    @Override
    public String getDescription() {
        return "Status and management for Jenkins/DUT Pool bridge";
    }

    @Override
    public String getDisplayName() {
        return "Axis Pool Management";
    }

    @Override
    public String getUrlName() {
        return "axis-pool-manager";
    }

    public static GlobalConfig getConfig() {
        return getResourceManager().getConfig();
    }

    public static AxisResourceManager getResourceManager() {
        return AxisResourceManager.getInstance();
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public Descriptor<AxisPoolManagement> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    /**
     * Builds and saves new {@link GlobalConfig}.
     *
     * @param req StaplerRequest The request
     * @param rsp StaplerResponse The response with our form data
     * @throws ServletException
     * @throws IOException
     * @throws InterruptedException
     */
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException {
        LOGGER.debug("submit " + req.toString());
        JSONObject form = req.getSubmittedForm();
        GlobalConfig config = GlobalConfig.fromJSON(form);
        AxisResourceManager axisResourceManager = AxisResourceManager.getInstance();
        axisResourceManager.setConfig(config);
        rsp.sendRedirect(".");
    }

    /**
     * Generic Descriptor for the {@link AxisPoolManagement} implementation of {@link ManagementLink} @Extension.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<AxisPoolManagement> {
        @Override
        public String getDisplayName() {
            return "Axis Pool Management";
        }
    }
}
