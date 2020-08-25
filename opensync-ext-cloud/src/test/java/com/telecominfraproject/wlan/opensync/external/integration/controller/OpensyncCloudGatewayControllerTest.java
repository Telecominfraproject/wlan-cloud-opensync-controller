package com.telecominfraproject.wlan.opensync.external.integration.controller;

import com.telecominfraproject.wlan.core.client.PingClient;
import com.telecominfraproject.wlan.core.model.service.GatewayType;
import com.telecominfraproject.wlan.datastore.exceptions.DsEntityNotFoundException;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentGatewayRecord;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;


class OpensyncCloudGatewayControllerTest {

    OpensyncCloudGatewayController opensyncCloudGatewayController = new OpensyncCloudGatewayController();

    RoutingServiceInterface routingSvc = Mockito.mock(RoutingServiceInterface.class);
    PingClient pingClient = Mockito.mock(PingClient.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(opensyncCloudGatewayController, "eqRoutingSvc", routingSvc);
        ReflectionTestUtils.setField(opensyncCloudGatewayController, "pingClient", pingClient);
    }

    @Test
    void cleanupStaleGwRecord() throws IOException {
        EquipmentGatewayRecord gatewayRecord = readGatewayRecord();
        Mockito.when(pingClient.isReachable(anyString(), anyInt())).thenReturn(true);
        opensyncCloudGatewayController.cleanupStaleGwRecord();
        verify(pingClient, atLeastOnce()).isReachable("1.1.1.1",123);
    }

    @Test
    void cleanupStaleGwRecord_pingFail() throws IOException {
        EquipmentGatewayRecord gatewayRecord = readGatewayRecord();
        Mockito.when(pingClient.isReachable(anyString(), anyInt())).thenReturn(false);
        Mockito.when(routingSvc.deleteGateway(anyLong())).thenReturn(gatewayRecord);
        opensyncCloudGatewayController.cleanupStaleGwRecord();
        verify(routingSvc, atLeastOnce()).deleteGateway(gatewayRecord.getId());
    }

    @Test
    void cleanupStaleEqptRoutingRecord_NoEntityFound() throws IOException {
        EquipmentRoutingRecord routingRecord = readRoutingRecord();
        Mockito.when(routingSvc.getGateway(anyLong())).thenThrow(new DsEntityNotFoundException());
        Mockito.when(routingSvc.delete(anyLong())).thenReturn(new EquipmentRoutingRecord());

        opensyncCloudGatewayController.cleanupStaleEqptRoutingRecord(123L);
        verify(routingSvc, atLeastOnce()).delete(routingRecord.getId());

    }

    @Test
    void cleanupStaleEqptRoutingRecord_GenericException() throws IOException {
        EquipmentRoutingRecord routingRecord = readRoutingRecord();
        Mockito.when(routingSvc.getGateway(anyLong())).thenThrow(new RuntimeException("Throwing exception to test"));
        Mockito.when(routingSvc.delete(anyLong())).thenReturn(new EquipmentRoutingRecord());

        opensyncCloudGatewayController.cleanupStaleEqptRoutingRecord(123L);
        verify(routingSvc, atLeast(0)).delete(routingRecord.getId());
    }

    @Test
    void cleanupStaleEqptRoutingRecord_GatewayResponds() throws IOException {
        EquipmentRoutingRecord routingRecord = readRoutingRecord();
        EquipmentGatewayRecord gatewayRecord = readGatewayRecord();
        Mockito.when(routingSvc.getGateway(anyLong())).thenReturn(gatewayRecord);
        Mockito.when(routingSvc.delete(anyLong())).thenReturn(new EquipmentRoutingRecord());
        Mockito.when(pingClient.isReachable(anyString(), anyInt())).thenReturn(true);

        opensyncCloudGatewayController.cleanupStaleEqptRoutingRecord(123L);
        verify(routingSvc, atLeast(0)).delete(routingRecord.getId());
        verify(pingClient, atLeastOnce()).isReachable(anyString(), anyInt());
    }

    @Test
    void cleanupStaleEqptRoutingRecord_GatewayFails() throws IOException {
        EquipmentRoutingRecord routingRecord = readRoutingRecord();
        EquipmentGatewayRecord gatewayRecord = readGatewayRecord();
        Mockito.when(routingSvc.getGateway(anyLong())).thenReturn(gatewayRecord);
        Mockito.when(routingSvc.delete(anyLong())).thenReturn(new EquipmentRoutingRecord());
        Mockito.when(pingClient.isReachable(anyString(), anyInt())).thenReturn(false);

        opensyncCloudGatewayController.cleanupStaleEqptRoutingRecord(123L);
        verify(routingSvc, atLeastOnce()).delete(routingRecord.getId());
        verify(pingClient, atLeastOnce()).isReachable(anyString(), anyInt());
    }

    private EquipmentGatewayRecord readGatewayRecord() throws IOException {
        EquipmentGatewayRecord gatewayRecord = EquipmentGatewayRecord.fromFile(
                OpensyncCloudGatewayControllerTest.class.getResource("EquipmentGatewayRecords.json").getFile(),
                EquipmentGatewayRecord.class);

        Mockito.when(routingSvc.getGateway(GatewayType.CEGW)).thenReturn(Arrays.asList(gatewayRecord));
        return gatewayRecord;
    }

    private EquipmentRoutingRecord readRoutingRecord() throws IOException {
        EquipmentRoutingRecord routingRecord = EquipmentRoutingRecord.fromFile(
                OpensyncCloudGatewayControllerTest.class.getResource("EquipmentRoutingRecord.json").getFile(),
                EquipmentRoutingRecord.class);

        Mockito.when(routingSvc.getRegisteredRouteList(anyLong())).thenReturn(Arrays.asList(routingRecord));
        return routingRecord;
    }
}