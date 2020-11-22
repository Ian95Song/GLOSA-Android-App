package com.example.dcaiti_ws2020

data class spat(
    val msgID: Int,
    val msgSubID: Int,
    val timestamp: Long,
    val intersectionStates: List<intersectionState>
) {
}
data class intersectionState(
    val intersectionId: Int,
    val regionId: Long,
    val revision: Int,
    //status: List<>
    val timestamp: Long,
    val timeshift: Int,
    val recvtime: Long,
    val source: String,
    val movementStates: List<movementState>
){
}
data class movementState(
    val signalGroupId: Int,
    val movementEvents: List<movementEvent>
){
}
data class movementEvent(
    val phaseState: String,
    val timeChange: timeChange
){
}
data class timeChange(
    val startTime: Int,
    val minEndTime: Int,
    val maxEndTime: Int,
    val likelyTime: Int,
    val confidence: Int,
    val nextTime: Int
){
}