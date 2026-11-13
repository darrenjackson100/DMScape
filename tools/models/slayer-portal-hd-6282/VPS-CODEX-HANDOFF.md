# Slayer portal 6282 — VPS integration handoff

Darren explicitly requested on 2026-11-13: push this and tell Textures/Objects/Imports on VPS to install and replace the existing teleporter. This authorizes destination-specific installation for this asset. No local cache has been changed.

Approved design: 3 x 1 plain beveled stone brick portal, HD skull-and-sword Slayer emblem embedded at the top, smoothly moving DARK red and black portal. No additional decorations. Use supplied art and geometry unchanged.

## Replacement target and remaining integration
Read CURRENT object 6282 and its server action handler first. Earlier read-only Windows inspection found name Teleporter, size 3x1, model57349/type10, animation20013, Task action, sound3139, interactType2/projectile blocking. Preserve current task teleport behavior, placements, rotation and footprint. Do not overwrite model57349 globally: it may be shared. Allocate verified unused model IDs and update only object6282. Old animation20013 must not deform the new static wall: use the new shader animation, with deliberate definition update.

The four DATs share an origin and must be rendered aligned. Total15416 triangles: left5880, right5880, arch-base3642, rift14. Production per-model GPU cap is6144 and sorting vertex array6500. DO NOT simply assign all four to a definition and allow ModelData combination to merge them into a single15k model; implement safe multipart rendering or a verified capacity change. Proof renders four independent models. Native roundtrip was verified, not destination installation.

## Authored GPU resources
DAT alone is not the approved appearance. The 4096px atlas contains stone plus accepted HD remastered Slayer emblem. Native markers49010/signed-16526 for atlas,49011/signed-16525 for portal are recognition tags, not native texture archives. Verify destination collisions and remap native and authored bindings together if needed.

ClientPatch/AuthoredModelTextures.java and ClientResources/.../frag.glsl are PREVIEW REFERENCE source, not safe whole-file overwrites. Merge their relevant functionality into current client while retaining ALL existing Olm/altar/item assets, registries, texture layers and shader branches. In particular perks-atlas.png and layer3 may already belong to the Ascension Altar: give this portal a dedicated resource name/layer/marker as necessary; never replace existing atlas blindly. Register four matching UVO1 UV files. Existing vertex face matching must remain intact after combination/rotation.

Shader branch41004 draws the continuous dark red/black vortex. Uniform slayerPortalTime supplied each render by monotonic clock modulo48 seconds. Pattern loops48 seconds. Preview is12 seconds/360frames at30fps. Integrate clock on active current shader program. Emblem priorities2 and aspect-preserving UVs prevent clipping/stretch. Maintain shader behavior and colors.

## Validation / deployment
Back up affected source and complete canonical VPS cache under /opt/Kryptic/Backups before integration. Follow current repository rules, coordinate cache lock, use authoritative packer, verify free IDs and decoded result. Source through Git; commit and push VPS integration before any later Windows work. Do not deploy dirty/unidentified source. Build required client/server, publish matching public JAR/hash using approved pipeline, restart gracefully as needed. Test object6282 in game from all angles/rotations, wall solidity, no z-fighting, emblem, continuous animation, task interaction/no-task case and collision. Check existing authored assets for regressions. Record commit, cache/asset hashes and verification limits. No new unlock scroll/entitlement is supplied because this replaces an existing world task teleporter rather than introducing a player-unlock animation.

Current validation: actual production SceneUploader GPU proof on AMD RX6700XT,360frames rendered; cacheWritten=false; loggedInTest=false. Editable Python/JSON sources included. No Blender source exists for this version. Source/run_tools.py and run_gpu.py reference Windows proof environment and runtime dependencies, adapt on VPS; use current matching repository cache tooling to integrate supplied native DATs. Do not execute them as an install script.
