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

package io.github.mzmine.project.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

/**
 * RawDataFile implementation. It provides storage of data points for scans and mass lists using the
 * storeDataPoints() and readDataPoints() methods. The data points are stored in a temporary file
 * (dataPointsFile) and the structure of the file is stored in two TreeMaps. The dataPointsOffsets
 * maps storage ID to the offset in the dataPointsFile. The dataPointsLength maps the storage ID to
 * the number of data points stored under this ID. When stored data points are deleted using
 * removeStoredDataPoints(), the dataPointsFile is not modified, the storage ID is just deleted from
 * the two TreeMaps. When the project is saved, the contents of the dataPointsFile are consolidated
 * - only data points referenced by the TreeMaps are saved (see the RawDataFileSaveHandler class).
 */
public class RawDataFileImpl implements RawDataFile {

  public static final String SAVE_IDENTIFIER = "Raw data file";

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  // Name of this raw data file - may be changed by the user
  private String dataFileName;

  private final Hashtable<Integer, Range<Double>> dataMZRange;
  private final Hashtable<Integer, Range<Float>> dataRTRange;
  private final Hashtable<Integer, Double> dataMaxBasePeakIntensity, dataMaxTIC;
  private final Hashtable<Integer, int[]> scanNumbersCache;

  private ByteBuffer buffer = ByteBuffer.allocate(20000);
  private final TreeMap<Integer, Long> dataPointsOffsets;
  private final TreeMap<Integer, Integer> dataPointsLengths;

  // Temporary file for scan data storage
  private File storageFileName;
  private RandomAccessFile storageFile;
  private final MemoryMapStorage storageMemoryMap = new MemoryMapStorage();



  private ObjectProperty<Color> color;

  // To store mass lists that have been added but not yet reflected in the GUI
  // by the
  // notifyUpdatedMassLists() method
  // private final List<MassList> newMassLists = new ArrayList<>();

  /**
   * Scans
   */
  protected final Hashtable<Integer, Scan> scans;

  public RawDataFileImpl(String dataFileName) throws IOException {

    this.dataFileName = dataFileName;

    // Prepare the hashtables for scan numbers and data limits.
    scanNumbersCache = new Hashtable<>();
    dataMZRange = new Hashtable<>();
    dataRTRange = new Hashtable<>();
    dataMaxBasePeakIntensity = new Hashtable<>();
    dataMaxTIC = new Hashtable<>();
    scans = new Hashtable<>();
    dataPointsOffsets = new TreeMap<>();
    dataPointsLengths = new TreeMap<>();

    color = new SimpleObjectProperty<>();
    color.setValue(MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor());

  }

  @Override
  public @Nonnull MemoryMapStorage getMemoryMapStorage() {
    return storageMemoryMap;
  }

  @Override
  public RawDataFile clone() throws CloneNotSupportedException {
    return (RawDataFile) super.clone();
  }

  /**
   * Create a new temporary data points file
   */
  public static File createNewDataPointsFile() throws IOException {
    return File.createTempFile("mzmine", ".scans");
  }

  /**
   * Returns the (already opened) data points file. Warning: may return null in case no scans have
   * been added yet to this RawDataFileImpl instance
   */
  public RandomAccessFile getDataPointsFile() {
    return storageFile;
  }

