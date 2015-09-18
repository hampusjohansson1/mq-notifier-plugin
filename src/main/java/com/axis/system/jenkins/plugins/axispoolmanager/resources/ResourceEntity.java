package com.axis.system.jenkins.plugins.axispoolmanager.resources;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.apache.http.NameValuePair;

import java.util.List;

/**
 * Extend from this class to introduce new resource types.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public abstract class ResourceEntity extends AbstractDescribableImpl<ResourceEntity> {
    private JSONObject managerMetaData = null;
    private boolean checkedOut = false;

    public abstract List<NameValuePair> getURICheckOutParameters();

    public abstract List<NameValuePair> getURICheckInParameters();

    public final void setManagerMetaData(JSONObject managerMetaData) {
        this.managerMetaData = managerMetaData;
    }

    public final JSONObject getManagerMetaData() {
        return managerMetaData;
    }

    public final boolean isCheckedOut() {
        return checkedOut;
    }

    public final void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    /**
     * The needed descriptor for all classes that extends from {@link ResourceEntity}.
     */
    public abstract static class ResourceEntityDescriptor extends Descriptor<ResourceEntity> {
    }

}
