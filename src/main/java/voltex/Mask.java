/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2023 Fiji developers.
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

package voltex;

import java.awt.Choice;
import java.awt.Color;
import java.awt.Font;
import java.awt.Label;
import java.awt.Polygon;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.TextureAttributes;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3i;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;

public class Mask extends VoltexVolume {

	private final VoltexVolume image;
	private final BranchGroup node;

	public enum BlendMethod {
		REPLACE(TextureAttributes.COMBINE_REPLACE, "C = C0"), MODULATE(
			TextureAttributes.COMBINE_MODULATE, "C = C0 C1"), ADD(
			TextureAttributes.COMBINE_ADD, "C = C0 + C1"), ADD_SIGNED(
			TextureAttributes.COMBINE_ADD_SIGNED, "C = C0 + C1 - 0.5"), SUBTRACT(
			TextureAttributes.COMBINE_SUBTRACT, "C = C0 - C1"), INTERPOLATE(
			TextureAttributes.COMBINE_INTERPOLATE, "C0 C2 + C1 (1 - C2)");

		private int value;
		private String desc;

		BlendMethod(final int v, final String d) {
			this.value = v;
			this.desc = d;
		}

		public String fullString() {
			return name() + ": " + desc;
		}
	}

	public enum BlendSource {
		TEXTURE(TextureAttributes.COMBINE_PREVIOUS_TEXTURE_UNIT_STATE), MASK(
			TextureAttributes.COMBINE_TEXTURE_COLOR), COLOR(
			TextureAttributes.COMBINE_CONSTANT_COLOR);

		private int value;

		BlendSource(final int v) {
			this.value = v;
		}
	}

	/** texture attributes for the mask */
	private TextureAttributes maskAttr;

	/** default color blend method */
	private BlendMethod colorMethod = BlendMethod.MODULATE;

	/** default alpha blend method */
	private BlendMethod alphaMethod = BlendMethod.REPLACE;

	/** default color sources; array of length 3 */
	private final BlendSource[] colorSource = new BlendSource[] {
		BlendSource.TEXTURE, BlendSource.MASK, BlendSource.COLOR };

	/** default alpha sources; array of length 3 */
	private final BlendSource[] alphaSource = new BlendSource[] {
		BlendSource.TEXTURE, BlendSource.MASK, BlendSource.COLOR };

	/** default blend color */
	private final Color4f blendColor = new Color4f(0, 0, 0, 0);

	public static final int BG = 50;

	public Mask(final VoltexVolume image, final BranchGroup node) {
		super(createMaskImage(image));
		this.image = image;
		this.node = node;
		initTextureAttributes();
	}

	private static ImagePlus createMaskImage(final VoltexVolume image) {
		final ImagePlus maskI =
			IJ.createImage("Mask", "8-bit white", image.xDim, image.yDim, image.zDim);
		maskI.setCalibration(image.getImagePlus().getCalibration().copy());
		return maskI;
	}

	public TextureAttributes getMaskAttributes() {
		return maskAttr;
	}

