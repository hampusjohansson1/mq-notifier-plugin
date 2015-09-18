package com.axis.system.jenkins.plugins.axispoolmanager;

import com.axis.system.jenkins.plugins.axispoolmanager.resources.ResourceEntity;
import hudson.model.AbstractBuild;

import java.util.Collections;
import java.util.List;

/**
 * ResourceGroup allows structuring a group of Resources (DUTs)
 * together and linking them to the build that checked them out.
 * <p/>
 * TODO: Make use of the groups to optimize check outs through a
 * all-or-nothing transaction using the Pool Management Interface
 * <p/>
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public final class ResourceGroup {
    private final AbstractBuild abstractBuild;
    private final int id;
    private final List<ResourceEntity> resourceEntities;

    public ResourceGroup(AbstractBuild abstractBuild,
                         int id,
                         List<ResourceEntity> resourceEntities) {
        this.abstractBuild = abstractBuild;
        this.id = id;
        this.resourceEntities = resourceEntities == null ? Collections.<ResourceEntity>emptyList() : resourceEntities;
    }

    /**
     * @return The build owning the resources
     */
    public AbstractBuild getBuild() {
        return abstractBuild;
    }

    /**
     * @return The resource group Id
     */
    public int getId() {
        return id;
    }

    /**
     * These resources should be checked out in an all-or-nothing fashion.
     * Can return empty list but never null.
     *
     * @return resources included in this group
     */
    public List<ResourceEntity> getResourceEntities() {
        return resourceEntities;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Resource group: ").append(getId())
            .append(" ").append(System.lineSeparator())
            .append(" ").append(getResourceEntities());
        return sb.toString();
    }
}
