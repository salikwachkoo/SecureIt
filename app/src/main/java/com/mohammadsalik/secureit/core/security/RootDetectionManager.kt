package com.mohammadsalik.secureit.core.security

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

object RootDetectionManager {

    private val knownRootApps = listOf(
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "eu.chainfire.supersu",
        "com.topjohnwu.magisk",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot"
    )

    private val knownRootPackages = listOf(
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "eu.chainfire.supersu",
        "com.topjohnwu.magisk",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot"
    )

    private val dangerousProps = mapOf(
        "ro.debuggable" to "1",
        "ro.secure" to "0",
        "ro.build.type" to "userdebug",
        "ro.build.type" to "eng"
    )

    private val suPaths = listOf(
        "/system/app/Superuser.apk",
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        "/system/su",
        "/system/bin/.ext/.su",
        "/system/etc/init.d/99SuperSUDaemon",
        "/dev/com.koushikdutta.superuser.daemon/",
        "/system/etc/.has_su_daemon",
        "/system/etc/.installed_su_daemon",
        "/dev/.mount_rw/",
        "/system/xbin/mu"
    )

    fun isDeviceRooted(context: Context): RootDetectionResult {
        return try {
            val checks = mutableListOf<RootCheck>()

            // Check for SU binary
            val suCheck = checkForSuBinary()
            checks.add(suCheck)

            // Check for root apps
            val rootAppsCheck = checkForRootApps(context)
            checks.add(rootAppsCheck)

            // Check for dangerous properties
            val propsCheck = checkDangerousProperties()
            checks.add(propsCheck)

            // Check for test keys
            val testKeysCheck = checkForTestKeys()
            checks.add(testKeysCheck)

            // Check for busybox
            val busyboxCheck = checkForBusybox()
            checks.add(busyboxCheck)

            // Check for RW paths
            val rwPathsCheck = checkForRWPaths()
            checks.add(rwPathsCheck)

            // Check for emulator
            val emulatorCheck = checkForEmulator()
            checks.add(emulatorCheck)

            val isRooted = checks.any { it.isRooted }
            val rootIndicators = checks.filter { it.isRooted }

            RootDetectionResult(
                isRooted = isRooted,
                checks = checks,
                rootIndicators = rootIndicators,
                riskLevel = calculateRiskLevel(rootIndicators)
            )
        } catch (e: Exception) {
            Log.w("RootDetection", "Error in root detection: ${e.message}")
            RootDetectionResult(
                isRooted = false,
                checks = emptyList(),
                rootIndicators = emptyList(),
                riskLevel = RootRiskLevel.LOW
            )
        }
    }

