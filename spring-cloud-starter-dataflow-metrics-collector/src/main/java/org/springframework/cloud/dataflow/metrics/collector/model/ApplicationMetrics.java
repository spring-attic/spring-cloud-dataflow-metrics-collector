/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.boot.actuate.metrics.Metric;


/**
 * @author Vinicius Carvalho
 */
public class ApplicationMetrics {

	public static final String STREAM_NAME = "spring.cloud.dataflow.stream.name";
	public static final String APPLICATION_NAME = "spring.cloud.dataflow.stream.app.label";
	public static final String SERVER_NAME = "spring.cloud.dataflow.server.name";
	public static final String APPLICATION_TYPE = "spring.cloud.dataflow.stream.app.type";
	public static final String APPLICATION_GUID = "spring.cloud.application.guid";
	public static final String INSTANCE_INDEX = "spring.cloud.stream.instanceIndex";
	public static final String APPLICATION_METRICS_JSON = "application/vnd.spring.cloud.stream.metrics.v1+json";

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private final Date createdTime;

	private String name;

	private Collection<Metric<?>> metrics;

	private Map<String, Object> properties;

	@JsonCreator
	public ApplicationMetrics(@JsonProperty("name") String name,
			@JsonProperty("metrics") Collection<Metric<?>> metrics) {
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

	public Collection<Metric<?>> getMetrics() {
		return metrics;
	}

	public void setMetrics(Collection<Metric<?>> metrics) {
		this.metrics = metrics;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ApplicationMetrics that = (ApplicationMetrics) o;

		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
