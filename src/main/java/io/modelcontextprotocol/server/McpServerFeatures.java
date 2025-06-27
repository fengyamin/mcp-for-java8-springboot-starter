/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.Utils;
import lombok.Data;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * MCP server features specification that a particular server can choose to support.
 *
 * @author Dariusz Jędrzejczyk
 * @author Jihoon Kim
 */
public class McpServerFeatures {

	/**
	 * Asynchronous server features specification.
	 */
	@Data
	public static class Async {
		private final McpSchema.Implementation serverInfo;
		private final McpSchema.ServerCapabilities serverCapabilities;
		private final List<AsyncToolSpecification> tools;
		private final Map<String, AsyncResourceSpecification> resources;
		private final List<McpSchema.ResourceTemplate> resourceTemplates;
		private final Map<String, AsyncPromptSpecification> prompts;
		private final Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions;
		private final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers;
		private final String instructions;

		public Async(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
				List<AsyncToolSpecification> tools, Map<String, AsyncResourceSpecification> resources,
				List<McpSchema.ResourceTemplate> resourceTemplates,
				Map<String, AsyncPromptSpecification> prompts,
				Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions,
				List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
				String instructions) {

			Assert.notNull(serverInfo, "Server info must not be null");

			this.serverInfo = serverInfo;
			this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
					:  new McpSchema.ServerCapabilities(
							null,
							null,
							new McpSchema.ServerCapabilities.LoggingCapabilities(),
							!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
							!Utils.isEmpty(resources) ? new McpSchema.ServerCapabilities.ResourceCapabilities(false,false) : null,
							!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

			this.tools = (tools != null) ? tools : Collections.emptyList();
			this.resources = (resources != null) ? resources : Collections.emptyMap();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : Collections.emptyList();
			this.prompts = (prompts != null) ? prompts : Collections.emptyMap();
			this.completions = (completions != null) ? completions : Collections.emptyMap();
			this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : Collections.emptyList();
			this.instructions = instructions;
		}

		public static Async fromSync(Sync syncSpec) {
			List<AsyncToolSpecification> tools = new ArrayList<>();
			for (SyncToolSpecification tool : syncSpec.getTools()) {
				tools.add(AsyncToolSpecification.fromSync(tool));
			}

			Map<String, AsyncResourceSpecification> resources = new HashMap<>();
			syncSpec.getResources().forEach((key, resource) -> {
				resources.put(key, AsyncResourceSpecification.fromSync(resource));
			});

			Map<String, AsyncPromptSpecification> prompts = new HashMap<>();
			syncSpec.getPrompts().forEach((key, prompt) -> {
				prompts.put(key, AsyncPromptSpecification.fromSync(prompt));
			});

			Map<McpSchema.CompleteReference, AsyncCompletionSpecification> completions = new HashMap<>();
			syncSpec.getCompletions().forEach((key, completion) -> {
				completions.put(key, AsyncCompletionSpecification.fromSync(completion));
			});

			List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers = new ArrayList<>();

			for (BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> rootChangeConsumer : syncSpec.getRootsChangeConsumers()) {
				rootChangeConsumers.add((exchange, list) -> Mono
					.<Void>fromRunnable(() -> rootChangeConsumer.accept(new McpSyncServerExchange(exchange), list))
					.subscribeOn(Schedulers.boundedElastic()));
			}

			return new Async(syncSpec.getServerInfo(), syncSpec.getServerCapabilities(), tools, resources,
					syncSpec.getResourceTemplates(), prompts, completions, rootChangeConsumers, syncSpec.getInstructions());
		}
	}

	/**
	 * Synchronous server features specification.
	 */
	@Data
	public static class Sync {
		private final McpSchema.Implementation serverInfo;
		private final McpSchema.ServerCapabilities serverCapabilities;
		private final List<SyncToolSpecification> tools;
		private final Map<String, SyncResourceSpecification> resources;
		private final List<McpSchema.ResourceTemplate> resourceTemplates;
		private final Map<String, SyncPromptSpecification> prompts;
		private final Map<McpSchema.CompleteReference, SyncCompletionSpecification> completions;
		private final List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers;
		private final String instructions;

