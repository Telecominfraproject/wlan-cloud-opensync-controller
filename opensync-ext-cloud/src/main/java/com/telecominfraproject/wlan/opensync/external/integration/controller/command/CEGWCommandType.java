package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CEGWCommandType {
	/**
	 * Notification to CE for configuration change
	 */
	Unknown, ConfigChangeNotification, FirmwareDownloadRequest, StartDebugEngine, StopDebugEngine, FirmwareFlashRequest,
	RebootRequest, BlinkRequest, CloseSessionRequest, NeighbourhoodReport, ClientDeauthRequest, CellSizeRequest,
	NewChannelRequest, ReportCurrentAPCRequest, FileUpdateRequest, InterferenceThresholdUpdateRequest,
	BestApConfigurationUpdateRequest,

	/**
	 * Will tell the AP to monitor these macs filters for hardwired traffic (aka:
	 * Rogue AP)
	 */
	BSSIDToMacMonitoringRequest,

	// Will tell the AP to dynamically change channels
	ChannelChangeAnnouncementRequest,

	/**
	 * Start packet capture into file
	 */
	StartPacketFileCapture,
	/**
	 * Stop packet capture
	 */
	StopPacketCapture,
	/**
	 * Check for routing record
	 */
	CheckRouting, ReportCurrentVLANRequest,

	/**
	 * Log file upload
	 */
	UploadLogFile,

	/**
	 * For toggle PoE port on switch.
	 */
	TogglePoERequest,

	/**
	 * Reset radio
	 */
	RadioReset,

	/**
	 * Clear scan table
	 */
	ClearScanTable,

	/**
	 * For commands related to wds link of Mesh.
	 */
	WdsRequest,

	UNSUPPORTED;

	@JsonCreator
	public static CEGWCommandType getByName(String value) {
		return deserializEnum(value, CEGWCommandType.class, UNSUPPORTED);
	}

	public static boolean isUnsupported(Object value) {
		return UNSUPPORTED.equals(value);
	}

	/**
	 * Deserialize enumeration with default value when it's unknown.
	 * 
	 * @param jsonValue
	 * @param enumType
	 * @param defaultValue
	 * @return decoded value
	 */
	public static <E extends Enum<E>> E deserializEnum(String jsonValue, Class<E> enumType, E defaultValue) {
		if (null == jsonValue) {
			return null;
		}
		try {
			E result = E.valueOf(enumType, jsonValue);
			return result;
		} catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}
}
