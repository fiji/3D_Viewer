/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2022 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package octree;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.ImageComponent;
import org.scijava.java3d.ImageComponent2D;
import org.scijava.java3d.Material;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.RenderingAttributes;
import org.scijava.java3d.Texture;
import org.scijava.java3d.Texture2D;
import org.scijava.java3d.TextureAttributes;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.vecmath.Color3f;

import ij3d.AxisConstants;

public class AppearanceCreator implements AxisConstants {

	private static final int TEX_MODE = Texture.INTENSITY;
	private static final int COMP_TYPE = ImageComponent.FORMAT_CHANNEL8;
	private static final boolean BY_REF = true;
	private static final boolean Y_UP = true;
	private static final int SIZE = VolumeOctree.SIZE;

	private TextureAttributes texAttr;
	private TransparencyAttributes transAttr;
	private PolygonAttributes polyAttr;
	private Material material;
	private ColoringAttributes colAttr;
	private RenderingAttributes rendAttr;

	private static AppearanceCreator instance;

	private AppearanceCreator() {
		initAttributes(null, 0.1f);
	}

	public static AppearanceCreator instance() {
		if (instance == null) instance = new AppearanceCreator();
		return instance;
	}

	public Appearance getAppearance(final CubeData cdata, final int index) {
		final Appearance a = new Appearance();
		a.setMaterial(material);
		a.setTransparencyAttributes(transAttr);
		a.setPolygonAttributes(polyAttr);
		a.setColoringAttributes(colAttr);
		a.setRenderingAttributes(rendAttr);

		a.setTexture(getTexture(cdata, index));
		a.setTexCoordGeneration(cdata.tg);
		a.setTextureAttributes(texAttr);
		return a;
	}

	public void setTransparency(final float f) {
		transAttr.setTransparency(f);
	}

	public void setThreshold(final float f) {
		rendAttr.setAlphaTestValue(f);
	}

	public void setColor(final Color3f c) {
		colAttr.setColor(c);
	}

	private Texture2D getTexture(final CubeData cdata, final int index) {
		final Texture2D tex =
			new Texture2D(Texture.BASE_LEVEL, TEX_MODE, SIZE, SIZE);
		final ImageComponent2D pArray =
			new ImageComponent2D(COMP_TYPE, SIZE, SIZE, BY_REF, Y_UP);
		pArray.set(cdata.images[index]);

		tex.setImage(0, pArray);
		tex.setEnable(true);
		tex.setMinFilter(Texture.BASE_LEVEL_LINEAR);
		tex.setMagFilter(Texture.BASE_LEVEL_LINEAR);

		tex.setBoundaryModeS(Texture.CLAMP);
		tex.setBoundaryModeT(Texture.CLAMP);
		return tex;
	}

	private void initAttributes(final Color3f color, final float transparency) {
		texAttr = new TextureAttributes();
		texAttr.setTextureMode(TextureAttributes.COMBINE);
		texAttr.setCombineRgbMode(TextureAttributes.COMBINE_MODULATE);
		texAttr.setPerspectiveCorrectionMode(TextureAttributes.NICEST);

		transAttr = new TransparencyAttributes();
		transAttr.setTransparency(0.1f);
		transAttr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		transAttr.setTransparencyMode(TransparencyAttributes.BLENDED);
		transAttr.setTransparency(transparency);

		polyAttr = new PolygonAttributes();
		polyAttr.setCullFace(PolygonAttributes.CULL_NONE);

		material = new Material();
		material.setLightingEnable(false);

		colAttr = new ColoringAttributes();
		colAttr.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		colAttr.setShadeModel(ColoringAttributes.NICEST);
		if (color == null) {
			colAttr.setColor(1f, 1f, 1f);
		}
		else {
			colAttr.setColor(color);
		}

		// Avoid rendering of voxels having an alpha value of zero
		rendAttr = new RenderingAttributes();
		rendAttr.setDepthTestFunction(RenderingAttributes.ALWAYS);
		rendAttr.setCapability(RenderingAttributes.ALLOW_ALPHA_TEST_VALUE_WRITE);
		rendAttr.setAlphaTestValue(0f);
		rendAttr.setAlphaTestFunction(RenderingAttributes.GREATER);
	}
}
