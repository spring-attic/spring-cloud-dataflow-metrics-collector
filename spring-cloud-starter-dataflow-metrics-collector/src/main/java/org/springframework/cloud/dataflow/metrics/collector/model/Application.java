/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author Vinicius Carvalho
 */
public class Application {

	private String name;

	private List<Instance> instances = new LinkedList<>();

	private Collection<Metric<Double>> aggregateMetrics = new LinkedList<>();

	@JsonCreator
	public Application(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Instance> getInstances() {
		return instances;
	}

	public void setInstances(List<Instance> instances) {
		this.instances = instances;
	}

	public Collection<Metric<Double>> getAggregateMetrics() {
		return getInstances().stream().map(instance -> instance.getMetrics()).flatMap(metrics -> metrics.stream())
				.filter(metric -> metric.getName().matches("integration\\.channel\\.(\\w*)\\.send\\.mean"))
				.collect(Collectors.groupingBy(Metric::getName, Collectors.summingDouble(Metric::getValue))).entrySet()
				.stream().map(entry -> new Metric<Double>(entry.getKey(), entry.getValue(), new Date()))
				.collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Application that = (Application) o;

		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
