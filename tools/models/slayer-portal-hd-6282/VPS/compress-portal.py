from pathlib import Path
import numpy as np,struct,sys
from PIL import Image
sys.path.insert(0,'/opt/Kryptic/tools/meshy/wall-quality-rebuild_2026-10-11_20-08-53/compression-deps')
import ispc_texcomp as itc
source=Image.open(sys.argv[1]).convert('RGBA')
assert source.size==(4096,4096)
for fmt in ('bc7','bc3'):
 profile=itc.BC7EncSettings.from_profile('basic') if fmt=='bc7' else None
 im=source.copy()
 p=Path(sys.argv[2])/('wall-slayer-portal.'+fmt)
 assert not p.exists()
 with p.open('wb') as f:
  f.write(struct.pack('>5i',0x43574231,1,4096,13,7 if profile else 3))
  for level in range(13):
   w=im.width
   arr=np.asarray(im)
   if w<4:arr=arr[np.minimum(np.arange(4),w-1)[:,None],np.minimum(np.arange(4),w-1)[None,:]]
   surface=itc.RGBASurface(arr.tobytes(),max(4,w),max(4,w),max(4,w)*4)
   blocks=itc.compress_blocks_bc7(surface,profile) if profile else itc.compress_blocks_bc3(surface)
   assert len(blocks)==max(1,w//4)**2*16
   f.write(struct.pack('>3i',level,w,len(blocks)));f.write(blocks)
   if w>1:im=im.resize((w//2,w//2),Image.Resampling.BOX)
 print(p,p.stat().st_size)
