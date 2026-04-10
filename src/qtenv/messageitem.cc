//==========================================================================
//  MESSAGEITEM.CC - part of
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

#include "messageitem.h"
#include <cmath>
#include <QtCore/QDebug>
#include <QtGui/QPen>
#include "qtenv.h"
#include "graphicsitems.h"
#include "displaystringaccess.h"
#include "omnetpp/cdisplaystring.h"
#include "omnetpp/cpacket.h"
#include "common/stringutil.h"

using namespace omnetpp::common;

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

// Returns the direction (unit vector) at fraction t along the polyline.
static QPointF directionAtFraction(const QPolygonF& poly, double t)
{
    if (poly.size() < 2)
        return QPointF(1, 0);
    double totalLen = polylineLength(poly);
    double targetLen = t * totalLen;
    double accumulated = 0;
    for (int i = 1; i < poly.size(); i++) {
        QPointF d = poly[i] - poly[i-1];
        double segLen = std::sqrt(d.x()*d.x() + d.y()*d.y());
        if (accumulated + segLen >= targetLen || i == poly.size()-1) {
            if (segLen > 0)
                return d / segLen;
            break;
        }
        accumulated += segLen;
    }
    // fallback: use last segment direction
    QPointF d = poly.last() - poly[poly.size()-2];
    double len = std::sqrt(d.x()*d.x() + d.y()*d.y());
    return (len > 0) ? d / len : QPointF(1, 0);
}

// Extracts the sub-polyline between fractions t1 and t2 (0..1) along the polyline.
// t1 should be <= t2. The result includes interpolated start/end points and any
// intermediate vertices of the original polyline that fall between them.
static QPolygonF subPolyline(const QPolygonF& poly, double t1, double t2)
{
    if (poly.size() < 2 || t1 >= t2) {
        QPointF pt = pointAtFraction(poly, t1);
        QPolygonF result;
        result << pt;
        return result;
    }
    double totalLen = polylineLength(poly);
    double startLen = t1 * totalLen;
    double endLen = t2 * totalLen;
    QPolygonF result;
    double accumulated = 0;
    bool started = false;
    for (int i = 1; i < poly.size(); i++) {
        QPointF d = poly[i] - poly[i-1];
        double segLen = std::sqrt(d.x()*d.x() + d.y()*d.y());
        double segEnd = accumulated + segLen;
        if (!started && segEnd >= startLen) {
            // start point is on this segment
            double frac = (segLen > 0) ? (startLen - accumulated) / segLen : 0;
            result << (poly[i-1] + d * frac);
            started = true;
        }
        if (started) {
            if (segEnd >= endLen) {
                // end point is on this segment
                double frac = (segLen > 0) ? (endLen - accumulated) / segLen : 0;
                result << (poly[i-1] + d * frac);
                break;
            }
            else {
                // intermediate vertex
                result << poly[i];
            }
        }
        accumulated = segEnd;
    }
    if (result.isEmpty())
        result << poly.last();
    return result;
}


// -------- MessageItemUtil --------

// these colors were chosen to be consistent with the tool and object icons
const QVector<QColor> MessageItemUtil::msgKindColors = {"#C00000", "#379143", "#6060ff", "#f0f0f0", "#c0c000", "#00c0c0", "#c000c0", "#404040"};

void MessageItemUtil::setupMessageCommon(MessageItem *mi, cMessage *msg)
{
    QtenvOptions *opt = getQtenv()->opt;

    QString label;
    if (opt->animationMsgClassNames)
        label += QString("(") + getObjectShortTypeName(msg) + ")";
    if (opt->animationMsgNames)
        label += msg->getFullName();
    mi->setText(label);
}

void MessageItemUtil::setupLineFromDisplayString(LineMessageItem *mi, cMessage *msg)
{
    cDisplayString ds = msg->getDisplayString();

    std::string buffer; // stores getTagArg return values after substitution
    DisplayStringAccess dsa(&ds, nullptr);

    mi->setData(ITEMDATA_COBJECT, QVariant::fromValue((cObject *)msg));

    QtenvOptions *opt = getQtenv()->opt;

    setupMessageCommon(mi, msg);

    QColor kindColor = msgKindColors[msg->getKind() % msgKindColors.size()];

    if (!ds.str()[0]) {  // default red line
        QColor color = opt->animationMsgColors ? kindColor : "red";
        mi->setColor(color);
    }
    else {  // as defined in the dispstr
        const char *fillColorName = dsa.getTagArg("b", 3, buffer);
        QColor fillColor = opp_streq(fillColorName, "kind")
                            ? kindColor
                            : parseColor(fillColorName, "red");
        mi->setColor(fillColor);
    }
}

