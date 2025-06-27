/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.client;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.Utils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Representation of features and capabilities for Model Context Protocol (MCP) clients.
 * This class provides two record types for managing client features:
 * <ul>
 * <li>{@link Async} for non-blocking operations with Project Reactor's Mono responses
 * <li>{@link Sync} for blocking operations with direct responses
 * </ul>
 *
 * <p>
 * Each feature specification includes:
 * <ul>
 * <li>Client implementation information and capabilities
 * <li>Root URI mappings for resource access
 * <li>Change notification handlers for tools, resources, and prompts
 * <li>Logging message consumers
 * <li>Message sampling handlers for request processing
 * </ul>
 *
 * <p>
 * The class supports conversion between synchronous and asynchronous specifications
 * through the {@link Async#fromSync} method, which ensures proper handling of blocking
 * operations in non-blocking contexts by scheduling them on a bounded elastic scheduler.
 *
 * @author Dariusz Jędrzejczyk
 * @see McpClient
 * @see McpSchema.Implementation
 * @see McpSchema.ClientCapabilities
 */
class McpClientFeatures {

	public static class Async {
		private final McpSchema.Implementation clientInfo;
		private final McpSchema.ClientCapabilities clientCapabilities;
		private final Map<String, McpSchema.Root> roots;
		private final List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers;
		private final List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers;
		private final List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers;
		private final List<Function<McpSchema.LoggingMessageNotification, Mono<Void>>> loggingConsumers;
		private final Function<McpSchema.CreateMessageRequest, Mono<McpSchema.CreateMessageResult>> samplingHandler;

		public Async(McpSchema.Implementation clientInfo, McpSchema.ClientCapabilities clientCapabilities,
				Map<String, McpSchema.Root> roots,
				List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers,
				List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers,
				List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers,
				List<Function<McpSchema.LoggingMessageNotification, Mono<Void>>> loggingConsumers,
				Function<McpSchema.CreateMessageRequest, Mono<McpSchema.CreateMessageResult>> samplingHandler) {

			Assert.notNull(clientInfo, "Client info must not be null");
			this.clientInfo = clientInfo;
			McpSchema.ClientCapabilities caps = McpSchema.ClientCapabilities.builder().build();
			caps.setExperimental(null);
			if (!Utils.isEmpty(roots)) {
				McpSchema.ClientCapabilities.RootCapabilities rootCaps = new McpSchema.ClientCapabilities.RootCapabilities();
				rootCaps.setListChanged(false);
				caps.setRoots(rootCaps);
			}
			if (samplingHandler != null) {
				caps.setSampling(new McpSchema.ClientCapabilities.Sampling());
			}
			this.clientCapabilities = (clientCapabilities != null) ? clientCapabilities : caps;
			this.roots = roots != null ? new ConcurrentHashMap<>(roots) : new ConcurrentHashMap<>();

			this.toolsChangeConsumers = toolsChangeConsumers != null ? toolsChangeConsumers : new ArrayList<>();
			this.resourcesChangeConsumers = resourcesChangeConsumers != null ? resourcesChangeConsumers : new ArrayList<>();
			this.promptsChangeConsumers = promptsChangeConsumers != null ? promptsChangeConsumers : new ArrayList<>();
			this.loggingConsumers = loggingConsumers != null ? loggingConsumers : new ArrayList<>();
			this.samplingHandler = samplingHandler;
		}

		public McpSchema.Implementation getClientInfo() {
			return clientInfo;
		}

		public McpSchema.ClientCapabilities getClientCapabilities() {
			return clientCapabilities;
		}

		public Map<String, McpSchema.Root> getRoots() {
			return roots;
		}

		public List<Function<List<McpSchema.Tool>, Mono<Void>>> getToolsChangeConsumers() {
			return toolsChangeConsumers;
		}

		public List<Function<List<McpSchema.Resource>, Mono<Void>>> getResourcesChangeConsumers() {
			return resourcesChangeConsumers;
		}

		public List<Function<List<McpSchema.Prompt>, Mono<Void>>> getPromptsChangeConsumers() {
			return promptsChangeConsumers;
		}

		public List<Function<McpSchema.LoggingMessageNotification, Mono<Void>>> getLoggingConsumers() {
			return loggingConsumers;
		}

		public Function<McpSchema.CreateMessageRequest, Mono<McpSchema.CreateMessageResult>> getSamplingHandler() {
			return samplingHandler;
		}

		public static Async fromSync(Sync syncSpec) {
			List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers = new ArrayList<>();
			for (Consumer<List<McpSchema.Tool>> consumer : syncSpec.getToolsChangeConsumers()) {
				toolsChangeConsumers.add(t -> Mono.<Void>fromRunnable(() -> consumer.accept(t))
					.subscribeOn(Schedulers.boundedElastic()));
			}

			List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers = new ArrayList<>();
			for (Consumer<List<McpSchema.Resource>> consumer : syncSpec.getResourcesChangeConsumers()) {
				resourcesChangeConsumers.add(r -> Mono.<Void>fromRunnable(() -> consumer.accept(r))
					.subscribeOn(Schedulers.boundedElastic()));
			}

			List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers = new ArrayList<>();
			for (Consumer<List<McpSchema.Prompt>> consumer : syncSpec.getPromptsChangeConsumers()) {
				promptsChangeConsumers.add(p -> Mono.<Void>fromRunnable(() -> consumer.accept(p))
					.subscribeOn(Schedulers.boundedElastic()));
			}

			List<Function<McpSchema.LoggingMessageNotification, Mono<Void>>> loggingConsumers = new ArrayList<>();
			for (Consumer<McpSchema.LoggingMessageNotification> consumer : syncSpec.getLoggingConsumers()) {
				loggingConsumers.add(l -> Mono.<Void>fromRunnable(() -> consumer.accept(l))
					.subscribeOn(Schedulers.boundedElastic()));
			}

			Function<McpSchema.CreateMessageRequest, Mono<McpSchema.CreateMessageResult>> samplingHandler = r -> Mono
				.fromCallable(() -> syncSpec.getSamplingHandler().apply(r))
				.subscribeOn(Schedulers.boundedElastic());

			return new Async(syncSpec.getClientInfo(), syncSpec.getClientCapabilities(), syncSpec.getRoots(),
					toolsChangeConsumers, resourcesChangeConsumers, promptsChangeConsumers, loggingConsumers,
					samplingHandler);
		}
	}

	public static class Sync {
		private final McpSchema.Implementation clientInfo;
		private final McpSchema.ClientCapabilities clientCapabilities;
		private final Map<String, McpSchema.Root> roots;
		private final List<Consumer<List<McpSchema.Tool>>> toolsChangeConsumers;
		private final List<Consumer<List<McpSchema.Resource>>> resourcesChangeConsumers;
		private final List<Consumer<List<McpSchema.Prompt>>> promptsChangeConsumers;
		private final List<Consumer<McpSchema.LoggingMessageNotification>> loggingConsumers;
		private final Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler;

		public Sync(McpSchema.Implementation clientInfo, McpSchema.ClientCapabilities clientCapabilities,
				Map<String, McpSchema.Root> roots, List<Consumer<List<McpSchema.Tool>>> toolsChangeConsumers,
				List<Consumer<List<McpSchema.Resource>>> resourcesChangeConsumers,
				List<Consumer<List<McpSchema.Prompt>>> promptsChangeConsumers,
				List<Consumer<McpSchema.LoggingMessageNotification>> loggingConsumers,
				Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler) {

			Assert.notNull(clientInfo, "Client info must not be null");
			this.clientInfo = clientInfo;
			McpSchema.ClientCapabilities caps = McpSchema.ClientCapabilities.builder().build();
			caps.setExperimental(null);
			if (!Utils.isEmpty(roots)) {
				McpSchema.ClientCapabilities.RootCapabilities rootCaps = new McpSchema.ClientCapabilities.RootCapabilities();
				rootCaps.setListChanged(false);
				caps.setRoots(rootCaps);
			}
			if (samplingHandler != null) {
				caps.setSampling(new McpSchema.ClientCapabilities.Sampling());
			}
			this.clientCapabilities = (clientCapabilities != null) ? clientCapabilities : caps;
			this.roots = roots != null ? new HashMap<>(roots) : new HashMap<>();

			this.toolsChangeConsumers = toolsChangeConsumers != null ? toolsChangeConsumers : new ArrayList<>();
			this.resourcesChangeConsumers = resourcesChangeConsumers != null ? resourcesChangeConsumers : new ArrayList<>();
			this.promptsChangeConsumers = promptsChangeConsumers != null ? promptsChangeConsumers : new ArrayList<>();
			this.loggingConsumers = loggingConsumers != null ? loggingConsumers : new ArrayList<>();
			this.samplingHandler = samplingHandler;
		}

		public McpSchema.Implementation getClientInfo() {
			return clientInfo;
		}

		public McpSchema.ClientCapabilities getClientCapabilities() {
			return clientCapabilities;
		}

		public Map<String, McpSchema.Root> getRoots() {
			return roots;
		}

		public List<Consumer<List<McpSchema.Tool>>> getToolsChangeConsumers() {
			return toolsChangeConsumers;
		}

		public List<Consumer<List<McpSchema.Resource>>> getResourcesChangeConsumers() {
			return resourcesChangeConsumers;
		}

		public List<Consumer<List<McpSchema.Prompt>>> getPromptsChangeConsumers() {
			return promptsChangeConsumers;
		}

		public List<Consumer<McpSchema.LoggingMessageNotification>> getLoggingConsumers() {
			return loggingConsumers;
		}

		public Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> getSamplingHandler() {
			return samplingHandler;
		}
	}

}
