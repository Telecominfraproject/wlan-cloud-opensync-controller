package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

public enum CEGWCommandResultCode {
	/**
	 * Successfully delivered to CE
	 */
	Success,
	/**
	 * No route to CE.
	 */
	NoRouteToCE,
	/**
	 * Failed to deliver the message to CE.
	 */
	FailedToSend,
	/**
	 * Timed out waiting for CE to response. CEGW will not return this code because
	 * communication is asynchronous.
	 */
	TimedOut,
	/**
	 * Failure reported by customer equipment CEGW will not return this code because
	 * communication is asynchronous.
	 */
	FailedOnCE,
	/**
	 * Command code not supported
	 */
	UnsupportedCommand;
}