void MessageItemUtil::setupSymbolFromDisplayString(SymbolMessageItem *mi, cMessage *msg, double imageSizeFactor)
{
    cDisplayString ds = msg->getDisplayString();

    std::string buffer; // stores getTagArg return values after substitution
    DisplayStringAccess dsa(&ds, nullptr);

    mi->setData(ITEMDATA_COBJECT, QVariant::fromValue((cObject *)msg));

    QtenvOptions *opt = getQtenv()->opt;

    setupMessageCommon(mi, msg);

    if (!ds.str()[0] && opt->penguinMode)
        ds = "i=abstract/penguin_s";  // ^^,

    // no zoom factor, it doesn't affect messages
    mi->setImageSizeFactor(imageSizeFactor);

    QColor kindColor = MessageItemUtil::getColorForMessageKind(msg->getKind());

    if (!ds.str()[0]) {  // default little red dot
        QColor color = opt->animationMsgColors ? kindColor : "red";
        mi->setShape(SymbolMessageItem::SHAPE_OVAL);
        mi->setWidth(10);
        mi->setHeight(10);
        mi->setFillColor(color);
        mi->setOutlineColor(color);
        mi->setImage(nullptr);
    }
    else {  // as defined in the dispstr
        bool widthOk, heightOk;
        double shapeWidth = dsa.getTagArgAsDouble("b", 0, 0.0, &widthOk);
        double shapeHeight = dsa.getTagArgAsDouble("b", 1, 0.0, &heightOk);

        if (!widthOk)
            shapeWidth = shapeHeight;
        if (!heightOk)
            shapeHeight = shapeWidth;
        if (!widthOk && !heightOk) {
            shapeWidth = 10;
            shapeHeight = 10;
        }

        mi->setWidth(shapeWidth);
        mi->setHeight(shapeHeight);

        QString shapeName = QString(dsa.getTagArg("b", 2, buffer)).toLower();

        auto shape = (ds.getNumArgs("b") <= 0)
                       ? SymbolMessageItem::SHAPE_NONE
                       : shapeName[0] == 'r'
                           ? SymbolMessageItem::SHAPE_RECT
                           : SymbolMessageItem::SHAPE_OVAL; // if unknown, this is the default
        mi->setShape(shape);

        const char *fillColorName = dsa.getTagArg("b", 3, buffer);
        QColor fillColor = opp_streq(fillColorName, "kind")
                            ? kindColor
                            : parseColor(fillColorName, "red");
        mi->setFillColor(fillColor);
        const char *outlineColorName = dsa.getTagArg("b", 4, buffer);
        mi->setOutlineColor(opp_streq(outlineColorName, "kind")
                             ? kindColor
                             : parseColor(outlineColorName, fillColor));

        mi->setOutlineWidth(dsa.getTagArgAsLong("b", 5, 2));

        const char *imageName = dsa.getTagArg("i", 0, buffer);
        std::string buffer2; // for imageSize while imageName is still needed
        mi->setImage(imageName[0] ? getQtenv()->icons.getImage(imageName, dsa.getTagArg("is", 0, buffer2)) : nullptr);

        const char *imageColorName = dsa.getTagArg("i", 1, buffer);
        mi->setImageColor(opp_streq(imageColorName, "kind") ? kindColor : parseColor(imageColorName));
        mi->setImageColorPercentage((ds.getNumArgs("i") == 2) // color given, but no percentage
                                      ? 30
                                      : dsa.getTagArgAsDouble("i", 2));
    }
}

// -------- MessageItemUtil --------

const QColor& MessageItemUtil::getColorForMessageKind(int messageKind)
{
    int colorIndex = messageKind % msgKindColors.size();
    if (colorIndex < 0)
        colorIndex += msgKindColors.size();
    return msgKindColors[colorIndex];
}

// -------- MessageItem --------

MessageItem::MessageItem(QGraphicsItem *parent) :
    QGraphicsObject(parent)
{
    textItem = new OutlinedTextItem(this);
    textItem->setZValue(1);
}

MessageItem::~MessageItem()
{
    delete textItem;
}

