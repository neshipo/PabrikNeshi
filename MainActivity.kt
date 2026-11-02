
package com.neshi.pabrikneshi

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val mapUrl = "https://aniidex.com/id/map/"
    private val storageKey = "aniidex.map-progress"
    private val prefsName = "PabrikNeshiCustomers"

    private lateinit var webView: WebView
    private lateinit var mainLayout: LinearLayout
    private lateinit var customerText: TextView

    private val prefs by lazy {
        getSharedPreferences(prefsName, MODE_PRIVATE)
    }

    private var activeCustomer = "Pelanggan Lama"
    private var customers = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCustomerData()
        buildInterface()
    }

    private fun loadCustomerData() {
        val saved = prefs.getString("customer_list", null)

        if (saved == null) {
            customers = mutableListOf("Pelanggan Lama")
            activeCustomer = "Pelanggan Lama"
            saveCustomerData()
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

    private fun saveCustomerData() {
        prefs.edit()
            .putString("customer_list", JSONArray(customers).toString())
            .putString("active_customer", activeCustomer)
            .apply()
    }

    private fun progressKey(customer: String): String {
        return "progress_$customer"
    }

    private fun getProgress(customer: String): String {
        return prefs.getString(progressKey(customer), "") ?: ""
    }

    private fun saveProgress(customer: String, value: String) {
        prefs.edit()
            .putString(progressKey(customer), value)
            .apply()
    }

    private fun buildInterface() {
        mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setBackgroundColor(Color.rgb(5, 8, 22))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(16, 12, 16, 10)
        header.setBackgroundColor(Color.rgb(17, 24, 39))

        val title = TextView(this)
        title.text = "PABRIK NESHI"
        title.textSize = 21f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 5, 0, 10)
        header.addView(title)

        customerText = TextView(this)
        customerText.textSize = 14f
        customerText.setTextColor(Color.WHITE)
        customerText.gravity = Gravity.CENTER
        customerText.setPadding(0, 5, 0, 10)
        header.addView(customerText)

        val customerButtons = LinearLayout(this)
        customerButtons.orientation = LinearLayout.HORIZONTAL

        customerButtons.addView(
            makeButton("Pilih Pelanggan") {
                showCustomerList()
            },
            weightedParams()
        )

        customerButtons.addView(
            makeButton("+ Tambah") {
                showAddCustomer()
            },
            weightedParams()
        )

        header.addView(customerButtons)
        mainLayout.addView(header)

        val navigation = LinearLayout(this)
        navigation.orientation = LinearLayout.HORIZONTAL
        navigation.setPadding(5, 5, 5, 5)

        navigation.addView(
            makeButton("HOME") { showHome() },
            weightedParams()
        )

        navigation.addView(
            makeButton("MAP") { showMap() },
            weightedParams()
        )

        navigation.addView(
            makeButton("INFO") { showInfo() },
            weightedParams()
        )

        mainLayout.addView(navigation)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.addJavascriptInterface(
            CustomerBridge(),
            "PabrikNeshiBridge"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (url?.startsWith(mapUrl) == true) {
                    installProgressWatcher()
                }
            }
        }

        mainLayout.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(mainLayout)

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
        label: String,
        action: () -> Unit
    ): Button {
        val button = Button(this)
        button.text = label
        button.setTextColor(Color.WHITE)
        button.setBackgroundColor(Color.rgb(65, 75, 120))
        button.setOnClickListener { action() }
        return button
    }

    private fun updateCustomerLabel() {
        customerText.text = "Pelanggan aktif: $activeCustomer"
    }

    private fun showHome() {
        webView.loadDataWithBaseURL(
            null,
            """
            <html>
            <body style="background:#050816;color:white;
            font-family:sans-serif;text-align:center;padding:40px">
            <h1 style="color:#7f5cff">PABRIK NESHI</h1>
            <p>Game Map Progress Manager</p>
            <p>Pilih menu MAP untuk membuka AniIdex.</p>
            </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun showMap() {
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
            <p>Progress peta disimpan berdasarkan pelanggan aktif.</p>
            </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun showAddCustomer() {
        val input = EditText(this)
        input.hint = "Nama pelanggan"

        AlertDialog.Builder(this)
            .setTitle("Tambah Pelanggan")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Nama tidak boleh kosong",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (customers.any { it.equals(name, true) }) {
                    Toast.makeText(
                        this,
                        "Nama pelanggan sudah ada",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                customers.add(name)
                saveProgress(name, "")
                saveCustomerData()
                switchCustomer(name)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

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

    private fun switchCustomer(newCustomer: String) {
        if (newCustomer == activeCustomer) return

        if (!::webView.isInitialized) {
            activeCustomer = newCustomer
            saveCustomerData()
            updateCustomerLabel()
            return
        }

        val oldName = JSONObject.quote(activeCustomer)

        val saveScript = """
            (function() {
                try {
                    var value =
                        localStorage.getItem('$storageKey') || '';

                    PabrikNeshiBridge.saveProgress(
                        $oldName,
                        value
                    );
                } catch(e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(saveScript) {
            activeCustomer = newCustomer
            saveCustomerData()
            updateCustomerLabel()

            val value = JSONObject.quote(getProgress(newCustomer))

            val restoreScript = """
                (function() {
                    try {
                        localStorage.setItem(
                            '$storageKey',
                            $value
                        );
                    } catch(e) {}
                })();
            """.trimIndent()

            webView.evaluateJavascript(restoreScript) {
                webView.loadUrl(mapUrl)
            }
        }
    }

    private fun installProgressWatcher() {
        val script = """
            (function() {
                try {
                    var key = '$storageKey';

                    function saveCurrentProgress(value) {
                        var customer =
                            PabrikNeshiBridge.getActiveCustomer();

                        PabrikNeshiBridge.saveProgress(
                            customer,
                            value || ''
                        );
                    }

                    saveCurrentProgress(
                        localStorage.getItem(key) || ''
                    );

                    if (!window.__pabrikNeshiWatcher) {
                        window.__pabrikNeshiWatcher = true;

                        var originalSet = Storage.prototype.setItem;

                        Storage.prototype.setItem = function(k, v) {
                            originalSet.call(this, k, v);

                            if (this === localStorage && k === key) {
                                saveCurrentProgress(String(v));
                            }
                        };

                        var originalRemove =
                            Storage.prototype.removeItem;

                        Storage.prototype.removeItem = function(k) {
                            originalRemove.call(this, k);

                            if (this === localStorage && k === key) {
                                saveCurrentProgress('');
                            }
                        };
                    }
                } catch(e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }

    inner class CustomerBridge {

        @JavascriptInterface
        fun saveProgress(customer: String, value: String) {
            this@MainActivity.saveProgress(customer, value)
        }

        @JavascriptInterface
        fun getActiveCustomer(): String {
            return activeCustomer
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            val script = """
                (function() {
                    try {
                        var customer =
                            PabrikNeshiBridge.getActiveCustomer();

                        PabrikNeshiBridge.saveProgress(
                            customer,
                            localStorage.getItem('$storageKey') || ''
                        );
                    } catch(e) {}
                })();
            """.trimIndent()

            webView.evaluateJavascript(script, null)
        }

        super.onPause()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
