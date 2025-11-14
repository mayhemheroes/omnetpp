//==========================================================================
//  MULTIWORDFILTERPROXYMODEL.H - part of
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

#ifndef __OMNETPP_QTENV_MULTIWORDFILTERPROXYMODEL_H
#define __OMNETPP_QTENV_MULTIWORDFILTERPROXYMODEL_H

#include <QtCore/QSortFilterProxyModel>
#include <QtCore/QString>
#include <QtCore/QList>
#include "qtenvdefs.h"

namespace omnetpp {
namespace qtenv {

/**
 * Custom proxy model for multi-word filtering. Allows filtering by multiple
 * space-separated words where all words must be present in the text (order independent).
 * Necessary because none of the built-in Qt::MatchFlags modes provided the functionality
 * for QCompleter that we wanted. So instead, we disable the filtering there with
 * QCompletion::UnfilteredPopupCompletion, and do it entirely manually using this class.
 */
class QTENV_API MultiWordFilterProxyModel : public QSortFilterProxyModel
{
    Q_OBJECT

    QList<QString> filterWords;

public:
    explicit MultiWordFilterProxyModel(QObject *parent = nullptr);

    /**
     * Sets the filter text. The text will be split by spaces, and all resulting
     * words must be present in a row for it to pass the filter.
     */
    void setFilterText(const QString &text);

protected:
    bool filterAcceptsRow(int sourceRow, const QModelIndex &sourceParent) const override;
};

}  // namespace qtenv
}  // namespace omnetpp

#endif // __OMNETPP_QTENV_MULTIWORDFILTERPROXYMODEL_H
