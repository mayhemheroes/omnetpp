//==========================================================================
//  FILTERINGCOMBOBOX.CC - part of
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

#include "filteringcombobox.h"
#include "multiwordfilterproxymodel.h"
#include <QtWidgets/QCompleter>
#include <QtWidgets/QLineEdit>

namespace omnetpp {
namespace qtenv {

FilteringComboBox::FilteringComboBox(QWidget *parent)
    : QComboBox(parent)
{
    // Make the combobox editable to allow filtering
    setEditable(true);
}

void FilteringComboBox::setupFiltering()
{
    // Create the multi-word filter proxy model
    proxyModel = new MultiWordFilterProxyModel(this);
    proxyModel->setSourceModel(model());

    // Create and configure the completer
    QCompleter *completer = new QCompleter(proxyModel, this);
    completer->setCaseSensitivity(Qt::CaseInsensitive);
    completer->setCompletionMode(QCompleter::UnfilteredPopupCompletion);

    // When navigating with arrow keys, suppress filter updates
    connect(completer, QOverload<const QString&>::of(&QCompleter::highlighted),
            this, [this](const QString &) {
        suppressFilterUpdate = true;
    });

    // Connect line edit text changes to update the filter
    // Skip updates when navigating with arrow keys (suppressFilterUpdate flag is set)
    connect(lineEdit(), &QLineEdit::textChanged, this, [this](const QString &text) {
        if (suppressFilterUpdate) {
            suppressFilterUpdate = false;  // Reset flag
        } else {
            proxyModel->setFilterText(text);
        }
    });

    // Set the completer on the combobox
    setCompleter(completer);
}

}  // namespace qtenv
}  // namespace omnetpp
