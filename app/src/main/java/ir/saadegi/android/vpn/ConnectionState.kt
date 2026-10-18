package ir.saadegi.android.vpn

/** Pure reducer: Android Service, UI and health-check events must use one state machine. */
enum class ConnectionPhase {
    IDLE, PREPARING, CONNECTING, VERIFYING, CONNECTED,
    RECONNECTING, DISCONNECTING, ERROR
}
sealed interface ConnectionEvent {
    data object RequestConnect : ConnectionEvent
    data object PermissionGranted : ConnectionEvent
    data object TunnelStarted : ConnectionEvent
    data object VerificationPassed : ConnectionEvent
    data object ConnectionLost : ConnectionEvent
    data object RequestDisconnect : ConnectionEvent
    data object Stopped : ConnectionEvent
    data object Failed : ConnectionEvent
}

object ConnectionReducer {
    fun reduce(state: ConnectionPhase, event: ConnectionEvent): ConnectionPhase = when (event) {
        ConnectionEvent.RequestConnect -> when (state) {
            ConnectionPhase.IDLE, ConnectionPhase.ERROR -> ConnectionPhase.PREPARING
            else -> state
        }
        ConnectionEvent.PermissionGranted ->
            if (state == ConnectionPhase.PREPARING) ConnectionPhase.CONNECTING else state
        ConnectionEvent.TunnelStarted ->
            if (state == ConnectionPhase.CONNECTING || state == ConnectionPhase.RECONNECTING)
                ConnectionPhase.VERIFYING else state
        ConnectionEvent.VerificationPassed ->
            if (state == ConnectionPhase.VERIFYING) ConnectionPhase.CONNECTED else state
        ConnectionEvent.ConnectionLost ->
            if (state == ConnectionPhase.CONNECTED || state == ConnectionPhase.VERIFYING)
                ConnectionPhase.RECONNECTING else state
        ConnectionEvent.RequestDisconnect ->
            if (state != ConnectionPhase.IDLE && state != ConnectionPhase.DISCONNECTING)
                ConnectionPhase.DISCONNECTING else state
        ConnectionEvent.Stopped -> ConnectionPhase.IDLE
        ConnectionEvent.Failed -> ConnectionPhase.ERROR
    }
}
