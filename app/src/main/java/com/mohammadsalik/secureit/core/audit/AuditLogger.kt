package com.mohammadsalik.secureit.core.audit

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface AuditLogger {
    fun log(event: String, attrs: Map<String, String> = emptyMap())
}

@Singleton
class LogcatAuditLogger @Inject constructor() : AuditLogger {
    override fun log(event: String, attrs: Map<String, String>) {
        Log.i("SecureVaultAudit", "$event ${attrs.entries.joinToString(", ") { it.key + "=" + it.value }}")
    }
}
