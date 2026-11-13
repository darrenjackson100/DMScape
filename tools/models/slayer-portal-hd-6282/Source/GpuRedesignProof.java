package net.runelite.client.plugins.gpu;

import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.runelite.api.Model;
import net.runelite.cache.definitions.*;
import net.runelite.cache.definitions.loaders.*;
import net.runelite.rlawt.AWTContext;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.opengl.GL33C.*;

/** Offline exported-DAT proof using Kryptic's SceneUploader and production GPU shaders. */
public class GpuRedesignProof {
 static Path root,resources; static Class<?> md,m; static Method light,transform;static Constructor<?> load;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 static String shader(String name)throws Exception {
  String s=Files.readString(resources.resolve(name));
  java.util.regex.Pattern p=java.util.regex.Pattern.compile("#include \\\"([^\\\"]+)\\\"");
  var m=p.matcher(s);StringBuffer b=new StringBuffer();while(m.find())m.appendReplacement(b,java.util.regex.Matcher.quoteReplacement(shader(m.group(1))));m.appendTail(b);
  return b.toString().replace("#include texture_config","#define TEXTURE_COUNT 256");
 }
 static int compile(int type,String name)throws Exception{int sh=glCreateShader(type);glShaderSource(sh,shader(name));glCompileShader(sh);check(glGetShaderi(sh,GL_COMPILE_STATUS)!=0,name+": "+glGetShaderInfoLog(sh));return sh;}
 static float[] perspective(float fov,float aspect,float near,float far){float f=1/(float)Math.tan(fov/2);return new float[]{f/aspect,0,0,0,0,f,0,0,0,0,(far+near)/(near-far),-1,0,0,2*far*near/(near-far),0};}
 static float[] cross(float[]a,float[]b){return new float[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
 static float dot(float[]a,float[]b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
 static float[] norm(float[]a){float n=(float)Math.sqrt(dot(a,a));return new float[]{a[0]/n,a[1]/n,a[2]/n};}
 static float[] view(float[]eye,float[]target){float[]f=norm(new float[]{target[0]-eye[0],target[1]-eye[1],target[2]-eye[2]}),s=norm(cross(f,new float[]{0,-1,0})),u=cross(s,f);return new float[]{s[0],u[0],-f[0],0,s[1],u[1],-f[1],0,s[2],u[2],-f[2],0,-dot(s,eye),-dot(u,eye),dot(f,eye),1};}
 static void sortAlpha(GpuIntBuffer vb,GpuFloatBuffer uv,float[]eye){
  IntBuffer b=vb.getBuffer();FloatBuffer u=uv.getBuffer();int n=b.limit()/12;int[]data=new int[b.limit()];float[]tex=new float[u.limit()];b.get(data).rewind();u.get(tex).rewind();Integer[]order=new Integer[n];double[]dist=new double[n];
  for(int i=0;i<n;i++){order[i]=i;double x=0,y=0,z=0;for(int j=0;j<3;j++){x+=Float.intBitsToFloat(data[i*12+j*4]);y+=Float.intBitsToFloat(data[i*12+j*4+1]);z+=Float.intBitsToFloat(data[i*12+j*4+2]);}dist[i]=Math.pow(x/3-eye[0],2)+Math.pow(y/3-eye[1],2)+Math.pow(z/3-eye[2],2);}
  Arrays.sort(order,(a,c)->{boolean aa=(data[a*12+3]>>>24)>0,cc=(data[c*12+3]>>>24)>0;if(aa!=cc)return aa?1:-1;return Double.compare(dist[c],dist[a]);});
  for(int i:order){b.put(data,i*12,12);u.put(tex,i*12,12);}b.flip();u.flip();
 }

 static void setup()throws Exception{
  Class<?> u=Class.forName("sun.misc.Unsafe");Field uf=u.getDeclaredField("theUnsafe");uf.setAccessible(true);Object host=u.getMethod("allocateInstance",Class.class).invoke(uf.get(null),Class.forName("client"));Field hf=Class.forName("bf").getDeclaredField("fg");hf.setAccessible(true);hf.set(null,host);
  md=Class.forName("jj");m=Class.forName("jm");load=md.getDeclaredConstructor(byte[].class);load.setAccessible(true);light=md.getMethod("light",int.class,int.class,int.class,int.class,int.class);transform=m.getMethod("cp",int.class,int[].class,int.class,int.class,int.class);
 }
 static Model floor()throws Exception{
  ModelDefinition f=new ModelDefinition();f.vertexCount=24;f.faceCount=12;f.vertexX=new int[24];f.vertexY=new int[24];f.vertexZ=new int[24];f.faceIndices1=new int[12];f.faceIndices2=new int[12];f.faceIndices3=new int[12];f.faceColors=new short[12];f.faceRenderTypes=new byte[12];int c=0;
  for(int x=-1;x<1;x++)for(int z=-1;z<2;z++){int v=c*4;int[]xx={x*128,(x+1)*128,(x+1)*128,x*128},zz={z*128-64,z*128-64,(z+1)*128-64,(z+1)*128-64};for(int k=0;k<4;k++){f.vertexX[v+k]=xx[k];f.vertexY[v+k]=2;f.vertexZ[v+k]=zz[k];}f.faceIndices1[c*2]=v;f.faceIndices2[c*2]=v+1;f.faceIndices3[c*2]=v+2;f.faceIndices1[c*2+1]=v;f.faceIndices2[c*2+1]=v+2;f.faceIndices3[c*2+1]=v+3;f.faceColors[c*2]=f.faceColors[c*2+1]=(short)(((x+z)&1)==0?24:28);f.faceRenderTypes[c*2]=f.faceRenderTypes[c*2+1]=1;c++;}
  Method enc=Class.forName("net.runelite.cache.tools.ItemTextureTool").getDeclaredMethod("encodeType3",ModelDefinition.class);enc.setAccessible(true);return (Model)light.invoke(load.newInstance((byte[])enc.invoke(null,f)),64,768,-50,-10,-50);
 }
 public static void main(String[]args){try{run(args);System.exit(0);}catch(Throwable t){t.printStackTrace();System.exit(1);}}
 static void run(String[]args)throws Exception{
  root=Path.of(args[0]);resources=Path.of(args[1]);String name=args[2];int frames=args.length>3?Integer.parseInt(args[3]):90;String angle=args.length>4?args[4]:"front";Path out=root.resolve("Previews/gpu/"+name+"-"+angle);Files.createDirectories(out);setup();
  boolean shatter=false;Path a=root.resolve("Animations/"+name);FramemapDefinition sk=shatter?new FramemapLoader().load(0,Files.readAllBytes(a.resolve("skeleton.dat"))):null;SequenceDefinition seq=shatter?new SequenceLoader().load(0,Files.readAllBytes(a.resolve("sequence.dat"))):null;byte[] data=Files.readAllBytes(root.resolve("Models/arch-base/concept.dat"));Model ground=floor();
  AuthoredModelTextures authored=new AuthoredModelTextures();net.runelite.api.Client fakeClient=(net.runelite.api.Client)java.lang.reflect.Proxy.newProxyInstance(net.runelite.api.Client.class.getClassLoader(),new Class[]{net.runelite.api.Client.class},(o,method,aa)->switch(method.getName()){case "getCenterX"->500;case "getCenterY"->450;case "get3dZoom"->1500;default->null;});SceneUploader uploader=new SceneUploader(fakeClient,null);uploader.initSortingBuffers();Field af=SceneUploader.class.getDeclaredField("authoredModelTextures");af.setAccessible(true);af.set(uploader,authored);
  Canvas canvas=new Canvas();canvas.setPreferredSize(new Dimension(32,32));Frame window=new Frame();window.add(canvas);window.pack();AWTContext.loadNatives();AWTContext ctx=new AWTContext(canvas);ctx.configurePixelFormat(0,0,0);ctx.createGLContext();GL.createCapabilities();System.out.println("GPU: "+glGetString(GL_RENDERER));
  int program=glCreateProgram();for(int sh:new int[]{compile(GL_VERTEX_SHADER,"vert.glsl"),compile(GL_GEOMETRY_SHADER,"geom.glsl"),compile(GL_FRAGMENT_SHADER,"frag.glsl")}){glAttachShader(program,sh);glDeleteShader(sh);}glLinkProgram(program);check(glGetProgrami(program,GL_LINK_STATUS)!=0,glGetProgramInfoLog(program));glUseProgram(program);
  int w=angle.equals("video")?1200:3200,h=angle.equals("video")?1080:2880;int fbo=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,fbo);int color=glGenTextures();glBindTexture(GL_TEXTURE_2D,color);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,color,0);int depth=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,w,h);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);check(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE,"Framebuffer");
  boolean platform=name.startsWith("platform");boolean cloud=name.equals("lower-cloud-bank");
  float[]eye=platform?(angle.equals("detail")?new float[]{1000,-1480,-500}:angle.equals("top")?new float[]{1100,-2700,-1400}:angle.equals("side")?new float[]{2400,-1450,0}:new float[]{1900,-1750,-2400}):cloud?new float[]{1400,-900,-1700}:new float[]{330,-280,-520};
  float[]target=platform?(angle.equals("detail")?new float[]{340,-1050,-20}:new float[]{0,-550,0}):cloud?new float[]{0,150,0}:new float[]{0,name.equals("entrance-warning-sign")?-120:-10,0};
  boolean assembly=platform&&angle.startsWith("assembly");
  if(name.equals("entrance-warning-sign")){
   eye=angle.equals("straight")?new float[]{0,-165,-1400}:angle.equals("back")?new float[]{700,-430,1150}:new float[]{700,-440,-1150};target=new float[]{0,-155,0};
  }
  if(assembly){
   eye=angle.equals("assembly-top")?new float[]{0,-2900,500}:angle.equals("assembly-back")?new float[]{1000,-2100,-2100}:angle.equals("assembly-straight")?new float[]{0,-1680,1900}:new float[]{1100,-2100,2100};
   target=new float[]{0,-1100,0};
  }
  if(platform&&angle.equals("opposite")){eye=new float[]{-1900,-1750,2400};target=new float[]{0,-550,0};}
  if(name.equals("ascension-altar") || name.equals("runic-gate") || name.equals("mastery-beacon")){
   eye=angle.equals("straight")?new float[]{0,-235,-1000}:angle.equals("back")?new float[]{620,-500,900}:angle.equals("top")?new float[]{0,-1300,-50}:new float[]{340,-390,-740};
   target=new float[]{0,-176,0};
   if(angle.startsWith("angle")){double t=Integer.parseInt(angle.substring(5))*Math.PI/4;eye=new float[]{(float)(900*Math.sin(t)),-450,(float)(-900*Math.cos(t))};}
  }
  eye=angle.equals("back")?new float[]{350,-400,850}:angle.equals("straight")?new float[]{0,-240,-1000}:new float[]{320,-360,-870};target=new float[]{0,-198,0};
  float[]cameraView=view(eye,target);
  net.runelite.api.Projection projection=(xx,yy,zz)->new float[]{cameraView[0]*xx+cameraView[4]*yy+cameraView[8]*zz+cameraView[12],-(cameraView[1]*xx+cameraView[5]*yy+cameraView[9]*zz+cameraView[13]),-(cameraView[2]*xx+cameraView[6]*yy+cameraView[10]*zz+cameraView[14])};
  float[]matrix=perspective((float)Math.toRadians(34),(float)w/h,10,10000);Mat4.mul(matrix,view(eye,target));
  glUniformMatrix4fv(glGetUniformLocation(program,"projectionMatrix"),false,matrix);glUniform1i(glGetUniformLocation(program,"useFog"),0);glUniform1i(glGetUniformLocation(program,"drawDistance"),10000);glUniform1f(glGetUniformLocation(program,"brightness"),.8f);glUniform1i(glGetUniformLocation(program,"textures"),1);glUniform1i(glGetUniformLocation(program,"authoredTextures"),3);
  int ubo=glGenBuffers();glBindBuffer(GL_UNIFORM_BUFFER,ubo);ByteBuffer ub=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());ub.putFloat(0).putFloat(0).putInt(w/2).putInt(h/2).putInt(1500).putFloat(eye[0]).putFloat(eye[1]).putFloat(eye[2]).flip();glBufferData(GL_UNIFORM_BUFFER,ub,GL_STATIC_DRAW);glBindBufferBase(GL_UNIFORM_BUFFER,0,ubo);glUniformBlockBinding(program,glGetUniformBlockIndex(program,"uniforms"),0);
  JsonArray bindings=new Gson().fromJson(Files.readString(root.resolve("Textures/bindings.json")),JsonArray.class);
  int maxTexture=0;for(JsonElement row:bindings)maxTexture=Math.max(maxTexture,row.getAsJsonObject().get("textureId").getAsInt());
  glActiveTexture(GL_TEXTURE1);int nativeTextures=glGenTextures();glBindTexture(GL_TEXTURE_2D_ARRAY,nativeTextures);glTexImage3D(GL_TEXTURE_2D_ARRAY,0,GL_RGBA8,128,128,maxTexture+1,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);
  glTexParameteri(GL_TEXTURE_2D_ARRAY,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D_ARRAY,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D_ARRAY,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
  for(JsonElement row:bindings){JsonObject q=row.getAsJsonObject();BufferedImage ti=ImageIO.read(root.resolve(q.get("png").getAsString()).toFile());ByteBuffer tb=ByteBuffer.allocateDirect(128*128*4);for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++){int rgb=ti.getRGB(xx*ti.getWidth()/128,yy*ti.getHeight()/128);tb.put((byte)(rgb>>16)).put((byte)(rgb>>8)).put((byte)rgb).put((byte)255);}tb.flip();glTexSubImage3D(GL_TEXTURE_2D_ARRAY,0,0,0,q.get("textureId").getAsInt(),128,128,1,GL_RGBA,GL_UNSIGNED_BYTE,tb);}
  glActiveTexture(GL_TEXTURE0);
  authored.bind();int vao=glGenVertexArrays();glBindVertexArray(vao);int vbo=glGenBuffers(),uvbo=glGenBuffers();glDisable(GL_DEPTH_TEST);glEnable(GL_BLEND);glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);glEnable(GL_CULL_FACE);glViewport(0,0,w,h);GpuIntBuffer vb=new GpuIntBuffer();GpuFloatBuffer uv=new GpuFloatBuffer();ByteBuffer pixels=MemoryUtil.memAlloc(w*h*4);
  for(int step=0;step<frames;step++){
   int frame=name.equals("ascension-altar") ? ((args.length>5?Integer.parseInt(args[5]):50)+step*(args.length>6?Integer.parseInt(args[6]):1))%1200 : Math.min(step*(frames==96?2:1),191);Model model=(Model)light.invoke(load.newInstance(data),platform?52:74,768,-50,-10,-50);if(shatter){FrameDefinition fd=new FrameLoader().load(sk,seq.frameIDs[frame],Files.readAllBytes(a.resolve(String.format("frames/%03d.dat",frame))));transform.invoke(model,0,new int[0],0,0,0);for(int j=0;j<fd.indexFrameIds.length;j++){int op=fd.indexFrameIds[j];transform.invoke(model,sk.types[op],sk.frameMaps[op],fd.translator_x[j],fd.translator_y[j],fd.translator_z[j]);}}
   int tick=step*10;java.util.List<Model> assembled=new java.util.ArrayList<>();
   for(String piece:new String[]{"rift","arch-base","wall-left","wall-right"}){
    Model q=(Model)light.invoke(load.newInstance(Files.readAllBytes(root.resolve("Models/"+piece+"/concept.dat"))),74,768,-50,-10,-50);
    assembled.add(q);
   }Model[] scene=assembled.toArray(new Model[0]);
   if(assembly&&name.equals("platform-entrance")){
    Model board=(Model)light.invoke(load.newInstance(Files.readAllBytes(root.resolve("Models/entrance-warning-sign.dat"))),64,768,-50,-10,-50);
    board.rotateY180Ccw();board.translate(0,-1200,-320);scene=new Model[]{model,board};
   }
   vb.clear();uv.clear();int length=0;for(Model p:name.equals("empty")?new Model[]{ground}:scene){p.calculateBoundsCylinder();length+=uploader.pushSortedModel(projection,p,0,0,0,0,vb,uv);uv.ensureCapacity(vb.getBuffer().position()-uv.getBuffer().position());while(uv.getBuffer().position()<vb.getBuffer().position())uv.put(0,0,0,0);}vb.flip();uv.flip(); authored.bind();
   glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,vb.getBuffer(),GL_STREAM_DRAW);glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,16,0);glEnableVertexAttribArray(1);glVertexAttribIPointer(1,1,GL_INT,16,12);glBindBuffer(GL_ARRAY_BUFFER,uvbo);glBufferData(GL_ARRAY_BUFFER,uv.getBuffer(),GL_STREAM_DRAW);glEnableVertexAttribArray(2);glVertexAttribPointer(2,4,GL_FLOAT,false,16,0);
   glUniform1f(glGetUniformLocation(program,"slayerPortalTime"),angle.equals("video")?step/30.0f:step*0.2f);
   glClearColor(.0706f,.0824f,.1176f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glDrawArrays(GL_TRIANGLES,0,length);check(glGetError()==GL_NO_ERROR,"GPU draw error");pixels.clear();glReadPixels(0,0,w,h,GL_RGBA,GL_UNSIGNED_BYTE,pixels);BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);int[]row=new int[w];for(int y=0;y<h;y++){for(int x=0;x<w;x++){int at=((h-1-y)*w+x)*4;row[x]=((pixels.get(at)&255)<<16)|((pixels.get(at+1)&255)<<8)|(pixels.get(at+2)&255);}im.setRGB(0,y,w,1,row,0,w);}ImageIO.write(im,"PNG",out.resolve(String.format("%04d.png",step)).toFile());
  }
  Files.writeString(out.resolve("gpu-validation.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("renderer",glGetString(GL_RENDERER),"shaders","production GPU vert/geom/frag","uploader","production SceneUploader","model",name,"nativeFrames",frames,"cacheWritten",false,"loggedInTest",false,"floor","preview only")));
  MemoryUtil.memFree(pixels);authored.close();ctx.destroy();window.dispose();System.out.println("GPU proof complete: "+name+" / "+frames+" frames");
 }
}


