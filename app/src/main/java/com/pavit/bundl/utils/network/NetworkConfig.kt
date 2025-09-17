package com.pavit.bundl.utils.network

object NetworkConfig {
    private const val BASE_HOSTNAME = "backend-bundl.1110777.xyz"
    // private const val BASE_HOSTNAME = "192.168.53.152:3002" // Local dev
    
    const val BASE_URL = "https://$BASE_HOSTNAME"
    const val BASE_WS_URL = "wss://$BASE_HOSTNAME/chat"
}
