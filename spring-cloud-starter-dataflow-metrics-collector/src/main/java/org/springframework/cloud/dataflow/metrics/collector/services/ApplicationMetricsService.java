package org.springframework.cloud.dataflow.metrics.collector.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cloud.dataflow.metrics.collector.model.Application;
import org.springframework.cloud.dataflow.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.dataflow.metrics.collector.model.Instance;
import org.springframework.cloud.dataflow.metrics.collector.model.StreamMetrics;
import org.springframework.cloud.dataflow.metrics.collector.utils.YANUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Vinicius Carvalho
 */
public class ApplicationMetricsService {

	private final Pattern pattern = Pattern.compile("integration\\.channel\\.(\\w*)\\.sendCount");

	private Lock rwLock = new ReentrantLock();

	private Cache<String, LinkedList<ApplicationMetrics>> storage;

	public ApplicationMetricsService(Cache<String, LinkedList<ApplicationMetrics>> storage) {
		this.storage = storage;
	}

	/**
	 * Appends an {@link ApplicationMetrics} to the underlying storage. Each key on the storage holds the last two readings in a LIFO list
	 * @param applicationMetrics
	 */
	public void add(ApplicationMetrics applicationMetrics) {
		try {
			this.rwLock.lock();
			LinkedList<ApplicationMetrics> values = this.storage.getIfPresent(applicationMetrics.getName());

			if (values == null) {
				values = new LinkedList<>();
				values.addFirst(applicationMetrics);
				storage.put(applicationMetrics.getName(), values);
			}
			else {
				values.addFirst(applicationMetrics);
				if (values.size() > 2) {
					values.removeLast();
				}

			}
		}
		finally {
			this.rwLock.unlock();
		}
	}

	/**
	 * Converts the plain model of {@link ApplicationMetrics} into a hierarchical representation of {@link StreamMetrics}
	 * @param filter Comma delimited list of stream names to be filtered on. If null or empty all streams are returned
	 * @return A collection of filtered {@link StreamMetrics}
	 */
	public Collection<StreamMetrics> toStreamMetrics(String filter) {
		Collection<StreamMetrics> entries = new LinkedList<>();
		Set<String> streamNames = null;
		try {
			this.rwLock.lock();
			if (StringUtils.isEmpty(filter)) {
				streamNames = storage.asMap().values().stream()
						.map(applicationMetrics -> String.valueOf(
								applicationMetrics.getFirst().getProperties().get(ApplicationMetrics.STREAM_NAME)))
						.collect(Collectors.toSet());
			}
			else {
				streamNames = StringUtils.commaDelimitedListToSet(filter);
			}

			for (String streamName : streamNames) {
				StreamMetrics streamMetrics = null;
				List<List<ApplicationMetrics>> filteredList = storage
						.asMap().values().stream().filter(applicationMetrics -> applicationMetrics.getFirst()
								.getProperties().get(ApplicationMetrics.STREAM_NAME).equals(streamName))
						.collect(Collectors.toList());
				for (List<ApplicationMetrics> applicationMetrics : filteredList) {
					streamMetrics = convert(applicationMetrics, streamMetrics);
				}
				if (streamMetrics != null) {
					entries.add(streamMetrics);
				}
			}
		}
		finally {
			this.rwLock.unlock();
		}
		return entries;
	}

	/**
	 * Converts a denormalized view of each application instance metric
	 * ({@link ApplicationMetrics}) into a hierarchical model {@link StreamMetrics}
	 * @param applicationMetricsList an LIFO list with the last two readings of an
	 * {@link ApplicationMetrics} event
	 * @param root The root object of the hierarchy - null if the first conversion
	 * @return a hierarchical view of metrics using {@link StreamMetrics} as the root
	 * object
	 */
	private StreamMetrics convert(List<ApplicationMetrics> applicationMetricsList, StreamMetrics root) {

		// For most properties, we should take the last inserted element on the list
		ApplicationMetrics applicationMetrics = applicationMetricsList.get(0);

		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME),
				"Missing STREAM_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME),
				"Missing APPLICATION_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID),
				"Missing APPLICATION_GUID from metrics properties");
		StreamMetrics streamMetrics = (root == null)
				? new StreamMetrics((String) applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME))
				: root;
		Application application = new Application(
				(String) applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME));

		Instance instance = new Instance(
				applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID).toString());

		if (applicationMetrics.getProperties().get(ApplicationMetrics.INSTANCE_INDEX) != null) {
			Integer instanceIndex = YANUtils
					.toInteger(applicationMetrics.getProperties().get(ApplicationMetrics.INSTANCE_INDEX));
			instance.setIndex(instanceIndex);
		}

		List<Metric<?>> instanceMetrics = new ArrayList<>(applicationMetrics.getMetrics().size());
		applicationMetrics.getMetrics().stream().forEach(metric -> {
			Metric copy = new Metric(metric.getName(),metric.getValue(),metric.getTimestamp());
			instanceMetrics.add(copy);
		});
		instance.setMetrics(instanceMetrics);

		instance.setProperties(applicationMetrics.getProperties());
		instance.setKey(applicationMetrics.getName());
		instance.getMetrics().addAll(computeRate(applicationMetricsList));

		int applicationIndex = streamMetrics.getApplications().indexOf(application);
		if (applicationIndex < 0) {
			application.getInstances().add(instance);
			streamMetrics.getApplications().add(application);
		}
		else {
			int idx = streamMetrics.getApplications().get(applicationIndex).getInstances().indexOf(instance);
			if (idx < 0) {
				streamMetrics.getApplications().get(applicationIndex).getInstances().add(instance);
			}
		}
		return streamMetrics;
	}

	private List<Metric<Double>> computeRate(List<ApplicationMetrics> applicationMetricsList) {
		List<Metric<Double>> result = new ArrayList<>();
		ApplicationMetrics applicationMetrics = applicationMetricsList.get(0);
		for (Metric<?> metric : applicationMetrics.getMetrics()) {
			Matcher matcher = pattern.matcher(metric.getName());
			if (matcher.matches()) {
				Metric previous = applicationMetricsList.size() < 2 ? null
						: findMetric(applicationMetricsList.get(1).getMetrics(), metric.getName());
				result.add(new Metric<Double>("integration.channel." + matcher.group(1) + ".send.mean",
						delta(metric, previous)));
			}
		}
		return result;
	}

	private Double delta(Metric<?> current, Metric<?> previous) {
		if (previous == null) {
			return 0.0;
		}
		else {
			return ((YANUtils.toDouble(current.getValue()) - YANUtils.toDouble(previous.getValue()))
					/ (current.getTimestamp().getTime() - previous.getTimestamp().getTime())) * 1000;
		}
	}

	private Metric<?> findMetric(Collection<Metric<?>> metrics, String name) {
		Metric<?> result = null;
		Optional<Metric<?>> optinal = metrics.stream().filter(metric -> metric.getName().equals(name)).findFirst();
		if (optinal.isPresent()) {
			result = optinal.get();
		}
		else {
			result = new Metric<Double>(name, 0.0);
		}
		return result;
	}
}
