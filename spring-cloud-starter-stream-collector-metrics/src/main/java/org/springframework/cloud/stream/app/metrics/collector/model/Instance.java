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

package org.springframework.cloud.stream.app.metrics.collector.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.springframework.cloud.stream.app.metrics.collector.utils.DeltaNumberHolder;


/**
 * @author Vinicius Carvalho
 */
public class Instance {

	private String guid;
	private Integer index;
	private Map<String,Object> properties;
	private DeltaNumberHolder incomingRate;
	private DeltaNumberHolder outgoingRate;

	@JsonCreator
	public Instance(String guid, Double incomeRate, Double outgoingRate) {
		this.guid = guid;
		this.incomingRate = new DeltaNumberHolder(incomeRate);
		this.outgoingRate = new DeltaNumberHolder(outgoingRate);
	}

	public Instance(String guid){
		this(guid,0.0,0.0);
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

	public Double getIncomingRate() {
		return incomingRate.getCurrentValue();
	}


	public Double getOutgoingRate() {
		return outgoingRate.getCurrentValue();
	}

	public void updateOutgoingRate(Double value){
		this.outgoingRate.update(value);
	}
	public void updateIncomingRate(Double value){
		this.incomingRate.update(value);
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
