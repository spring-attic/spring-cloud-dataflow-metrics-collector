/*
 * Copyright 2018 the original author or authors.
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

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.lang.Nullable;

/**
 * Immutable class that wraps the micrometer's {@link HistogramSnapshot}.
 *
 * @param <T> the value of type {@link Number}
 *
 * @author Oleg Zhurakousky
 * @author Christian Tzolov
 */
@JsonPropertyOrder({ "id", "timestamp", "sum", "count", "mean", "upper", "total" })
public class MicrometerMetric<T extends Number> {

	private Date timestamp;

	private Id id;

	private Number sum = 0d;

	private Number count = 0d;

	private Number mean = 0d;

	private Number upper = 0d;

	private Number total = 0d;

	public static class Tag {
		private String key;

		private String value;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class Id {
		private String name;

		private List<Tag> tags;

		private Meter.Type type;

		@Nullable
		private String description;

		@Nullable
		private String baseUnit;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Tag> getTags() {
			return tags;
		}

		public void setTags(List<Tag> tags) {
			this.tags = tags;
		}

		public Meter.Type getType() {
			return type;
		}

		public void setType(Meter.Type type) {
			this.type = type;
		}

		public String getDescription() {
			return description;
		}

		public String getBaseUnit() {
			return baseUnit;
		}

		public void setBaseUnit(String baseUnit) {
			this.baseUnit = baseUnit;
		}

		public String getTag(String key) {
			for (Tag tag : tags) {
				if (tag.getKey().equals(key))
					return tag.getValue();
			}
			return null;
		}
	}

	public Id getId() {
		return this.id;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	public Date getTimestamp() {
		return this.timestamp;
	}

	public Number getSum() {
		return sum;
	}

	public Number getCount() {
		return count;
	}

	public Number getMean() {
		return mean;
	}

	public Number getUpper() {
		return upper;
	}

	public Number getTotal() {
		return total;
	}


	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public void setSum(Number sum) {
		this.sum = sum;
	}

	public void setCount(Number count) {
		this.count = count;
	}

	public void setMean(Number mean) {
		this.mean = mean;
	}

	public void setUpper(Number upper) {
		this.upper = upper;
	}

	public void setTotal(Number total) {
		this.total = total;
	}

	@Override
	public String toString() {
		return "MicrometerMetric [id=" + this.id +
				", sum=" + this.sum +
				", count=" + this.count +
				", mean=" + this.mean +
				", upper=" + this.upper +
				", total=" + this.total +
				", timestamp=" + this.timestamp + "]";
	}
}
