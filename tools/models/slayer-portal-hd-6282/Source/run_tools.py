import os,subprocess,sys,shutil
from pathlib import Path
R=Path(__file__).resolve().parents[1];W=Path(os.environ.get('KRYPTIC_WORKSPACE',str(R.parents[2]) if os.name=='nt' else '/opt/Kryptic'))
JAVA=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot'))/'bin' if os.name=='nt' or os.environ.get('JAVA_HOME') else Path(shutil.which('java')).resolve().parent
LIB=R/'VPS/runtime'
suffix='.exe' if os.name=='nt' else ''
CP=os.pathsep.join([str(R/'classes'),*[str(p) for p in LIB.glob('*.jar')],str(W/'osrs server/client kryptic/client/lib/runelite-injected.bin')])
if sys.argv[1]=='compile':
 (R/'classes').mkdir(exist_ok=True);cmd=[str(JAVA/('javac'+suffix)),'--release','17','-cp',CP,'-d',str(R/'classes'),*[str(p) for p in (R/'Source').glob('*.java') if not p.name.startswith('Gpu') and p.name!='SceneUploader.java']]
elif sys.argv[1]=='java':cmd=[str(JAVA/('java'+suffix)),'-Xmx4g','-cp',CP,sys.argv[2],str(R),*sys.argv[3:]]
elif sys.argv[1]=='blender':cmd=[os.environ.get('BLENDER',r'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' if os.name=='nt' else 'blender'),'--background','--threads','6','--python-exit-code','1','--python',str(R/'Source'/sys.argv[2])]
else:raise ValueError(sys.argv[1])
subprocess.run(cmd,check=True)
