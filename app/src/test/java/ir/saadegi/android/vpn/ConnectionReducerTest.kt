package ir.saadegi.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionReducerTest {
    @Test fun onlyVerifiedTunnelCanBeConnected() {
        var phase = ConnectionPhase.IDLE
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.VerificationPassed)
        assertEquals(ConnectionPhase.IDLE, phase)
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.RequestConnect)
        assertEquals(ConnectionPhase.PREPARING, phase)
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.PermissionGranted)
        assertEquals(ConnectionPhase.CONNECTING, phase)
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.TunnelStarted)
        assertEquals(ConnectionPhase.VERIFYING, phase)
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.VerificationPassed)
        assertEquals(ConnectionPhase.CONNECTED, phase)
    }

    @Test fun disconnectAndRevokeMustClearConnectedStatus() {
        var phase = ConnectionPhase.CONNECTED
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.ConnectionLost)
        assertEquals(ConnectionPhase.RECONNECTING, phase)
        phase = ConnectionReducer.reduce(phase, ConnectionEvent.RequestDisconnect)
        assertEquals(ConnectionPhase.DISCONNECTING, phase)
        assertEquals(ConnectionPhase.IDLE, ConnectionReducer.reduce(phase, ConnectionEvent.Stopped))
    }

    @Test fun failureIsNotSilentlyConnected() {
        assertEquals(ConnectionPhase.ERROR, ConnectionReducer.reduce(ConnectionPhase.CONNECTING, ConnectionEvent.Failed))
        assertEquals(ConnectionPhase.PREPARING, ConnectionReducer.reduce(ConnectionPhase.ERROR, ConnectionEvent.RequestConnect))
    }
}
