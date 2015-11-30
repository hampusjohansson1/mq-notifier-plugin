package com.axis.system.jenkins.plugins.axispoolmanager.exceptions;

import java.io.IOException;

/**
 * Something went wrong when checking in resources.
 *
 * @author Bekim Berisha <bekim.berisha@axis.com> (C) Axis 2015
 */
public class TransientErrorException extends IOException {
    public TransientErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransientErrorException(String message) {
        super(message);
    }
}
