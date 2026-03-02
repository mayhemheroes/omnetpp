/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.inifile.core.model;

/**
 * Callback interface for handling analysis timeout events.
 * The editor plugin supplies a UI implementation (e.g. showing a dialog).
 *
 * @author andras
 */
public interface IAnalysisTimeoutHandler {
    int CANCEL = 0;
    int WAIT = 1;
    int DISABLE_AND_CANCEL = 2;

    /**
     * Called when the analysis times out. Should return one of
     * {@link #CANCEL}, {@link #WAIT}, or {@link #DISABLE_AND_CANCEL}.
     */
    int handleTimeout();
}
