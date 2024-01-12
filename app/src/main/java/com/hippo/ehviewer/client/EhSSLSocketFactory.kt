package com.hippo.ehviewer.client

import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.Settings
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.URI
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class EhSSLSocketFactory : SSLSocketFactory() {
    private val factory: SSLSocketFactory

    init {
        val context = SSLContext.getInstance("TLS")
        context.init(null, null, null)
        factory = context.socketFactory
    }

    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        val socket = factory.createSocket(s, host, port, autoClose) as SSLSocket
        val noProxy = EhApplication.ehProxySelector.select(URI("https://$host")).get(0) == Proxy.NO_PROXY
        if (!Settings.doH || !noProxy) return socket
        val params = socket.sslParameters
        params.serverNames = listOf(SNIHostName("eh"))
        socket.sslParameters = params
        return socket
    }

    override fun createSocket(host: String?, port: Int): Socket = factory.createSocket(host, port)

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket = factory.createSocket(host, port, localHost, localPort)

    override fun createSocket(host: InetAddress?, port: Int): Socket = factory.createSocket(host, port)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = factory.createSocket(address, port, localAddress, localPort)

    override fun getDefaultCipherSuites(): Array<String> = factory.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = factory.supportedCipherSuites
}
