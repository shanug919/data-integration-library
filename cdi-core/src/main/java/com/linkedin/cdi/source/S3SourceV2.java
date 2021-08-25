// Copyright 2021 LinkedIn Corporation. All rights reserved.
// Licensed under the BSD-2 Clause license.
// See LICENSE in the project root for license information.

package com.linkedin.cdi.source;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.linkedin.cdi.configuration.MultistageProperties;
import com.linkedin.cdi.connection.S3Connection;
import com.linkedin.cdi.extractor.MultistageExtractor;
import com.linkedin.cdi.keys.S3Keys;
import com.linkedin.cdi.util.EndecoUtils;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.gobblin.configuration.State;
import org.apache.gobblin.configuration.WorkUnitState;
import org.apache.gobblin.source.extractor.Extractor;
import software.amazon.awssdk.regions.Region;


@Slf4j
public class S3SourceV2 extends MultistageSource<Schema, GenericRecord> {
  private static final String KEY_REGION = "region";
  private static final String KEY_CONNECTION_TIMEOUT = "connection_timeout";
  private static final HashSet<String> S3_REGIONS_SET =
      Region.regions().stream().map(region -> region.toString()).collect(Collectors.toCollection(HashSet::new));
  @Getter
  private S3Keys s3SourceV2Keys = new S3Keys();

  public S3SourceV2() {
    s3SourceV2Keys = new S3Keys();
    jobKeys = s3SourceV2Keys;
  }
  protected void initialize(State state) {
    super.initialize(state);
    s3SourceV2Keys.logUsage(state);
    HttpUrl url = HttpUrl.parse(MultistageProperties.MSTAGE_SOURCE_URI.getValidNonblankWithDefault(state));
    if (url == null || url.host().isEmpty()) {
      throw new RuntimeException("Incorrect configuration in " +
          MultistageProperties.MSTAGE_SOURCE_URI.toString());
    }

    // set region, note that aws SDK won't raise an error here if region is invalid,
    // later on, an exception will be raised when the actual request is issued
    JsonObject parameters = MultistageProperties.MSTAGE_SOURCE_S3_PARAMETERS.getValidNonblankWithDefault(state);
    if (parameters.has(KEY_REGION)) {
      String region = parameters.get(KEY_REGION).getAsString();
      if (!S3_REGIONS_SET.contains(region)) {
        throw new IllegalArgumentException(region + " is not a valid S3 region.");
      }
      s3SourceV2Keys.setRegion(Region.of(region));
    } else {
      // Default to us-west-2
      s3SourceV2Keys.setRegion(Region.US_WEST_2);
    }

    // set S3 connection timeout, non-positive integers are rejected
    if (parameters.has(KEY_CONNECTION_TIMEOUT)) {
      int connectionTimeout = parameters.get(KEY_CONNECTION_TIMEOUT).getAsInt();
      if (connectionTimeout <= 0) {
        throw new IllegalArgumentException(connectionTimeout + " is not a valid timeout value.");
      }
      s3SourceV2Keys.setConnectionTimeout(connectionTimeout);
    }

    // separate the endpoint, which should be a URL without bucket name, from the domain name
    s3SourceV2Keys.setEndpoint("https://" + getEndpointFromHost(url.host()));

    // URL path might have variables, by default HttpUrl will encode '{' and '}'
    // Here we decode those back to their plain form
    s3SourceV2Keys.setPrefix(EndecoUtils.decode(url.encodedPath().substring(1)));

    // separate the bucket name from URI domain name
    s3SourceV2Keys.setBucket(url.host().split("\\.")[0]);

    s3SourceV2Keys.setFilesPattern(MultistageProperties.MSTAGE_SOURCE_FILES_PATTERN.getProp(state));
    s3SourceV2Keys.setMaxKeys(MultistageProperties.MSTAGE_S3_LIST_MAX_KEYS.getValidNonblankWithDefault(state));
    s3SourceV2Keys.setAccessKey(MultistageProperties.SOURCE_CONN_USERNAME.getValidNonblankWithDefault(state));
    s3SourceV2Keys.setSecretId(MultistageProperties.SOURCE_CONN_PASSWORD.getValidNonblankWithDefault(state));
    s3SourceV2Keys.setTargetFilePattern(
        MultistageProperties.MSTAGE_EXTRACTOR_TARGET_FILE_NAME.getValidNonblankWithDefault(state));
    s3SourceV2Keys.logDebugAll();
  }

  /**
   * Create extractor based on the input WorkUnitState, the extractor.class
   * configuration, and a new S3Connection
   *
   * @param state WorkUnitState passed in from Gobblin framework
   * @return the MultistageExtractor object
   */

  @Override
  public Extractor<Schema, GenericRecord> getExtractor(WorkUnitState state) {
    initialize(state);
    MultistageExtractor<Schema, GenericRecord> extractor =
        (MultistageExtractor<Schema, GenericRecord>) super.getExtractor(state);
    extractor.setConnection(new S3Connection(state, this.s3SourceV2Keys, extractor.getExtractorKeys()));
    return extractor;
  }

  /**
   * split the host name, and remove the bucket name from the beginning, and return the rest
   * @param host hostname with bucket name in the beginning
   * @return the endpoint name without the bucket name
   */
  private String getEndpointFromHost(String host) {
    List<String> segments = Lists.newArrayList(host.split("\\."));
    Preconditions.checkArgument(segments.size() > 1, "Host name format is incorrect");
    segments.remove(0);
    return Joiner.on(".").join(segments);
  }
}