  /**
   * Opens the given file as a data points file for this RawDataFileImpl instance. If the file is
   * not empty, the TreeMaps supplied as parameters have to describe the mapping of storage IDs to
   * data points in the file.
   */
  public synchronized void openDataPointsFile(File dataPointsFileName) throws IOException {

    if (this.storageFile != null) {
      throw new IOException("Cannot open another data points file, because one is already open");
    }

    this.storageFileName = dataPointsFileName;
    this.storageFile = new RandomAccessFile(dataPointsFileName, "rw");
    FileChannel fileChannel = storageFile.getChannel();
    // this.storageMemoryMap = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0,
    // Integer.MAX_VALUE);

    // Locks the temporary file so it is not removed when another instance
    // of MZmine is starting. Lock will be automatically released when this
    // instance of MZmine exits. Locking may fail on network-mounted
    // filesystems.
    try {
      fileChannel.lock();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to lock the file " + dataPointsFileName, e);
    }

    // Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
    // bug #4171239. We will try to remove the temporary files in a
    // shutdown hook registered in the main.ShutDownHook class
    dataPointsFileName.deleteOnExit();

  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getNumOfScans()
   */
  @Override
  public int getNumOfScans() {
    return scans.size();
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getScan(int)
   */
  @Override
  public @Nullable Scan getScan(int scanNumber) {
    return scans.get(scanNumber);
  }

  /**
   * @param rt The rt
   * @param mslevel The ms level
   * @return The scan number at a given retention time within a range of 2 (min/sec?) or -1 if no
   *         scan can be found.
   */
  @Override
  public int getScanNumberAtRT(float rt, int mslevel) {
    if (rt > getDataRTRange(mslevel).upperEndpoint()) {
      return -1;
    }
    Range<Float> range = Range.closed(rt - 2, rt + 2);
    int[] scanNumbers = getScanNumbers(mslevel, range);
    double minDiff = 10E6;

    for (int i = 0; i < scanNumbers.length; i++) {
      int scanNum = scanNumbers[i];
      double diff = Math.abs(rt - getScan(scanNum).getRetentionTime());
      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) { // not triggered in first run
        return scanNumbers[i - 1]; // the previous one was better
      }
    }
    return -1;
  }

  /**
   * @param rt The rt
   * @return The scan number at a given retention time within a range of 2 (min/sec?) or -1 if no
   *         scan can be found.
   */
  @Override
  public int getScanNumberAtRT(float rt) {
    if (rt > getDataRTRange().upperEndpoint()) {
      return -1;
    }
    int[] scanNumbers = getScanNumbers();
    double minDiff = 10E10;

    for (int i = 0; i < scanNumbers.length; i++) {
      int scanNum = scanNumbers[i];
      double diff = Math.abs(rt - getScan(scanNum).getRetentionTime());
      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) { // not triggered in first run
        return scanNumbers[i - 1]; // the previous one was better
      }
    }
    return -1;
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getScanNumbers(int)
   */
  @Override
  public @Nonnull int[] getScanNumbers(int msLevel) {
    if (scanNumbersCache.containsKey(msLevel)) {
      return scanNumbersCache.get(msLevel);
    }
    Range<Float> all = Range.all();
    int scanNumbers[] = getScanNumbers(msLevel, all);
    scanNumbersCache.put(msLevel, scanNumbers);
    return scanNumbers;
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getScanNumbers(int, Range)
   */
  @Override
  public @Nonnull int[] getScanNumbers(int msLevel, @Nonnull Range<Float> rtRange) {

    assert rtRange != null;

    ArrayList<Integer> eligibleScanNumbers = new ArrayList<Integer>();

    Enumeration<? extends Scan> scansEnum = scans.elements();
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();

      if ((scan.getMSLevel() == msLevel) && (rtRange.contains(scan.getRetentionTime()))) {
        eligibleScanNumbers.add(scan.getScanNumber());
      }
    }

    int[] numbersArray = Ints.toArray(eligibleScanNumbers);
    Arrays.sort(numbersArray);

    return numbersArray;
  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getScanNumbers()
   */
  @Override
  @Nonnull
  public int[] getScanNumbers() {

    if (scanNumbersCache.containsKey(0) && scanNumbersCache.get(0).length == scans.size()) {
      return scanNumbersCache.get(0);
    }

    Set<Integer> allScanNumbers = scans.keySet();
    int[] numbersArray = Ints.toArray(allScanNumbers);
    Arrays.sort(numbersArray);

    scanNumbersCache.put(0, numbersArray);

    return numbersArray;

  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getMSLevels()
   */
  @Override
  @Nonnull
  public int[] getMSLevels() {

    Set<Integer> msLevelsSet = new HashSet<Integer>();

    Enumeration<? extends Scan> scansEnum = scans.elements();
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();
      msLevelsSet.add(scan.getMSLevel());
    }

    int[] msLevels = Ints.toArray(msLevelsSet);
    Arrays.sort(msLevels);
    return msLevels;

  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getDataMaxBasePeakIntensity(int)
   */
  @Override
  public double getDataMaxBasePeakIntensity(int msLevel) {

    // check if we have this value already cached
    Double maxBasePeak = dataMaxBasePeakIntensity.get(msLevel);
    if (maxBasePeak != null) {
      return maxBasePeak;
    }

    // find the value
    Enumeration<? extends Scan> scansEnum = scans.elements();
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();

      // ignore scans of other ms levels
      if (scan.getMSLevel() != msLevel) {
        continue;
      }

      Double scanBasePeak = scan.getBasePeakIntensity();
      if (scanBasePeak == null) {
        continue;
      }

      if ((maxBasePeak == null) || (scanBasePeak > maxBasePeak)) {
        maxBasePeak = scanBasePeak;
      }

    }

    // return -1 if no scan at this MS level
    if (maxBasePeak == null) {
      maxBasePeak = -1d;
    }

    // cache the value
    dataMaxBasePeakIntensity.put(msLevel, maxBasePeak);

    return maxBasePeak;

  }

  /**
   * @see io.github.mzmine.datamodel.RawDataFile#getDataMaxTotalIonCurrent(int)
   */
  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {

    // check if we have this value already cached
    Double maxTIC = dataMaxTIC.get(msLevel);
    if (maxTIC != null) {
      return maxTIC.doubleValue();
    }

    // find the value
    Enumeration<? extends Scan> scansEnum = scans.elements();
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();

      // ignore scans of other ms levels
      if (scan.getMSLevel() != msLevel) {
        continue;
      }

      if ((maxTIC == null) || (scan.getTIC() > maxTIC)) {
        maxTIC = scan.getTIC();
      }

    }

    // return -1 if no scan at this MS level
    if (maxTIC == null) {
      maxTIC = -1d;
    }

    // cache the value
    dataMaxTIC.put(msLevel, maxTIC);

    return maxTIC;

  }

  public synchronized int storeDataPoints(DataPoint dataPoints[]) throws IOException {

    if (storageFile == null) {
      File newFile = RawDataFileImpl.createNewDataPointsFile();
      openDataPointsFile(newFile);
    }

    final long currentOffset = storageFile.length();

    final int currentID;
    if (!dataPointsOffsets.isEmpty()) {
      currentID = dataPointsOffsets.lastKey() + 1;
    } else {
      currentID = 1;
    }

    final int numOfDataPoints = dataPoints.length;

    // Convert the dataPoints into a byte array. Each double takes 8 bytes,
    // so we get the current double offset by dividing the size of the file
    // by 8
    final int numOfBytes = numOfDataPoints * 2 * 8;

    if (buffer.capacity() < numOfBytes) {
      buffer = ByteBuffer.allocate(numOfBytes * 2);
    } else {
      // JDK 9 breaks compatibility with JRE8: need to cast
      // https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
      ((Buffer) buffer).clear();
    }

    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
    for (DataPoint dp : dataPoints) {
      doubleBuffer.put(dp.getMZ());
      doubleBuffer.put(dp.getIntensity());
    }

    storageFile.seek(currentOffset);
    storageFile.write(buffer.array(), 0, numOfBytes);

    dataPointsOffsets.put(currentID, currentOffset);
    dataPointsLengths.put(currentID, numOfDataPoints);

    return currentID;
  }

  public synchronized DataPoint[] readDataPoints(int ID) throws IOException {

    final Long currentOffset = dataPointsOffsets.get(ID);
    final Integer numOfDataPoints = dataPointsLengths.get(ID);

    if ((currentOffset == null) || (numOfDataPoints == null)) {
      throw new IllegalArgumentException("Unknown storage ID " + ID);
    }

    final int numOfBytes = numOfDataPoints * 2 * 8;

    if (buffer.capacity() < numOfBytes) {
      buffer = ByteBuffer.allocate(numOfBytes * 2);
    } else {
      // JDK 9 breaks compatibility with JRE8: need to cast
      // https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
      ((Buffer) buffer).clear();
    }

    storageFile.seek(currentOffset);
    storageFile.read(buffer.array(), 0, numOfBytes);

    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();

    DataPoint dataPoints[] = new DataPoint[numOfDataPoints];

    for (int i = 0; i < numOfDataPoints; i++) {
      double mz = doubleBuffer.get();
      double intensity = doubleBuffer.get();
      dataPoints[i] = new SimpleDataPoint(mz, intensity);
    }

    return dataPoints;

  }

  public synchronized void removeStoredDataPoints(int ID) throws IOException {
    dataPointsOffsets.remove(ID);
    dataPointsLengths.remove(ID);
  }

  @Override
  public synchronized void addScan(Scan newScan) throws IOException {

    // When we are loading the project, scan data file is already prepare
    // and we just need store the reference
    scans.put(newScan.getScanNumber(), newScan);

  }


  @Override
  @Nonnull
  public Range<Double> getDataMZRange() {
    return getDataMZRange(0);
  }

  @Override
  @Nonnull
  public Range<Double> getDataMZRange(int msLevel) {

    // check if we have this value already cached
    Range<Double> mzRange = dataMZRange.get(msLevel);
    if (mzRange != null) {
      return mzRange;
    }

    // find the value
    for (Scan scan : scans.values()) {

      // ignore scans of other ms levels
      if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) {
        continue;
      }

      if (mzRange == null) {
        mzRange = scan.getDataPointMZRange();
      } else {
        mzRange = mzRange.span(scan.getDataPointMZRange());
      }

    }

    // cache the value, if we found any
    if (mzRange != null) {
      dataMZRange.put(msLevel, mzRange);
    } else {
      mzRange = Range.singleton(0.0);
    }

    return mzRange;

  }

