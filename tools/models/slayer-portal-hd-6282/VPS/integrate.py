from pathlib import Path
import sys,shutil,struct,datetime
root=Path('/opt/Kryptic');w=Path(sys.argv[1]);src=w/'slayer-portal-hd-6282'
base=root/'osrs server/client kryptic/client/runelite-client/src/main'
j=base/'java/net/runelite/client/plugins/gpu';r=base/'resources/net/runelite/client/plugins/gpu'
backup=root/'Backups'/('slayer-portal_6282_'+datetime.datetime.now().strftime('%Y-%m-%d_%H-%M-%S'))
files=[j/n for n in ('AuthoredModelTextures.java','CrystalWallTextures.java','GpuPlugin.java','SceneUploader.java')]+[r/'frag.glsl']
sys.path.insert(0,str(root/'tools/interfaces/perk-path-editor-1421'))
import apply_layout as acl
for p in files:
 q=backup/p.relative_to(root);q.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,q);assert q.stat().st_size and q.read_bytes()==p.read_bytes()
(backup/'BACKUP-NOTES.txt').write_text('Restore matching source paths, rebuild the client, and restore cache-original with all readers closed.\n')
acl.tree_permissions(backup)
(w/'deploy-backup').write_text(str(backup))
def edit(p,replacements):
 s=p.read_text()
 for a,b in replacements:
  assert s.count(a)==1,(p,a)
  s=s.replace(a,b)
 p.write_text(s);acl.shared(p)
edit(files[0],[
 ('CrystalWallTextures.NAMES.length + 1','CrystalWallTextures.NAMES.length + 4'),
 ('allNames[allNames.length - 1] = "wall-teleporter-nest";','''allNames[allNames.length - 5] = "wall-teleporter-nest";
        allNames[allNames.length - 4] = "wall-slayer-portal";
        allNames[allNames.length - 3] = "slayer-portal-right";
        allNames[allNames.length - 2] = "slayer-portal-arch";
        allNames[allNames.length - 1] = "slayer-portal-rift";'''),
 ('layer > 30','layer > 31'),
 ('if (layer == 30) code = textures[0] & 0xffff;', '''if (layer == 30) code = textures[0] & 0xffff;
                if (layer == 31) code = textures[0] == (short)49015 ? 41007 : 41006;'''),
 ('\tEntry find(Model model)','''    static boolean isSlayerPortal(Model model)
    {
        short[] textures = model.getFaceTextures();
        return model.getFaceCount() == 15416 && textures != null && textures[0] == (short)49012;
    }

	Entry find(Model model)''')
])
edit(files[1],[
 ('"teleporter-scenery"}','"teleporter-scenery", "slayer-portal"}'),
 ('LAST_TEXTURE = NEST_TEXTURE','SLAYER_PORTAL_TEXTURE = 41006, LAST_TEXTURE = SLAYER_PORTAL_TEXTURE'),
 ('else if (i == 24) tag = SCENERY_TEXTURE;','else if (i == 24) tag = SCENERY_TEXTURE;\n            else if (i == 25) tag = SLAYER_PORTAL_TEXTURE;'),
 ('if (wanted.get(24))','if (wanted.get(24) || wanted.get(25))')
])
edit(files[2],[
 ('private int uniBrightness;','private int uniBrightness;\n    private int uniSlayerPortalTime;'),
 ('uniBrightness = GL43C.glGetUniformLocation(glProgram, "brightness");','uniBrightness = GL43C.glGetUniformLocation(glProgram, "brightness");\n        uniSlayerPortalTime = GL43C.glGetUniformLocation(glProgram, "slayerPortalTime");'),
 ('GL43C.glUniform1f(uniBrightness, (float) textureProvider.getBrightness());','GL43C.glUniform1f(uniBrightness, (float) textureProvider.getBrightness());\n            GL43C.glUniform1f(uniSlayerPortalTime, Math.floorMod(System.nanoTime(), 48_000_000_000L) / 1_000_000_000f);')
])
edit(files[3],[
 ('if (model.getSceneId() == sceneId)','''// Keep the complete portal on the oversized-model CPU sorting path.
        if (AuthoredModelTextures.isSlayerPortal(model)) return;

		if (model.getSceneId() == sceneId)''')
])
frag=(src/'ClientResources/net/runelite/client/plugins/gpu/frag.glsl').read_text()
motion=frag.split('  } else if (fTextureId == 41004) {',1)[1].split('  } else if',1)[0]
edit(files[4],[
 ('uniform int crystalWallLayers[25];','uniform int crystalWallLayers[26];\nuniform float slayerPortalTime;'),
 ('  } else if (fTextureId >= 40027 && fTextureId <= 40029) {','''  } else if (fTextureId == 41006) {
    int layer = crystalWallLayers[25];
    c = layer < 0 ? vec4(fColor.rgb, 1.0) : texture(crystalWallTextures, vec3(fUv, float(layer)));
    c.a = fAlpha;
  } else if (fTextureId == 41007) {'''+motion+'  } else if (fTextureId >= 40027 && fTextureId <= 40029) {')
])
auth=r/'authored'; incoming=src/'ClientResources/net/runelite/client/plugins/gpu/authored'
for i,(source,name) in enumerate(zip(('wall-left','wall-right','arch-base','rift'),('wall-slayer-portal','slayer-portal-right','slayer-portal-arch','slayer-portal-rift'))):
 d=bytearray((incoming/(source+'.uv')).read_bytes())
 struct.pack_into('>i',d,8,31)
 for offset in range(28,len(d),40):struct.pack_into('>i',d,offset,49012+i-65536)
 p=auth/(name+'.uv');assert not p.exists();p.write_bytes(d);acl.shared(p)
