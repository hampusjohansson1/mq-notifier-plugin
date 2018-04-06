package com.axis.system.jenkins.plugins.axispoolmanager;

import com.axis.system.jenkins.plugins.axispoolmanager.actions.AxisPoolParameterAction;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.CheckInException;
import com.axis.system.jenkins.plugins.axispoolmanager.exceptions.TransientErrorException;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.net.URISyntaxException;

/**
 * Listens on all completed or failed builds and ensures no resources are
 * left in a checked out state.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
@Extension
public final class AxisResourcesGarbageCollector extends RunListener<Run> {
    @Override
    public void onCompleted(Run run, TaskListener listener) {
        super.onCompleted(run, listener);
        AxisResourceManager axisResourceManager = AxisResourceManager.getInstance();
        try {
            axisResourceManager.checkInAll(run);
            AxisPoolParameterAction.disableEnvVars(run);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (CheckInException e) {
            e.printStackTrace();
        } catch (TransientErrorException e) {
            e.printStackTrace();
        }
    }
}
