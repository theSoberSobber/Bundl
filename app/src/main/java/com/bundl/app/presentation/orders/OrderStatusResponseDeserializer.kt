package com.bundl.app.presentation.orders

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class OrderStatusResponseDeserializer : JsonDeserializer<OrderStatusResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): OrderStatusResponse {
        val jsonObject = json.asJsonObject
        
        // Log the raw JSON object to see exactly what we're working with
        Log.d("BUNDL_DESERIALIZER", "Raw JSON: $jsonObject")
        
        // Check for phoneNumerMap or phoneNumberMap
        val phoneMap = if (jsonObject.has("phoneNumerMap")) {
            Log.d("BUNDL_DESERIALIZER", "Found 'phoneNumerMap' in JSON")
            jsonObject.getAsJsonObject("phoneNumerMap")
        } else if (jsonObject.has("phoneNumberMap")) {
            Log.d("BUNDL_DESERIALIZER", "Found 'phoneNumberMap' in JSON")
            jsonObject.getAsJsonObject("phoneNumberMap")
        } else {
            Log.d("BUNDL_DESERIALIZER", "No phone map found in JSON")
            null
        }
        
        // Convert phoneMap to Map<String, Int>
        val phoneNumberMap = if (phoneMap != null) {
            val map = mutableMapOf<String, Int>()
            for (entry in phoneMap.entrySet()) {
                val key = entry.key
                val value = entry.value.asInt
                map[key] = value
                Log.d("BUNDL_DESERIALIZER", "Added phone entry: $key -> $value")
            }
            map
        } else {
            null
        }
        
        // Get note field
        val note = if (jsonObject.has("note") && !jsonObject.get("note").isJsonNull) {
            jsonObject.get("note").asString
        } else {
            null
        }
        
        // Extract other fields
        return OrderStatusResponse(
            id = jsonObject.get("id").asString,
            amountNeeded = jsonObject.get("amountNeeded").asString,
            totalPledge = jsonObject.get("totalPledge").asString,
            totalUsers = jsonObject.get("totalUsers").asInt,
            longitude = jsonObject.get("longitude").asString,
            latitude = jsonObject.get("latitude").asString,
            creatorId = jsonObject.get("creatorId").asString,
            platform = jsonObject.get("platform").asString,
            status = jsonObject.get("status").asString,
            pledgeMap = getPledgeMap(jsonObject),
            phoneNumerMap = phoneNumberMap,
            note = note
        )
    }
    
    private fun getPledgeMap(jsonObject: JsonObject): Map<String, Int> {
        if (!jsonObject.has("pledgeMap")) return emptyMap()
        
        val pledgeMapJson = jsonObject.getAsJsonObject("pledgeMap")
        val pledgeMap = mutableMapOf<String, Int>()
        
        for (entry in pledgeMapJson.entrySet()) {
            pledgeMap[entry.key] = entry.value.asInt
        }
        
        return pledgeMap
    }
} 