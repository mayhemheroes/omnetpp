//==========================================================================
//  OBJECTTREEINSPECTOR.CC - part of
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

#include "objecttreeinspector.h"
#include "inspectorfactory.h"
#include "genericobjecttreemodel.h"
#include "genericobjecttreenodes.h"
#include "highlighteritemdelegate.h"
#include "inspectorutil.h"
#include "qtenv.h"
#include <omnetpp/cfutureeventset.h>
#include <omnetpp/cmodule.h>
#include <QtWidgets/QLayout>
#include <QtWidgets/QTreeView>
#include <QtWidgets/QHeaderView>
#include <QtWidgets/QScrollBar>
#include <QtGui/QKeyEvent>
#include <functional>
#include <algorithm>

#include <QtCore/QDebug>

namespace omnetpp {
namespace qtenv {

void _dummy_for_objecttreeinspector() {}

class ObjectTreeInspectorFactory : public InspectorFactory
{
  public:
    ObjectTreeInspectorFactory(const char *name) : InspectorFactory(name) {}

    bool supportsObject(cObject *object) override { return dynamic_cast<cSimulation *>(object) != nullptr; }
    InspectorType getInspectorType() override { return INSP_OBJECTTREE; }
    double getQualityAsDefault(cObject *) override { return 0; }
    Inspector *createInspector(QWidget *parent, bool isTopLevel) override { return new ObjectTreeInspector(parent, isTopLevel, this); }
};

Register_InspectorFactory(ObjectTreeInspectorFactory);

ObjectTreeInspector::ObjectTreeInspector(QWidget *parent, bool isTopLevel, InspectorFactory *f) : Inspector(parent, isTopLevel, f)
{
    QGridLayout *layout = new QGridLayout(this);
    view = new QTreeView();
    layout->addWidget(view, 0, 0);
    layout->setContentsMargins(0, 0, 0, 0);
    model = new GenericObjectTreeModel(nullptr, GenericObjectTreeModel::Mode::CHILDREN, false, {}, this);

    view->setModel(model);
    view->setUniformRowHeights(true);
    view->setHeaderHidden(true);
    view->setAttribute(Qt::WA_MacShowFocusRect, false);
    view->setContextMenuPolicy(Qt::CustomContextMenu);

    auto delegate = new HighlighterItemDelegate(view);
    view->setItemDelegate(delegate);

    view->header()->setStretchLastSection(true);
    view->header()->setSectionResizeMode(QHeaderView::ResizeToContents);

    parent->setMinimumSize(20, 20);

    connect(view, SIGNAL(customContextMenuRequested(QPoint)), this, SLOT(createContextMenu(QPoint)));
    connect(view, SIGNAL(clicked(QModelIndex)), this, SLOT(onClick(QModelIndex)));
    connect(view, SIGNAL(doubleClicked(QModelIndex)), this, SLOT(onDoubleClick(QModelIndex)));

    connectSelectionSignals();

    // getting the data into any items newly brought into view
    connect(view, SIGNAL(expanded(QModelIndex)), this, SLOT(gatherVisibleDataIfSafe()));
    connect(view, SIGNAL(collapsed(QModelIndex)), this, SLOT(gatherVisibleDataIfSafe()));
    connect(view->horizontalScrollBar(), SIGNAL(valueChanged(int)), this, SLOT(gatherVisibleDataIfSafe()));
    connect(view->verticalScrollBar(), SIGNAL(valueChanged(int)), this, SLOT(gatherVisibleDataIfSafe()));
}

void ObjectTreeInspector::doSetObject(cObject *obj)
{
    if (obj == object)
        return;

    Inspector::doSetObject(obj);

    refresh();
}

void ObjectTreeInspector::refresh()
{
    Inspector::refresh();

    cSimulation *simulation = dynamic_cast<cSimulation *>(object);
    std::vector<cObject*> roots;
    if (simulation != nullptr) {
        roots.push_back(simulation->getSystemModule());
        roots.push_back(simulation->getFES());
    }

    if (roots != model->getRootObjects()) {
        // the FES and Network are recreated on run restart or config change
        delete model;
        model = new GenericObjectTreeModel(roots, GenericObjectTreeModel::Mode::CHILDREN, false, {}, this);
        view->setModel(model);
        connectSelectionSignals();
    }

    model->refreshTreeStructure();

    gatherVisibleData();

    // because properly doing it is super slow
    view->dataChanged(QModelIndex(), QModelIndex());
    view->resizeColumnToContents(0); // and this is needed because of it
}

void ObjectTreeInspector::createContextMenu(QPoint pos)
{
    QModelIndex index = view->indexAt(pos);
    if (index.isValid()) {
        QVector<cObject *> objects;
        cObject *obj = model->getCObjectPointer(index);
        objects.push_back(obj);

        cObject *objToInspect = model->getCObjectPointerToInspect(index);
        if (objToInspect != nullptr && objToInspect != obj)
            objects.push_back(objToInspect);

        QMenu *menu = InspectorUtil::createInspectorContextMenu(objects, this);
        menu->exec(mapToGlobal(pos));
        delete menu;
    }
}

bool ObjectTreeInspector::gatherVisibleData()
{
    bool changed = false;

    QModelIndexList indices;

    QModelIndex topIndex = view->indexAt(view->rect().topLeft());
    QModelIndex bottomIndex = view->indexAt(view->rect().bottomLeft());

    for (QModelIndex i = topIndex; i != bottomIndex; i = view->indexBelow(i))
        indices.append(i);

    if (bottomIndex.isValid())
        indices.append(bottomIndex);

    for (auto i : indices) {
        TreeNode *node = static_cast<TreeNode *>(i.internalPointer());
        if (node->updateData()) { // gatherDataIfMissing()?
            // not doing it, super slow, see caller
            //Q_EMIT dataChanged(i, i);
            changed = true;
        }
    }

    return changed;
}

bool ObjectTreeInspector::gatherVisibleDataIfSafe()
{
    bool changed = false;
    if (getQtenv()->inspectorsAreFresh()) {
        changed = gatherVisibleData();

        if (changed) {
            // because properly doing it is super slow
            view->dataChanged(QModelIndex(), QModelIndex());
            view->resizeColumnToContents(0); // and this is needed because of it
        }
    }

    return changed;
}

void ObjectTreeInspector::resizeEvent(QResizeEvent *event)
{
    Inspector::resizeEvent(event);
    gatherVisibleDataIfSafe();
}

void ObjectTreeInspector::onClick(QModelIndex index)
{
    if (index.isValid())
        Q_EMIT selectionChanged(model->getCObjectPointer(index));
}

void ObjectTreeInspector::onDoubleClick(QModelIndex index)
{
    if (index.isValid())
        if (cModule *module = dynamic_cast<cModule*>(model->getCObjectPointer(index)))
            Q_EMIT showInGraphicsRequested(module);
}

void ObjectTreeInspector::onCurrentChanged(const QModelIndex &current, const QModelIndex &previous)
{
    Q_UNUSED(previous);
    if (current.isValid())
        Q_EMIT selectionChanged(model->getCObjectPointer(current));
}

void ObjectTreeInspector::connectSelectionSignals()
{
    if (view->selectionModel()) {
        connect(view->selectionModel(), SIGNAL(currentChanged(const QModelIndex&, const QModelIndex&)),
                this, SLOT(onCurrentChanged(const QModelIndex&, const QModelIndex&)));
    }
}

void ObjectTreeInspector::keyPressEvent(QKeyEvent *event)
{
    QModelIndex current = view->currentIndex();
    if (!current.isValid()) {
        Inspector::keyPressEvent(event);
        return;
    }

    switch (event->key()) {
        case Qt::Key_Return:
        case Qt::Key_Enter: {
            cObject *obj = model->getCObjectPointer(current);
            if (obj) {
                // Check if it's a module - only modules can be shown in graphics
                cModule *module = dynamic_cast<cModule*>(obj);
                if (module) {
                    Q_EMIT showInGraphicsRequested(obj);
                    event->accept();
                    return;
                }
            }
            break;
        }
        default:
            break;
    }

    Inspector::keyPressEvent(event);
}

void ObjectTreeInspector::highlightModule(cModule *module)
{
    if (!module || !model)
        return;

    // Build path from module up to network root
    std::vector<cModule *> path;
    cModule *current = module;
    while (current) {
        path.push_back(current);
        current = current->getParentModule();
    }

    // Reverse to get root-to-target path
    std::reverse(path.begin(), path.end());

    // Walk the tree following the path
    QModelIndex currentIndex = QModelIndex(); // Start at tree root

    for (cModule *targetModule : path) {
        // Ensure children are loaded at this level
        if (model->canFetchMore(currentIndex))
            model->fetchMore(currentIndex);

        // Search for targetModule among children
        int rowCount = model->rowCount(currentIndex);
        bool found = false;
        for (int row = 0; row < rowCount; ++row) {
            QModelIndex childIndex = model->index(row, 0, currentIndex);
            if (!childIndex.isValid())
                continue;

            cObject *childObj = model->getCObjectPointer(childIndex);
            if (childObj == targetModule) {
                currentIndex = childIndex;
                found = true;
                break;
            }
        }

        if (!found) {
            // Path is broken, can't find this module in the tree
            return;
        }
    }

    // Found the target! Now expand parents and select it
    if (currentIndex.isValid()) {
        // Expand all parent nodes
        QModelIndex parent = currentIndex.parent();
        while (parent.isValid()) {
            view->expand(parent);
            parent = parent.parent();
        }

        // Select the item and scroll to it
        view->setCurrentIndex(currentIndex);
        view->scrollTo(currentIndex, QAbstractItemView::EnsureVisible);

        // We could expand the target node itself to show its children. However,
        // without auto-collapsing expanded nodes and/or a Collapse All functionality,
        // that is more annoying than useful.
        //view->expand(currentIndex);
    }
}

void ObjectTreeInspector::highlightGate(cGate *gate)
{
    if (!gate || !model)
        return;

    // First, find and select the owner module
    cModule *ownerModule = gate->getOwnerModule();
    if (!ownerModule)
        return;

    highlightModule(ownerModule);

    // Now find the gate within the module's children
    QModelIndex moduleIndex = view->currentIndex();
    if (!moduleIndex.isValid())
        return;

    // Ensure the module node is expanded to show its children (including gates)
    view->expand(moduleIndex);

    // Ensure children are loaded
    if (model->canFetchMore(moduleIndex))
        model->fetchMore(moduleIndex);

    // Search for the gate among the module's children
    int rowCount = model->rowCount(moduleIndex);
    for (int row = 0; row < rowCount; ++row) {
        QModelIndex childIndex = model->index(row, 0, moduleIndex);
        if (!childIndex.isValid())
            continue;

        cObject *childObj = model->getCObjectPointer(childIndex);
        if (childObj == gate) {
            // Found the gate! Select it and scroll to it
            view->setCurrentIndex(childIndex);
            view->scrollTo(childIndex, QAbstractItemView::EnsureVisible);
            return;
        }
    }
}

}  // namespace qtenv
}  // namespace omnetpp
