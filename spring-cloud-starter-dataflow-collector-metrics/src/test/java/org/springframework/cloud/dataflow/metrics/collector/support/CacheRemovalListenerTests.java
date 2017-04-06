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

package org.springframework.cloud.dataflow.metrics.collector.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.dataflow.metrics.collector.BaseCacheTests;
import org.springframework.cloud.dataflow.metrics.collector.model.Application;
import org.springframework.cloud.dataflow.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.dataflow.metrics.collector.model.Instance;
import org.springframework.cloud.dataflow.metrics.collector.model.Stream;


/**
 * @author Vinicius Carvalho
 */
public class CacheRemovalListenerTests extends BaseCacheTests {

	@Test
	public void testRemoval() throws Exception{
		FakeTicker ticker = getTicker();
		Cache<String,Stream> streamCache = Caffeine.<String,Stream>newBuilder()
				.build();
		CacheRemovalListener removalListener = new CacheRemovalListener(streamCache);
		Cache<String,ApplicationMetrics> rawCache = Caffeine.newBuilder()
				.expireAfterAccess(30,TimeUnit.SECONDS)
				.ticker(ticker)
				.removalListener(removalListener)
				.build();

		ApplicationMetrics metrics = new ApplicationMetrics("httpIngest.http.foo", Collections.emptyList());
		Map<String,Object> properties = new HashMap<>();
		properties.put(ApplicationMetrics.STREAM_NAME,"httpIngest");
		properties.put(ApplicationMetrics.APPLICATION_NAME,"http");
		properties.put(ApplicationMetrics.APPLICATION_GUID,"bar");
		metrics.setProperties(properties);

		rawCache.put(metrics.getName(),metrics);

		Stream stream = new Stream("httpIngest");
		Application application = new Application("http");
		Instance i1 = new Instance("foo",0.0,0.0);
		Instance i2 = new Instance("bar",0.0,0.0);
		application.getInstances().add(i1);
		application.getInstances().add(i2);
		stream.getApplications().add(application);

		streamCache.put(stream.getName(),stream);

		ticker.advance(31,TimeUnit.MINUTES);
		rawCache.getIfPresent(metrics.getName());

		Thread.sleep(1000);
		Assert.assertEquals(1,streamCache.getIfPresent("httpIngest").getApplications().get(0).getInstances().size());

	}

}
