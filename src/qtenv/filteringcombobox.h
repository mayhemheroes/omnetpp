//==========================================================================
//  FILTERINGCOMBOBOX.H - part of
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

#ifndef __OMNETPP_QTENV_FILTERINGCOMBOBOX_H
#define __OMNETPP_QTENV_FILTERINGCOMBOBOX_H

#include <QtWidgets/QComboBox>
#include "qtenvdefs.h"

namespace omnetpp {
namespace qtenv {

class MultiWordFilterProxyModel;

/**
 * A QComboBox with built-in multi-word filtering support. When the user types
 * in the combobox, items are filtered to show only those that contain all
 * space-separated words (order-independent).
 */
class QTENV_API FilteringComboBox : public QComboBox
{
    Q_OBJECT

    MultiWordFilterProxyModel *proxyModel = nullptr;
    bool suppressFilterUpdate = false;  // Flag to prevent filter updates during completer navigation

public:
    explicit FilteringComboBox(QWidget *parent = nullptr);

    /**
     * Sets up the filtering behavior on the combobox. Must be called after
     * the model has been populated with items.
     */
    void setupFiltering();
};

}  // namespace qtenv
}  // namespace omnetpp

#endif // __OMNETPP_QTENV_FILTERINGCOMBOBOX_H
