package top.zoyn.molexiptc.util

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import kotlin.random.Random

object HttpUtils {
    private const val CONNECT_TIME_OUT = 3000
    private const val READ_TIME_OUT = 3000
    private val IP_Header = listOf(
        "218.78.", "221.14.", "202.100.",
        "123.101.", "122.198.", "122.119.",
        "119.16.", "118.239.", "116.95.",
        "116.13.", "114.135.", "114.132.",
        "114.28.", "113.31.", "124.89."
    )

    fun get(url: String, mapParam: Map<String, String>?, block: HttpURLConnection.() -> Unit): Map<String, String> {
        val urlParam = if (mapParam == null) url else "${url}?${converMap2String(mapParam)}"
        val connection = buildURLConnection(urlParam)
        block(connection)
        val code = connection.responseCode
        val inStream = (if (code == 200) connection.inputStream else connection.errorStream)
            ?: return mapOf("code" to code.toString(), "result" to "")
        val result = inStream.bufferedReader().lineSequence().joinToString()
        connection.disconnect()
        return mapOf("code" to code.toString(), "result" to result)
    }

    fun get(url: String, mapParam: Map<String, String>?): Map<String, String> {
        val urlParam = if (mapParam == null) url else "${url}?${converMap2String(mapParam)}"
        val connection = buildURLConnection(urlParam)
        connection.setContentType()
        connection.setRequestProperty("X-Real-IP", getRandomIp())
        //connection.setRequestProperty("Cookie", "appver=2.7.1;Referer: http://music.163.com/")
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"
        )
        connection.connect()
        val code = connection.responseCode
        val inStream = (if (code == 200) connection.inputStream else connection.errorStream)
            ?: return mapOf("code" to code.toString(), "result" to "")
        val result = inStream.bufferedReader().lineSequence().joinToString()
        connection.disconnect()
        return mapOf("code" to code.toString(), "result" to result)
    }

    fun post(url: String, param: Map<String, String>): Map<String, String> {
        val connection = buildURLConnection(url, "POST")
        connection.setContentType(1)
        connection.setRequestProperty("X-Real-IP", getRandomIp())
        connection.setRequestProperty("Cookie", "appver=2.7.1;Referer: http://music.163.com/")
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36"
        )
        connection.connect()
        connection.outputStream.let {
            it.write(converMap2String(param).toByteArray())
            it.flush()
            it.close()
        }
        val code = connection.responseCode
        val inStream = if (code == 200) connection.inputStream else connection.errorStream
        val result = inStream.bufferedReader().lineSequence().joinToString()
        connection.disconnect()
        return mapOf("code" to code.toString(), "result" to result)
    }

    private fun buildURLConnection(urlLink: String, requestMethod: String = "GET"): HttpURLConnection {
        val url = URL(urlLink)
        val connection = url.openConnection() as HttpURLConnection
        connection.let {
            it.requestMethod = requestMethod
            it.connectTimeout = CONNECT_TIME_OUT
            it.readTimeout = READ_TIME_OUT
            it.doInput = true
            it.doOutput = true
            it.useCaches = requestMethod == "GET"
            it.instanceFollowRedirects = true
        }
        return connection
    }

    private fun URLConnection.setContentType(type: Int = 0) {
        val typeString = when (type) {
            1 -> "application/x-www-form-urlencoded"
            2 -> "application/x-java-serialized-object"
            else -> "application/json;charset=UTF-8"
        }
        this.setRequestProperty("Content-Type", typeString)
    }

    private fun converMap2String(param: Map<String, String>, isEncode: Boolean = true): String {
        return param.keys.joinToString(separator = "&") { key ->
            val value = if (isEncode) URLEncoder.encode(param[key], "UTF-8") else param[key]
            "$key=$value"
        }
    }

    private fun getRandomIp(): String {
        return IP_Header.random() +
                Random.nextInt(0, 256) +
                "." +
                Random.nextInt(0, 256)
    }
}