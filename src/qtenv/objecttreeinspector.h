//==========================================================================
//  OBJECTTREEINSPECTOR.H - part of
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

#ifndef __OMNETPP_QTENV_OBJECTTREEINSPECTOR_H
#define __OMNETPP_QTENV_OBJECTTREEINSPECTOR_H

#include <set>
#include <QtCore/QModelIndex>
#include "qtenvdefs.h"
#include "inspector.h"

class QTreeView;

namespace omnetpp {
namespace qtenv {

class GenericObjectTreeModel;

class QTENV_API ObjectTreeInspector : public Inspector
{
    Q_OBJECT
private:
    GenericObjectTreeModel *model = nullptr;
    QTreeView *view = nullptr;

    void resizeEvent(QResizeEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
    void connectSelectionSignals();
    QModelIndex findObjectInTree(cObject *obj, const QModelIndex &root);
    void collectExpandedObjects(const QModelIndex &root, std::set<cObject *> &result);
    void restoreExpandedObjects(const QModelIndex &root, const std::set<cObject *> &objects);

private Q_SLOTS:
    void onClick(QModelIndex index);
    void onDoubleClick(QModelIndex index);
    void onCurrentChanged(const QModelIndex &current, const QModelIndex &previous);

    bool gatherVisibleData();
    bool gatherVisibleDataIfSafe();

public Q_SLOTS:
    void createContextMenu(QPoint pos);
    void highlightModule(cModule *module);
    void highlightGate(cGate *gate);

Q_SIGNALS:
    void showInGraphicsRequested(cObject *object);

public:
    ObjectTreeInspector(QWidget *parent, bool isTopLevel, InspectorFactory *f);

    void doSetObject(cObject *obj) override;
    void refresh() override;
};

}  // namespace qtenv
}  // namespace omnetpp

#endif // __OMNETPP_QTENV_OBJECTTREEINSPECTOR_H
