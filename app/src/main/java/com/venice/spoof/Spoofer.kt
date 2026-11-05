package com.venice.spoof
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object Spoofer {
    private const val UA = "Mozilla/5.0 (Linux; Android 13; SM-A536B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"
    fun outbound(link: String, bug: String): JSONObject {
        val raw = if (link.startsWith("vmess://"))
            JSONObject(String(Base64.decode(link.removePrefix("vmess://"), Base64.DEFAULT)))
        else parseUri(link)
        val tls = raw.optBoolean("tls", false)
        val net = raw.optString("net", "tcp")
        val stream = JSONObject().put("network", net)
        when {
            tls -> stream.put("security", "tls")
            net == "tcp" -> stream.put("security", "none").put("tcpSettings", JSONObject().put("header", JSONObject().put("type", "http").put("request", JSONObject().put("version", "1.1").put("method", "GET").put("path", JSONArray().put("/")).put("headers", JSONObject().put("Host", JSONArray().put(bug)).put("X-Online-Host", JSONArray().put(bug)).put("User-Agent", JSONArray().put(UA)).put("Accept-Encoding", JSONArray().put("gzip, deflate")).put("Connection", JSONArray().put("keep-alive"))))))
            net.startsWith("ws") -> stream.put("security", if (tls) "tls" else "none").put("wsSettings", JSONObject().put("path", raw.optString("path", "/")).put("headers", JSONObject().put("Host", bug)))
        }
        return JSONObject().put("type", if (raw.optString("proto") == "vless") "vless" else "vmess").put("tag", "proxy").put("server", raw.optString("add")).put("server_port", raw.optInt("port")).put("uuid", raw.optString("id")).put("transport", stream).put("tls", JSONObject().put("enabled", tls).put("server_name", bug).put("insecure", true))
    }
    private fun parseUri(link: String): JSONObject {
        val u = Uri.parse(link)
        return JSONObject().put("proto", u.scheme).put("id", u.userInfo).put("add", u.host).put("port", u.port).put("net", u.getQueryParameter("type") ?: "tcp").put("path", u.getQueryParameter("path") ?: "/").put("tls", u.getQueryParameter("security") == "tls")
    }
}
