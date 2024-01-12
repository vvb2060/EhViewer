package com.hippo.ehviewer.client

import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.Settings
import java.net.InetAddress
import java.net.Proxy
import java.net.Socket
import java.net.URI
import javax.net.ssl.SSLSocketFactory

class EhSSLSocketFactory(val factory: SSLSocketFactory) : SSLSocketFactory() {

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val noProxy = EhApplication.ehProxySelector.select(URI("https://$host"))[0] == Proxy.NO_PROXY
        val newHost = if (Settings.doH && noProxy) s.inetAddress.hostAddress else host
        return factory.createSocket(s, newHost, port, autoClose)
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