  @Override
  @Nonnull
  public Range<Float> getDataRTRange() {
    return getDataRTRange(0);
  }

  // @Nonnull
  // @Override
  // public Range<Double> getDataMobilityRange() {
  // return null;
  // }

  @Nonnull
  @Override
  public Range<Float> getDataRTRange(Integer msLevel) {
    if (msLevel == null) {
      return getDataRTRange();
    }
    // check if we have this value already cached
    Range<Float> rtRange = dataRTRange.get(msLevel);
    if (rtRange != null) {
      return rtRange;
    }

    // find the value
    for (Scan scan : scans.values()) {

      // ignore scans of other ms levels
      if ((msLevel != 0) && (scan.getMSLevel() != msLevel)) {
        continue;
      }

      if (rtRange == null) {
        rtRange = Range.singleton(scan.getRetentionTime());
      } else {
        rtRange = rtRange.span(Range.singleton(scan.getRetentionTime()));
      }

    }

    // cache the value
    if (rtRange != null) {
      dataRTRange.put(msLevel, rtRange);
    } else {
      rtRange = Range.singleton(0.0f);
    }

    return rtRange;
  }

  // @Nonnull
  // @Override
  // public Range<Double> getDataMobilityRange(int msLevel) {
  // return mobilityRange;
  // }
  //
  // @Nonnull
  // @Override
  // public MobilityType getMobilityType() {
  // return mobilityType;
  // }

