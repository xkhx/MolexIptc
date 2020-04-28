package top.zoyn.molexiptc

import com.google.gson.Gson
import org.junit.Test
import top.zoyn.molexiptc.util.HttpUtils
import kotlin.test.assertEquals

class HelloTest {

    private val json = Gson()


    @Test
    fun testGetIpLocation() {
        val ip = HttpUtils
            .get(
                "http://opendata.baidu.com/api.php?query=ip地址&co=&resource_id=6006&oe=utf8",
                null
            )
        val code = (ip["code"] ?: "-1").toInt()
        val location = json.fromJson(ip["result"], Location::class.java)
        println(location.status)
        println(location.data[0].location)
    }

    private class Location {
        val status = -1
        val data = listOf<LocationData>()
    }

    private class LocationData {
        val location = "获取失败"
    }

    @Test
    fun testHitokoto() {
        val text = HttpUtils
            .get(
                "https://v1.hitokoto.cn/",
                null
            )
        if (text["code"]?.toInt() != 200) {
            println("数据错误")
        }
        val hitokotoData = json.fromJson(text["result"], HitokotoData::class.java)
        println(hitokotoData.from)
        println(hitokotoData.hitokoto)
    }

    private class HitokotoData() {
        val hitokoto = "数据错误"
        val from = "数据错误"
    }
}
