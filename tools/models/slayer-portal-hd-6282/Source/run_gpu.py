from pathlib import Path
import subprocess,os,sys
R=Path(__file__).resolve().parents[1];W=R.parents[2];C=W/'osrs server/client kryptic/client';J=Path(r'C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot\bin')
cp=os.pathsep.join([str(R/'classes'),str(R/'ClientResources'),str(C/'runelite-client/build/classes/java/main'),str(C/'runelite-client/src/main/resources'),str(C/'runelite-client/build/libs/runelite-client-1.10.33-SNAPSHOT-all.jar'),str(C/'lib/runelite-injected.bin'),str(R/'VPS/runtime/*')])
if sys.argv[1]=='compile':cmd=[str(J/'javac.exe'),'-cp',cp,'-d',str(R/'classes'),str(R/'Source/GpuRedesignProof.java'),str(R/'ClientPatch/AuthoredModelTextures.java')]
else:cmd=[str(J/'java.exe'),'-Xmx4g','-cp',cp,'net.runelite.client.plugins.gpu.GpuRedesignProof',str(R),str(R/'ClientResources/net/runelite/client/plugins/gpu'),*sys.argv[1:]]
subprocess.run(cmd,check=True)

