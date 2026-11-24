package com.example.data.explore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiInterpretation(val analysis:String,val citations:List<String>,val model:String)

/** Upstream model credentials remain on the gateway, never in the Android build. */
object GroundedAiClient {
    fun validBaseUrl(value:String):Boolean=runCatching {
        val url=URL(value)
        url.protocol=="https"&&url.host.isNotBlank()&&url.userInfo==null&&url.query==null&&url.ref==null
    }.getOrDefault(false)

    fun parseResponse(raw:String,allowedIds:Set<String>):AiInterpretation {
        val o=JSONObject(raw)
        require(o.getString("classification")=="ai_interpretation_unverified")
        val text=o.getString("analysis");require(text.length in 1..3000)
        val citations=o.getJSONArray("citations")
        val ids=(0 until citations.length()).map {citations.getString(it)}
        require(ids.isNotEmpty()&&ids.all {it in allowedIds}) {"The AI cited evidence outside this question"}
        return AiInterpretation(text,ids.distinct(),o.optString("model","Configured model").take(100))
    }
    suspend fun ask(baseUrl:String,token:String,answer:PitWallAnswer):AiInterpretation=withContext(Dispatchers.IO) {
        require(validBaseUrl(baseUrl)&&token.isNotBlank())
        require(token.length<=512&&token.all {it.code in 33..126}) {"Invalid gateway token format"}
        val evidence=JSONArray().put(JSONObject().put("id","source").put("kind","Observed").put("text",answer.source))
        answer.evidence.forEachIndexed {i,e->evidence.put(JSONObject().put("id","e$i").put("kind",e.kind.name).put("text",e.text))}
        val allowed=(answer.evidence.indices.map {"e$it"}+"source").toSet()
        val body=JSONObject().put("question",answer.question).put("evidence",evidence).toString().toByteArray(Charsets.UTF_8)
        require(body.size<=24000)
        val connection=URL(baseUrl.trimEnd('/')+"/v1/pit-wall").openConnection() as HttpURLConnection
        try {
            connection.requestMethod="POST";connection.connectTimeout=10000;connection.readTimeout=20000
            connection.instanceFollowRedirects=false
            connection.setRequestProperty("Authorization","Bearer $token")
            connection.setRequestProperty("Content-Type","application/json")
            connection.doOutput=true;connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use {it.write(body)}
            val code=connection.responseCode
            check(code in 200..299) {when(code){401->"Gateway token was rejected. Check the connection settings.";503->"AI service is unavailable or unconfigured. Local evidence remains available.";else->"AI gateway returned HTTP $code"}}
            val raw=connection.inputStream.bufferedReader().use {reader->
                val buffer=CharArray(2048);val out=StringBuilder()
                while(true){val count=reader.read(buffer);if(count<0)break;check(out.length+count<=16000){"AI response exceeds safe limit"};out.append(buffer,0,count)}
                out.toString()
            }
            parseResponse(raw,allowed)
        } finally {connection.disconnect()}
    }
}
