package com.axis.system.jenkins.plugins.axispoolmanager.resources;

import com.axis.system.jenkins.plugins.axispoolmanager.rest.ResponseFields;
import hudson.Extension;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an exact product to check out.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 * @see <a href="http://staginglab.staging.rnd.axis.com/pool/">staginglab.staging.rnd.axis.com</a>
 */
public final class ExactProductResource extends ResourceEntity {
    // ipAddress here for backward compability reasons.
    private transient  final String ipAddress = null;
    private final String macAddress;

    @DataBoundConstructor
    public ExactProductResource(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public ResourceEntity getCopy() {
        return new ExactProductResource(ipAddress, macAddress);
    }

    @Override
    public List<NameValuePair> getURICheckOutParameters() {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair("mac", getMacAddress()));
        return param;
    }

    @Override
    public List<NameValuePair> getURICheckInParameters() {
        ArrayList param = new ArrayList<NameValuePair>();
        param.add(new BasicNameValuePair(ResponseFields.REFERENCE, getManagerMetaData().getString(ResponseFields.REFERENCE)));
        return param;
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getSimpleName() + ": [MAC Address: %s]", getMacAddress());
    }

    /**
     * Extends from a {@link ResourceEntityDescriptor}. Used for instancing the
     * ResourceEntities from config.jelly form posts.
     */
    @Extension
    public static final class ExactProductResourceDescriptor extends ResourceEntityDescriptor {

        @Override
        public String getDisplayName() {
            return "Checkout a Single Product by MAC";
        }
    }

}