		public Sync(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
				List<SyncToolSpecification> tools,
				Map<String, SyncResourceSpecification> resources,
				List<McpSchema.ResourceTemplate> resourceTemplates,
				Map<String, SyncPromptSpecification> prompts,
				Map<McpSchema.CompleteReference, SyncCompletionSpecification> completions,
				List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumers,
				String instructions) {

			Assert.notNull(serverInfo, "Server info must not be null");

			this.serverInfo = serverInfo;
			this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
					: new McpSchema.ServerCapabilities(
					null,
					null,
					new McpSchema.ServerCapabilities.LoggingCapabilities(),
					!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
					!Utils.isEmpty(resources) ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
					!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

			this.tools = (tools != null) ? tools : new ArrayList<>();
			this.resources = (resources != null) ? resources : new HashMap<>();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : new ArrayList<>();
			this.prompts = (prompts != null) ? prompts : new HashMap<>();
			this.completions = (completions != null) ? completions : new HashMap<>();
			this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : new ArrayList<>();
			this.instructions = instructions;
		}
	}

	@Data
	public static class AsyncToolSpecification {
		private final McpSchema.Tool tool;
		private final BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call;

		public McpSchema.Tool tool() {
			return this.tool;
		}

		public BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call() {
			return this.call;
		}

		public static AsyncToolSpecification fromSync(SyncToolSpecification tool) {
			if (tool == null) {
				return null;
			}
			return new AsyncToolSpecification(tool.getTool(),
					(exchange, map) -> Mono
						.fromCallable(() -> tool.getCall().apply(new McpSyncServerExchange(exchange), map))
						.subscribeOn(Schedulers.boundedElastic()));
		}
	}

	@Data
	public static class AsyncResourceSpecification {
		private final McpSchema.Resource resource;
		private final BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler;

		public McpSchema.Resource resource() {
			return this.resource;
		}

		public BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler() {
			return this.readHandler;
		}

		public static AsyncResourceSpecification fromSync(SyncResourceSpecification resource) {
			if (resource == null) {
				return null;
			}
			return new AsyncResourceSpecification(resource.getResource(),
					(exchange, req) -> Mono
						.fromCallable(() -> resource.getReadHandler().apply(new McpSyncServerExchange(exchange), req))
						.subscribeOn(Schedulers.boundedElastic()));
		}
	}

	@Data
	public static class AsyncPromptSpecification {
		private final McpSchema.Prompt prompt;
		private final BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler;

		public McpSchema.Prompt prompt() {
			return this.prompt;
		}

		public BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler() {
			return this.promptHandler;
		}

		public static AsyncPromptSpecification fromSync(SyncPromptSpecification prompt) {
			if (prompt == null) {
				return null;
			}
			return new AsyncPromptSpecification(prompt.getPrompt(),
					(exchange, req) -> Mono
						.fromCallable(() -> prompt.getPromptHandler().apply(new McpSyncServerExchange(exchange), req))
						.subscribeOn(Schedulers.boundedElastic()));
		}
	}

	@Data
	public static class AsyncCompletionSpecification {
		private final McpSchema.CompleteReference referenceKey;
		private final BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> completionHandler;

		public McpSchema.CompleteReference referenceKey() {
			return this.referenceKey;
		}

		public BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> completionHandler() {
			return this.completionHandler;
		}

		public static AsyncCompletionSpecification fromSync(SyncCompletionSpecification completion) {
			if (completion == null) {
				return null;
			}
			return new AsyncCompletionSpecification(completion.getReferenceKey(),
					(exchange, request) -> Mono.fromCallable(
							() -> completion.getCompletionHandler().apply(new McpSyncServerExchange(exchange), request))
						.subscribeOn(Schedulers.boundedElastic()));
		}
	}

	@Data
	public static class SyncToolSpecification {
		private final McpSchema.Tool tool;
		private final BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call;
	}

	@Data
	public static class SyncResourceSpecification {
		private final McpSchema.Resource resource;
		private final BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler;
	}

	@Data
	public static class SyncPromptSpecification {
		private final McpSchema.Prompt prompt;
		private final BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler;
	}

	@Data
	public static class SyncCompletionSpecification {
		private final McpSchema.CompleteReference referenceKey;
		private final BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> completionHandler;
	}
}
