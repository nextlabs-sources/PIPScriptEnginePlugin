/*
 * Copyright 2017 by Nextlabs Inc.
 *
 * All rights reserved worldwide.
 * Created on 8 Aug 2017
 * 
 * MLI - amilasilva88@gmail.com
 */
package com.nextlabs.plugins.pip.scriptengine.exceptions;

/**
 * PIP plugin related exceptions handles here
 * 
 * @author Amila Silva
 * @since 8.5
 *
 */
public class PIPPluginException extends Exception {

	private static final long serialVersionUID = 7174247167044710794L;

	public PIPPluginException() {
	}

	/**
	 * @param message
	 */
	public PIPPluginException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public PIPPluginException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public PIPPluginException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public PIPPluginException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
