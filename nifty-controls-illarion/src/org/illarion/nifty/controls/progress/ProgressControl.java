/*
 * This file is part of the Illarion Nifty-GUI Controls.
 *
 * Copyright © 2012 - Illarion e.V.
 *
 * The Illarion Nifty-GUI Controls is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Illarion Nifty-GUI Controls is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Illarion Nifty-GUI Controls.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.illarion.nifty.controls.progress;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.xml.xpp3.Attributes;
import org.illarion.nifty.controls.Progress;

import java.util.Properties;

/**
 * The control of the progress bar.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 * @deprecated Use {@link Progress}
 */
public final class ProgressControl extends AbstractController implements Progress {
    private int minImageWidth;
    private int maxWidth;

    @Override
    public void bind(final Nifty nifty, final Screen screen, final Element element, final Properties parameter,
                     final Attributes controlDefinitionAttributes) {
        bind(element);

        minImageWidth = Integer.parseInt(parameter.getProperty("minImageWidth", "0"));
    }

    @Override
    public void onStartScreen() {
        maxWidth = getElement().findElementByName("#fillArea").getWidth();
    }

    @Override
    public boolean inputEvent(final NiftyInputEvent inputEvent) {
        return false;
    }

    /**
     * Set the value of the progress. All values will be clamped to {@code 0.f} and {@code 1.f}.
     *
     * @param value the progress value
     */
    @Override
    public void setProgress(final float value) {
        final Element wrapper = getElement().findElementByName("#fillWrapper");
        final Element fill = getElement().findElementByName("#fill");

        final float usedValue;
        if (value < 0.f) {
            usedValue = 0.f;
        } else if (value > 1.f) {
            usedValue = 1.f;
        } else {
            usedValue = value;
        }

        final int width = Math.round(maxWidth * usedValue);

        fill.setConstraintWidth(SizeValue.px(Math.max(width, minImageWidth)));
        wrapper.setConstraintWidth(SizeValue.px(width));

        getElement().layoutElements();
    }
}
