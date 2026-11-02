
package com.neshi.pabrikneshi

import android.widget.Toast
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val mapUrl = "https://aniidex.com/id/map/"
    private val progressStorageKey = "aniidex.map-progress"
    private val prefsName = "PabrikNeshiCustomers"

    private lateinit var webView: WebView
    private lateinit var customerText: TextView

    private val prefs by lazy {
        getSharedPreferences(prefsName, MODE_PRIVATE)

    private fun inspectMapStorage() {
    Toast.makeText(this, "Cek Data berhasil dipanggil", Toast.LENGTH_LONG).show()
    
    }

    private var customers = mutableListOf<String>()
    private var activeCustomer = "Pelanggan Lama"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCustomers()
        createInterface()
    }

    // =========================
    // DATA PELANGGAN
    // =========================

    private fun loadCustomers() {
        val saved = prefs.getString("customer_list", null)

        if (saved == null) {
            customers = mutableListOf("Pelanggan Lama")
            activeCustomer = "Pelanggan Lama"
            saveCustomers()
        } else {
            try {
                val array = JSONArray(saved)
                customers.clear()

                for (i in 0 until array.length()) {
                    customers.add(array.getString(i))
                }
            } catch (_: Exception) {
                customers = mutableListOf("Pelanggan Lama")
            }

            if (customers.isEmpty()) {
                customers.add("Pelanggan Lama")
            }

            activeCustomer = prefs.getString(
                "active_customer",
                customers.first()
            ) ?: customers.first()

            if (!customers.contains(activeCustomer)) {
                activeCustomer = customers.first()
            }
        }
    }

    private fun saveCustomers() {
        prefs.edit()
            .putString("customer_list", JSONArray(customers).toString())
            .putString("active_customer", activeCustomer)
            .apply()
    }

    private fun customerProgressKey(name: String): String {
        return "progress_" + name
    }

    private fun getCustomerProgress(name: String): String {
        return prefs.getString(customerProgressKey(name), "") ?: ""
    }

    private fun saveCustomerProgress(name: String, value: String) {
        prefs.edit()
            .putString(customerProgressKey(name), value)
            .apply()
    }

    // =========================
    // TAMPILAN APLIKASI
    // =========================

    private fun createInterface() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(5, 8, 22))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(12, 12, 12, 8)
        header.setBackgroundColor(Color.rgb(17, 24, 39))

        val title = TextView(this)
        title.text = "PABRIK NESHI"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        header.addView(title)

        customerText = TextView(this)
        customerText.textSize = 14f
        customerText.setTextColor(Color.WHITE)
        customerText.gravity = Gravity.CENTER
        customerText.setPadding(0, 10, 0, 10)
        header.addView(customerText)

        val customerRow = LinearLayout(this)
        customerRow.orientation = LinearLayout.HORIZONTAL

        customerRow.addView(
            makeButton("Pilih Pelanggan") {
                showCustomerList()
            },
            weightedParams()
        )

        customerRow.addView(
            makeButton("+ Tambah") {
                addCustomer()
            },
            weightedParams()
        )

        header.addView(customerRow)

        header.addView(
            makeButton("CEK DATA PETA") {
                inspectMapStorage()
            }
        )

        root.addView(header)

        val navigation = LinearLayout(this)
        navigation.orientation = LinearLayout.HORIZONTAL

        navigation.addView(
            makeButton("HOME") {
                showHome()
            },
            weightedParams()
        )

        navigation.addView(
            makeButton("MAP") {
                openMap()
            },
            weightedParams()
        )

        navigation.addView(
            makeButton("INFO") {
                showInfo()
            },
            weightedParams()
        )

        root.addView(navigation)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.addJavascriptInterface(
            CustomerBridge(),
            "PabrikNeshiBridge"
        )

        webView.addJavascriptInterface(
            StorageInspector(),
            "AndroidStorageInspector"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                if (url != null && url.startsWith(mapUrl)) {
                    installProgressWatcher()
                }
            }
        }

        root.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        updateCustomerLabel()
        showHome()
    }

    private fun weightedParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): Button {
        val button = Button(this)
        button.text = text
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(Color.rgb(65, 75, 120))
        button.setOnClickListener { action() }
        return button
    }

    private fun updateCustomerLabel() {
        customerText.text = "Pelanggan aktif: $activeCustomer"
    }

    // =========================
    // NAVIGASI
    // =========================

    private fun showHome() {
        webView.loadDataWithBaseURL(
            null,
            """
            <html>
            <body style="background:#050816;color:white;
            font-family:sans-serif;text-align:center;padding:40px">
            <h1 style="color:#7f5cff">PABRIK NESHI</h1>
            <p>Game Map Progress Manager</p>
            <p>Pilih MAP untuk membuka AniIdex.</p>
            </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun openMap() {
        webView.loadUrl(mapUrl)
    }

    private fun showInfo() {
        webView.loadDataWithBaseURL(
            null,
            """
            <html>
            <body style="background:#050816;color:white;
            font-family:sans-serif;padding:30px">
            <h2 style="color:#7f5cff">INFO</h2>
            <p>Pabrik Neshi</p>
            <p>Progress peta disimpan berdasarkan pelanggan.</p>
            </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    // =========================
    // TAMBAH PELANGGAN
    // =========================

    private fun addCustomer() {
        val input = EditText(this)
        input.hint = "Nama pelanggan"

        AlertDialog.Builder(this)
            .setTitle("Tambah Pelanggan")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    toast("Nama pelanggan tidak boleh kosong")
                    return@setPositiveButton
                }

                if (customers.any {
                    it.equals(name, ignoreCase = true)
                }) {
                    toast("Nama pelanggan sudah tersedia")
                    return@setPositiveButton
                }

                customers.add(name)
                saveCustomerProgress(name, "")
                saveCustomers()
                switchCustomer(name)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // =========================
    // PILIH PELANGGAN
    // =========================

    private fun showCustomerList() {
        val names = customers.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pilih Pelanggan")
            .setSingleChoiceItems(
                names,
                customers.indexOf(activeCustomer)
            ) { dialog, which ->
                val selected = customers[which]
                dialog.dismiss()
                switchCustomer(selected)
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    // =========================
    // GANTI PELANGGAN
    // =========================

    private fun switchCustomer(newCustomer: String) {
        if (newCustomer == activeCustomer) return

        val oldCustomer = activeCustomer

        val saveScript = """
            (function() {
                try {
                    var value =
                        localStorage.getItem('$progressStorageKey') || '';

                    PabrikNeshiBridge.saveProgress(
                        ${JSONObject.quote(oldCustomer)},
                        value
                    );
                } catch(e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(saveScript) {
            activeCustomer = newCustomer
            saveCustomers()
            updateCustomerLabel()

            val savedProgress = getCustomerProgress(newCustomer)
            val quotedProgress = JSONObject.quote(savedProgress)

            val restoreScript = """
                (function() {
                    try {
                        var value = $quotedProgress;

                        if (value.length > 0) {
                            localStorage.setItem(
                                '$progressStorageKey',
                                value
                            );
                        } else {
                            localStorage.removeItem(
                                '$progressStorageKey'
                            );
                        }
                    } catch(e) {}
                })();
            """.trimIndent()

            webView.evaluateJavascript(restoreScript) {
                webView.loadUrl(mapUrl)
            }
        }
    }

    // =========================
    // PEMANTAU PROGRESS
    // =========================

    private fun installProgressWatcher() {
        val script = """
            (function() {
                try {
                    var key = '$progressStorageKey';

                    function saveCurrent(value) {
                        var customer =
                            PabrikNeshiBridge.getActiveCustomer();

                        PabrikNeshiBridge.saveProgress(
                            customer,
                            value || ''
                        );
                    }

                    saveCurrent(
                        localStorage.getItem(key) || ''
                    );

                    if (!window.__pabrikNeshiWatcher) {
                        window.__pabrikNeshiWatcher = true;

                        var originalSet = Storage.prototype.setItem;

                        Storage.prototype.setItem = function(k, v) {
                            originalSet.call(this, k, v);

                            if (
                                this === localStorage &&
                                k === key
                            ) {
                                saveCurrent(String(v));
                            }
                        };

                        var originalRemove = Storage.prototype.removeItem;

                        Storage.prototype.removeItem = function(k) {
                            originalRemove.call(this, k);

                            if (
                                this === localStorage &&
                                k === key
                            ) {
                                saveCurrent('');
                            }
                        };
                    }
                } catch(e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    // =========================
    // BRIDGE PELANGGAN
    // =========================

    inner class CustomerBridge {

        @JavascriptInterface
        fun saveProgress(customer: String, value: String) {
            this@MainActivity.saveCustomerProgress(
                customer,
                value
            )
        }

        @JavascriptInterface
        fun getActiveCustomer(): String {
            return activeCustomer
        }
    }

    // =========================
    // PEMERIKSA DATA PETA
    // =========================

    private fun inspectMapStorage() {
        if (!webView.url.orEmpty().startsWith(mapUrl)) {
            toast("Buka MAP terlebih dahulu")
            return
        }

        val script = """
            (function() {
                try {
                    var data = {
                        status: "Berhasil membaca penyimpanan",
                        localStorage: {},
                        sessionStorage: {},
                        progressValue: localStorage.getItem(
                            "aniidex.map-progress"
                        ),
                        indexedDBNames: []
                    };

                    for (var i = 0; i < localStorage.length; i++) {
                        var key = localStorage.key(i);
                        data.localStorage[key] =
                            localStorage.getItem(key);
                    }

                    for (var j = 0; j < sessionStorage.length; j++) {
                        var key2 = sessionStorage.key(j);
                        data.sessionStorage[key2] =
                            sessionStorage.getItem(key2);
                    }

                    function sendResult() {
                        AndroidStorageInspector.report(
                            JSON.stringify(data, null, 2)
                        );
                    }

                    // Kirim data langsung, tidak menunggu IndexedDB.
                    sendResult();

                    // Pemeriksaan IndexedDB hanya tambahan.
                    if (
                        window.indexedDB &&
                        typeof indexedDB.databases === "function"
                    ) {
                        indexedDB.databases().then(function(dbs) {
                            data.indexedDBNames = dbs.map(function(db) {
                                return db.name || "(tanpa nama)";
                            });
                        }).catch(function() {});
                    }
                } catch(e) {
                    AndroidStorageInspector.report(
                        "Pemeriksaan gagal: " + String(e)
                    );
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    inner class StorageInspector {

        @JavascriptInterface
        fun report(result: String) {
            runOnUiThread {
                val text = TextView(this@MainActivity)
                text.text = result
                text.textSize = 12f
                text.setTextColor(Color.WHITE)
                text.setPadding(16, 16, 16, 16)

                val scroll = ScrollView(this@MainActivity)
                scroll.addView(text)

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Data Penyimpanan AniIdex")
                    .setView(scroll)
                    .setPositiveButton("Tutup", null)
                    .show()
            }
        }
    }

    // =========================
    // SIMPAN SAAT APLIKASI JEDA
    // =========================

    override fun onPause() {
        if (::webView.isInitialized) {
            val script = """
                (function() {
                    try {
                        PabrikNeshiBridge.saveProgress(
                            PabrikNeshiBridge.getActiveCustomer(),
                            localStorage.getItem(
                                '$progressStorageKey'
                            ) || ''
                        );
                    } catch(e) {}
                })();
            """.trimIndent()

            webView.evaluateJavascript(script, null)
        }

        super.onPause()
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
