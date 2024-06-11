package cambridge.io;

import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.HashMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import cambridge.craftworld.MaterialLookupData;

public class MaterialLookupTableSerializer implements JsonSerializer<HashMap<Integer, MaterialLookupData>>, JsonDeserializer<HashMap<Integer, MaterialLookupData>>
{
    @Override
    public HashMap<Integer, MaterialLookupData> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    throws JsonParseException 
    {
        HashMap<Integer, MaterialLookupData> MLUT = new HashMap<>();
        JsonObject object = json.getAsJsonObject();
        for (String key : object.keySet())
        {
            JsonObject value = object.get(key).getAsJsonObject();

            int uid = 0;
            try
            {
                uid = Integer.parseUnsignedInt(key);
            }
            catch (NumberFormatException ex)
            {
                String tag = "\\" + key + ".mip";
                for (int materialUid : MaterialLookupData.CACHED_MATERIAL_UID_MAP.keySet())
                {
                    String fp = MaterialLookupData.CACHED_MATERIAL_UID_MAP.get(materialUid);
                    if (fp.endsWith(tag))
                    {
                        uid = materialUid;
                        break;
                    }
                }

                if (uid == 0)
                {
                    throw new JsonParseException("Invalid material UID in JSON!");
                }
            }
            
            long 
                render = -1,
                physics = -1,
                bevel = -1;
            
            if (value.has("gmat"))
                render = value.get("gmat").getAsNumber().longValue();
            if (value.has("physics"))
                physics = value.get("physics").getAsNumber().longValue();
            if (value.has("bevel"))
                bevel = value.get("bevel").getAsNumber().longValue();
            
            MLUT.put(uid, new MaterialLookupData(physics, render, bevel));
        }

        return MLUT;
    }

    @Override
    public JsonElement serialize(HashMap<Integer, MaterialLookupData> src, Type typeOfSrc, JsonSerializationContext context) 
    {
        JsonObject map = new JsonObject();
        for (int key : src.keySet())
        {
            MaterialLookupData data = src.get(key);
            JsonObject value = new JsonObject();

            value.add("gmat", new JsonPrimitive(data.renderMaterial.getGUID().getValue()));
            if (data.physicsResource != null)
                value.add("physics", new JsonPrimitive(data.physicsResource.getGUID().getValue()));
            if (data.bevel != null)
                value.add("bevel", new JsonPrimitive(data.bevel.getGUID().getValue()));
            

            String fp = MaterialLookupData.CACHED_MATERIAL_UID_MAP.get(key);
            String name;

            if (fp == null) name = Integer.toUnsignedString(key);
            else name = Paths.get(fp).getFileName().toString().split("[.]")[0];

            map.add(name, value);
        }

        return map;
    }
}
