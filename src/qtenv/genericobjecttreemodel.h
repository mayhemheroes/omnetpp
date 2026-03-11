//==========================================================================
//  GENERICOBJECTTREEMODEL.H - part of
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

#ifndef __OMNETPP_QTENV_GENERICOBJECTTREEMODEL_H
#define __OMNETPP_QTENV_GENERICOBJECTTREEMODEL_H

#include <unordered_map>
#include <QtCore/QAbstractItemModel>
#include <QtWidgets/QMenu>
#include <QtWidgets/QTreeView>
#include "omnetpp/cobject.h"
#include "omnetpp/cclassdescriptor.h"
#include "qtenvdefs.h"

namespace omnetpp {
namespace qtenv {

class RootNode;

// this is wrapped in a QVariant to be returned by the model
// when the UserRole data is requested, so the itemdelegate
// of the TreeView can highlight a section in the string
struct QTENV_API HighlightRange {
    int64_t start; // the index of the first highlighted character
    int64_t length; // the number of highlighted characters
};

// encapsulates the tree model, handles QModelIndexes, etc
class QTENV_API GenericObjectTreeModel : public QAbstractItemModel
{
    Q_OBJECT

public:
    // enum classes, so we can typedef them in TreeNode and the Inspector
    enum class Mode {
        CHILDREN,
        DETAILS,
        OPAQUE      // node is a leaf, cannot be opened (no expand arrow)
    };

    enum class DetailsMode {
        GROUPED,
        FLAT,
        INHERITANCE,
        PACKET
    };

    enum class DataRole : int {
        HIGHLIGHT_RANGE = Qt::UserRole,
    };

    struct NodeModeOverride {
        Mode mode;
        DetailsMode detailsMode; // only meaningful when mode == DETAILS
    };
    typedef std::unordered_map<std::string, NodeModeOverride> NodeModeOverrideMap;

private:
    bool sortByName = true;
    bool allowModeOverrides = true;
    std::vector<RootNode *> rootNodes;
    // maps nodeIdentifier to overridden Mode, for nodes whose mode was overridden by the user
    NodeModeOverrideMap nodeModeOverrides;

public:
    GenericObjectTreeModel(cObject *object, bool sortByName, const NodeModeOverrideMap& modeOverrides, bool allowModeOverrides = true, QObject *parent = nullptr);
    GenericObjectTreeModel(std::vector<cObject *> roots, bool sortByName, const NodeModeOverrideMap& modeOverrides, bool allowModeOverrides = true, QObject *parent = nullptr);

    bool getSortByName() const { return sortByName; }

    std::vector<cObject *> getRootObjects();

    const NodeModeOverrideMap& getNodeModeOverrides() const { return nodeModeOverrides;}

    void setNodeMode(const QModelIndex &index, Mode mode, DetailsMode detailsMode = DetailsMode::GROUPED);
    void unsetNodeMode(const QModelIndex &index);

    QModelIndex index(int row, int column, const QModelIndex &parent) const override;
    QModelIndex parent(const QModelIndex &child) const override;
    bool hasChildren(const QModelIndex &parent = QModelIndex()) const override;
    int rowCount(const QModelIndex &parent) const override;
    int columnCount(const QModelIndex &parent) const override;
    QVariant data(const QModelIndex &index, int role) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;
    Qt::ItemFlags flags(const QModelIndex &index) const override;

    bool canFetchMore(const QModelIndex &parent) const override;
    void fetchMore(const QModelIndex &parent) override;

    void refreshTreeStructure();
    void refreshNodeChildrenRec(const QModelIndex &index, bool emitSignals = true);
    void refreshChildList(const QModelIndex &index, bool emitSignals = true);

    cObject *getCObjectPointer(const QModelIndex &index);
    // same as above, but translates cWatchObj pointers to their watched cObject pointers
    cObject *getCObjectPointerToInspect(const QModelIndex &index);

    ~GenericObjectTreeModel();

Q_SIGNALS:
    void dataEdited(const QModelIndex& index) ;
};

}  // namespace qtenv
}  // namespace omnetpp

// this is needed to wrap the HighlightRange into a QVariant
// according to the Qt docs, this must not be in a namespace
Q_DECLARE_METATYPE(omnetpp::qtenv::HighlightRange)

#endif // __OMNETPP_QTENV_GENERICOBJECTTREEMODEL_H