    private fun checkForSuBinary(): RootCheck {
        return try {
            val suExists = suPaths.any { File(it).exists() }
            RootCheck(
                name = "SU Binary Check",
                isRooted = suExists,
                description = if (suExists) "SU binary found" else "No SU binary detected"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "SU Binary Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun checkForRootApps(context: Context): RootCheck {
        return try {
            val packageManager = context.packageManager
            val installedApps = packageManager.getInstalledPackages(0)

            val rootAppsFound = installedApps.any { packageInfo ->
                knownRootApps.contains(packageInfo.packageName)
            }

            RootCheck(
                name = "Root Apps Check",
                isRooted = rootAppsFound,
                description = if (rootAppsFound) "Root apps detected" else "No root apps found"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "Root Apps Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun checkDangerousProperties(): RootCheck {
        return try {
            val dangerousPropsFound = dangerousProps.any { (key, value) ->
                getSystemProperty(key) == value
            }

            RootCheck(
                name = "Dangerous Properties Check",
                isRooted = dangerousPropsFound,
                description = if (dangerousPropsFound) "Dangerous properties found" else "No dangerous properties"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "Dangerous Properties Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun checkForTestKeys(): RootCheck {
        return try {
            val testKeys = listOf(
                "ro.build.fingerprint" to "test-keys",
                "ro.build.tags" to "test-keys"
            )

            val testKeysFound = testKeys.any { (key, value) ->
                getSystemProperty(key)?.contains(value) == true
            }

            RootCheck(
                name = "Test Keys Check",
                isRooted = testKeysFound,
                description = if (testKeysFound) "Test keys detected" else "No test keys"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "Test Keys Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun checkForBusybox(): RootCheck {
        return try {
            val busyboxPaths = listOf(
                "/system/bin/busybox",
                "/system/xbin/busybox",
                "/sbin/busybox",
                "/system/bin/.ext/busybox"
            )

            val busyboxFound = busyboxPaths.any { File(it).exists() }

            RootCheck(
                name = "Busybox Check",
                isRooted = busyboxFound,
                description = if (busyboxFound) "Busybox found" else "No busybox"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "Busybox Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun checkForRWPaths(): RootCheck {
        return try {
            val rwPaths = listOf(
                "/system",
                "/vendor",
                "/system/bin",
                "/system/xbin"
            )

            val rwFound = rwPaths.any { path ->
                val file = File(path)
                file.exists() && file.canWrite()
            }

            RootCheck(
                name = "RW Paths Check",
                isRooted = rwFound,
                description = if (rwFound) "Writable system paths found" else "No writable system paths"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "RW Paths Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun checkForEmulator(): RootCheck {
        return try {
            val emulatorProps = listOf(
                "ro.kernel.qemu" to "1",
                "ro.hardware" to "goldfish",
                "ro.hardware" to "ranchu",
                "ro.hardware" to "vbox86",
                "ro.product.cpu.abi" to "x86",
                "ro.product.cpu.abi2" to "x86"
            )

            val emulatorDetected = emulatorProps.any { (key, value) ->
                getSystemProperty(key) == value
            }

            RootCheck(
                name = "Emulator Check",
                isRooted = emulatorDetected,
                description = if (emulatorDetected) "Emulator detected" else "No emulator"
            )
        } catch (e: Exception) {
            RootCheck(
                name = "Emulator Check",
                isRooted = false,
                description = "Check failed: ${e.message}"
            )
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            process.waitFor()
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateRiskLevel(rootIndicators: List<RootCheck>): RootRiskLevel {
        return when {
            rootIndicators.size >= 3 -> RootRiskLevel.CRITICAL
            rootIndicators.size == 2 -> RootRiskLevel.HIGH
            rootIndicators.size == 1 -> RootRiskLevel.MEDIUM
            else -> RootRiskLevel.LOW
        }
    }

    fun shouldBlockApp(context: Context): Boolean {
        return try {
            val result = isDeviceRooted(context)
            result.isRooted && result.riskLevel in listOf(RootRiskLevel.HIGH, RootRiskLevel.CRITICAL)
        } catch (e: Exception) {
            Log.w("RootDetection", "Error in shouldBlockApp: ${e.message}")
            false
        }
    }

    fun getSecurityRecommendations(context: Context): List<String> {
        return try {
            val result = isDeviceRooted(context)
            val recommendations = mutableListOf<String>()

            when (result.riskLevel) {
                RootRiskLevel.CRITICAL -> {
                    recommendations.add("🚨 CRITICAL: Device appears to be rooted")
                    recommendations.add("🛑 App access blocked for security")
                    recommendations.add("🔒 Use unrooted device for maximum security")
                }
                RootRiskLevel.HIGH -> {
                    recommendations.add("⚠️ HIGH: Multiple root indicators detected")
                    recommendations.add("🔒 Consider using unrooted device")
                    recommendations.add("🛡️ Enable additional security measures")
                }
                RootRiskLevel.MEDIUM -> {
                    recommendations.add("⚠️ MEDIUM: Some root indicators detected")
                    recommendations.add("🔒 Proceed with caution")
                    recommendations.add("🛡️ Monitor for suspicious activity")
                }
                RootRiskLevel.LOW -> {
                    recommendations.add("✅ LOW: No significant root indicators")
                    recommendations.add("🔒 Device appears secure")
                }
            }

            if (result.rootIndicators.isNotEmpty()) {
                recommendations.add("📋 Root indicators found:")
                result.rootIndicators.forEach { check ->
                    recommendations.add("  • ${check.name}: ${check.description}")
                }
            }

            recommendations
        } catch (e: Exception) {
            Log.w("RootDetection", "Error getting recommendations: ${e.message}")
            listOf("⚠️ Security check failed - proceeding with caution")
        }
    }
}

data class RootDetectionResult(
    val isRooted: Boolean,
    val checks: List<RootCheck>,
    val rootIndicators: List<RootCheck>,
    val riskLevel: RootRiskLevel
)

data class RootCheck(
    val name: String,
    val isRooted: Boolean,
    val description: String
)

enum class RootRiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}
