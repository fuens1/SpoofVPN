package com.venice.spoof
import android.app.Service
import android.net.VpnService
import android.os.ParcelFileDescriptor
import io.nekohasekai.libbox.*

class Platform(private val svc: Service) : PlatformInterface {
    var tun: ParcelFileDescriptor? = null
    override fun openTun(options: TunOptions): ParcelFileDescriptor {
        val b = VpnService.Builder().setSession("spoof").setMtu(options.mtu).addAddress(options.inet4Address.address, options.inet4Address.prefix).addRoute("0.0.0.0", 0).addRoute("::", 0).addDnsServer(options.dnsServerAddress.address).setBlocking(true)
        options.inet6Address?.let { b.addAddress(it.address, it.prefix) }
        return b.establish()!!.also { tun = it }
    }
    override fun autoDetectInterfaceControl(fd: Int) { svc.protect(fd) }
    override fun clearDNSCache() = Unit
    override fun findConnectionOwner(ipProtocol: Int, sourceAddress: String, sourcePort: Int, destinationAddress: String, destinationPort: Int): Int = -1
    override fun getInterfaces(): InterfaceList = InterfaceList()
    override fun packageNameByUid(uid: Int): String? = null
    override fun readWIFIState(): WIFIState? = null
    override fun sendNotification(notification: Notification) = Unit
    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) = Unit
    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) = Unit
    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun systemCertificates() = null
    override fun underNetworkExtension(): Boolean = false
    override fun includeAllNetworks(): Boolean = false
}
