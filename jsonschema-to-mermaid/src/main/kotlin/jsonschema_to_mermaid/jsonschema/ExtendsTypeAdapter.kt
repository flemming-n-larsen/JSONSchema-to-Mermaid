package jsonschema_to_mermaid.jsonschema

import com.google.gson.*
import java.lang.reflect.Type

class ExtendsTypeAdapter : JsonDeserializer<Extends> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Extends {
        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> Extends.Ref(json.asString)
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val ref = obj["\$ref"]?.asString ?: throw JsonParseException($$"Missing $ref in extends object")
                Extends.Object(ref)
            }
            else -> throw JsonParseException("Invalid extends value: $json")
        }
    }
}
