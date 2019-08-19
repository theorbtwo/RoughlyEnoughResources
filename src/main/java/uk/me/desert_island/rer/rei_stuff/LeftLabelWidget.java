/*
 * Roughly Enough Items by Danielshe.
 * Licensed under the MIT License.
 */

package uk.me.desert_island.rer.rei_stuff;

import me.shedaniel.math.api.Rectangle;
import me.shedaniel.rei.gui.widget.WidgetWithBounds;
import net.minecraft.client.gui.Element;

import java.util.Collections;
import java.util.List;

public class LeftLabelWidget extends WidgetWithBounds {

    public int x;
    public int y;
    public String text;

    public LeftLabelWidget(int x, int y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
    }
    
    @Override
    public Rectangle getBounds() {
        int width = font.getStringWidth(text);
        return new Rectangle(x - width / 2 - 1, y - 5, width + 2, 14);
    }
    
    @Override
    public List<? extends Element> children() {
        return Collections.emptyList();
    }
    
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        drawString(font, text, x, y, -1);
    }
    
}
