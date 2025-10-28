package com.ts.platform.reflection.serializers;

import com.google.gson.*;
import com.ts.platform.reflection.Reflections;
import com.ts.platform.reflection.io.Files;
import com.ts.platform.reflection.util.*;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;


@SuppressWarnings("rawtypes")
public class JsonSerializer implements Serializer {
    private Gson gson;

    public Reflections read(InputStream inputStream) {
        return getGson().fromJson(new InputStreamReader(inputStream), Reflections.class);
    }

    public File save(Reflections reflections, String filename) {
        try {
            File file = Utils.prepareFile(filename);
            Files.write(toString(reflections), file, Charset.defaultCharset());
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString(Reflections reflections) {
        return getGson().toJson(reflections);
    }

    private Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Multimap.class, new com.google.gson.JsonSerializer<Multimap>() {
						public JsonElement serialize(Multimap multimap, Type type, JsonSerializationContext jsonSerializationContext) {
                            return jsonSerializationContext.serialize(multimap.asMap());
                        }
                    })
                    .registerTypeAdapter(Multimap.class, new JsonDeserializer<Multimap>() {
                        public Multimap deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                            final SetMultimap<String,String> map = Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
                                public Set<String> get() {
                                    return Sets.newHashSet();
                                }
                            });
                            for (Map.Entry<String, JsonElement> entry : ((JsonObject) jsonElement).entrySet()) {
                                for (JsonElement element : (JsonArray) entry.getValue()) {
                                    map.get(entry.getKey()).add(element.getAsString());
                                }
                            }
                            return map;
                        }
                    })
                    .setPrettyPrinting()
                    .create();

        }
        return gson;
    }
}
