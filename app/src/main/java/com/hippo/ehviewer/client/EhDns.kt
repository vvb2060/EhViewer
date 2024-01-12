package com.hippo.ehviewer.client

import com.hippo.ehviewer.Settings
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.tls.HandshakeCertificates

object EhDns : Dns {
    private val dnsIP = listOf(
        InetAddress.getByName("104.16.248.249"),
        InetAddress.getByName("104.16.249.249"),
    )
    private val certs = HandshakeCertificates.Builder()
        .addPlatformTrustedCertificates()
        .build()
    private val dnsClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .sslSocketFactory(EhSSLSocketFactory(certs.sslSocketFactory()), certs.trustManager)
        .build()
    private val doh = DnsOverHttps.Builder().apply {
        client(dnsClient)
        url("https://1dot1dot1dot1.cloudflare-dns.com/dns-query".toHttpUrl())
        includeIPv6(false)
        bootstrapDnsHosts(dnsIP)
        resolvePrivateAddresses(true)
    }.build()

    // origin server IP, not cloudflare IP
    private val sexIP = mutableListOf(
        InetAddress.getByName("178.175.128.253"),
        InetAddress.getByName("178.175.128.254"),
        InetAddress.getByName("178.175.129.253"),
        InetAddress.getByName("178.175.129.254"),
        InetAddress.getByName("178.175.132.21"),
        InetAddress.getByName("178.175.132.22"),
    )
    private val exIP = mutableListOf(
        InetAddress.getByName("178.175.128.251"),
        InetAddress.getByName("178.175.128.252"),
        InetAddress.getByName("178.175.129.251"),
        InetAddress.getByName("178.175.129.252"),
        InetAddress.getByName("178.175.132.19"),
        InetAddress.getByName("178.175.132.20"),
    ).apply { addAll(sexIP) }
    private val forumsIP = mutableListOf(
        InetAddress.getByName("94.100.18.243"),
    )

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        if (!Settings.doH) {
            return Dns.SYSTEM.lookup(hostname)
        }
        if (hostname == "exhentai.org") {
            exIP.shuffle()
            return exIP
        }
        if (hostname.endsWith(".exhentai.org")) {
            sexIP.shuffle()
            return sexIP
        }
        if (hostname == "forums.e-hentai.org") {
            return forumsIP
        }
        return try {
            doh.lookup(hostname)
        } catch (_: UnknownHostException) {
            Dns.SYSTEM.lookup(hostname)
        }
    }
}
