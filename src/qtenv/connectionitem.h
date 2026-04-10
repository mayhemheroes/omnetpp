//==========================================================================
//  CONNECTIONITEM.H - part of
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

#ifndef __OMNETPP_QTENV_CONNECTIONITEM_H
#define __OMNETPP_QTENV_CONNECTIONITEM_H

#include <QtWidgets/QGraphicsObject>
#include <QtWidgets/QGraphicsPathItem>
#include <QtGui/QPolygonF>
#include "omnetpp/cdisplaystring.h"
#include "qtenvdefs.h"
#include "qtutil.h"

namespace omnetpp {
namespace qtenv {

class MultiLineOutlinedTextItem;
class ArrowheadItem;

class ConnectionItem;
class QTENV_API ConnectionItemUtil {
public:
    // only stylistic tags are handled here (color, width, style, etc), positioning is in the ModuleCanvasViewer
    static void setupFromDisplayString(ConnectionItem *ci, cGate *gate, bool showArrowhead);
};

class QTENV_API ConnectionItem : public QGraphicsObject
{
    Q_OBJECT

protected:
    QPolygonF points; // polyline points (2 for straight, 3+ for routed)
    double lineWidth = 1;
    QColor lineColor = colors::BLACK;
    Qt::PenStyle lineStyle = Qt::SolidLine; // not directly fed to the lineItem
    double dashOffset = 0;
    QString text;
    Qt::Alignment textAlignment = Qt::AlignCenter; // Left for source, Right for dest, Center for middle
    QColor textColor = colors::DARKGREEN;
    bool lineEnabled = true;
    bool halfLength = false;
    // Returned by shape(), computed in updateLineItem(), used for mouse picking,
    // is 3/4 of the length of the full "connection line".
    QPainterPath shape_;

    QGraphicsPathItem *lineItem;
    MultiLineOutlinedTextItem *textItem; // This is a managed sibling!
    ArrowheadItem *arrowItem;

protected Q_SLOTS:
    void updateLineItem();
    void updateTextItem();
    void updateArrowItem();

public:
    explicit ConnectionItem(QGraphicsItem *parent = 0);
    virtual ~ConnectionItem();

    void setPoints(const QPolygonF &points);
    void setLine(const QLineF &line); // convenience: 2-point polyline
    void setSource(const QPointF &source); // convenience: updates first point
    void setDestination(const QPointF &destination); // convenience: updates last point
    void setWidth(double width);
    void setColor(const QColor &color);
    void setLineStyle(Qt::PenStyle style);
    void setDashOffset(double offset);
    void setText(const QString &text);
    void setTextPosition(Qt::Alignment alignment);
    void setTextBackgroundColor(const QColor &color);
    void setTextOutlineColor(const QColor &color);
    void setTextColor(const QColor &color);
    void setLineEnabled(bool enabled);
    bool isLineEnabled() const { return lineEnabled; }
    void setArrowEnabled(bool enabled);
    bool isArrowEnabled() const;
    // This is used in two-way connections to show the styles
    // in both cGate's cDisplayStrings next to each other.
    void setHalfLength(bool enabled);
    bool isHalfLength() const { return halfLength; }

    QRectF boundingRect() const override { return points.boundingRect(); }
    void paint(QPainter *, const QStyleOptionGraphicsItem *, QWidget *) override { /* empty */ }
    QPainterPath shape() const override { return shape_; }
};

}  // namespace qtenv
}  // namespace omnetpp

#endif // __OMNETPP_QTENV_CONNECTIONITEM_H
