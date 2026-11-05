package com.venice.spoof
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object Servers {
    private val feeds = listOf(
        "https://raw.githubusercontent.com/mohamadfg-dev/telegram-v2ray-configs-collector/main/category/tcp.txt",
        "https://raw.githubusercontent.com/0xRadikal/Free-v2ray-Configs/refs/heads/main/protocols/vmess_base64.txt",
        "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/All_Configs_Sub.txt"
    )
    suspend fun pick(ctx: Context): String = withContext(Dispatchers.IO) {
        for (feed in feeds) {
            val found = runCatching {
                val txt = URL(feed).readText()
                val all = Regex("(vmess|vless)://[^\\s\"']+").findAll(txt).map { it.value }.toList()
                val tcp = all.filter { !it.contains("type=ws") && !it.contains("security=tls") }
                (tcp.ifEmpty { all }).shuffled().firstOrNull()
            }.getOrNull()
            if (!found.isNullOrBlank()) return@withContext found
        }
        ""
    }
}
