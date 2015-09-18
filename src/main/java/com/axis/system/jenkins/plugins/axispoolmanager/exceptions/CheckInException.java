package com.axis.system.jenkins.plugins.axispoolmanager.exceptions;

import java.io.IOException;

/**
 * Something went wrong when checking in resources.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public class CheckInException extends IOException {
    public CheckInException(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckInException(String message) {
        super(message);
    }
}
