package com.ariabridge.lite

import android.content.Context
import android.os.Build
import android.sun.misc.BASE64Encoder
import android.sun.security.provider.X509Factory
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.PrivateKeyUsageExtension
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random

/**
 * Minimal ADB key/certificate manager adapted from DP-Hridayan/aShellYou.
 * The upstream repository is GPL-3.0 and its libadb sources are GPL-3.0-or-later OR Apache-2.0.
 */
class AdbConnectionManager private constructor(context: Context) : AbsAdbConnectionManager() {
    private var privateKey: PrivateKey?
    private var certificate: Certificate?
    private val adbDeviceName = "AriaBridgeLite"

    init {
        api = Build.VERSION.SDK_INT
        privateKey = readPrivateKeyFromFile(context)
        certificate = readCertificateFromFile(context)
        if (privateKey == null || certificate == null) {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
            val generated = generator.generateKeyPair()
            val publicKey = generated.public
            privateKey = generated.private

            val algorithmName = "SHA512withRSA"
            val x500Name = X500Name("CN=$adbDeviceName")
            val notBefore = Date()
            val notAfter = Date(System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L)
            val extensions = CertificateExtensions().apply {
                set("SubjectKeyIdentifier", SubjectKeyIdentifierExtension(KeyIdentifier(publicKey).identifier))
                set("PrivateKeyUsage", PrivateKeyUsageExtension(notBefore, notAfter))
            }
            val info = X509CertInfo().apply {
                set("version", CertificateVersion(2))
                set("serialNumber", CertificateSerialNumber(Random().nextInt() and Int.MAX_VALUE))
                set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithmName)))
                set("subject", CertificateSubjectName(x500Name))
                set("key", CertificateX509Key(publicKey))
                set("validity", CertificateValidity(notBefore, notAfter))
                set("issuer", CertificateIssuerName(x500Name))
                set("extensions", extensions)
            }
            certificate = X509CertImpl(info).apply { sign(privateKey, algorithmName) }
            writePrivateKeyToFile(context, privateKey!!)
            writeCertificateToFile(context, certificate!!)
        }
    }

    override fun getPrivateKey(): PrivateKey = privateKey!!
    override fun getCertificate(): Certificate = certificate!!
    override fun getDeviceName(): String = adbDeviceName

    companion object {
        @Volatile private var INSTANCE: AbsAdbConnectionManager? = null

        fun getInstance(context: Context): AbsAdbConnectionManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AdbConnectionManager(context.applicationContext).also { INSTANCE = it }
        }

        @Throws(IOException::class, CertificateException::class)
        private fun readCertificateFromFile(context: Context): Certificate? {
            val file = File(context.filesDir, "cert.pem")
            if (!file.exists()) return null
            FileInputStream(file).use { return CertificateFactory.getInstance("X.509").generateCertificate(it) }
        }

        @Throws(CertificateEncodingException::class, IOException::class)
        private fun writeCertificateToFile(context: Context, certificate: Certificate) {
            val file = File(context.filesDir, "cert.pem")
            val encoder = BASE64Encoder()
            FileOutputStream(file).use { os ->
                os.write(X509Factory.BEGIN_CERT.toByteArray(StandardCharsets.UTF_8))
                os.write('\n'.code)
                encoder.encode(certificate.encoded, os)
                os.write('\n'.code)
                os.write(X509Factory.END_CERT.toByteArray(StandardCharsets.UTF_8))
            }
        }

        @Throws(IOException::class, NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        private fun readPrivateKeyFromFile(context: Context): PrivateKey? {
            val file = File(context.filesDir, "private.key")
            if (!file.exists()) return null
            val bytes = ByteArray(file.length().toInt())
            FileInputStream(file).use { it.read(bytes) }
            val keyFactory = KeyFactory.getInstance("RSA")
            val spec: EncodedKeySpec = PKCS8EncodedKeySpec(bytes)
            return keyFactory.generatePrivate(spec)
        }

        private fun writePrivateKeyToFile(context: Context, key: PrivateKey) {
            FileOutputStream(File(context.filesDir, "private.key")).use { it.write(key.encoded) }
        }
    }
}
