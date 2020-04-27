package top.zoyn.molexiptc

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.LightApp
import net.mamoe.mirai.message.data.Message
import top.zoyn.molexiptc.util.HttpUtils

class MolexIptc : PluginBase() {

    private val json = Gson()
    private val coolDown = mutableMapOf<Long, Long>()

    private fun Group.isCooling(cooling: Long): Boolean {
        val current = System.currentTimeMillis()
        val v = coolDown[this.id] ?: kotlin.run {
            coolDown[this.id] = current + cooling
            return false
        }
        return if (current > v) {
            coolDown[this.id] = current + cooling
            false
        } else true
    }

    private fun getIpLocation(ip: String): String {
        val result = HttpUtils
            .get(
                "http://opendata.baidu.com/api.php?query=$ip&co=&resource_id=6006&oe=utf8",
                null
            )
        val code = (result["code"] ?: "-1").toInt()
        val data = result["result"] ?: ""
        if (code != 200 || data.isEmpty()) {
            return "获取失败"
        }
        val location = json.fromJson(data, Location::class.java)
        return if (location.data.isEmpty()) "获取失败" else location.data[0].location
    }

    private class Location {
        val data = listOf<LocationData>()
    }
    private class LocationData {
        val location = "获取失败"
    }

    private fun hitokoto(): HitokotoData {
        val text = HttpUtils
            .get(
                "https://v1.hitokoto.cn/",
                null
            )
        if (text["code"]?.toInt() != 200) {
            return HitokotoData()
        }
        return json.fromJson(text["result"], HitokotoData::class.java)
    }

    private class HitokotoData() {
        val hitokoto = "数据错误"
        val from = "数据错误"
    }

    private fun buildJsonMessage(time: Long): Message {
        val data = hitokoto()
        data.from
        data.hitokoto
        return LightApp(
            "{\"app\":\"com.tencent.structmsg\"," +
                    "\"desc\":\"IP探测\",\"view\":\"news\"," +
                    "\"ver\":\"0.0.0.1\",\"prompt\":\"\"," +
                    "\"meta\":{\"news\":{\"title\":\"${data.from}\"," +
                    "\"desc\":\"${data.hitokoto}\"," +
                    "\"preview\":\"http://url/1.php?username=$time\"," +
                    "\"tag\":\"IP探测 - 10秒后回复\",\"jumpUrl\":\"https://www.zoyn.top\"," +
                    "\"appid\":100446242,\"app_type\":1,\"action\":\"\"," +
                    "\"source_url\":\"\",\"source_icon\":\"\",\"android_pkg_name\":\"\"}}}"
        )
    }

    override fun onEnable() {

        subscribeGroupMessages {
            startsWith("#", removePrefix = true) {
                val command = it.split(" ")
                val root = command[0]
                //val args = if (command.size > 1) command.subList(1, command.size) else listOf()
                when (root.toLowerCase()) {
                    "探测" -> {
                        if (subject.isCooling(60000)) {
                            reply("[探测] 当前还在冷却哦")
                            return@startsWith
                        }
                        val now = System.currentTimeMillis()
                        reply(buildJsonMessage(now))

                        launch {
                            delay(10000)
                            val text = HttpUtils
                                .get(
                                    "http://url/$now.txt",
                                    null
                                )
                            val code = (text["code"] ?: "-1").toInt()
                            val ips = (text["result"] ?: "").removeSuffix("#")
                            if (code != 200 || ips.isEmpty()) {
                                reply("[探测] 探测失败")
                                return@launch
                            }
                            val listIp = ips.split("#")
                            val stringBuilder = StringBuilder("探测结果 ->\n")
                            for (ip in listIp) {
                                val location = getIpLocation(ip)
                                val splitIp = ip.split(".")
                                val processing = splitIp[0] + "." + splitIp[1] + ".*.*"
                                stringBuilder.append("$processing >> $location\n")
                            }
                            reply(stringBuilder.toString().removeSuffix("\n"))
                        }
                    }
                }
            }
        }
    }
}