package com.example.proyectodepspot.utils

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class TwilioService {
    companion object {
        private const val ACCOUNT_SID = "AC3686b1bdf8eab370f581439233ba1407"
        private const val AUTH_TOKEN = "b11ec733a7980e21f562bebcc1643a09"
        private const val MESSAGING_SERVICE_SID = "MGdc8d256f858f6cdf8bafdff6f1721fc3"
        private const val BASE_URL = "https://api.twilio.com/2010-04-01/Accounts/$ACCOUNT_SID/Messages.json"
    }

    fun enviarSMS(numeroDestino: String, mensaje: String, callback: (Boolean, String?) -> Unit) {
        val client = OkHttpClient()
        
        val formBody = FormBody.Builder()
            .add("To", numeroDestino)
            .add("MessagingServiceSid", MESSAGING_SERVICE_SID)
            .add("Body", mensaje)
            .build()

        val request = Request.Builder()
            .url(BASE_URL)
            .post(formBody)
            .addHeader("Authorization", Credentials.basic(ACCOUNT_SID, AUTH_TOKEN))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val success = response.isSuccessful
                callback(success, responseBody)
            }
        })
    }
} 