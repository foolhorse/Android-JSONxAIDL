package me.machao.jsonxaidl.library.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

import java.io.IOException
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.TypeAdapterFactory



/**
 * Date  2019/2/6
 *
 * @author charliema
 */
class ClassTypeAdapter : TypeAdapter<Class<*>>() {

    @Throws(IOException::class)
    override fun write(jsonWriter: JsonWriter, clazz: Class<*>?) {
        if (clazz == null) {
            jsonWriter.nullValue()
            return
        }
        jsonWriter.value(clazz.name)
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): Class<*>? {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull()
            return null
        }
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName(jsonReader.nextString())
        } catch (exception: ClassNotFoundException) {
            throw IOException(exception)
        }

        return clazz
    }
}

class ClassTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        return if (!Class::class.java.isAssignableFrom(typeToken.rawType)) {
            null
        } else ClassTypeAdapter() as TypeAdapter<T>
    }
}