from mesh import *
import struct,random
from PIL import Image,ImageFilter
random.seed(192)
class StoneMesh(Mesh):
 def __init__(self):super().__init__();self.tex=[];self.uv=[];self.marker=-16526
 def patch(self,points,faces,center,color=100):
  nf=len(self.f);self.part(points,faces,['777777'],center)
  for i,f in enumerate(self.f[nf:],nf):
   self.c[i]=color;self.pr[i]=0;self.tex.append(self.marker)
   pts=np.array([self.v[k] for k in f]);n=np.cross(pts[1]-pts[0],pts[2]-pts[0]);axes=[a for a in range(3) if a!=np.argmax(abs(n))]
   self.uv.append([(.42+p[axes[0]]/1200,.10+p[axes[1]]/1200) for p in pts])
 def brick(self,mapfn,light=100,grid=3):
  # All six broad faces, twelve chamfer edges and eight corner facets are 3D.
  b=.13;lo=1-b;center=np.array(mapfn(np.zeros(3)))
  for axis in range(3):
   axes=[a for a in range(3) if a!=axis]
   for sign in [-1,1]:
    v=[]
    for i in range(grid+1):
     for j in range(grid+1):
      p=np.zeros(3);p[axis]=sign;p[axes[0]]=-lo+2*lo*i/grid;p[axes[1]]=-lo+2*lo*j/grid;v.append(mapfn(p))
    faces=[]
    for i in range(grid):
     for j in range(grid):
      a=i*(grid+1)+j;faces.append((a,a+1,a+grid+2,a+grid+1))
    self.patch(v,faces,center,light)
  for free in range(3):
   aa=[a for a in range(3) if a!=free]
   for s in [-1,1]:
    for t in [-1,1]:
     pts=[]
     for f,u,v in [(-lo,1,lo),(lo,1,lo),(lo,lo,1),(-lo,lo,1)]:
      p=np.zeros(3);p[free]=f;p[aa[0]]=s*u;p[aa[1]]=t*v;pts.append(mapfn(p))
     self.patch(pts,[(0,1,2,3)],center,light+5)
  for a in [-1,1]:
   for b0 in [-1,1]:
    for c in [-1,1]:
     v=[]
     for axis in range(3):
      p=np.array([a,b0,c])*lo;p[axis]=[a,b0,c][axis];v.append(mapfn(p))
     self.patch(v,[(0,1,2)],center,light)
 def block(self,c,size,light=100,grid=3):
  c=np.array(c);s=np.array(size)/2;self.brick(lambda p:tuple(c+s*p),light,grid)
 def save_native(self,name):
  original=[(x,-z,y) for x,y,z in self.v];v=[];lookup={};mapping=[]
  for pt in original:
   if pt not in lookup:lookup[pt]=len(v);v.append(pt)
   mapping.append(lookup[pt])
  f=[[mapping[j] for j in face] for face in self.f]
  assert len(v)<6500 and len(f)<6144,(name,len(v),len(f))
  d=dict(id=0,vertexCount=len(v),faceCount=len(f),vertexX=[a[0] for a in v],vertexY=[a[1] for a in v],vertexZ=[a[2] for a in v],faceIndices1=[a[0] for a in f],faceIndices2=[a[1] for a in f],faceIndices3=[a[2] for a in f],faceRenderTypes=[1]*len(f),faceColors=self.c,faceTextures=self.tex,textureCoords=[-1]*len(f),faceRenderPriorities=self.pr,numTextureFaces=0)
  p=R/'Models'/name;p.mkdir(exist_ok=True);(p/'model.json').write_text(json.dumps(d))
  with (OUT/(name+'.uv')).open('wb') as o:
   o.write(struct.pack('>4i',0x55564f31,len(f),3,0))
   for ids,texture,uv in zip(f,self.tex,self.uv):o.write(struct.pack('>4i6f',*ids,texture,*[a for p in uv for a in p]))
  return dict(part=name,vertices=len(v),triangles=len(f))
