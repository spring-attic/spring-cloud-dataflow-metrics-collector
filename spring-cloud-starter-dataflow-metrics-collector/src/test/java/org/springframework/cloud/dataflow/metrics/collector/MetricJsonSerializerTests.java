package org.springframework.cloud.dataflow.metrics.collector;

import java.util.Date;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
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
