package org.omnetpp.ide.installer;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class provides the default project installer behavior as follows:
 *  - download distribution file
 *  - extract distribution under workspace
 *  - import project into workspace
 *  - open project
 *  - build project
 *  - optionally show welcome page
 *
 * @author levy
 */
public class DefaultProjectInstaller extends AbstractProjectInstaller {
    public DefaultProjectInstaller(ProjectDescription projectDescription, ProjectInstallationOptions projectInstallationOptions) {
        super(projectDescription, projectInstallationOptions);
    }

    @Override
    public void run(IProgressMonitor progressMonitor) throws CoreException {
        progressMonitor.beginTask("Installing " + projectDescription.getTitle() + " into the workspace", 5);
        File projectDistributionFile = downloadProjectDistribution(progressMonitor, projectDescription.getDistributionURL());
        File projectDirectory = extractProjectDistribution(progressMonitor, projectDistributionFile);
        // Wrap workspace-modifying operations to defer resource change notifications,
        // preventing ProjectFeaturesListener from racing with our own feature initialization.
        IProject[] projectHolder = new IProject[1];
        ResourcesPlugin.getWorkspace().run(monitor -> {
            projectHolder[0] = importProjectIntoWorkspace(progressMonitor, projectDirectory);
            IProject project = projectHolder[0];
            openProject(progressMonitor, project);
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            String welcomePage = projectDescription.getWelcomePage();
            if (welcomePage != null)
                openEditor(project.getFile(welcomePage));
            expandProject(project);
            initializeProjectFeaturesState(project);
            setActiveBuildConfiguration(project);
            setBuildEnvironmentVariables(project);
        }, progressMonitor);
        buildProject(progressMonitor, projectHolder[0]);
        progressMonitor.done();
    }
}