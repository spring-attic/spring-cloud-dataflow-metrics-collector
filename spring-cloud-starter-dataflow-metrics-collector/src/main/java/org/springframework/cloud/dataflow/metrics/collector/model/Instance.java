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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.springframework.boot.actuate.metrics.Metric;

/**
 * @author Vinicius Carvalho
 */
public class Instance {

	private String guid;

	private Integer index;

	private String key;

	private Map<String,Object> properties;

	private Collection<Metric<Double>> metrics;

	@JsonCreator
	public Instance(String guid) {
		this.guid = guid;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}


	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}


	public Collection<Metric<Double>> getMetrics() {
		return metrics;
	}

	public void setMetrics(Collection<Metric<Double>> metrics) {
		this.metrics = metrics;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Instance instance = (Instance) o;

		return guid != null ? guid.equals(instance.guid) : instance.guid == null;
	}

	@Override
	public int hashCode() {
		return guid != null ? guid.hashCode() : 0;
	}
}
