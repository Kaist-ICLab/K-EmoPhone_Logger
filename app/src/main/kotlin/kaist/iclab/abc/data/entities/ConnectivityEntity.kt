package kaist.iclab.abc.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity


import kaist.iclab.abc.data.types.ConnectivityNetworkType
import kaist.iclab.abc.data.types.ConnectivityNetworkTypeConverter
/**
 * It represents an online-connection status of a phone.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property isConnected indicates that a phone is connected to a network
 * @property type a network questionType that is currently connected (e.g., LTE, WiFi)
 */

@Entity
data class ConnectivityEntity (
    val isConnected: Boolean = false,
    @Convert(converter = ConnectivityNetworkTypeConverter::class, dbType = String::class)
    val type: ConnectivityNetworkType = ConnectivityNetworkType.UNDEFINED
): Base()