	public void subtractInverse(final Canvas3D canvas, final Roi roi) {
		final Polygon p = roi.getPolygon();

		final Transform3D volToIP = new Transform3D();
		volumeToImagePlate(canvas, volToIP);

		final Point2d onCanvas = new Point2d();
		final Point3i pos = new Point3i(0, 0, 0);
		for (int z = 0; z < zDim; z++) {
			for (int y = 0; y < yDim; y++) {
				for (int x = 0; x < xDim; x++) {
					volumePointInCanvas(canvas, volToIP, x, y, z, onCanvas);
					if (!p.contains(onCanvas.x, onCanvas.y)) {
						setNoCheckNoUpdate(x, y, z, BG);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, zDim);
		}
		updateData();
	}

	public void subtract(final Canvas3D canvas, final Roi roi) {}

	public ImagePlus getMask() {
		return imp;
	}

	public void cropToMask() {}

	public void upateMask() {}

	/* ***************************************************************
	 * Blending stuff
	 * **************************************************************/
	public void setBlendColor(final Color4f col) {
		blendColor.set(col);
		maskAttr.setTextureBlendColor(blendColor);
	}

	public void setColorSource(final int index, final BlendSource c) {
		colorSource[index] = c;
		maskAttr.setCombineRgbSource(index, colorSource[index].value);
	}

	public BlendSource getColorSource(final int index) {
		return colorSource[index];
	}

	public void setColorMethod(final BlendMethod m) {
		colorMethod = m;
		maskAttr.setCombineRgbMode(colorMethod.value);
	}

	public BlendMethod getColorMethod() {
		return colorMethod;
	}

	public void setAlphaSource(final int index, final BlendSource c) {
		alphaSource[index] = c;
		maskAttr.setCombineAlphaSource(index, alphaSource[index].value);
	}

	public BlendSource getAlphaSource(final int index) {
		return alphaSource[index];
	}

	public void setAlphaMethod(final BlendMethod m) {
		alphaMethod = m;
		maskAttr.setCombineAlphaMode(alphaMethod.value);
	}

	public BlendMethod getAlphaMethod() {
		return alphaMethod;
	}

	public void interactivelyChangeBlending() {
		final String[] methods = new String[BlendMethod.values().length];
		int i = 0;
		for (final BlendMethod bm : BlendMethod.values())
			methods[i++] = bm.fullString();

		final String[] sources = new String[BlendSource.values().length];
		i = 0;
		for (final BlendSource bs : BlendSource.values())
			sources[i++] = bs.name();

		final GenericDialog gd = new GenericDialog("Blending");

		gd.setInsets(5, 0, 0);
		gd.addMessage("RGB blending parameters:");
		Label l = (Label) gd.getMessage();
		l.setFont(new Font("Helvetica", Font.BOLD, 12));
		l.setForeground(Color.BLUE);
		gd.addChoice("Color blend method", methods, methods[colorMethod.ordinal()]);
		gd.addChoice("C1", sources, sources[colorSource[0].ordinal()]);
		gd.addChoice("C2", sources, sources[colorSource[1].ordinal()]);
		gd.addChoice("C3", sources, sources[colorSource[2].ordinal()]);

		gd.setInsets(5, 0, 0);
		gd.addMessage("Alpha blending parameters:");
		l = (Label) gd.getMessage();
		l.setFont(new Font("Helvetica", Font.BOLD, 12));
		l.setForeground(Color.BLUE);
		gd.addChoice("Alpha blend method", methods, methods[alphaMethod.ordinal()]);
		gd.addChoice("A1", sources, sources[alphaSource[0].ordinal()]);
		gd.addChoice("A2", sources, sources[alphaSource[1].ordinal()]);
		gd.addChoice("A3", sources, sources[alphaSource[2].ordinal()]);

		gd.setInsets(5, 0, 0);
		gd.addMessage("COLOR values:");
		l = (Label) gd.getMessage();
		l.setFont(new Font("Helvetica", Font.BOLD, 12));
		l.setForeground(Color.BLUE);
		gd.addSlider("Red", 0.0, 255.0, 255 * blendColor.x);
		gd.addSlider("Green", 0.0, 255.0, 255 * blendColor.y);
		gd.addSlider("Blue", 0.0, 255.0, 255 * blendColor.z);
		gd.addSlider("Alpha", 0.0, 255.0, 255 * blendColor.w);

// 		Vector choices = gd.getChoices();
// 		for(i = 0; i < choices.size(); i++) {
// 			((Choice)choices.get(i)).addItemListener(new ItemListener() {
// 				public void itemStateChanged(ItemEvent e) {
// 					updateMaskAttributes();
// 				}
// 			});
// 		}

		final Vector choices = gd.getChoices();
		// color
		final Choice c1 = (Choice) choices.get(0);
		c1.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setColorMethod(BlendMethod.values()[c1.getSelectedIndex()]);
			}
		});

