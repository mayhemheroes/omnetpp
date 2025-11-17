//==========================================================================
//  GOTOMODULEDIALOG.CC - part of
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

#include "gotomoduledialog.h"
#include "ui_gotomoduledialog.h"
#include "filteringcombobox.h"
#include "qtenv.h"
#include "omnetpp/cmodule.h"
#include "omnetpp/csimulation.h"
#include <QtCore/QDebug>
#include <QtWidgets/QLineEdit>

namespace omnetpp {
namespace qtenv {

GotoModuleDialog::GotoModuleDialog(cModule *currentModule, QWidget *parent) :
    QDialog(parent),
    ui(new Ui::GotoModuleDialog)
{
    ui->setupUi(this);
    setFont(getQtenv()->getBoldFont());

    // Collect all module paths from the network
    QStringList modulePaths;

    // Start from the system module (root of the network)
    cModule *systemModule = getSimulation()->getSystemModule();
    if (systemModule) {
        collectModulePaths(systemModule, modulePaths);
    }

    // Sort the paths for better usability
    modulePaths.sort(Qt::CaseInsensitive);

    // Add all module paths to the combobox
    ui->modulePathComboBox->addItems(modulePaths);

    // Set up the filtering behavior (completer, proxy model, etc.)
    ui->modulePathComboBox->setupFiltering();

    // Pre-fill with current module path if available
    if (currentModule) {
        QString currentPath = QString::fromStdString(currentModule->getFullPath());
        ui->modulePathComboBox->setCurrentText(currentPath);
        ui->modulePathComboBox->lineEdit()->selectAll();
    }

    // Set focus to the combobox
    ui->modulePathComboBox->setFocus();
}

GotoModuleDialog::~GotoModuleDialog()
{
    delete ui;
}

QString GotoModuleDialog::getSelectedModulePath() const
{
    return ui->modulePathComboBox->currentText();
}

void GotoModuleDialog::collectModulePaths(cModule *module, QStringList &paths)
{
    if (!module)
        return;

    // Add this module's path
    paths.append(QString::fromStdString(module->getFullPath()));

    // Recursively collect paths from all submodules
    for (cModule::SubmoduleIterator it(module); !it.end(); ++it) {
        cModule *submodule = *it;
        if (submodule) {
            collectModulePaths(submodule, paths);
        }
    }
}

}  // namespace qtenv
}  // namespace omnetpp
