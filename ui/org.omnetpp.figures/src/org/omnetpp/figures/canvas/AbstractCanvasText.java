package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionDimension;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.figures.misc.AnchoredRectangle;
import org.omnetpp.figures.misc.AnchoredRectangle.Anchor;

/**
 * HACK uses 1<<2 in the style bitfield as if it was SWT.UNDERLINE in the fontData getter and setter
 */
@SuppressWarnings("deprecation") // because of the font field, which will get hidden sometime, but just insert it here if it gets removed
public abstract class AbstractCanvasText extends AbstractCanvasFigure {

    protected static final int UNDERLINE = 1 << 2;

    public enum Alignment { ALIGN_LEFT, ALIGN_RIGHT, ALIGN_CENTER };

    protected TextLayout layout;

    // the size of this is slaved to the layout's size
    protected AnchoredRectangle anchoringRectangle;

    protected FontData fontData;
    protected boolean underline = false;
    protected double opacity = 1;

    public AbstractCanvasText() {
        layout = new TextLayout(Display.getDefault());

        fontData = new FontData("Arial", 10, SWT.NORMAL);
        anchoringRectangle = new AnchoredRectangle();
    }

    protected void updateRectangleSize() {
        org.eclipse.swt.graphics.Rectangle bounds = layout.getBounds();
        anchoringRectangle.setSize(new PrecisionDimension(bounds.width, bounds.height));
    }

    protected void updateLayout() {
        font = new Font(Display.getDefault(), fontData);

        layout.setFont(font);

        TextStyle style = new TextStyle();

        style.underline = underline;
        style.font = font;

        layout.setStyle(style, 0, layout.getText().length());

        updateRectangleSize();

        repaint();
    }

    public Anchor getAnchor() {
        return anchoringRectangle.getAnchor();
    }

    public void setAnchor(Anchor anchor) {
        if (anchoringRectangle.getAnchor() != anchor) {
            anchoringRectangle.setAnchor(anchor);
            repaint();
        }
    }

    public String getText() {
        return layout.getText();
    }

    public void setText(String text) {
        if (!layout.getText().equals(text)) {
            layout.setText(StringUtils.nullToEmpty(text));
            updateLayout();
            repaint();
        }
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font f) {
        setFontData(f.getFontData()[0]);

        updateLayout();
    }

    public FontData getFontData() {
        int style = fontData.getStyle();
        style = style & (~UNDERLINE);

        if (underline) {
            style = style | UNDERLINE;
        }

        fontData.setStyle(style);

        return fontData;
    }

    public void setFontData(FontData fd) {
        if (!fontData.equals(fd)) {
            fontData = fd;
            underline = ((fontData.getStyle() & UNDERLINE) != 0);

            updateLayout();
        }
    }

    public String getFontName() {
        return fontData.getName();
    }

    public void setFontName(String name) {
        if (!getFontName().equals(name)) {
            fontData.setName(name);
            updateLayout();
        }
    }

    public int getFontSize() {
        return fontData.getHeight();
    }

    public void setFontSize(int size) {
        if (getFontSize() != size) {
            fontData.setHeight(size);
            updateLayout();
        }
    }

    public boolean getBold() {
        return (fontData.getStyle() & SWT.BOLD) != 0;
    }

    public void setBold(boolean bold) {
        if (getBold() != bold) {

            int style = fontData.getStyle();
            style = style & (~SWT.BOLD);

            if (bold) {
                style = style | SWT.BOLD;
            }

            fontData.setStyle(style);

            updateLayout();
        }
    }

    public boolean getItalic() {
        return (fontData.getStyle() & SWT.ITALIC) != 0;
    }

    public void setItalic(boolean italic) {
        if (getItalic() != italic) {

            int style = fontData.getStyle();
            style = style & (~SWT.ITALIC);

            if (italic) {
                style = style | SWT.ITALIC;
            }

            fontData.setStyle(style);

            updateLayout();
        }
    }

    public boolean getUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        if (this.underline != underline) {
            this.underline = underline;
            updateLayout();
        }
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        opacity = Math.min(Math.max(opacity, 0), 1); // clamping
        if (this.opacity != opacity) {
            this.opacity = opacity;
            repaint();
        }
    }

    public Alignment getAlignment() {
        switch (layout.getAlignment()) {
        case SWT.CENTER:
            return Alignment.ALIGN_CENTER;
        case SWT.RIGHT:
            return Alignment.ALIGN_RIGHT;
        default:
            return Alignment.ALIGN_LEFT;
        }
    }

    public void setAlignment(Alignment align) {
        if (getAlignment() != align) {
            switch (align) {
            case ALIGN_CENTER:
                layout.setAlignment(SWT.CENTER);
                break;
            case ALIGN_RIGHT:
                layout.setAlignment(SWT.RIGHT);
                break;
            default:
                layout.setAlignment(SWT.LEFT);
            }

            repaint();
        }
    }

    public PrecisionPoint getPosition() {
        return anchoringRectangle.getLocation();
    }

    public void setPosition(Point p) {
        if (!anchoringRectangle.getLocation().equals(p)) {
            anchoringRectangle.setLocation(p);
            repaint();
        }
    }
}