OUT=R/'ClientResources/net/runelite/client/plugins/gpu/authored';OUT.mkdir(exist_ok=True)
rng=np.random.default_rng(441);field=np.zeros((1024,1024))
for n,weight in [(4,9),(16,6),(64,4),(256,2),(1024,1)]:
 noise=Image.fromarray(rng.integers(0,256,(n,n),dtype=np.uint8)).resize((1024,1024),Image.Resampling.BICUBIC);field+=(np.array(noise,dtype=float)-128)/128*weight
rgb=np.stack([77+field,76+field,74+field],axis=-1).clip(0,255).astype(np.uint8);atlas=Image.fromarray(rgb).resize((4096,4096),Image.Resampling.BICUBIC)
# Original cache Slayer sprite, preserved at its own aspect ratio in a stone recess.
icon=Image.open(R/'Reference/slayer-emblem-hd.png').convert('RGBA');icon=icon.crop(icon.getbbox());icon.thumbnail((1360,1360),Image.Resampling.LANCZOS)
atlas.paste((48,47,45),(2560,2560,4096,4096));atlas.paste(icon,(2560+(1536-icon.width)//2,2560+(1536-icon.height)//2),icon);atlas.save(OUT/'perks-atlas.png')
report=[]
for side,name in [(-1,'wall-left'),(1,'wall-right')]:
 m=StoneMesh()
 # Two depth courses, staggered joints and genuine mortar recesses.
 for row in range(8):
  for depth in [-1,1]:
   if row%2==0:widths=[35,35];centers=[-18.5,18.5]
   else:widths=[16,34,16];centers=[-28,0,28]
   for x,w in zip(centers,widths):m.block((side*150+x,depth*24,54+row*26),(w,46,24),random.randrange(95,110))
 for z,w,h in [(30,83,20),(258,84,20)]:m.block((side*150,0,z),(w,106,h),105)
 report.append(m.save_native(name))
m=StoneMesh()
for x,w in [(-144,92),(-48,92),(48,92),(144,92)]:m.block((x,0,9),(w,124,18),100)
for x in [-72,0,72]:m.block((x,-3,24),(69,110,10),106)
outer=[(-150,250),(-115,298),(-65,339),(0,380),(65,339),(115,298),(150,250)]
inner=[(-106,245),(-79,273),(-42,301),(0,327),(42,301),(79,273),(106,245)]
for i in range(6):
 for k in range(3):
  t0=k/3+.008;t1=(k+1)/3-.008
  def mapping(p,i=i,t0=t0,t1=t1):
   t=t0+(p[0]+1)/2*(t1-t0);q=(p[2]+1)/2
   a=np.array(outer[i])*(1-t)+np.array(outer[i+1])*t;b=np.array(inner[i])*(1-t)+np.array(inner[i+1])*t;c=b*(1-q)+a*q
   return (c[0],p[1]*46,c[1])
  m.brick(mapping,random.randrange(95,110))
# Central carved keystone, flush inset image face, no attached metal frame.
m.block((0,-2,350),(66,98,62),110)
nf=len(m.f);m.patch([(-24,-52,326),(24,-52,326),(24,-52,374),(-24,-52,374)],[(0,1,2,3)],(0,0,350),126)
uvs=[(.625,1),(1,1),(1,.625),(.625,.625)]
for i,face in enumerate(m.f[nf:],nf):
 m.pr[i]=2;m.uv[i]=[uvs[j] for j in ([0,1,2] if i==nf else [0,2,3])]
report.append(m.save_native('arch-base'))
m=StoneMesh();m.marker=-16525
outline=[(-105,37),(105,37),(105,242),(76,270),(40,297),(0,324),(-40,297),(-76,270),(-105,242)]
for reverse in [False,True]:
 pts=[(x,12,z) for x,z in outline];nf=len(m.f);m.patch(pts,[tuple(range(len(pts)))],(0,50 if not reverse else -50,175),126)
 for i,f in enumerate(m.f[nf:],nf):m.uv[i]=[((m.v[j][0]+105)/210,(m.v[j][2]-37)/287) for j in f]
report.append(m.save_native('rift'))
(R/'assembly.json').write_text(json.dumps(report,indent=2));print(json.dumps(report));print('Total triangles',sum(p['triangles'] for p in report))
