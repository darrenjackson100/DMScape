package net.runelite.client.plugins.gpu;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import net.runelite.api.Model;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.system.MemoryUtil;

/** Original UV artwork for explicitly identified custom models, independent of cache palettes. */
@Singleton
class AuthoredModelTextures
{
	static final int FIRST_TEXTURE = 40001;
	private final Entry[] entries;
	private boolean requested;
	private int textureArray;

	AuthoredModelTextures()
	{
		String[] names = {"wall-left", "wall-right", "arch-base", "rift"};
		entries = new Entry[names.length];
		try
		{
			for (int i = 0; i < names.length; i++)
			{
				try (DataInputStream in = new DataInputStream(resource(names[i] + ".uv")))
				{
					entries[i] = new Entry(in);
				}
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Unable to load authored model UVs", ex);
		}
	}

	private static InputStream resource(String name) throws IOException
	{
		InputStream in = AuthoredModelTextures.class.getResourceAsStream("authored/" + name);
		if (in == null)
		{
			throw new IOException("Missing authored texture resource: " + name);
		}
		return in;
	}

	Entry find(Model model)
	{
		short[] textures = model.getFaceTextures();
		if (textures == null || model.getFaceCount() == 0)
		{
			return null;
		}
		for (Entry entry : entries)
		{
			if (entry.matches(model, textures))
			{
				requested = true;
				return entry;
			}
		}
		return null;
	}

	/** Called on the rendering thread after model uploads. Unit 3 is reserved for authored artwork. */
	void bind()
	{
        int program=GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM);
        if(program!=0) GL33C.glUniform1f(GL33C.glGetUniformLocation(program,"slayerPortalTime"),(float)((System.nanoTime()*1e-9)%48.0));
		if (!requested)
		{
			return;
		}
		int previousActive = GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
		GL33C.glActiveTexture(GL33C.GL_TEXTURE3);
		try
		{
			if (textureArray == 0)
			{
				if (GL33C.glGetInteger(GL33C.GL_MAX_TEXTURE_SIZE) < 4096)
				{
					throw new IllegalStateException("Original artwork requires 4096-pixel GPU textures");
				}
				textureArray = GL33C.glGenTextures();
				GL33C.glBindTexture(GL33C.GL_TEXTURE_2D_ARRAY, textureArray);
				GL33C.glTexImage3D(GL33C.GL_TEXTURE_2D_ARRAY, 0, GL33C.GL_RGBA8, 4096, 4096, 4, 0,
					GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
				try
				{
					for (int layer = 0; layer < 4; layer++)
					{
						BufferedImage image;
						try (InputStream in = resource(layer == 3 ? "perks-atlas.png" : layer + ".png")) { image = ImageIO.read(in); }
						if (image == null || image.getWidth() != 4096 || image.getHeight() != 4096)
						{
							throw new IOException("Invalid original texture dimensions");
						}
						ByteBuffer pixels = MemoryUtil.memAlloc(4096 * 4096 * 4);
						try
						{
							int[] row = new int[4096];
							for (int y = 0; y < 4096; y++)
							{
								image.getRGB(0, y, 4096, 1, row, 0, 4096);
								for (int rgb : row)
								{
									pixels.put((byte)(rgb >> 16)).put((byte)(rgb >> 8)).put((byte)rgb).put((byte)(rgb >> 24));
								}
							}
							pixels.flip();
							GL33C.glTexSubImage3D(GL33C.GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, 4096, 4096, 1,
								GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, pixels);
						}
						finally { MemoryUtil.memFree(pixels); }
					}
				}
				catch (IOException ex)
				{
					close();
					throw new IllegalStateException("Unable to upload original artwork", ex);
				}
				GL33C.glGenerateMipmap(GL33C.GL_TEXTURE_2D_ARRAY);
				GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D_ARRAY, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR_MIPMAP_LINEAR);
				GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D_ARRAY, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
				GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D_ARRAY, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
				GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D_ARRAY, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
				if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic)
				{
					GL33C.glTexParameterf(GL33C.GL_TEXTURE_2D_ARRAY, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
						Math.min(8, GL33C.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)));
				}
			}
			GL33C.glBindTexture(GL33C.GL_TEXTURE_2D_ARRAY, textureArray);
		}
		finally { GL33C.glActiveTexture(previousActive); }
	}

	void close()
	{
		if (textureArray != 0) { GL33C.glDeleteTextures(textureArray); }
		textureArray = 0;
		requested = false;
	}

	static final class Entry
	{
		final int count, layer;
		final int[] topology, textures;
		final double[] weights;

		Entry(DataInputStream in) throws IOException
		{
			if (in.readInt() != 0x55564f31) { throw new IOException("Invalid UV format"); }
			count = in.readInt(); layer = in.readInt(); in.readInt();
			if (count < 1 || count > 6144 || layer < 0 || layer > 3) { throw new IOException("Invalid UV header"); }
			topology = new int[count * 3]; textures = new int[count]; weights = new double[count * 9];
			for (int face = 0; face < count; face++)
			{
				for (int k = 0; k < 3; k++) { topology[face * 3 + k] = in.readInt(); }
				textures[face] = in.readInt();
				double u0=in.readFloat(), v0=in.readFloat(), u1=in.readFloat(), v1=in.readFloat(), u2=in.readFloat(), v2=in.readFloat();
				double det=(u1-u0)*(v2-v0)-(u2-u0)*(v1-v0);
				if (!Double.isFinite(det) || Math.abs(det) < 1e-12) { throw new IOException("Degenerate authored UV"); }
				for (int k = 0; k < 3; k++)
				{
					double u=(k==1?1:0)-u0, v=(k==2?1:0)-v0;
					double b=(u*(v2-v0)-v*(u2-u0))/det, c=(v*(u1-u0)-u*(v1-v0))/det;
					int offset=face*9+k*3;
					weights[offset]=1-b-c; weights[offset+1]=b; weights[offset+2]=c;
				}
			}
			if (in.read() != -1) { throw new IOException("Unexpected UV trailing data"); }
		}

		boolean matches(Model model, short[] textureIds)
		{
			if (model.getFaceCount() != count || textureIds[0] != textures[0]) { return false; }
			// ModelData combination can renumber/weld vertices. Face order and the complete
			// reserved texture assignment are preserved by that operation and by animation.
			for (int i=0; i<count; i++)
			{
				if (textureIds[i]!=textures[i]) { return false; }
			}
			return true;
		}

		/** Reconstruct the UV basis from this frame's vertices, so skinning never slides the artwork. */
		void push(Model model, int face, float[] x, float[] y, float[] z, GpuFloatBuffer out)
		{
			if (layer == 3 && textures[face] == -1) { for (int k=0;k<3;k++) out.put(0,0,0,0); return; }
			int a=model.getFaceIndices1()[face], b=model.getFaceIndices2()[face], c=model.getFaceIndices3()[face];
			for (int k=0; k<3; k++)
			{
				int i=face*9+k*3;
				double wa=weights[i], wb=weights[i+1], wc=weights[i+2];
				out.put(layer == 3 ? (textures[face] == -16525 ? 41004 : textures[face] == -16531 ? 41003 : textures[face] == -16532 ? 41002 : 41001) : FIRST_TEXTURE+layer, (float)(wa*x[a]+wb*x[b]+wc*x[c]),
					(float)(wa*y[a]+wb*y[b]+wc*y[c]), (float)(wa*z[a]+wb*z[b]+wc*z[c]));
			}
		}
	}
}
