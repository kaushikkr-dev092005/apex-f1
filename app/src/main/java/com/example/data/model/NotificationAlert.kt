package com.example.data.model

data class NotificationAlert(
    val id: String,
    val title: String,
    val message: String,
    val type: AlertType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    REDFLAG,
    YELLOWFLAG,
    GREENFLAG,
    LAPUPDATE,
    DRS,
    OVERTAKE,
    CRASH
}
