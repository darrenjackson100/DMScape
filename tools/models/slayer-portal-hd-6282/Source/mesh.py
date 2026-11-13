from pathlib import Path
import math,json,colorsys
import numpy as np
R=Path(__file__).resolve().parents[1]
G=['D5A13B','F0CE73','A77326','E7B958','BD8730','F8D889']
D=['252C34','34414B','202831','414D57']
BLUE=['487CE0','91B4FF','2847A3','648CF0']
GREEN=['3EA879','90D9A3','215C46','53C58C']
RED=['B94540','E78964','73312F','D25C47']
PURPLE=['9065D5','CB9BED','4C347C','AC7EE2']
def hsl(c):
 h,l,s=colorsys.rgb_to_hls(*[int(c[i:i+2],16)/255 for i in (0,2,4)])
 n=(int(h*63)<<10)|(int(s*7)<<7)|max(2,min(126,round(l*127)))
 return n if n<32768 else n-65536
class Mesh:
 def __init__(self):self.v=[];self.f=[];self.c=[];self.pr=[];self.threshold=-8
 def part(self,v,faces,colors,center):
  off=len(self.v);v=[tuple(round(x) for x in p) for p in v];self.v.extend(v)
  for j,face in enumerate(faces):
   for i in range(1,len(face)-1):
    f=[face[0],face[i],face[i+1]];pts=np.array([v[k] for k in f]);n=np.cross(pts[1]-pts[0],pts[2]-pts[0])
    if np.dot(n,n)==0:continue
    if np.dot(n,pts.mean(axis=0)-center)<0:f.reverse()
    self.f.append([off+k for k in f]);self.c.append(hsl(colors[j%len(colors)]));self.pr.append(2 if center[1]<self.threshold else 0)
 def box(self,c,size,col):
  x,y,z=c;a,b,d=[q/2 for q in size];v=[(x+i*a,y+j*b,z+k*d) for i,j,k in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]]
  self.part(v,[(0,1,2,3),(4,7,6,5),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)],col,c)
 def rod(self,a,b,r,col,n=8):
  a=np.array(a,float);b=np.array(b,float);axis=b-a;axis/=np.linalg.norm(axis);side=np.cross(axis,[0,1,0] if abs(axis[1])<.9 else [1,0,0]);side/=np.linalg.norm(side);other=np.cross(axis,side)
  v=[tuple(p+r*(math.cos(i*math.tau/n)*side+math.sin(i*math.tau/n)*other)) for p in (a,b) for i in range(n)]
  self.part(v,[tuple(range(n-1,-1,-1)),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)],col,(a+b)/2)
 def ring(self,c,rx,rz,t,col,n=24):
  x,y,z=c
  for i in range(n):
   a=i*math.tau/n;b=(i+1)*math.tau/n
   self.rod((x+rx*math.cos(a),y,z+rz*math.sin(a)),(x+rx*math.cos(b),y,z+rz*math.sin(b)),t,col,6)
 def disc(self,c,r,depth,col,n=16):self.rod((c[0],c[1]-depth/2,c[2]),(c[0],c[1]+depth/2,c[2]),r,col,n)
 def gem(self,c,rx,rz,depth,col,n=6):
  x,y,z=c;v=[(x,y-depth,z),(x,y+depth*.45,z)]+[(x+rx*math.cos(i*math.tau/n),y,z+rz*math.sin(i*math.tau/n)) for i in range(n)]
  self.part(v,[(0,2+i,2+(i+1)%n) for i in range(n)]+[(1,2+(i+1)%n,2+i) for i in range(n)],col,c)
 def line(self,points,r=2,col=G):
  for a,b in zip(points,points[1:]):self.rod(a,b,r,col,6)
 def tree(self,y,z,scale=1):
  paths=[[(0,0),(0,13),(-11,23),(-15,31)],[(0,13),(10,23),(15,33)],[(0,20),(0,35)],[(0,6),(-12,-2)],[(0,6),(12,-2)],[(-8,21),(-20,22)],[(7,20),(20,24)]]
  for path in paths:self.line([(x*scale,y,z+h*scale) for x,h in path],max(2,2.5*scale))
 def four(self,y,z,rx,rz,size=5):
  for (x,h),col in zip([(-rx,z),(rx,z),(0,z+rz),(0,z-rz)],[BLUE,GREEN,RED,PURPLE]):
   self.disc((x,y+1,h),size+2,3,G,8);self.gem((x,y-2,h),size,size,3,col,4)
 def save(self,spec):
  vs=[(x,-z,y) for x,y,z in self.v];f=self.f
  d=dict(id=spec['model'],vertexCount=len(vs),vertexX=[v[0] for v in vs],vertexY=[v[1] for v in vs],vertexZ=[v[2] for v in vs],faceCount=len(f),faceIndices1=[a[0] for a in f],faceIndices2=[a[1] for a in f],faceIndices3=[a[2] for a in f],faceColors=self.c,faceRenderPriorities=self.pr,numTextureFaces=0)
  assert len(f)<6144 and len(vs)<6500
  o=R/'Models'/spec['slug'];o.mkdir(parents=True,exist_ok=True);(o/'model.json').write_text(json.dumps(d));print(spec['name'],len(vs),len(f))