void MessageItem::setText(const QString& text)
{
    if (this->text != text) {
        this->text = text;
        updateTextItem();
    }
}

void MessageItem::updateTextItem()
{
    textItem->setText(text);
    QRectF textRect = textItem->textRect();
    QPointF textPos = getTextPosition();
    textItem->setPos(textPos.x() - textRect.width() / 2, textPos.y());
}


// -------- LineMessageItem --------


LineMessageItem::LineMessageItem(QGraphicsItem *parent) : MessageItem(parent)
{
    lineItem = new QGraphicsPathItem(this);
    txUpdateMarkerItem = new QGraphicsLineItem(this);
    arrowheadItem = new ArrowheadItem(this);

    arrowheadItem->setFillRatio(1);
    arrowheadItem->setPen(Qt::NoPen);

    txUpdateMarkerItem->setZValue(0.5);
}

LineMessageItem::~LineMessageItem()
{
    delete lineItem;
    delete txUpdateMarkerItem;
    delete arrowheadItem;
}

void LineMessageItem::setColor(const QColor& color)
{
    if (this->color != color) {
        this->color = color;
        updateLineItem();
    }
}

void LineMessageItem::setLineEnabled(bool enabled)
{
    if (lineEnabled != enabled) {
        lineEnabled = enabled;
        updateLineItem();
    }
}

void LineMessageItem::setArrowheadEnabled(bool enabled)
{
    if (arrowheadEnabled != enabled) {
        arrowheadEnabled = enabled;
        updateLineItem();
    }
}

void LineMessageItem::setTxUpdateMarkerEnabled(bool enabled)
{
    if (txUpdateMarkerEnabled != enabled) {
        txUpdateMarkerEnabled = enabled;
        updateLineItem();
    }
}

QPointF LineMessageItem::getTextPosition()
{
    QPointF offset = getSideOffsetForWidth((txUpdateMarkerEnabled) ? 12 : 6);
    return offset + QPointF(0, 3);
}

void LineMessageItem::positionOntoLine(const QPolygonF& connPolyline, double t1, double t2, bool asUpdatePacket)
{
    // Extract the sub-polyline between t2 (trailing) and t1 (leading edge)
    // Note: t1 >= t2, and the polyline goes from trailing to leading edge
    QPolygonF packetPoly = subPolyline(connPolyline, t2, t1);

    bool lineEnabled = true;

    if (t1 == t2 && asUpdatePacket) {// we are marker-only
        double tFake = std::max(0.0, t1 - 0.001);
        packetPoly = subPolyline(connPolyline, tFake, t1);
        lineEnabled = false; // the packetPoly is fake
    }

    setPolyline(packetPoly);
    setLineEnabled(lineEnabled);
    setArrowheadEnabled(t1 > 0.0 && t1 < 1.0 && !asUpdatePacket);
    setTxUpdateMarkerEnabled(t1 > 0.0 && t1 < 1.0 && asUpdatePacket);
}

QRectF LineMessageItem::boundingRect() const
{
    QRectF result;
    if (lineEnabled)
        result = result.united(lineItem->boundingRect());
    if (arrowheadEnabled)
        result = result.united(arrowheadItem->boundingRect());
    if (txUpdateMarkerEnabled)
        result = result.united(txUpdateMarkerItem->boundingRect());
    return result;
}

QPainterPath LineMessageItem::shape() const
{
    QPainterPath result;
    if (lineEnabled)
        result = result.united(lineItem->shape());
    if (arrowheadEnabled)
        result = result.united(arrowheadItem->shape());
    if (txUpdateMarkerEnabled)
        result = result.united(txUpdateMarkerItem->shape());
    return result;
}

void LineMessageItem::setPolyline(const QPolygonF& poly)
{
    if (this->polyline != poly) {
        this->polyline = poly;
        // position at midpoint of the polyline
        setPos(pointAtFraction(poly, 0.5));
        updateLineItem();
        updateTextItem();
    }
}

QPointF LineMessageItem::getSideOffsetForWidth(float width) const
{
    if (polyline.size() < 2)
        return QPointF(0, 0);
    // Use direction at midpoint to compute the normal
    QPointF dir = directionAtFraction(polyline, 0.5);
    QPointF normal(-dir.y(), dir.x()); // perpendicular
    return normal * (width / 2 + 2);
}

