package com.venice.spoof
import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import io.nekohasekai.libbox.*
import org.json.JSONArray
import org.json.JSONObject

class TunnelService : VpnService() {
    private var box: BoxInstance? = null
    private var platform: Platform? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("vpn", "VPN", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { teardown(); return Service.START_NOT_STICKY }
        val link = intent?.getStringExtra("link") ?: return Service.START_NOT_STICKY
        val bug = intent.getStringExtra("bug")?.trim()?.ifBlank { null } ?: DEFAULT_BUG
        startForeground(1, notification("Bağlanıyor…", false))
        val p = Platform(this)
        platform = p
        val config = JSONObject().put("log", JSONObject().put("level", "warn")).put("inbounds", JSONArray().put(JSONObject().put("type", "tun").put("tag", "tun").put("address", JSONArray().put("172.19.0.1/30")).put("mtu", 1400).put("stack", "gvisor").put("auto_route", true).put("strict_route", false))).put("outbounds", JSONArray().put(Spoofer.outbound(link, bug)).put(JSONObject().put("type", "direct").put("tag", "direct")))
        runCatching {
            box = BoxInstance(p, config.toString(), OverrideOptions()).also { it.start() }
            push("Bağlı — ${bug}")
            notify(notification("Bağlı — ${bug}", true))
        }.onFailure {
            push("Hata: ${it.message}")
            notify(notification("Bağlanamadı", true))
            teardown()
        }
        return Service.START_STICKY
    }
    private fun teardown() {
        runCatching { box?.close() }
        runCatching { platform?.tun?.close() }
        box = null
        platform = null
        push("Kapalı")
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private fun push(text: String) { sendBroadcast(Intent(ACTION_STATUS).putExtra("text", text)) }
    private fun notify(n: Notification) { getSystemService(NotificationManager::class.java).notify(1, n) }
    private fun notification(text: String, ongoing: Boolean): Notification = Notification.Builder(this, "vpn").setContentTitle("Spoof").setContentText(text).setSmallIcon(android.R.drawable.stat_sys_vpn_ic).setOngoing(ongoing).build()
    override fun onDestroy() { teardown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_STOP = "com.venice.spoof.STOP"; const val ACTION_STATUS = "com.venice.spoof.STATUS"; const val DEFAULT_BUG = "kktcell.com" }
}
