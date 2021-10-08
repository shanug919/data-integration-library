// Copyright 2021 LinkedIn Corporation. All rights reserved.
// Licensed under the BSD-2 Clause license.
// See LICENSE in the project root for license information.

package com.linkedin.cdi.keys;

import com.google.common.collect.Lists;
import com.linkedin.cdi.configuration.MultistageProperties;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericRecord;
import org.apache.gobblin.configuration.State;
import org.apache.gobblin.source.workunit.WorkUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.cdi.configuration.MultistageProperties.*;


/**
 * These attributes are defined and maintained in AvroExtractor
 *
 * @author esong
 */
public class AvroExtractorKeys extends ExtractorKeys {
  public DataFileStream<GenericRecord> getAvroRecordIterator() {
    return avroRecordIterator;
  }

  public void setAvroRecordIterator(DataFileStream<GenericRecord> avroRecordIterator) {
    this.avroRecordIterator = avroRecordIterator;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public long getCurrentPageNumber() {
    return currentPageNumber;
  }

  public void setCurrentPageNumber(long currentPageNumber) {
    this.currentPageNumber = currentPageNumber;
  }

  public Schema getAvroOutputSchema() {
    return avroOutputSchema;
  }

  public void setAvroOutputSchema(Schema avroOutputSchema) {
    this.avroOutputSchema = avroOutputSchema;
  }

  public Boolean getIsValidOutputSchema() {
    return isValidOutputSchema;
  }

  public void setIsValidOutputSchema(Boolean validOutputSchema) {
    isValidOutputSchema = validOutputSchema;
  }

  public GenericRecord getSampleData() {
    return sampleData;
  }

  public void setSampleData(GenericRecord sampleData) {
    this.sampleData = sampleData;
  }

  private static final Logger LOG = LoggerFactory.getLogger(AvroExtractorKeys.class);
  final private static List<MultistageProperties> ESSENTIAL_PARAMETERS = Lists.newArrayList(
      MSTAGE_DATA_FIELD,
      MSTAGE_TOTAL_COUNT_FIELD);

  private DataFileStream<GenericRecord> avroRecordIterator = null;
  private long totalCount;
  // TODO: move this to ExtractorKeys if pagination is needed
  private long currentPageNumber = 0;
  private Schema avroOutputSchema = null;
  private Boolean isValidOutputSchema = true;
  private GenericRecord sampleData = null;

  public void incrCurrentPageNumber() {
    currentPageNumber++;
  }



  @Override
  public void logDebugAll(WorkUnit workUnit) {
    super.logDebugAll(workUnit);
    LOG.debug("These are values of JsonExtractor regarding to Work Unit: {}",
        workUnit == null ? "testing" : workUnit.getProp(DATASET_URN_KEY.toString()));
    LOG.debug("Total rows expected or processed: {}", totalCount);
  }

  @Override
  public void logUsage(State state) {
    super.logUsage(state);
    for (MultistageProperties p: ESSENTIAL_PARAMETERS) {
      LOG.info("Property {} ({}) has value {} ", p.toString(), p.getClassName(), p.getValidNonblankWithDefault(state));
    }
  }
}
