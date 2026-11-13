import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import com.google.gson.*;import net.runelite.cache.definitions.*;import net.runelite.cache.definitions.loaders.*;
public class ExportConceptParts {
 public static void main(String[]a)throws Exception{Path r=Path.of(a[0]);Gson g=new GsonBuilder().setPrettyPrinting().create();Method enc=Class.forName("net.runelite.cache.tools.ItemTextureTool").getDeclaredMethod("encodeType3",ModelDefinition.class);enc.setAccessible(true);
 for(String name:new String[]{"wall-left","wall-right","arch-base","rift"}){Path p=r.resolve("Models/"+name);ModelDefinition m=g.fromJson(Files.readString(p.resolve("model.json")),ModelDefinition.class);byte[] bytes=(byte[])enc.invoke(null,m);ModelDefinition rt=new ModelLoader().load(0,bytes);for(String key:new String[]{"vertexX","vertexY","vertexZ","faceIndices1","faceIndices2","faceIndices3","faceColors","faceTextures","faceTransparencies"}){Field f=ModelDefinition.class.getField(key);if(!Objects.deepEquals(f.get(m),f.get(rt)))throw new Exception(name+" "+key);}Files.write(p.resolve("concept.dat"),bytes);System.out.println(name+" exact native roundtrip "+m.faceCount+" faces");}
 }
}