void LineMessageItem::updateLineItem()
{
    setPos(pointAtFraction(polyline, 0.5));

    double width = 6;

    // to make it go on the right side of the connection, with a bit of spacing
    QPointF sideOffset = getSideOffsetForWidth(width);

    QPen pen(color, width, Qt::SolidLine, Qt::FlatCap);

    // Build a local polyline path, relative to pos()
    // The polyline goes from trailing edge to leading edge (t2..t1)
    QPolygonF localPoly;
    for (const QPointF& pt : polyline)
        localPoly << (pt - pos() + sideOffset);

    double totalLen = polylineLength(localPoly);
    double arrowheadLength = arrowheadEnabled ? width : 0;

    // don't let it get too short
    if (totalLen < 2 && localPoly.size() >= 2) {
        QPointF dir = localPoly.last() - localPoly.first();
        double len = std::sqrt(dir.x()*dir.x() + dir.y()*dir.y());
        if (len > 0) dir = dir / len;
        else dir = QPointF(1, 0);
        QPointF center = (localPoly.first() + localPoly.last()) / 2;
        localPoly.clear();
        localPoly << (center - dir) << (center + dir);
        totalLen = 2;
    }

    arrowheadLength = std::min(arrowheadLength, totalLen / 2);

    // Arrowhead at the leading edge (last point of the polyline)
    if (localPoly.size() >= 2) {
        QPointF arrowStart = localPoly[localPoly.size()-2];
        QPointF arrowEnd = localPoly.last();
        arrowheadItem->setEndPoints(arrowStart, arrowEnd);
    }

    // Shorten the last segment by arrowheadLength for the line path
    QPolygonF drawPoly = localPoly;
    if (arrowheadLength > 0 && drawPoly.size() >= 2) {
        QPointF& last = drawPoly[drawPoly.size()-1];
        QPointF& prev = drawPoly[drawPoly.size()-2];
        QPointF d = last - prev;
        double segLen = std::sqrt(d.x()*d.x() + d.y()*d.y());
        if (segLen > arrowheadLength)
            last = last - d / segLen * arrowheadLength;
        else
            last = prev; // segment too short, collapse
    }

    // Build QPainterPath from the polyline
    QPainterPath path;
    if (!drawPoly.isEmpty()) {
        path.moveTo(drawPoly[0]);
        for (int i = 1; i < drawPoly.size(); i++)
            path.lineTo(drawPoly[i]);
    }
    lineItem->setPath(path);
    lineItem->setVisible(lineEnabled);
    lineItem->setPen(pen);

    arrowheadItem->setBrush(color);
    arrowheadItem->setArrowWidth(width);
    arrowheadItem->setArrowLength(arrowheadLength + 0.5); // +0.5 is just to make it "watertight" (AA and imprecision and stuff)
    arrowheadItem->setVisible(arrowheadEnabled);

    // txUpdateMarker at the leading edge
    if (localPoly.size() >= 2) {
        QPointF lastPt = localPoly.last();
        txUpdateMarkerItem->setLine(QLineF(lastPt + sideOffset * 1.5, lastPt - sideOffset * 1.5));
    }
    txUpdateMarkerItem->setVisible(txUpdateMarkerEnabled);
    pen.setWidth(width / 2);
    txUpdateMarkerItem->setPen(pen);
}


// -------- SymbolMessageItem --------


SymbolMessageItem::SymbolMessageItem(QGraphicsItem *parent) : MessageItem(parent)
{
    shapeItem = new QGraphicsEllipseItem(this);
    imageItem = new QGraphicsPixmapItem(this);

    updateShapeItem();
}

SymbolMessageItem::~SymbolMessageItem()
{
    delete shapeItem;
    delete imageItem;
}

void SymbolMessageItem::setImageSizeFactor(double imageSize)
{
    if (imageSizeFactor != imageSize) {
        imageSizeFactor = imageSize;
        updateImageItem();
    }
}

void SymbolMessageItem::setShape(Shape shape)
{
    if (this->shapeType != shape) {
        this->shapeType = shape;
        updateShapeItem();
    }
}

void SymbolMessageItem::setWidth(double width)
{
    if (shapeWidth != width) {
        shapeWidth = width;
        updateShapeItem();
    }
}

void SymbolMessageItem::setHeight(double height)
{
    if (shapeHeight != height) {
        shapeHeight = height;
        updateShapeItem();
    }
}

