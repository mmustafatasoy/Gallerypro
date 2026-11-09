package com.atasoy.gallerypro.feature.collections

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * Gizli albümü parmak izi, yüz veya ekran kilidiyle açar. Android'in yerleşik
 * BiometricPrompt'u kullanılır (ek kütüphane yok). Cihazda kilit yoksa doğrudan açılır.
 */
internal object DeviceUnlock {

    fun authenticate(activity: Activity, title: String, subtitle: String, onResult: (Boolean) -> Unit) {
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isDeviceSecure) {
            onResult(true)
            return
        }
        val builder = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        runCatching {
            builder.build().authenticate(
                CancellationSignal(),
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onResult(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        onResult(false)
                    }
                }
            )
        }.onFailure { onResult(false) }
    }
}
