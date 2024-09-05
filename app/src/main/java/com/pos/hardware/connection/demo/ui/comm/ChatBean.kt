package com.pos.hardware.connection.demo.ui.comm

data class ChatBean(

    var id: String = System.currentTimeMillis().toString(),
    var time: Long = System.currentTimeMillis(),
    var receiver: String = "",
    var content: String = "",
    var sender: String = "",

)