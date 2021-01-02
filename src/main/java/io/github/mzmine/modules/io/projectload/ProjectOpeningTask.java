/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.projectload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.google.common.io.CountingInputStream;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.version_2_5.PeakListOpenHandler_2_5;
import io.github.mzmine.modules.io.projectload.version_2_5.RawDataFileOpenHandler_2_5;
import io.github.mzmine.modules.io.projectload.version_2_5.UserParameterOpenHandler_2_5;
import io.github.mzmine.modules.io.projectload.version_3_0.PeakListOpenHandler_3_0;
import io.github.mzmine.modules.io.projectload.version_3_0.RawDataFileOpenHandler_3_0;
import io.github.mzmine.modules.io.projectload.version_3_0.UserParameterOpenHandler_3_0;
import io.github.mzmine.modules.io.projectsave.ProjectSavingTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.StreamCopy;
import javafx.scene.control.ButtonType;

public class ProjectOpeningTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File openFile;
  private MZmineProjectImpl newProject;

  private RawDataFileOpenHandler rawDataFileOpenHandler;
  private PeakListOpenHandler peakListOpenHandler;
  private UserParameterOpenHandler userParameterOpenHandler;
  private StreamCopy copyMachine;

  private CountingInputStream cis;
  private long totalBytes, finishedBytes;
  private String currentLoadedObjectName;

  // This hashtable maps stored IDs to raw data file objects
  private final Hashtable<String, RawDataFile> dataFilesIDMap = new Hashtable<>();
  private final Hashtable<String, File> scanFilesIDMap = new Hashtable<>();

  public ProjectOpeningTask(ParameterSet parameters) {
    this.openFile = parameters.getParameter(ProjectLoaderParameters.projectFile).getValue();
  }

  public ProjectOpeningTask(File openFile) {
    this.openFile = openFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    if (currentLoadedObjectName == null) {
      return "Opening project " + openFile;
    }
    return "Opening project " + openFile + " (" + currentLoadedObjectName + ")";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {

    if (totalBytes == 0) {
      return 0;
    }

    long totalReadBytes = this.finishedBytes;

    // Add the current ZIP entry progress to totalReadBytes
    synchronized (this) {
      if (cis != null) {
        totalReadBytes += cis.getCount();
      }
    }

    return (double) totalReadBytes / totalBytes;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    try {
      // Check if existing raw data files are present
      ProjectManager projectManager = MZmineCore.getProjectManager();
      if (projectManager.getCurrentProject().getDataFiles().length > 0) {

        ButtonType confirm = MZmineCore.getDesktop().displayConfirmation(
            "Loading the project will replace the existing raw data files and feature lists. Do you want to proceed?",
            ButtonType.YES, ButtonType.NO);

        if (confirm != ButtonType.YES) {
          cancel();
          return;
        }

      }

      logger.info("Started opening project " + openFile);
      setStatus(TaskStatus.PROCESSING);

      // Create a new project
      newProject = new MZmineProjectImpl();
      newProject.setProjectFile(openFile);

      // Close all windows related to previous project
      GUIUtils.closeAllWindows();

      // Replace the current project with the new one
      projectManager.setCurrentProject(newProject);

      // Open the ZIP file
      ZipFile zipFile = new ZipFile(openFile);

      // Get total uncompressed size
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        totalBytes += entry.getSize();
      }

      final Pattern rawFilePattern =
          Pattern.compile(RawDataFileImpl.SAVE_IDENTIFIER + " #([\\d]+) (.*)\\.xml$");
      final Pattern imsRawFilePattern =
          Pattern.compile(IMSRawDataFileImpl.SAVE_IDENTIFIER + " #([\\d]+) (.*)\\.xml$");
      final Pattern imagingRawFilePattern =
          Pattern.compile(ImagingRawDataFileImpl.SAVE_IDENTIFIER + " #([\\d]+) (.*)\\.xml$");

      // We have two patterns, since the data points file is named in accordance to the
      // raw file name. However, we load it in exactly the same way.
      final Pattern scansFilePattern =
          Pattern.compile(RawDataFileImpl.SAVE_IDENTIFIER + " #([\\d]+) (.*)\\.scans$");
      final Pattern imsScansFilePattern =
          Pattern.compile(IMSRawDataFileImpl.SAVE_IDENTIFIER + " #([\\d]+) (.*)\\.scans$");
      final Pattern imagingScansFilePattern =
          Pattern.compile(ImagingRawDataFileImpl.SAVE_IDENTIFIER + " #([\\d]+) (.*)\\.scans$");

      final Pattern peakListPattern = Pattern.compile("Peak list #([\\d]+) (.*)\\.xml$");

      boolean versionInformationLoaded = false;

      // Iterate over the entries and read them
      entries = zipFile.entries();
      while (entries.hasMoreElements()) {

        if (isCanceled()) {
          zipFile.close();
          return;
        }

        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        cis = new CountingInputStream(zipFile.getInputStream(entry));

        // Load version
        if (entryName.equals(ProjectSavingTask.VERSION_FILENAME)) {
          loadVersion(cis);
          versionInformationLoaded = true;
        }

        // Load configuration
        if (entryName.equals(ProjectSavingTask.CONFIG_FILENAME)) {
          loadConfiguration(cis);
        }

        // Load user parameters
        if (entryName.equals(ProjectSavingTask.PARAMETERS_FILENAME)) {
          loadUserParameters(cis);
        }

        final Matcher imsRawFileMatcher = imsRawFilePattern.matcher(entryName);
        if (imsRawFileMatcher.matches()) {
          logger.info("loading ims raw from " + entryName);
          final String fileID = imsRawFileMatcher.group(1);
          final String fileName = imsRawFileMatcher.group(2);
          loadRawDataFile(cis, fileID, fileName, true, false);
        }

        final Matcher imagingRawFileMatcher = imagingRawFilePattern.matcher(entryName);
        if (imagingRawFileMatcher.matches()) {
          logger.info("loading imaging raw from " + entryName);
          final String fileID = imagingRawFileMatcher.group(1);
          final String fileName = imagingRawFileMatcher.group(2);
          loadRawDataFile(cis, fileID, fileName, false, true);
        }

        // Load a raw data file
        final Matcher rawFileMatcher = rawFilePattern.matcher(entryName);
        if (rawFileMatcher.matches()) {
          logger.info("loading normal raw from " + entryName);
          final String fileID = rawFileMatcher.group(1);
          final String fileName = rawFileMatcher.group(2);
          loadRawDataFile(cis, fileID, fileName, false, false);
        }

        // Load the scan data of a raw data file
        final Matcher scansFileMatcher = scansFilePattern.matcher(entryName);
        if (scansFileMatcher.matches()) {
          final String fileID = scansFileMatcher.group(1);
          final String fileName = scansFileMatcher.group(2);
          loadScansFile(cis, fileID, fileName);
        }
        final Matcher imsScansFileMatcher = imsScansFilePattern.matcher(entryName);
        if (imsScansFileMatcher.matches()) {
          final String fileID = imsScansFileMatcher.group(1);
          final String fileName = imsScansFileMatcher.group(2);
          loadScansFile(cis, fileID, fileName);
        }
        final Matcher imagingScansFileMatcher = imagingScansFilePattern.matcher(entryName);
        if (imagingScansFileMatcher.matches()) {
          final String fileID = imagingScansFileMatcher.group(1);
          final String fileName = imagingScansFileMatcher.group(2);
          loadScansFile(cis, fileID, fileName);
        }

        // Load a feature list
        final Matcher peakListMatcher = peakListPattern.matcher(entryName);
        if (peakListMatcher.matches()) {
          final String peakListName = peakListMatcher.group(2);
          loadFeatureList(cis, peakListName);
        }

        // Close the ZIP entry
        cis.close();

        // Add the uncompressed entry size finishedBytes
        synchronized (this) {
          finishedBytes += entry.getSize();
          cis = null;
        }

      }

      // Finish and close the project ZIP file
      zipFile.close();

      if (!versionInformationLoaded) {
        throw new IOException(
            "This file is not valid MZmine project. It does not contain version information.");
      }

      // Final check for cancel
      if (isCanceled()) {
        return;
      }

      logger.info("Finished opening project " + openFile);
      setStatus(TaskStatus.FINISHED);

      // add to last loaded projects
      MZmineCore.getConfiguration().getLastProjectsParameter().addFile(openFile);

    } catch (Throwable e) {

      // If project opening was canceled, parser was stopped by a
      // SAXException which can be safely ignored
      if (isCanceled()) {
        return;
      }

      setStatus(TaskStatus.ERROR);
      e.printStackTrace();
      setErrorMessage("Failed opening project: " + ExceptionUtils.exceptionToString(e));
    }

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#cancel()
   */
  @Override
  public void cancel() {

    logger.info("Canceling opening of project " + openFile);

    setStatus(TaskStatus.CANCELED);

    if (rawDataFileOpenHandler != null) {
      rawDataFileOpenHandler.cancel();
    }

    if (peakListOpenHandler != null) {
      peakListOpenHandler.cancel();
    }

    if (userParameterOpenHandler != null) {
      userParameterOpenHandler.cancel();
    }

    if (copyMachine != null) {
      copyMachine.cancel();
    }

  }

  /**
   * Load the version info from the ZIP file and checks whether such version can be opened with this
   * MZmine
   */
  private void loadVersion(InputStream is) throws IOException {

    logger.info("Checking project version");

    currentLoadedObjectName = "Version";

    Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)");

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String projectVersionString = reader.readLine();
    String mzmineVersionString = MZmineCore.getMZmineVersion();

    Matcher m = versionPattern.matcher(mzmineVersionString);
    if (!m.find()) {
      throw new IOException("Invalid MZmine version " + mzmineVersionString);
    }
    int mzmineMajorVersion = Integer.valueOf(m.group(1));
    int mzmineMinorVersion = Integer.valueOf(m.group(2));

    m = versionPattern.matcher(projectVersionString);
    if (!m.find()) {
      throw new IOException("Invalid project version " + projectVersionString);
    }
    int projectMajorVersion = Integer.valueOf(m.group(1));
    int projectMinorVersion = Integer.valueOf(m.group(2));

    // Check if project was saved with an old version
    if ((projectMajorVersion == 1) || ((projectMajorVersion == 2) && (projectMinorVersion <= 4))) {
      throw new IOException("This project was saved with an old version (MZmine "
          + projectVersionString + ") and it cannot be opened in MZmine " + mzmineVersionString);
    }

    // Check if the project version is > 2.5
    if ((projectMajorVersion == 2) && (projectMinorVersion > 4)) {
      // Default opening handler for MZmine.5 and higher
      rawDataFileOpenHandler = new RawDataFileOpenHandler_2_5();
      peakListOpenHandler = new PeakListOpenHandler_2_5(dataFilesIDMap);
      userParameterOpenHandler = new UserParameterOpenHandler_2_5(newProject, dataFilesIDMap);
      return;
    }

    // Check if project was saved with a newer version
    if (mzmineMajorVersion > 0) {
      if ((projectMajorVersion > mzmineMajorVersion) || ((projectMajorVersion == mzmineMajorVersion)
          && (projectMinorVersion > mzmineMinorVersion))) {
        String warning = "Warning: this project was saved with a newer version of MZmine ("
            + projectVersionString + "). Opening this project in MZmine " + mzmineVersionString
            + " may result in errors or loss of information.";
        MZmineCore.getDesktop().displayMessage(warning);
      }
    }

    // Default opening handler for MZmine 3 and higher
    rawDataFileOpenHandler = new RawDataFileOpenHandler_3_0();
    peakListOpenHandler = new PeakListOpenHandler_3_0(dataFilesIDMap);
    userParameterOpenHandler = new UserParameterOpenHandler_3_0(newProject, dataFilesIDMap);

  }

  /**
   * Load the configuration file from the project zip file
   */
  private void loadConfiguration(InputStream is) throws IOException {

    logger.info("Loading configuration file");

    currentLoadedObjectName = "Configuration";

    File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
    FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
    copyMachine = new StreamCopy();
    copyMachine.copy(is, fileStream);
    fileStream.close();

    try {
      MZmineCore.getConfiguration().loadConfiguration(tempConfigFile);
    } catch (Exception e) {
      logger.warning(
          "Could not load configuration from the project: " + ExceptionUtils.exceptionToString(e));
    }

    tempConfigFile.delete();
  }

  private void loadRawDataFile(InputStream is, String fileID, String fileName,
      boolean isIMSRawDataFile, boolean isImagingFile) throws IOException,
      ParserConfigurationException, SAXException, InstantiationException, IllegalAccessException {

    logger.info("Loading raw data file #" + fileID + ": " + fileName);

    currentLoadedObjectName = fileName;

    File scansFile = scanFilesIDMap.get(fileID);
    if (scansFile == null) {
      throw new IOException("Missing scans data for file ID " + fileID);
    }

    RawDataFile newFile =
        rawDataFileOpenHandler.readRawDataFile(is, scansFile, isIMSRawDataFile, isImagingFile);
    newProject.addFile(newFile);
    dataFilesIDMap.put(fileID, newFile);

  }

  private void loadScansFile(InputStream is, String fileID, String fileName) throws IOException {

    logger.info("Loading scans data #" + fileID + ": " + fileName);

    currentLoadedObjectName = fileName + " scan data";

    final File tempFile = RawDataFileImpl.createNewDataPointsFile();
    logger.info("Saving scans data of #" + fileID + " to " + tempFile);

    final FileOutputStream os = new FileOutputStream(tempFile);

    // If the project was saved with 2.5 version < 3.0
    copyMachine =
        (rawDataFileOpenHandler instanceof RawDataFileOpenHandler_2_5) ? new StreamCopy32to64()
            : new StreamCopy();
    copyMachine.copy(is, os);
    os.close();

    scanFilesIDMap.put(fileID, tempFile);

  }

  private void loadFeatureList(InputStream is, String featureListName) throws IOException,
      ParserConfigurationException, SAXException, InstantiationException, IllegalAccessException {
    logger.info("Loading feature list " + featureListName);

    currentLoadedObjectName = featureListName;

    FeatureList newFeatureList = peakListOpenHandler.readPeakList(is);

    newProject.addFeatureList(newFeatureList);
  }

  private void loadUserParameters(InputStream is) throws IOException, ParserConfigurationException,
      SAXException, InstantiationException, IllegalAccessException {

    // Older versions of MZmine had no parameter saving
    if (userParameterOpenHandler == null) {
      return;
    }

    logger.info("Loading user parameters");

    currentLoadedObjectName = "User parameters";

    userParameterOpenHandler.readUserParameters(is);

  }

}
