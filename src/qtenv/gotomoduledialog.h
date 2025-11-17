//==========================================================================
//  GOTOMODULEDIALOG.H - part of
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

#ifndef __OMNETPP_QTENV_GOTOMODULEDIALOG_H
#define __OMNETPP_QTENV_GOTOMODULEDIALOG_H

#include <QtWidgets/QDialog>
#include "qtenvdefs.h"

namespace Ui {
class GotoModuleDialog;
}

namespace omnetpp {

class cModule;

namespace qtenv {

class QTENV_API GotoModuleDialog : public QDialog
{
    Q_OBJECT

public:
    explicit GotoModuleDialog(cModule *currentModule, QWidget *parent = nullptr);
    ~GotoModuleDialog();

    QString getSelectedModulePath() const;

private:
    Ui::GotoModuleDialog *ui;

    void collectModulePaths(cModule *module, QStringList &paths);
};

}  // namespace qtenv
}  // namespace omnetpp

#endif // __OMNETPP_QTENV_GOTOMODULEDIALOG_H
