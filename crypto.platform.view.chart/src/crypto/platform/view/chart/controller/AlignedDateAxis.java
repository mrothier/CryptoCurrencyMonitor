package crypto.platform.view.chart.controller;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTick;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueTick;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;

/**
 * Extension of DateAxis for jFreeChart graphs that can render tick labels in two ways: 
 *   1) labels under ticks (common way, same as DateAxis)
 *   2) labels in the middle of interval (new feature)
 * @author Honza Spurny, Czech Republic
 * @version 1.0
 */
public class AlignedDateAxis extends DateAxis {

    /**
     * Tick labels alignment setting.
     */
    private TickLabelPosition tickLabelPosition = TickLabelPosition.DEFAULT_VALUE;

    /**
     * Value for interface Serializable.
     */
    static private final long serialVersionUID = 1;

    // ***********************
    // ***      Enums      ***
    // ***********************

    /**
     * Tick label alignment modes.
     */
    public enum TickLabelPosition {
        /**
         * Tick label is rendered under/next by own tick. 
         * (common rendering same as in DateAxis)
         */
        INTERVAL_START,
        /**
         * Tick label is placed in the middle of interval
         * (between two subsequent ticks) 
         */
        INTERVAL_MIDDLE;

        static public final TickLabelPosition DEFAULT_VALUE = INTERVAL_START;
    }

    // ******************************
    // ***      Constructors      ***
    // ******************************

    /**
     * Default constructor.
     */
    public AlignedDateAxis() {
        super();
    }

    /**
     * Constructor.
     * @param tickLabelPos tick label alignment mode setting for this axis.
     */
    public AlignedDateAxis(TickLabelPosition tickLabelPos) {
        this();
        this.setTickLabelPosition(tickLabelPos);
    }

    // *********************************
    // ***      GET/SET methods      ***
    // *********************************

    public TickLabelPosition getTickLabelPosition() {
        return this.tickLabelPosition;
    }

    public void setTickLabelPosition(TickLabelPosition value) {
        this.tickLabelPosition = value;
    }

    // *******************************************************
    // ***      Overrided methods for label rendering      ***
    // *******************************************************

    /**
     * Auxiliary method to calculate tick label position of given tick.
     * @param tick tick we need calculate label position for (DateTick is expected)
     * @param cursor the cursor
     * @param dataArea area to draw the ticks and labels in
     * @param edge edge of dataArea
     * @return Returns coordinates [x,y] where label should be placed.
     */
    @Override
    protected float[] calculateAnchorPoint(ValueTick tick, double cursor, Rectangle2D dataArea, RectangleEdge edge) {
        float[] resultAnchor = super.calculateAnchorPoint(tick, cursor, dataArea, edge);

        // Time of tick
        Date tickDate = (tick instanceof DateTick) ? 
                        ((DateTick) tick).getDate() : 
                        new Date((long)tick.getValue());

        // Tick label shift.
        // (for INTERVAL_START it is 0, for INTERVAL_MIDDLE it is calculated)
        double labelShift;

        switch (this.getTickLabelPosition()) {
            case INTERVAL_MIDDLE:
                // Getting next tick value...
                DateTickUnit unit = this.getTickUnit();
                Date nextTickDate = unit.addToDate(tickDate, this.getTimeZone());
                double nextTickVal = this.valueToJava2D(nextTickDate.getTime(), dataArea, edge);

                // Shifting label in between ticks...
                labelShift = (nextTickVal - resultAnchor[0]) / 4;
                break;

            case INTERVAL_START:
            default:
                labelShift = 0;
                break;
        }

        // Edge defines which coordinate is shifted.
        if (RectangleEdge.isTopOrBottom(edge)) {
            resultAnchor[0] += labelShift;
        } else if (RectangleEdge.isLeftOrRight(edge)) {
            resultAnchor[1] += labelShift;
        }

        return resultAnchor;
    }

