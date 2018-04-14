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

package org.springframework.cloud.dataflow.metrics.collector;

import java.util.Date;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Test;


import org.springframework.cloud.dataflow.metrics.collector.model.Metric;
import org.springframework.cloud.dataflow.metrics.collector.support.MetricJsonSerializer;

/**
 * @author Vinicius Carvalho
 */
public class MetricJsonSerializerTests {

	@Test
	public void sprechenSieDeutsch() throws Exception {
		Locale.setDefault(Locale.GERMANY);
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Metric.class,new MetricJsonSerializer.Deserializer());
		module.addSerializer(Metric.class,new MetricJsonSerializer.Serializer());
		mapper.registerModule(module);
		Metric sample = new Metric("sample",3.14159, new Date());
		String json = mapper.writeValueAsString(sample);
		Metric m = mapper.readValue(json,Metric.class);
		Assert.assertEquals(3.14, m.getValue().doubleValue(),0.0);
	}
}
