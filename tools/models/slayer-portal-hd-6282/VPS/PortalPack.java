import java.nio.file.*;
import java.util.*;
import net.runelite.cache.fs.Store;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.*;
import net.runelite.cache.definitions.savers.ObjectSaver;
class PortalPack extends HurricanePacker {
 public static void main(String[] args)throws Exception {
  try(var s=new Store(Path.of(args[0]).toFile())){
   s.load();var defs=files(s,2,6);var d=new ObjectLoader().load(6282,defs.get(6282));
   System.out.println("6282 "+d.getName()+" "+d.getSizeX()+"x"+d.getSizeY()+" "+Arrays.toString(d.getObjectModels())+" "+Arrays.toString(d.getObjectTypes())+" anim="+d.getAnimationID()+" options="+Arrays.toString(d.getActions()));
   if(args.length==1)return;
   ok(d.getSizeX()==3&&d.getSizeY()==1&&Arrays.equals(d.getObjectTypes(),new int[]{10})&&"Task".equalsIgnoreCase(d.getActions()[0]),"Live portal identity");
   var encode=Class.forName("net.runelite.cache.tools.ItemTextureTool").getDeclaredMethod("encodeType3",ModelDefinition.class);
   encode.setAccessible(true);
   int id=62000;var models=new int[4];int i=0;
   for(String part:List.of("wall-left","wall-right","arch-base","rift")){
    while(s.findIndex(7).getArchive(id)!=null)id++;
    var mesh=new ModelLoader().load(id,Files.readAllBytes(Path.of(args[1],"Models",part,"concept.dat")));
    for(int face=0;face<mesh.faceCount;face++){
     ok(mesh.faceTextures[face]==(short)(i==3?49011:49010),"Source material marker");
     mesh.faceTextures[face]=(short)(49012+i);
    }
    var data=(byte[])encode.invoke(null,mesh);
    var result=new ModelLoader().load(id,data);
    ok(result.faceCount==mesh.faceCount&&Arrays.equals(result.faceTextures,mesh.faceTextures),"Native roundtrip");
    save(s,7,id,Map.of(0,data),true);models[i++]=id++;
   }
   d.setObjectModels(models);d.setObjectTypes(new int[]{10,10,10,10});
   d.setAnimationID(-1);
   var out=new ObjectSaver().save(d);defs.put(6282,out);
   save(s,2,6,defs,false);s.save();
   System.out.println("Packed "+Arrays.toString(models));
  }
 }
}
