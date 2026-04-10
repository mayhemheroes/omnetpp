//==========================================================================
//  CONNECTIONITEM.CC - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "connectionitem.h"

#include <cmath>
#include <QtCore/QDebug>
#include <QtGui/QPainter>
#include <QtGui/QPainterPathStroker>
#include <omnetpp/cgate.h>
#include <omnetpp/cchannel.h>
#include "graphicsitems.h"
#include "displaystringaccess.h"
#include "qtutil.h"

namespace omnetpp {
namespace qtenv {

// Returns the total length of a polyline.
static double polylineLength(const QPolygonF& poly)
{
    double len = 0;
    for (int i = 1; i < poly.size(); i++) {
        QPointF d = poly[i] - poly[i-1];
        len += std::sqrt(d.x()*d.x() + d.y()*d.y());
    }
    return len;
}

// Returns the point at fraction t (0..1) along the total polyline length.
static QPointF pointAtFraction(const QPolygonF& poly, double t)
{
    if (poly.size() < 2)
        return poly.isEmpty() ? QPointF() : poly[0];
    double totalLen = polylineLength(poly);
    double targetLen = t * totalLen;
    double accumulated = 0;
    for (int i = 1; i < poly.size(); i++) {
        QPointF d = poly[i] - poly[i-1];
        double segLen = std::sqrt(d.x()*d.x() + d.y()*d.y());
        if (accumulated + segLen >= targetLen) {
            double frac = (segLen > 0) ? (targetLen - accumulated) / segLen : 0;
            return poly[i-1] + d * frac;
        }
        accumulated += segLen;
    }
    return poly.last();
}

// Returns a sub-polyline between fractions t1 and t2 (0..1) of the total length.
static QPolygonF subPolyline(const QPolygonF& poly, double t1, double t2)
{
    if (poly.size() < 2)
        return poly;
    double totalLen = polylineLength(poly);
    double startLen = t1 * totalLen;
    double endLen = t2 * totalLen;
    QPolygonF result;
    double accumulated = 0;
    bool started = false;
    for (int i = 1; i < poly.size(); i++) {
        QPointF d = poly[i] - poly[i-1];
        double segLen = std::sqrt(d.x()*d.x() + d.y()*d.y());
        double segStart = accumulated;
        double segEnd = accumulated + segLen;
        if (!started && segEnd >= startLen) {
            double frac = (segLen > 0) ? (startLen - segStart) / segLen : 0;
            result << (poly[i-1] + d * frac);
            started = true;
        }
        if (started) {
            if (segEnd >= endLen) {
                double frac = (segLen > 0) ? (endLen - segStart) / segLen : 0;
                result << (poly[i-1] + d * frac);
                break;
            }
            result << poly[i];
        }
        accumulated += segLen;
    }
    return result;
}

// Builds a QPainterPath from a polyline.
static QPainterPath polylinePath(const QPolygonF& poly)
{
    QPainterPath path;
    if (poly.size() >= 2) {
        path.moveTo(poly[0]);
        for (int i = 1; i < poly.size(); i++)
            path.lineTo(poly[i]);
    }
    return path;
}

void ConnectionItemUtil::setupFromDisplayString(ConnectionItem *ci, cGate *gate, bool showArrowhead)
{
    cChannel *chan = gate->getChannel();

    cDisplayString ds = chan && chan->hasDisplayString() && chan->parametersFinalized()
            ? chan->getDisplayString()
            : cDisplayString();

    // replacing $param args with the actual parameter values
    std::string buffer;
    DisplayStringAccess dsa(&ds, chan);

    ci->setData(ITEMDATA_COBJECT, QVariant::fromValue((cObject *)gate));
    ci->setData(ITEMDATA_TOOLTIP, dsa.getTagArg("tt", 0, buffer));

    ci->setColor(parseColor(dsa.getTagArg("ls", 0, buffer), colors::BLACK));

    bool ok;
    double width = dsa.getTagArgAsDouble("ls", 1, 0.0, &ok);
    ci->setWidth(width);  // will display even with 0 width, as hairline

    // explicit 0 width, so hiding the line completely
    if (ok && width == 0)
        ci->setColor(colors::TRANSPARENT);

    const char *style = dsa.getTagArg("ls", 2, buffer);
    ci->setLineStyle(style[0] == 'd'
                      ? style[1] == 'a'
                         ? Qt::DashLine
                         : Qt::DotLine
                      : Qt::SolidLine);

    const char *text = dsa.getTagArg("t", 0, buffer);
    ci->setText(text);

    const char *textPos = dsa.getTagArg("t", 1, buffer);

    switch (textPos[0]) {
        case 'l': ci->setTextPosition(Qt::AlignLeft);   break;
        case 'r': ci->setTextPosition(Qt::AlignRight);  break;
        default:  ci->setTextPosition(Qt::AlignCenter); break;
    }

    ci->setTextColor(parseColor(dsa.getTagArg("t", 2, buffer), colors::DARKGREEN));

    bool twoWay = isTwoWayConnection(gate);

    // no need for arrowheads on a bidirectional connection
    ci->setArrowEnabled(!twoWay && showArrowhead);
    // only drawing the line from one side if the connection is bidirectional
    ci->setHalfLength(twoWay);
}

void ConnectionItem::updateLineItem()
{
    if (!lineEnabled || points.size() < 2) {
        lineItem->setPath(QPainterPath());
        lineItem->setPen(Qt::NoPen);
        shape_.clear();
        return;
    }

    // Compute the polyline to paint and the polyline for picking (shape)
    QPolygonF paintingPoly = points;
    QPolygonF pickingPoly = points;

    if (halfLength) {
        paintingPoly = subPolyline(points, 0, 0.5);
        pickingPoly = subPolyline(points, 0, 0.75);
    }
    else if (arrowItem->isVisible()) {
        // shorten the last segment so the line doesn't stick out of the arrowhead
        double totalLen = polylineLength(paintingPoly);
        if (totalLen > lineWidth)
            paintingPoly = subPolyline(points, 0, (totalLen - lineWidth) / totalLen);
    }

    QPen pen(lineColor, lineWidth);
    pen.setCapStyle(Qt::FlatCap);
    pen.setJoinStyle(Qt::MiterJoin);

    switch (lineStyle) {
        case Qt::DashLine: pen.setDashPattern(QVector<double>() << 2 << 2); break;
        case Qt::DotLine: pen.setDashPattern(QVector<double>() << 1 << 1); break;
        default: pen.setStyle(Qt::SolidLine);
    }

    pen.setDashOffset(dashOffset);

    lineItem->setPen(pen);

    // compute shape for picking using the picking polyline
    lineItem->setPath(polylinePath(pickingPoly));
    shape_ = lineItem->shape().simplified();

    // set the actual painting path
    lineItem->setPath(polylinePath(paintingPoly));
}

void ConnectionItem::updateTextItem()
{
    textItem->setText(text);
    textItem->setBrush(textColor);
    textItem->setVisible(isVisible());

    if (points.size() < 2)
        return;

    QPointF src = points.first();
    QPointF dest = points.last();
    QRectF textRect = textItem->boundingRect();
    QPointF textSize(textRect.width() + 4, textRect.height() + 4);

    switch (textAlignment) {
        case Qt::AlignLeft:
            textItem->setPos(pointAtFraction(points, 0.25) - textSize * 0.5 + QPoint(2, 2));
            textItem->setAlignment(Qt::AlignLeft);
            break;
        case Qt::AlignRight:
            textItem->setPos(pointAtFraction(points, 0.75) - textSize * 0.5 + QPoint(2, 2));
            textItem->setAlignment(Qt::AlignRight);
            break;
        default: // Center
            textItem->setPos(pointAtFraction(points, 0.5)
                              - QPoint(textSize.x() * 0.5,
                                       ((src.x()==dest.x()) ? (src.y()<dest.y()) : (src.x()<dest.x()))
                                        ? 0
                                        : textSize.y()) + QPoint(2, 2));
            textItem->setAlignment(Qt::AlignCenter);
    }
}

void ConnectionItem::updateArrowItem()
{
    if (!arrowItem->isVisible() || points.size() < 2) {
        arrowItem->setVisible(false);
        return;
    }

    // Use the direction of the last segment for arrowhead orientation
    QPointF lastStart = points[points.size()-2];
    QPointF lastEnd = points[points.size()-1];

    arrowItem->setVisible(true);
    arrowItem->setColor(lineColor);
    arrowItem->setEndPoints(lastStart, lastEnd);
    arrowItem->setSizeForPenWidth(lineWidth);
    arrowItem->setLineWidth(lineWidth);
    arrowItem->setData(ITEMDATA_COBJECT, data(ITEMDATA_COBJECT));
    arrowItem->setData(ITEMDATA_TOOLTIP, data(ITEMDATA_TOOLTIP));
}

ConnectionItem::ConnectionItem(QGraphicsItem *parent) :
    QGraphicsObject(parent)
{
    lineItem = new QGraphicsPathItem(this);
    // The text has to be a sibling, otherwise the pair line
    // of a twoway connection would obscure it.
    textItem = new MultiLineOutlinedTextItem(parentItem());
    textItem->setZValue(1); // connect is to update visibility
    connect(this, SIGNAL(visibleChanged()), this, SLOT(updateTextItem()));
    // TODO arrowItem disappear when a part of lineItem is out of view.
    arrowItem = new ArrowheadItem(lineItem);
}

ConnectionItem::~ConnectionItem()
{
    delete lineItem;
    delete textItem;
}

void ConnectionItem::setPoints(const QPolygonF& newPoints)
{
    if (points != newPoints) {
        points = newPoints;
        updateTextItem();
        updateArrowItem();
        updateLineItem();
    }
}

void ConnectionItem::setLine(const QLineF& line)
{
    QPolygonF poly;
    poly << line.p1() << line.p2();
    setPoints(poly);
}

void ConnectionItem::setSource(const QPointF& source)
{
    if (points.isEmpty()) {
        QPolygonF poly;
        poly << source << source;
        setPoints(poly);
    }
    else if (points.first() != source) {
        points[0] = source;
        updateTextItem();
        updateArrowItem();
        updateLineItem();
    }
}

void ConnectionItem::setDestination(const QPointF& destination)
{
    if (points.isEmpty()) {
        QPolygonF poly;
        poly << destination << destination;
        setPoints(poly);
    }
    else if (points.last() != destination) {
        points[points.size()-1] = destination;
        updateTextItem();
        updateArrowItem();
        updateLineItem();
    }
}

void ConnectionItem::setWidth(double width)
{
    if (width != lineWidth) {
        lineWidth = width;
        updateArrowItem();
        updateLineItem();
    }
}

void ConnectionItem::setColor(const QColor& color)
{
    if (color != lineColor) {
        lineColor = color;
        updateArrowItem();
        updateLineItem();
    }
}

void ConnectionItem::setLineStyle(Qt::PenStyle style)
{
    ASSERT2(style == Qt::SolidLine
             || style == Qt::DashLine
             || style == Qt::DotLine,
           "Unsupported line style");

    if (style != lineStyle) {
        lineStyle = style;
        updateLineItem();
    }
}

void ConnectionItem::setDashOffset(double offset)
{
    if (dashOffset != offset) {
        dashOffset = offset;
        updateLineItem();
    }
}

void ConnectionItem::setText(const QString& text)
{
    if (this->text != text) {
        this->text = text;
        updateTextItem();
    }
}

void ConnectionItem::setTextPosition(Qt::Alignment alignment)
{
    ASSERT2(alignment == Qt::AlignLeft
             || alignment == Qt::AlignRight
             || alignment == Qt::AlignCenter,
            "Unsupported text alignment");

    if (textAlignment != alignment) {
        textAlignment = alignment;
        updateTextItem();
    }
}

void ConnectionItem::setTextBackgroundColor(const QColor& color)
{
    textItem->setBackgroundBrush(color);
}

void ConnectionItem::setTextOutlineColor(const QColor& color)
{
    textItem->setPen(color);
}

void ConnectionItem::setTextColor(const QColor& color)
{
    if (textColor != color) {
        textColor = color;
        updateTextItem();
    }
}

void ConnectionItem::setLineEnabled(bool enabled)
{
    if (enabled != lineEnabled) {
        lineEnabled = enabled;
        updateLineItem();
    }
}

void ConnectionItem::setArrowEnabled(bool enabled)
{
    if (enabled != arrowItem->isVisible()) {
        arrowItem->setVisible(enabled);
        updateArrowItem();
        updateLineItem();
    }
}

bool ConnectionItem::isArrowEnabled() const
{
    return arrowItem->isVisible();
}

void ConnectionItem::setHalfLength(bool enabled)
{
    if (enabled != halfLength) {
        halfLength = enabled;
        updateLineItem();
    }
}

}  // namespace qtenv
}  // namespace omnetpp