		final Choice c2 = (Choice) choices.get(1);
		c2.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setColorSource(0, BlendSource.values()[c2.getSelectedIndex()]);
			}
		});

		final Choice c3 = (Choice) choices.get(2);
		c3.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setColorSource(1, BlendSource.values()[c3.getSelectedIndex()]);
			}
		});

		final Choice c4 = (Choice) choices.get(3);
		c4.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setColorSource(2, BlendSource.values()[c4.getSelectedIndex()]);
			}
		});

		// alpha
		final Choice c5 = (Choice) choices.get(4);
		c5.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setAlphaMethod(BlendMethod.values()[c5.getSelectedIndex()]);
			}
		});

		final Choice c6 = (Choice) choices.get(5);
		c6.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setAlphaSource(0, BlendSource.values()[c6.getSelectedIndex()]);
			}
		});

		final Choice c7 = (Choice) choices.get(6);
		c7.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setAlphaSource(1, BlendSource.values()[c7.getSelectedIndex()]);
			}
		});

		final Choice c8 = (Choice) choices.get(7);
		c8.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				setAlphaSource(2, BlendSource.values()[c8.getSelectedIndex()]);
			}
		});

		// blend color
		final Vector sliders = gd.getSliders();
		final Scrollbar s1 = (Scrollbar) sliders.get(0);
		s1.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				blendColor.x = s1.getValue() / 255f;
				maskAttr.setTextureBlendColor(blendColor);
			}
		});

		final Scrollbar s2 = (Scrollbar) sliders.get(1);
		s2.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				blendColor.y = s2.getValue() / 255f;
				maskAttr.setTextureBlendColor(blendColor);
			}
		});

		final Scrollbar s3 = (Scrollbar) sliders.get(2);
		s3.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				blendColor.z = s3.getValue() / 255f;
				maskAttr.setTextureBlendColor(blendColor);
			}
		});

		final Scrollbar s4 = (Scrollbar) sliders.get(3);
		s4.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				blendColor.w = s4.getValue() / 255f;
				maskAttr.setTextureBlendColor(blendColor);
			}
		});

		gd.setModal(false);
		gd.showDialog();

// 		if(gd.wasCanceled())
// 			return;
//
// 		colorMethod = BlendMethod.values()[gd.getNextChoiceIndex()];
// 		colorSource[0] = BlendSource.values()[gd.getNextChoiceIndex()];
// 		colorSource[1] = BlendSource.values()[gd.getNextChoiceIndex()];
// 		colorSource[2] = BlendSource.values()[gd.getNextChoiceIndex()];
//
// 		alphaMethod = BlendMethod.values()[gd.getNextChoiceIndex()];
// 		alphaSource[0] = BlendSource.values()[gd.getNextChoiceIndex()];
// 		alphaSource[1] = BlendSource.values()[gd.getNextChoiceIndex()];
// 		alphaSource[2] = BlendSource.values()[gd.getNextChoiceIndex()];
//
// 		blendColor.set((float)gd.getNextNumber() / 255f, // red
// 			(float)gd.getNextNumber() / 255f, // green
// 			(float)gd.getNextNumber() / 255f, // blue
// 			(float)gd.getNextNumber() / 255f); // alpha
//
//
// 		updateMaskAttributes();
	}

	public void updateMaskAttributes() {
		/* For the RGB components:
		 * C' = C0 * C1, where C0 is the color of the prev unit state
		 *               and C1 is the texture color
		 */
		maskAttr.setCombineRgbMode(colorMethod.value);
		maskAttr.setCombineRgbSource(0, colorSource[0].value);
		maskAttr.setCombineRgbSource(1, colorSource[1].value);
		maskAttr.setCombineRgbSource(2, colorSource[2].value);

		/* For the alpha component:
		 * Use alpha from the prev. texture unit state.
		 * C' = C0, where C0 is the color or the prev. unit state.
		 */
		maskAttr.setCombineAlphaMode(alphaMethod.value);
		maskAttr.setCombineAlphaSource(0, alphaSource[0].value);
		maskAttr.setCombineAlphaSource(1, alphaSource[1].value);
		maskAttr.setCombineAlphaSource(2, alphaSource[2].value);

		maskAttr.setTextureBlendColor(blendColor);
	}

	/* ***************************************************************
	 * Private helpher functions.
	 * **************************************************************/
	private final Point3d locInImagePlate = new Point3d();

	private void volumePointInCanvas(final Canvas3D canvas,
		final Transform3D volToIP, final int x, final int y, final int z,
		final Point2d out)
	{

		locInImagePlate.set(x * pw, y * ph, z * pd);
		volToIP.transform(locInImagePlate);
		canvas.getPixelLocationFromImagePlate(locInImagePlate, out);
	}

	private void volumeToImagePlate(final Canvas3D canvas,
		final Transform3D volToIP)
	{
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();

		final Transform3D toVWorld = new Transform3D();
		node.getLocalToVworld(toVWorld);
		volToIP.mul(toVWorld);
	}

	private void initTextureAttributes() {
		this.maskAttr = new TextureAttributes();
		maskAttr.setCapability(TextureAttributes.ALLOW_BLEND_COLOR_WRITE);
		maskAttr.setCapability(TextureAttributes.ALLOW_COMBINE_WRITE);
		maskAttr.setTextureMode(TextureAttributes.COMBINE);
		maskAttr.setPerspectiveCorrectionMode(TextureAttributes.NICEST);

		updateMaskAttributes();
	}
}
