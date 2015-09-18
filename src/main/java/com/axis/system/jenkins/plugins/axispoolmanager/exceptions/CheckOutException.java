package com.axis.system.jenkins.plugins.axispoolmanager.exceptions;

import java.io.IOException;

/**
 * Resources could not be checked out.
 *
 * @author Gustaf Lundh <gustaf.lundh@axis.com> (C) Axis 2015
 */
public class CheckOutException extends IOException {
    public CheckOutException(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckOutException(String message) {
        super(message);
    }
}
