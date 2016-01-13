package com.axis.system.jenkins.plugins.axispoolmanager.resources;

import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckOutException;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.EnvVars;
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

    public abstract List<NameValuePair> getURICheckOutParameters(EnvVars envVars) throws CheckOutException;

    public abstract List<NameValuePair> getURICheckInParameters();

    /**
     * Ugh. Jenkins allows parallel jobs to share the same _instance_ of the checkout builder,
     * but we also need to use the ResourceEntities as a storage for meta data.
     *
     * I also want to avoid clone() which is discouraged since we lose control over derived
     * classes (Object already implements clone).
     *
     * @see CheckOutBuilder.getCopyOfResourceEntities()
     */
    public abstract ResourceEntity getCopy();

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
