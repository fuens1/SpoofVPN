package com.venice.spoof
import android.Manifest
import android.content.*
import android.net.VpnService
import android.os.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var linkField: EditText
    private lateinit var bugField: EditText
    private lateinit var statusView: TextView
    private var pendingLink: String? = null
    private val prefs by lazy { getSharedPreferences("cfg", Context.MODE_PRIVATE) }
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == RESULT_OK) pendingLink?.let(::startTunnel) }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val statusReceiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { statusView.text = intent?.getStringExtra("text").orEmpty() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(56, 120, 56, 56) }
        root.addView(label("VLESS / VMess linki"))
        linkField = EditText(this).apply { hint = "vless://… veya vmess://…"; setText(prefs.getString("link", "")) }
        root.addView(linkField)
        root.addView(label("Bug Host"))
        bugField = EditText(this).apply { setText(prefs.getString("bug", TunnelService.DEFAULT_BUG)); setSingleLine(true) }
        root.addView(bugField)
        root.addView(Button(this).apply {
            text = "Boşsa sunucu çek"
            setOnClickListener { lifecycleScope.launch { statusView.text = "Sunucu aranıyor…"; val found = Servers.pick(this@MainActivity); if (found.isBlank()) { statusView.text = "Sunucu bulunamadı, link yapıştır" } else { linkField.setText(found); statusView.text = "Sunucu hazır" } } }
        })
        root.addView(Button(this).apply {
            text = "BAĞLAN"; textSize = 22f
            setOnClickListener {
                val link = linkField.text.toString().trim()
                if (link.isBlank()) { statusView.text = "Link boş"; return@setOnClickListener }
                prefs.edit().putString("link", link).putString("bug", bugField.text.toString().trim()).apply()
                pendingLink = link
                val prepare = VpnService.prepare(this@MainActivity)
                if (prepare != null) vpnPermission.launch(prepare) else startTunnel(link)
            }
        })
        root.addView(Button(this).apply { text = "BAĞLANTIYI KES"; setOnClickListener { startService(Intent(this@MainActivity, TunnelService::class.java).setAction(TunnelService.ACTION_STOP)) } })
        statusView = TextView(this).apply { text = "Hazır"; setPadding(0, 48, 0, 0) }
        root.addView(statusView)
        setContentView(ScrollView(this).apply { addView(root) })
        ContextCompat.registerReceiver(this, statusReceiver, IntentFilter(TunnelService.ACTION_STATUS), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    private fun label(text: String) = TextView(this).apply { this.text = text; setPadding(0, 32, 0, 8) }
    private fun startTunnel(link: String) { ContextCompat.startForegroundService(this, Intent(this, TunnelService::class.java).putExtra("link", link).putExtra("bug", bugField.text.toString().trim())) }
    override fun onDestroy() { runCatching { unregisterReceiver(statusReceiver) }; super.onDestroy() }
}
