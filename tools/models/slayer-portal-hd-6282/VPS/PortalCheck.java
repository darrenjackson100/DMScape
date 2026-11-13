import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Array;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.cache.fs.Store;
import net.runelite.cache.definitions.loaders.ObjectLoader;
class PortalCheck extends HurricanePacker {
 public static void main(String[] args)throws Exception {
  try(var a=new Store(Path.of(args[0]).toFile());var b=new Store(Path.of(args[1]).toFile())){
   a.load();b.load();
   var defs=files(b,2,6); var old=files(a,2,6);
   var loader=new ObjectLoader();
   var original=loader.load(6282,old.get(6282));var portal=loader.load(6282,defs.get(6282));
   ok(portal.getSizeX()==3&&portal.getSizeY()==1&&portal.getAnimationID()==-1,"Portal shape");
   ok(Arrays.equals(portal.getActions(),original.getActions())&&portal.getInteractType()==original.getInteractType()
      &&portal.isBlocksProjectile()==original.isBlocksProjectile()&&portal.getAmbientSoundId()==original.getAmbientSoundId(),"Task/collision/sound");
   ok(defs.keySet().equals(old.keySet()),"Object IDs unchanged");
   for(var e:old.entrySet())if(e.getKey()!=6282)ok(Arrays.equals(e.getValue(),defs.get(e.getKey())),"Unrelated object");
   int archives=0;
   for(var ix:a.getIndexes())for(var ar:ix.getArchives()){
    if(ix.getId()==2&&ar.getArchiveId()==6)continue;
    ok(Arrays.equals(a.getStorage().loadArchive(ar),b.getStorage().loadArchive(b.findIndex(ix.getId()).getArchive(ar.getArchiveId()))),"Unrelated archive");
    archives++;
   }
   var type=Class.forName("jj");
   var ctor=type.getDeclaredConstructor(byte[].class);ctor.setAccessible(true);
   var parts=Array.newInstance(type,4);
   for(int i=0;i<4;i++)Array.set(parts,i,ctor.newInstance((Object)files(b,7,portal.getObjectModels()[i]).get(0)));
   var merge=type.getConstructor(parts.getClass(),int.class);
   var combined=(ModelData)merge.newInstance(parts,4);
   var model=combined.light(64,768,-50,-10,-50);
   ok(model.getFaceCount()==15416&&model.getVerticesCount()<=20000,"Native merged model fits CPU sorter");
   int face=0;
   for(int i=0;i<4;i++)for(int j=0;j<new int[]{5880,5880,3642,14}[i];j++)ok(model.getFaceTextures()[face++]==(short)(49012+i),"Exact native face order");
   var installed=Class.forName("net.runelite.client.ClientArtwork").getDeclaredField("installed");
   installed.setAccessible(true);
   installed.set(null,Path.of("/opt/Kryptic/osrs server/client kryptic/client/runelite-client/src/main/resources"));
   String gpu="net.runelite.client.plugins.gpu.";
   Class<?> uploader=Class.forName(gpu+"SceneUploader");
   var init=Arrays.stream(uploader.getDeclaredConstructors()).filter(c->c.getParameterCount()==6).findFirst().orElseThrow();
   init.setAccessible(true);Object scene=init.newInstance(new Object[6]);
   for(String[] entry:new String[][]{{"authoredModelTextures","AuthoredModelTextures"},{"bladeParticleMaterials","BladeParticleMaterials"}}){
    var f=uploader.getDeclaredField(entry[0]);f.setAccessible(true);
    var c=Class.forName(gpu+entry[1]).getDeclaredConstructor();c.setAccessible(true);
    f.set(scene,c.newInstance());
   }
   var ib=Class.forName(gpu+"GpuIntBuffer");var fb=Class.forName(gpu+"GpuFloatBuffer");
   var ic=ib.getDeclaredConstructor();ic.setAccessible(true);
   var fc=fb.getDeclaredConstructor();fc.setAccessible(true);
   var push=uploader.getDeclaredMethod("pushModel",Model.class,ib,fb);push.setAccessible(true);
   var get=fb.getDeclaredMethod("getBuffer");get.setAccessible(true);
   for(int direction=0;direction<4;direction++){
    var u=fc.newInstance();
    ok((int)push.invoke(scene,model,ic.newInstance(),u)==15416*3,"Full GPU upload");
    var uv=(java.nio.FloatBuffer)get.invoke(u);
    int stone=0,rift=0;
    for(int at=0;at<uv.position();at+=4){if(uv.get(at)==-41006)stone++;if(uv.get(at)==-41007)rift++;}
    ok(stone==15402*3&&rift==14*3,"All approved per-face UVs for orientation "+direction);
    model.rotateY90Ccw();
   }
   System.out.println("PASS native merged "+model.getFaceCount()+" faces "+model.getVerticesCount()+" vertices, "+archives+" unrelated archives, same Task/clipping/sound");
  }
  System.exit(0);
 }
}
