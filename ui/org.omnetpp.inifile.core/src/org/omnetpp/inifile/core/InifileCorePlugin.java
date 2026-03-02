/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.inifile.core;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class InifileCorePlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static String PLUGIN_ID;

    // The shared instance
    private static InifileCorePlugin plugin;

    /**
     * The constructor
     */
    public InifileCorePlugin() {
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        PLUGIN_ID = getBundle().getSymbolicName();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static InifileCorePlugin getDefault() {
        return plugin;
    }

    public static void logError(Throwable exception) {
        logError(exception.toString(), exception);
    }

    public static void logError(String message, Throwable exception) {
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception));
        }
        else {
            System.err.println(message);
            exception.printStackTrace();
        }
    }

    public static CoreException wrapIntoCoreException(Throwable exception) {
        String msg = StringUtils.defaultIfEmpty(exception.getMessage(), exception.getClass().getSimpleName());
        return wrapIntoCoreException(msg, exception);
    }

    public static CoreException wrapIntoCoreException(String message, Throwable exception) {
        return new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception));
    }

}
