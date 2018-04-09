/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.metrics.collector.model;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Vinicius Carvalho
 * @author Oleg Zhurakousky
 * @author Christian Tzolov
 */
@JsonPropertyOrder({ "name", "createdTime", "properties", "metrics" })
public class ApplicationMetrics<T> {

	public static final String STREAM_NAME = "spring.cloud.dataflow.stream.name";
	public static final String APPLICATION_NAME = "spring.cloud.dataflow.stream.app.label";
	public static final String SERVER_NAME = "spring.cloud.dataflow.server.name";
	public static final String APPLICATION_TYPE = "spring.cloud.dataflow.stream.app.type";
	public static final String APPLICATION_GUID = "spring.cloud.application.guid";
	public static final String INSTANCE_INDEX = "spring.application.index";
	public static final String APPLICATION_METRICS_JSON = "application/vnd.spring.cloud.stream.metrics.v1+json";
	public static final String STREAM_METRICS_VERSION = "spring.cloud.dataflow.stream.metrics.version";

	public static final String METRICS_VERSION_1 = "1.0";
	public static final String METRICS_VERSION_2 = "2.0";


	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Date createdTime;

	private String name;

	private Collection<T> metrics;

	private long interval = 1000; //[ms]

	private Map<String, Object> properties;

	@JsonCreator
	public ApplicationMetrics(@JsonProperty("name") String name, @JsonProperty("metrics") Collection<T> metrics) {
		this.name = name;
		this.metrics = metrics;
		this.createdTime = new Date();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<T> getMetrics() {
		return metrics;
	}

	public void setMetrics(Collection<T> metrics) {
		this.metrics = metrics;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ApplicationMetrics that = (ApplicationMetrics) o;

		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