void SymbolMessageItem::setFillColor(const QColor& color)
{
    if (this->shapeFillColor != color) {
        this->shapeFillColor = color;
        updateShapeItem();
    }
}

void SymbolMessageItem::setOutlineColor(const QColor& color)
{
    if (this->shapeOutlineColor != color) {
        this->shapeOutlineColor = color;
        updateShapeItem();
    }
}

void SymbolMessageItem::setOutlineWidth(double width)
{
    if (shapeOutlineWidth != width) {
        shapeOutlineWidth = width;
        updateShapeItem();
    }
}

void SymbolMessageItem::setImage(QImage *image)
{
    if (this->image != image) {
        this->image = image;
        updateImageItem();
        updateTextItem();
    }
}

void SymbolMessageItem::setImageColor(const QColor& color)
{
    if (colorizeEffect) {
        colorizeEffect->setColor(color);
    }
}

void SymbolMessageItem::setImageColorPercentage(int percent)
{
    if (colorizeEffect) {
        colorizeEffect->setStrength(percent / 100.0f);
    }
}

QPointF SymbolMessageItem::getTextPosition()
{
    return QPointF(0, shapeImageBoundingRect().bottom());
}

void SymbolMessageItem::positionOntoLine(const QPolygonF& polyline, double t1, double t2, bool asUpdatePacket)
{
    ASSERT(t1 == t2);
    ASSERT(!asUpdatePacket);

    setPos(pointAtFraction(polyline, t1));
}

QRectF SymbolMessageItem::boundingRect() const
{
    return shapeImageBoundingRect();
}

QPainterPath SymbolMessageItem::shape() const
{
    QPainterPath path;
    path.addRect(shapeImageBoundingRect());
    return path;
}

QRectF SymbolMessageItem::shapeImageBoundingRect() const
{
    QRectF rect;
    if (imageItem) {
        QRectF imageRect = imageItem->boundingRect();
        // Image scaling is done with a transformation, and boundingRect does
        // not factor that in, so we have to account the factor in here.
        imageRect.setTopLeft(imageRect.topLeft() * imageSizeFactor);
        imageRect.setBottomRight(imageRect.bottomRight() * imageSizeFactor);
        rect = rect.united(imageRect);
    }
    if (shapeItem) {
        QRectF shapeRect = shapeItem->boundingRect();
        // Shape size is not zoomed in messages;
        rect = rect.united(shapeRect);
    }
    return rect;
}

void SymbolMessageItem::updateShapeItem()
{
    delete shapeItem;
    shapeItem = nullptr;

    QRectF rect(-shapeWidth / 2.0, -shapeHeight / 2.0, shapeWidth, shapeHeight);
    rect.setTopLeft(rect.topLeft());
    rect.setBottomRight(rect.bottomRight());
    rect = rect.adjusted(shapeOutlineWidth / 2, shapeOutlineWidth / 2, -shapeOutlineWidth / 2, -shapeOutlineWidth / 2);

    switch (shapeType) {
        case SHAPE_OVAL: shapeItem = new QGraphicsEllipseItem(rect, this); break;
        case SHAPE_RECT: shapeItem = new QGraphicsRectItem(rect, this); break;
        default: break; // nothing
    }

    if (shapeItem) {
        shapeItem->setBrush(shapeFillColor.isValid() ? shapeFillColor : Qt::NoBrush);
        shapeItem->setPen(shapeOutlineColor.isValid()
                           ? QPen(shapeOutlineColor, shapeOutlineWidth,
                                  Qt::SolidLine, Qt::FlatCap, Qt::MiterJoin)
                           : Qt::NoPen);
    }
    updateTextItem();
}

void SymbolMessageItem::updateImageItem()
{
    delete imageItem;
    imageItem = nullptr;

    if (image) {
        imageItem = new QGraphicsPixmapItem(this);
        imageItem->setPixmap(QPixmap::fromImage(*image));
        imageItem->setOffset(-image->width() / 2.0, -image->height() / 2.0);
        imageItem->setScale(imageSizeFactor);
        imageItem->setTransformationMode(Qt::SmoothTransformation);
        colorizeEffect = new QGraphicsColorizeEffect(this);
        colorizeEffect->setStrength(0);
        imageItem->setGraphicsEffect(colorizeEffect);
    }
    updateTextItem();
}


}  // namespace qtenv
}  // namespace omnetpp