    /**
     * Renders this axis with ticks and labels.
     * @param g2 graphics to draw the axis in.
     * @param cursor the cursor
     * @param plotArea area to draw the chart in
     * @param dataArea area to draw this axis in
     * @param edge edge of dataArea
     * @return Returns state of axis.
     */
    @Override
    protected AxisState drawTickMarksAndLabels(Graphics2D g2, double cursor, Rectangle2D plotArea, Rectangle2D dataArea, RectangleEdge edge) {
        AxisState state = new AxisState(cursor);
        if (this.isAxisLineVisible()) this.drawAxisLine(g2, cursor, dataArea, edge);

        @SuppressWarnings("unchecked")
        List<DateTick> ticks = this.refreshTicks(g2, state, dataArea, edge);
        state.setTicks(ticks);
        g2.setFont(this.getTickLabelFont());

        for (DateTick tick: ticks) {        
            if (this.isTickLabelsVisible()) {
                g2.setPaint(this.getTickLabelPaint());
                float anchorPoint[] = this.calculateAnchorPoint(tick, cursor, dataArea, edge);

                // TextUtilities.drawRotatedString(tick.getText(), g2, anchorPoint[0], anchorPoint[1], tick.getTextAnchor(), tick.getAngle(), tick.getRotationAnchor());
                // Commented code above is original code from DateAxis.

                // ---[Override]---
                // Position of tick label is shifted so it can point outside the dataArea.
                // We have to check whether the tick label on this position is drawable into the given area.                            
                Shape labelBounds = TextUtilities.calculateRotatedStringBounds(tick.getText(), g2, anchorPoint[0], anchorPoint[1], tick.getTextAnchor(), tick.getAngle(), tick.getRotationAnchor());
                double labelEdge = (RectangleEdge.isTopOrBottom(edge)) ? (labelBounds.getBounds2D().getMaxX()) : (labelBounds.getBounds2D().getMaxY());
                double dataAreaBound = (RectangleEdge.isTopOrBottom(edge)) ? (dataArea.getMaxX()) : (dataArea.getMaxY());

                // Magic constant 5: tick label can be rendered although it exceeds area edge for max. 5px
                // (it still looks good - visualy tested :-)
                if ((labelEdge - 5) <= dataAreaBound) {
                    TextUtilities.drawRotatedString(tick.getText(), g2, anchorPoint[0], anchorPoint[1], tick.getTextAnchor(), tick.getAngle(), tick.getRotationAnchor());
                }
                // ---[/Override]---
            }

            if ( (this.isTickMarksVisible() && tick.getTickType().equals(TickType.MAJOR)) || (this.isMinorTickMarksVisible() && tick.getTickType().equals(TickType.MINOR)) ) {
                double ol = tick.getTickType().equals(TickType.MINOR) ? this.getMinorTickMarkOutsideLength() : this.getTickMarkOutsideLength();
                double il = tick.getTickType().equals(TickType.MINOR) ? this.getMinorTickMarkInsideLength() : this.getTickMarkInsideLength();
                float tickVal = (float)this.valueToJava2D(tick.getValue(), dataArea, edge);
                Line2D mark = null;
                g2.setStroke(this.getTickMarkStroke());
                g2.setPaint(this.getTickMarkPaint());
                if(edge == RectangleEdge.LEFT) {
                    mark = new Line2D.Double(cursor - ol, tickVal, cursor + il, tickVal);
                } else if(edge == RectangleEdge.RIGHT) {
                    mark = new Line2D.Double(cursor + ol, tickVal, cursor - il, tickVal);
                } else if(edge == RectangleEdge.TOP) {
                    mark = new Line2D.Double(tickVal, cursor - ol, tickVal, cursor + il);
                } else if(edge == RectangleEdge.BOTTOM) {
                    mark = new Line2D.Double(tickVal, cursor + ol, tickVal, cursor - il);
                }
                g2.draw(mark);
            }
        }

        if (this. isTickLabelsVisible()) {
            double used = 0.0;            
                if (edge == RectangleEdge.LEFT) {
                    used += this.findMaximumTickLabelWidth(ticks, g2, plotArea, this.isVerticalTickLabels());
                    state.cursorLeft(used);
                } else if (edge == RectangleEdge.RIGHT) {
                    used = this.findMaximumTickLabelWidth(ticks, g2, plotArea, this.isVerticalTickLabels());
                    state.cursorRight(used);
                } else if (edge == RectangleEdge.TOP) {
                    used = this.findMaximumTickLabelHeight(ticks, g2, plotArea, this.isVerticalTickLabels());
                    state.cursorUp(used);
                } else if (edge == RectangleEdge.BOTTOM) {
                    used = this.findMaximumTickLabelHeight(ticks, g2, plotArea, this.isVerticalTickLabels());
                    state.cursorDown(used);
                }
        }

        return state;
    }
}