  @Override
  public void setRTRange(int msLevel, Range<Float> rtRange) {
    dataRTRange.put(msLevel, rtRange);
  }

  @Override
  public void setMZRange(int msLevel, Range<Double> mzRange) {
    dataMZRange.put(msLevel, mzRange);
  }

  @Override
  public int getNumOfScans(int msLevel) {
    return getScanNumbers(msLevel).length;
  }

  public synchronized TreeMap<Integer, Long> getDataPointsOffsets() {
    return dataPointsOffsets;
  }

  public synchronized TreeMap<Integer, Integer> getDataPointsLengths() {
    return dataPointsLengths;
  }

  @Override
  public List<PolarityType> getDataPolarity() {
    Enumeration<Scan> scansEnum = scans.elements();
    // create an enum set to store different polarity types encountered within the file
    EnumSet<PolarityType> polarityTypes = EnumSet.noneOf(PolarityType.class);
    while (scansEnum.hasMoreElements()) {
      Scan scan = scansEnum.nextElement();
      polarityTypes.add(scan.getPolarity());
    }
    // return as list
    return polarityTypes.stream().collect(Collectors.toList());
  }

  @Override
  public java.awt.Color getColorAWT() {
    return FxColorUtil.fxColorToAWT(color.getValue());
  }

  @Override
  public javafx.scene.paint.Color getColor() {
    return color.getValue();
  }

  @Override
  public void setColor(javafx.scene.paint.Color color) {
    this.color.setValue(color);
  }

  @Override
  public ObjectProperty<javafx.scene.paint.Color> colorProperty() {
    return color;
  }

  @Override
  public synchronized void close() {
    try {
      if (storageFileName != null) {
        storageFile.close();
        storageFileName.delete();
      }
    } catch (IOException e) {
      logger.warning("Could not close file " + storageFileName + ": " + e.toString());
    }
  }

  @Override
  @Nonnull
  public String getName() {
    return dataFileName;
  }

  @Override
  public void setName(@Nonnull String name) {
    this.dataFileName = name;
  }

  @Override
  public String toString() {
    return dataFileName;
  }


  // TODO make sure that equals and hashCode() works
}
