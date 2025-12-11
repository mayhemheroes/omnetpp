//==========================================================================
//  ARROW.CC - part of
//
//                     OMNeT++/OMNEST
//
//  Implementation of
//   connection arrow positioning in module drawing
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


#include "arrow.h"

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cmath>
#include <algorithm>
#include "qtutil.h"

#include "omnetpp/cexception.h" // for ASSERT2

namespace omnetpp {
namespace qtenv {

// Adjust p1 toward p2 so that it fits on the rectangle boundary.
// If p2 is inside the rectangle, do nothing.
static void clip_line_to_rect(QPointF& p1, const QPointF& p2,
        const QRectF& rect)
{
    int p1_inside = rect.contains(p1);
    int p2_inside = rect.contains(p2);

    // we'll clip the line to two edges of the rect: y=cy (horiz) and x=cx (vert)
    double cx, cy;

    if (p1_inside && p2_inside) {
        cx = p1.x() < p2.x() ? rect.left() : rect.right();
        cy = p1.y() < p2.y() ? rect.top() : rect.bottom();
    }
    else if (p1_inside) {
        cx = p1.x() < p2.x() ? rect.right() : rect.left();
        cy = p1.y() < p2.y() ? rect.bottom() : rect.top();
    }
    else if (p2_inside) {
        cx = p1.x() < p2.x() ? rect.left() : rect.right();
        cy = p1.y() < p2.y() ? rect.top() : rect.bottom();
    }
    else {
        cx = p1.x() < p2.x() ? rect.right() : rect.left();
        cy = p1.y() < p2.y() ? rect.bottom() : rect.top();
    }

    // first, deal with the special cases: line is vert or horiz
    if (p1.x() == p2.x()) {
        if (p1.x() < rect.left())
            p1.setX(rect.left());
        if (p1.x() > rect.right())
            p1.setX(rect.right());
        if (p1.x() != rect.left() && p1.x() != rect.right())
            p1.setY(cy);
        return;
    }

    if (p1.y() == p2.y()) {
        if (p1.y() < rect.top())
            p1.setY(rect.top());
        if (p1.y() > rect.bottom())
            p1.setY(rect.bottom());
        if (p1.y() != rect.top() && p1.y() != rect.bottom())
            p1.setX(cx);
        return;
    }

    // write the line into y=ax+b form
    double a = (p2.y()-p1.y())/(p2.x()-p1.x());
    double b = p1.y() - a*p1.x();

    double xx, yy;

    // clip to the y=cy horizontal edge of the rect
    xx = (cy-b)/a;  // ==> (xx,cy)

    // clip to the x=cx vertical edge of the rect
    yy = a*cx+b;  // ==> (cx,yy)

    if (xx >= rect.left() && xx <= rect.right()) {
        p1.setX(xx);
        p1.setY(cy);
    }
    else if (yy >= rect.top() && yy <= rect.bottom()) {
        p1.setX(cx);
        p1.setY(yy);
    }
    else {
        p1.setX(cx);
        p1.setY(cy);
    }
    // default: rect and line don't have any common point :-(
    // leave line unchanged
}

// A special case of "arrowcoords" where one rectangle is
// fully contained within the other.
// NOTE: The result will always point from inner to outer.
static QLineF arrowcoords_contained(
    const QRectF& innerRect,
    const QRectF& outerRect,
    int inner_i, int inner_n, // inner vector gate index and size
    char mode) // "anews"
{
    double src_x, src_y, dest_x, dest_y;

    // a - N,E,S,W (to nearest border,calculated from side)
    if (mode == 'a') {
        double top = innerRect.top() - outerRect.top(),
                bottom = outerRect.bottom() - innerRect.bottom(),
                left = innerRect.left() - outerRect.left(),
                right = outerRect.right() - innerRect.right();
        if (top <= bottom && top <= left && top <= right)
            mode = 'n';
        else if (right <= bottom && right <= left && right <= top)
            mode = 'e';
        else if (left <= bottom && left <= right && left <= top)
            mode = 'w';
        else if (bottom <= left && bottom <= right && bottom <= top)
            mode = 's';
    }

    //  E,W - connection points E or W. Vert shift by gate indices
    //  N,S - connection points N or S. Horiz shift by gate indices
    switch (mode) {
        case 'n':
            src_x = dest_x = innerRect.left() + (inner_i+1) * innerRect.width() / (inner_n+1);
            src_y = innerRect.top();
            dest_y = outerRect.top();
            break;

        case 's':
            src_x = dest_x = innerRect.left() + (inner_i+1) * innerRect.width() / (inner_n+1);
            src_y = innerRect.bottom();
            dest_y = outerRect.bottom();
            break;

        case 'e':
            src_x = innerRect.right();
            dest_x = outerRect.right();
            src_y = dest_y = innerRect.top() + (inner_i+1) * innerRect.height() / (inner_n+1);
            break;

        case 'w':
            src_x = innerRect.left();
            dest_x = outerRect.left();
            src_y = dest_y = innerRect.top() + (inner_i+1) * innerRect.height() / (inner_n+1);
            break;
    }

    return QLineF(src_x, src_y, dest_x, dest_y);
}

// Adjusts two intervals ([smaller_1, smaller_2] and [larger_1, larger_2]),
// so that they are the same size (at least half of smaller), and overlap
// as much as possible, while not expanding either in any direction.
// Think of it as a "specialized intersection with a lower bound on size".
static void junctionrect(double &smaller_1, double &smaller_2, double &larger_1, double &larger_2)
{
    double smaller_s = smaller_2 - smaller_1;
    double intersection_1 = std::max(smaller_1, larger_1);
    double intersection_2 = std::min(smaller_2, larger_2);
    double intersection_s = intersection_2 - intersection_1;
    if (intersection_s >= smaller_s / 2) {
        smaller_1 = intersection_1;
        smaller_2 = intersection_2;
        larger_1 = intersection_1;
        larger_2 = intersection_2;
    }
    else {
        double final_s = std::max(smaller_s / 2, intersection_s);
        if (smaller_1 < larger_1) {
            smaller_1 = smaller_2 - final_s;
            larger_2 = larger_1 + final_s;
        }
        else {
            smaller_2 = smaller_1 + final_s;
            larger_1 = larger_2 - final_s;
        }
    }
}

static double line_point_distance(const QLineF& line, const QPointF& point)
{
    if (line.isNull())
        return QLineF(line.p1(), point).length();
    // distance from point to line
    double a = line.dy();
    double b = -line.dx();
    double c = line.x2()*line.y1() - line.x1()*line.y2();
    return (a*point.x() + b*point.y() + c) / line.length();
    // explanation: https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
}

QLineF arrowcoords(const QRectF &srcRect, const QRectF &destRect,
                  int bundle_i, int bundle_n, // bundle index and size
                  char mode, // amnews
                  QPointF srcAnch, // src anchor percentages
                  QPointF destAnch) // dest anchor percentages
{
    QPointF src, dest;

    // error checks
    ASSERT2(strchr("amnews", mode), "mode must be one of (a,m,n,e,w,s)");

    // see if the two rects are the same, one is in the other etc.
    enum class Relation {
        SAME_RECT,
        SRC_WITHIN_DEST,
        DEST_WITHIN_SRC,
        OVERLAPPING,
        DISJOINT
    };

    Relation rel =
        (srcRect == destRect) ? Relation::SAME_RECT :
        srcRect.contains(destRect) ? Relation::DEST_WITHIN_SRC :
        destRect.contains(srcRect) ? Relation::SRC_WITHIN_DEST :
        srcRect.intersected(destRect).isValid() ? Relation::OVERLAPPING :
        Relation::DISJOINT;

    // prepare for mode 'm'
    if (mode != 'm')
        srcAnch = destAnch = QPointF(50, 50);
    src.setX(srcRect.left() + srcAnch.x()*srcRect.width()/100);
    src.setY(srcRect.top() + srcAnch.y()*srcRect.height()/100);
    dest.setX(destRect.left() + destAnch.x()*destRect.width()/100);
    dest.setY(destRect.top() + destAnch.y()*destRect.height()/100);
    // handle 'm' mode first
    if (mode == 'm') {
        if (rel != Relation::OVERLAPPING) {
            clip_line_to_rect(src, dest, srcRect);
            clip_line_to_rect(dest, src, destRect);
        }

        return QLineF(src, dest);
    }

    // do all rectangle relations one-by-one
    switch (rel) {
        case Relation::SAME_RECT: {
            // a - height>width: E or W
            //     otherwise:    N or S
            if (mode == 'a')
                mode = (srcRect.height() > srcRect.width()) ? 'e' : 'n';

            //  E,W - connection points E or W. Vert shift by gate indices
            //  N,S - connection points N or S. Horiz shift by gate indices
            switch (mode) {
                case 'n':
                    src.rx() = dest.rx() = srcRect.left() + (bundle_i+1) * srcRect.width() / (bundle_n+1);
                    src.ry() = srcRect.bottom();
                    dest.ry() = srcRect.top();
                    break;

                case 's':
                    src.rx() = dest.rx() = srcRect.left() + (bundle_i+1) * srcRect.width() / (bundle_n+1);
                    src.ry() = srcRect.top();
                    dest.ry() = srcRect.bottom();
                    break;

                case 'e':
                    src.rx() = srcRect.left();
                    dest.rx() = srcRect.right();
                    src.ry() = dest.ry() = srcRect.top() + (bundle_i+1) * srcRect.height() / (bundle_n+1);
                    break;

                case 'w':
                    src.rx() = srcRect.right();
                    dest.rx() = srcRect.left();
                    src.ry() = dest.ry() = srcRect.top() + (bundle_i+1) * srcRect.height() / (bundle_n+1);
                    break;
            }
        }
        break;
        case Relation::SRC_WITHIN_DEST:
            return arrowcoords_contained(srcRect,
                                     destRect,
                                     bundle_i, bundle_n,
                                     mode);
        case Relation::DEST_WITHIN_SRC: {
            QLineF l = arrowcoords_contained(destRect,
                                            srcRect,
                                            bundle_i, bundle_n,
                                            mode);
            // flip the line so it points inward
            return QLineF(l.p2(), l.p1());
        }
        case Relation::OVERLAPPING:
        case Relation::DISJOINT: {
            // disjoint (or partially overlapping) rectangles
            //  a - E,W if one module's y range is within y range of other module
            //      N,S if one module's x range is within x range of other module
            //      otherwise M mode with (50%,50%) (50%,50%)

            // The algorithm finds smaller "junction rectangles" (of equal size),
            // one within both srcRect and destRect, as close to each other as
            // possible - using junctionrect(). The endpoints of the connection
            // lines will be first placed within these rectangles, and then
            // clipped to the edge of the original rectangles.

            QRectF src_wireRect = srcRect;
            QRectF dest_wireRect = destRect;

            if (srcRect.width() < destRect.width()) {
                double smaller_1 = srcRect.left();
                double smaller_2 = srcRect.right();
                double larger_1 = destRect.left();
                double larger_2 = destRect.right();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                src_wireRect.setLeft(smaller_1);
                src_wireRect.setRight(smaller_2);
                dest_wireRect.setLeft(larger_1);
                dest_wireRect.setRight(larger_2);
            }

            if (destRect.width() < srcRect.width()) {
                double smaller_1 = destRect.left();
                double smaller_2 = destRect.right();
                double larger_1 = srcRect.left();
                double larger_2 = srcRect.right();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                dest_wireRect.setLeft(smaller_1);
                dest_wireRect.setRight(smaller_2);
                src_wireRect.setLeft(larger_1);
                src_wireRect.setRight(larger_2);
            }

            if (srcRect.height() < destRect.height()) {
                double smaller_1 = srcRect.top();
                double smaller_2 = srcRect.bottom();
                double larger_1 = destRect.top();
                double larger_2 = destRect.bottom();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                src_wireRect.setTop(smaller_1);
                src_wireRect.setBottom(smaller_2);
                dest_wireRect.setTop(larger_1);
                dest_wireRect.setBottom(larger_2);
            }

            if (destRect.height() < srcRect.height()) {
                double smaller_1 = destRect.top();
                double smaller_2 = destRect.bottom();
                double larger_1 = srcRect.top();
                double larger_2 = srcRect.bottom();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                dest_wireRect.setTop(smaller_1);
                dest_wireRect.setBottom(smaller_2);
                src_wireRect.setTop(larger_1);
                src_wireRect.setBottom(larger_2);
            }

            src = src_wireRect.center();
            dest = dest_wireRect.center();

            double halfSpreadSrcNeg = 0;
            double halfSpreadSrcPos = 0;
            for (QPointF p : {src_wireRect.topLeft(), src_wireRect.topRight(), src_wireRect.bottomLeft(), src_wireRect.bottomRight()}) {
                double d = line_point_distance(QLineF(src, dest), p);
                if (d < halfSpreadSrcNeg)
                    halfSpreadSrcNeg = d;
                if (d > halfSpreadSrcPos)
                    halfSpreadSrcPos = d;
            }
            double halfSpreadDestNeg = 0;
            double halfSpreadDestPos = 0;
            for (QPointF p : {dest_wireRect.topLeft(), dest_wireRect.topRight(), dest_wireRect.bottomLeft(), dest_wireRect.bottomRight()}) {
                double d = line_point_distance(QLineF(src, dest), p);
                if (d < halfSpreadDestNeg)
                    halfSpreadDestNeg = d;
                if (d > halfSpreadDestPos)
                    halfSpreadDestPos = d;
            }
            double halfSpread = std::min({-halfSpreadSrcNeg, halfSpreadSrcPos, -halfSpreadDestNeg, halfSpreadDestPos})*2.0;

            QLineF centerLine(src, dest);
            QLineF norm = centerLine.normalVector().unitVector();
            QPointF normDir = norm.p2() - norm.p1();
            if (normDir.x() + normDir.y() < 0) // make direction consistent
                normDir = -normDir;

            src += normDir * halfSpread * (-0.5 + (bundle_i + 1.0) / (bundle_n + 1.0));
            dest += normDir * halfSpread * (-0.5 + (bundle_i + 1.0) / (bundle_n + 1.0));

            // clip the line to the bounding rectangles if they are not overlapping
            if (rel == Relation::DISJOINT) {
                clip_line_to_rect(src, dest, srcRect);
                clip_line_to_rect(dest, src, destRect);
            }
        }
    }

    return QLineF(src, dest);
}

}  // namespace qtenv
}  // namespace omnetpp

