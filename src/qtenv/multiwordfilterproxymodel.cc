//==========================================================================
//  MULTIWORDFILTERPROXYMODEL.CC - part of
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

#include "multiwordfilterproxymodel.h"

namespace omnetpp {
namespace qtenv {

MultiWordFilterProxyModel::MultiWordFilterProxyModel(QObject *parent)
    : QSortFilterProxyModel(parent)
{
    setFilterCaseSensitivity(Qt::CaseInsensitive);
}

void MultiWordFilterProxyModel::setFilterText(const QString &text)
{
    filterWords = text.split(' ', Qt::SkipEmptyParts);
    invalidateFilter();
}

bool MultiWordFilterProxyModel::filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const
{
    QModelIndex index = sourceModel()->index(sourceRow, 0, sourceParent);
    QString text = sourceModel()->data(index).toString();

    // Check that all words are contained in the text (order independent)
    for (const QString &word : filterWords) {
        if (!text.contains(word, Qt::CaseInsensitive)) {
            return false;
        }
    }

    return true;
}

}  // namespace qtenv
}  // namespace omnetpp
