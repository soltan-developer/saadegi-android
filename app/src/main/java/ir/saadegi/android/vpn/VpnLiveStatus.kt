package ir.saadegi.android.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TunnelPhase { IDLE, STARTING, VERIFYING, CONNECTED, STOPPING, ERROR }
data class TunnelStatus(val phase: TunnelPhase = TunnelPhase.IDLE, val profileId: String? = null, val message: String? = null)

object VpnLiveStatus {
    private val mutable = MutableStateFlow(TunnelStatus())
    val state = mutable.asStateFlow()
    fun set(value: TunnelStatus) { mutable.value = value }
}
