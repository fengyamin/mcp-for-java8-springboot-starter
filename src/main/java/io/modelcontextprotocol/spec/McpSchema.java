/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.util.Assert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Based on the <a href="http://www.jsonrpc.org/specification">JSON-RPC 2.0
 * specification</a> and the <a href=
 * "https://github.com/modelcontextprotocol/specification/blob/main/schema/2024-11-05/schema.ts">Model
 * Context Protocol Schema</a>.
 *
 * @author Christian Tzolov
 */
public final class McpSchema {

	private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

	private McpSchema() {
	}

	public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

	public static final String JSONRPC_VERSION = "2.0";

	// ---------------------------
	// Method Names
	// ---------------------------

	// Lifecycle Methods
	public static final String METHOD_INITIALIZE = "initialize";

	public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

	public static final String METHOD_PING = "ping";

	// Tool Methods
	public static final String METHOD_TOOLS_LIST = "tools/list";

	public static final String METHOD_TOOLS_CALL = "tools/call";

	public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

	// Resources Methods
	public static final String METHOD_RESOURCES_LIST = "resources/list";

	public static final String METHOD_RESOURCES_READ = "resources/read";

	public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

	public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

	public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

	public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

	// Prompt Methods
	public static final String METHOD_PROMPT_LIST = "prompts/list";

	public static final String METHOD_PROMPT_GET = "prompts/get";

	public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	public static final String METHOD_COMPLETION_COMPLETE = "completion/complete";

	// Logging Methods
	public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

	public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

	// Roots Methods
	public static final String METHOD_ROOTS_LIST = "roots/list";

	public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

	// Sampling Methods
	public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// ---------------------------
	// JSON-RPC Error Codes
	// ---------------------------
	/**
	 * Standard error codes used in MCP JSON-RPC responses.
	 */
	public static final class ErrorCodes {

		/**
		 * Invalid JSON was received by the server.
		 */
		public static final int PARSE_ERROR = -32700;

		/**
		 * The JSON sent is not a valid Request object.
		 */
		public static final int INVALID_REQUEST = -32600;

		/**
		 * The method does not exist / is not available.
		 */
		public static final int METHOD_NOT_FOUND = -32601;

		/**
		 * Invalid method parameter(s).
		 */
		public static final int INVALID_PARAMS = -32602;

		/**
		 * Internal JSON-RPC error.
		 */
		public static final int INTERNAL_ERROR = -32603;

	}

	public interface Request {
	}

	private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	/**
	 * Deserializes a JSON string into a JSONRPCMessage object.
	 * @param objectMapper The ObjectMapper instance to use for deserialization
	 * @param jsonText The JSON string to deserialize
	 * @return A JSONRPCMessage instance using either the {@link JSONRPCRequest},
	 * {@link JSONRPCNotification}, or {@link JSONRPCResponse} classes.
	 * @throws IOException If there's an error during deserialization
	 * @throws IllegalArgumentException If the JSON structure doesn't match any known
	 * message type
	 */
	public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
			throws IOException {

		logger.debug("Received JSON message: {}", jsonText);

		HashMap<String, Object> map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

		// Determine message type based on specific JSON structure
		if (map.containsKey("method") && map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCRequest.class);
		}
		else if (map.containsKey("method") && !map.containsKey("id")) {
			return objectMapper.convertValue(map, JSONRPCNotification.class);
		}
		else if (map.containsKey("result") || map.containsKey("error")) {
			return objectMapper.convertValue(map, JSONRPCResponse.class);
		}

		throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
	}

	// ---------------------------
	// JSON-RPC Message Types
	// ---------------------------
	public interface JSONRPCMessage {

		String jsonrpc();

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCRequest implements JSONRPCMessage {
		@JsonProperty("jsonrpc")
		private String jsonrpc;
		
		@JsonProperty("method")
		private String method;
		
		@JsonProperty("id")
		private Object id;
		
		@JsonProperty("params")
		private Object params;

		@Override
		public String jsonrpc() {
			return jsonrpc;
		}

		public String method() {
			return method;
		}

		public Object id() {
			return id;
		}

		public Object params() {
			return params;
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCNotification implements JSONRPCMessage {
		@JsonProperty("jsonrpc")
		private String jsonrpc;
		
		@JsonProperty("method")
		private String method;
		
		@JsonProperty("params")
		private Object params;

		@Override
		public String jsonrpc() {
			return jsonrpc;
		}

		public String method() {
			return method;
		}

		public Object params() {
			return params;
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JSONRPCResponse implements JSONRPCMessage {
		@JsonProperty("jsonrpc")
		private String jsonrpc;
		
		@JsonProperty("id")
		private Object id;
		
		@JsonProperty("result")
		private Object result;
		
		@JsonProperty("error")
		private JSONRPCError error;

		@Override
		public String jsonrpc() {
			return jsonrpc;
		}

		public JSONRPCError error() {
			return error;
		}

		public Object result() {
			return result;
		}

		public Object id() {
			return id;
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class JSONRPCError {
			@JsonProperty("code")
			private int code;
			
			@JsonProperty("message")
			private String message;
			
			@JsonProperty("data")
			private Object data;
		}
	}

	// ---------------------------
	// Initialization
	// ---------------------------
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeRequest implements Request {
		@JsonProperty("protocolVersion")
		private String protocolVersion;
		
		@JsonProperty("capabilities")
		private ClientCapabilities capabilities;
		
		@JsonProperty("clientInfo")
		private Implementation clientInfo;

		public String protocolVersion() {
			return protocolVersion;
		}

		public ClientCapabilities capabilities() {
			return capabilities;
		}	

		public Implementation clientInfo() {
			return clientInfo;
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InitializeResult {
		@JsonProperty("protocolVersion")
		private String protocolVersion;
		
		@JsonProperty("capabilities")
		private ServerCapabilities capabilities;
		
		@JsonProperty("serverInfo")
		private Implementation serverInfo;
		
		@JsonProperty("instructions")
		private String instructions;
	}

	/**
	 * Clients can implement additional features to enrich connected MCP servers with
	 * additional capabilities. These capabilities can be used to extend the functionality
	 * of the server, or to provide additional information to the server about the
	 * client's capabilities.
	 *
	 * @param experimental WIP
	 * @param roots define the boundaries of where servers can operate within the
	 * filesystem, allowing them to understand which directories and files they have
	 * access to.
	 * @param sampling Provides a standardized way for servers to request LLM sampling
	 * (“completions” or “generations”) from language models via clients.
	 *
	 */
	@Data
//	@Builder
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ClientCapabilities {
		@JsonProperty("experimental")
		private Map<String, Object> experimental;
		
		@JsonProperty("roots")
		private RootCapabilities roots;
		
		@JsonProperty("sampling")
		private Sampling sampling;

		@Data
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class RootCapabilities {
			@JsonProperty("listChanged")
			private Boolean listChanged;
		}

		@Data
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class Sampling {
		}
		
		 public static class Builder {
		 	private Map<String, Object> experimental;
		 	private RootCapabilities roots;
		 	private Sampling sampling;

		 	public Builder experimental(Map<String, Object> experimental) {
		 		this.experimental = experimental;
		 		return this;
		 	}

		 	public Builder roots(Boolean listChanged) {
		 		this.roots = new RootCapabilities();
		 		this.roots.setListChanged(listChanged);
		 		return this;
		 	}

		 	public Builder sampling() {
		 		this.sampling = new Sampling();
		 		return this;
		 	}

		 	public ClientCapabilities build() {
		 		ClientCapabilities capabilities = new ClientCapabilities();
		 		capabilities.setExperimental(experimental);
		 		capabilities.setRoots(roots);
		 		capabilities.setSampling(sampling);
		 		return capabilities;
		 	}
		 }

		 public static Builder builder() {
		 	return new Builder();
		 }
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ServerCapabilities {
		@JsonProperty("completions")
		private CompletionCapabilities completions;
		
		@JsonProperty("experimental")
		private Map<String, Object> experimental;
		
		@JsonProperty("logging")
		private LoggingCapabilities logging;
		
		@JsonProperty("prompts")
		private PromptCapabilities prompts;
		
		@JsonProperty("resources")
		private ResourceCapabilities resources;
		
		@JsonProperty("tools")
		private ToolCapabilities tools;

		@Data
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class CompletionCapabilities {
		}

		@Data
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class LoggingCapabilities {
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class PromptCapabilities {
			@JsonProperty("listChanged")
			private Boolean listChanged;
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ResourceCapabilities {
			@JsonProperty("subscribe")
			private Boolean subscribe;
			
			@JsonProperty("listChanged")
			private Boolean listChanged;
		}

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		public static class ToolCapabilities {
			@JsonProperty("listChanged")
			private Boolean listChanged;
		}

		public static class Builder {
		 	private CompletionCapabilities completions;
		 	private Map<String, Object> experimental;
		 	private LoggingCapabilities logging = new LoggingCapabilities();
		 	private PromptCapabilities prompts;
		 	private ResourceCapabilities resources;
		 	private ToolCapabilities tools;

		 	public Builder completions() {
		 		this.completions = new CompletionCapabilities();
		 		return this;
		 	}

		 	public Builder experimental(Map<String, Object> experimental) {
		 		this.experimental = experimental;
		 		return this;
		 	}

		 	public Builder logging() {
		 		this.logging = new LoggingCapabilities();
		 		return this;
		 	}

		 	public Builder prompts(Boolean listChanged) {
		 		this.prompts = new PromptCapabilities();
		 		this.prompts.setListChanged(listChanged);
		 		return this;
		 	}

		 	public Builder resources(Boolean subscribe, Boolean listChanged) {
		 		this.resources = new ResourceCapabilities();
		 		this.resources.setSubscribe(subscribe);
		 		this.resources.setListChanged(listChanged);
		 		return this;
		 	}

		 	public Builder tools(Boolean listChanged) {
		 		this.tools = new ToolCapabilities();
		 		this.tools.setListChanged(listChanged);
		 		return this;
		 	}

		 	public ServerCapabilities build() {
		 		ServerCapabilities capabilities = new ServerCapabilities();
		 		capabilities.setCompletions(completions);
		 		capabilities.setExperimental(experimental);
		 		capabilities.setLogging(logging);
		 		capabilities.setPrompts(prompts);
		 		capabilities.setResources(resources);
		 		capabilities.setTools(tools);
		 		return capabilities;
		 	}
		 }

		 public static Builder builder() {
		 	return new Builder();
		 }
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Implementation {
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("version")
		private String version;
	}

	// Existing Enums and Base Types (from previous implementation)
	public enum Role {// @formatter:off

		@JsonProperty("user") USER,
		@JsonProperty("assistant") ASSISTANT
	}// @formatter:on

	// ---------------------------
	// Resource Interfaces
	// ---------------------------
	/**
	 * Base for objects that include optional annotations for the client. The client can
	 * use annotations to inform how objects are used or displayed
	 */
	public interface Annotated {

		Annotations annotations();

	}

	/**
	 * Optional annotations for the client. The client can use annotations to inform how
	 * objects are used or displayed.
	 *
	 * @param audience Describes who the intended customer of this object or data is. It
	 * can include multiple entries to indicate content useful for multiple audiences
	 * (e.g., `["user", "assistant"]`).
	 * @param priority Describes how important this data is for operating the server. A
	 * value of 1 means "most important," and indicates that the data is effectively
	 * required, while 0 means "least important," and indicates that the data is entirely
	 * optional. It is a number between 0 and 1.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Annotations {
		@JsonProperty("audience")
		private List<Role> audience;
		
		@JsonProperty("priority")
		private Double priority;
	}

	/**
	 * A known resource that the server is capable of reading.
	 *
	 * @param uri the URI of the resource.
	 * @param name A human-readable name for this resource. This can be used by clients to
	 * populate UI elements.
	 * @param description A description of what this resource represents. This can be used
	 * by clients to improve the LLM's understanding of available resources. It can be
	 * thought of like a "hint" to the model.
	 * @param mimeType The MIME type of this resource, if known.
	 * @param annotations Optional annotations for the client. The client can use
	 * annotations to inform how objects are used or displayed.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Resource implements Annotated {
		@JsonProperty("uri")
		private String uri;
		
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("description")
		private String description;
		
		@JsonProperty("mimeType")
		private String mimeType;
		
		@JsonProperty("annotations")
		private Annotations annotations;

		public String uri() {
			return this.uri;
		}

		public String name() {
			return this.name;
		}

		public String description() {
			return this.description;
		}

		public String mimeType() {
			return this.mimeType;
		}

		public Annotations annotations() {
			return this.annotations;
		}
	}

	/**
	 * Resource templates allow servers to expose parameterized resources using URI
	 * templates.
	 *
	 * @param uriTemplate A URI template that can be used to generate URIs for this
	 * resource.
	 * @param name A human-readable name for this resource. This can be used by clients to
	 * populate UI elements.
	 * @param description A description of what this resource represents. This can be used
	 * by clients to improve the LLM's understanding of available resources. It can be
	 * thought of like a "hint" to the model.
	 * @param mimeType The MIME type of this resource, if known.
	 * @param annotations Optional annotations for the client. The client can use
	 * annotations to inform how objects are used or displayed.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ResourceTemplate implements Annotated {
		@JsonProperty("uriTemplate")
		private String uriTemplate;
		
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("description")
		private String description;
		
		@JsonProperty("mimeType")
		private String mimeType;
		
		@JsonProperty("annotations")
		private Annotations annotations;

		@Override
		public Annotations annotations() {
			return annotations;
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourcesResult {
		@JsonProperty("resources")
		private List<Resource> resources;
		
		@JsonProperty("nextCursor")
		private String nextCursor;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListResourceTemplatesResult {
		@JsonProperty("resourceTemplates")
		private List<ResourceTemplate> resourceTemplates;
		
		@JsonProperty("nextCursor")
		private String nextCursor;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceRequest {
		@JsonProperty("uri")
		private String uri;

		public String uri() {
			return this.uri;
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ReadResourceResult {
		@JsonProperty("contents")
		private List<ResourceContents> contents;
	}

	/**
	 * Sent from the client to request resources/updated notifications from the server
	 * whenever a particular resource changes.
	 *
	 * @param uri the URI of the resource to subscribe to. The URI can use any protocol;
	 * it is up to the server how to interpret it.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SubscribeRequest {
		@JsonProperty("uri")
		private String uri;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UnsubscribeRequest {
		@JsonProperty("uri")
		private String uri;
	}

	/**
	 * The contents of a specific resource or sub-resource.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
			@JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
	public interface ResourceContents {

		/**
		 * The URI of this resource.
		 * @return the URI of this resource.
		 */
		String uri();

		/**
		 * The MIME type of this resource.
		 * @return the MIME type of this resource.
		 */
		String mimeType();

	}

	/**
	 * Text contents of a resource.
	 *
	 * @param uri the URI of this resource.
	 * @param mimeType the MIME type of this resource.
	 * @param text the text of the resource. This must only be set if the resource can
	 * actually be represented as text (not binary data).
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextResourceContents implements ResourceContents {
		@JsonProperty("uri")
		private String uri;
		
		@JsonProperty("mimeType")
		private String mimeType;
		
		@JsonProperty("text")
		private String text;

		public TextResourceContents() {
		}

		public TextResourceContents(String content) {
			this.text = content;
		}

		@Override
		public String uri() {
			return uri;
		}

		@Override
		public String mimeType() {
			return mimeType;
		}
	}

	/**
	 * Binary contents of a resource.
	 *
	 * @param uri the URI of this resource.
	 * @param mimeType the MIME type of this resource.
	 * @param blob a base64-encoded string representing the binary data of the resource.
	 * This must only be set if the resource can actually be represented as binary data
	 * (not text).
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class BlobResourceContents implements ResourceContents {
		@JsonProperty("uri")
		private String uri;
		
		@JsonProperty("mimeType")
		private String mimeType;
		
		@JsonProperty("blob")
		private String blob;

		@Override
		public String uri() {
			return uri;
		}

		@Override
		public String mimeType() {
			return mimeType;
		}
	}

	// ---------------------------
	// Prompt Interfaces
	// ---------------------------
	/**
	 * A prompt or prompt template that the server offers.
	 *
	 * @param name The name of the prompt or prompt template.
	 * @param description An optional description of what this prompt provides.
	 * @param arguments A list of arguments to use for templating the prompt.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Prompt {
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("description")
		private String description;
		
		@JsonProperty("arguments")
		private List<PromptArgument> arguments;

		public String name() {
			return this.name;
		}

		public String description() {
			return this.description;
		}

		public List<PromptArgument> arguments() {
			return this.arguments;
		}
	}

	/**
	 * Describes an argument that a prompt can accept.
	 *
	 * @param name The name of the argument.
	 * @param description A human-readable description of the argument.
	 * @param required Whether this argument must be provided.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptArgument {
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("description")
		private String description;
		
		@JsonProperty("required")
		private Boolean required;
	}

	/**
	 * Describes a message returned as part of a prompt.
	 *
	 * This is similar to `SamplingMessage`, but also supports the embedding of resources
	 * from the MCP server.
	 *
	 * @param role The sender or recipient of messages and data in a conversation.
	 * @param content The content of the message of type {@link Content}.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptMessage {
		@JsonProperty("role")
		private Role role;
		
		@JsonProperty("content")
		private Content content;
	}

	/**
	 * The server's response to a prompts/list request from the client.
	 *
	 * @param prompts A list of prompts that the server provides.
	 * @param nextCursor An optional cursor for pagination. If present, indicates there
	 * are more prompts available.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListPromptsResult {
		@JsonProperty("prompts")
		private List<Prompt> prompts;
		
		@JsonProperty("nextCursor")
		private String nextCursor;
	}

	/**
	 * Used by the client to get a prompt provided by the server.
	 *
	 * @param name The name of the prompt or prompt template.
	 * @param arguments Arguments to use for templating the prompt.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptRequest implements Request {
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("arguments")
		private Map<String, Object> arguments;

		public String name() {
			return this.name;
		}

		public Map<String, Object> arguments() {
			return this.arguments;
		}
	}

	/**
	 * The server's response to a prompts/get request from the client.
	 *
	 * @param description An optional description for the prompt.
	 * @param messages A list of messages to display as part of the prompt.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GetPromptResult {
		@JsonProperty("description")
		private String description;
		
		@JsonProperty("messages")
		private List<PromptMessage> messages;
	}

	// ---------------------------
	// Tool Interfaces
	// ---------------------------
	/**
	 * The server's response to a tools/list request from the client.
	 *
	 * @param tools A list of tools that the server provides.
	 * @param nextCursor An optional cursor for pagination. If present, indicates there
	 * are more tools available.
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListToolsResult {
		@JsonProperty("tools")
		private List<Tool> tools;
		
		@JsonProperty("nextCursor")
		private String nextCursor;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JsonSchema {
		@JsonProperty("type")
		private String type;
		
		@JsonProperty("properties")
		private Map<String, Object> properties;
		
		@JsonProperty("required")
		private List<String> required;
		
		@JsonProperty("additionalProperties")
		private Boolean additionalProperties;
		
		@JsonProperty("$defs")
		private Map<String, Object> defs;
		
		@JsonProperty("definitions")
		private Map<String, Object> definitions;
	}

	/**
	 * Represents a tool that the server provides. Tools enable servers to expose
	 * executable functionality to the system. Through these tools, you can interact with
	 * external systems, perform computations, and take actions in the real world.
	 *
	 * @param name A unique identifier for the tool. This name is used when calling the
	 * tool.
	 * @param description A human-readable description of what the tool does. This can be
	 * used by clients to improve the LLM's understanding of available tools.
	 * @param inputSchema A JSON Schema object that describes the expected structure of
	 * the arguments when calling this tool. This allows clients to validate tool
	 * arguments before sending them to the server.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Tool {
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("description")
		private String description;
		
		@JsonProperty("inputSchema")
		private JsonSchema inputSchema;

		public String name() {
			return this.name;
		}

		public String description() {
			return this.description;
		}

		public JsonSchema inputSchema() {
			return this.inputSchema;
		}

		public Tool() {
		}

		public Tool(String name, String description, JsonSchema inputSchema) {
			this.name = name;
			this.description = description;
			this.inputSchema = inputSchema;
		}

		public Tool(String name, String description, String schema) {
			this(name, description, parseSchema(schema));
		}
		
	}

	private static JsonSchema parseSchema(String schema) {
		try {
			return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Invalid schema: " + schema, e);
		}
	}

	/**
	 * Used by the client to call a tool provided by the server.
	 *
	 * @param name The name of the tool to call. This must match a tool name from
	 * tools/list.
	 * @param arguments Arguments to pass to the tool. These must conform to the tool's
	 * input schema.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolRequest implements Request {
		@JsonProperty("name")
		private String name;
		
		@JsonProperty("arguments")
		private Map<String, Object> arguments;

		public String name() {
			return this.name;
		}

		public Map<String, Object> arguments() {
			return this.arguments;
		}

		public CallToolRequest() {
		}

		public CallToolRequest(String name, Map<String, Object> arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		public CallToolRequest(String name, String jsonArguments) {
			this(name, parseJsonArguments(jsonArguments));
		}

		private static Map<String, Object> parseJsonArguments(String jsonArguments) {
			try {
				return OBJECT_MAPPER.readValue(jsonArguments, MAP_TYPE_REF);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Invalid arguments: " + jsonArguments, e);
			}
		}
	}

	/**
	 * The server's response to a tools/call request from the client.
	 *
	 * @param content A list of content items representing the tool's output. Each item can be text, an image,
	 *                or an embedded resource.
	 * @param isError If true, indicates that the tool execution failed and the content contains error information.
	 *                If false or absent, indicates successful execution.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CallToolResult {
		@JsonProperty("content")
		private List<Content> content;
		
		@JsonProperty("isError")
		private Boolean isError;

		public CallToolResult() {
		}

		public CallToolResult(List<Content> content, Boolean isError) {
			this.content = content;
			this.isError = isError;
		}

		public CallToolResult(String content, Boolean isError) {
			this(Collections.singletonList(new TextContent(content)), isError);
		}

		public static class Builder {
			private List<Content> content = new ArrayList<>();
			private Boolean isError;

			public Builder() {
			}

			public Builder content(List<Content> content) {
				Assert.notNull(content, "content must not be null");
				this.content = content;
				return this;
			}

			public Builder textContent(List<String> textContent) {
				Assert.notNull(textContent, "textContent must not be null");
				textContent.stream()
					.map(TextContent::new)
					.forEach(this.content::add);
				return this;
			}

			public Builder addContent(Content contentItem) {
				Assert.notNull(contentItem, "contentItem must not be null");
				if (this.content == null) {
					this.content = new ArrayList<>();
				}
				this.content.add(contentItem);
				return this;
			}

			public Builder addTextContent(String text) {
				Assert.notNull(text, "text must not be null");
				return addContent(new TextContent(text));
			}

			public Builder isError(Boolean isError) {
				Assert.notNull(isError, "isError must not be null");
				this.isError = isError;
				return this;
			}

			public CallToolResult build() {
				return new CallToolResult(content, isError);
			}
		}

		public static Builder builder() {
			return new Builder();
		}
	}

	// ---------------------------
	// Sampling Interfaces
	// ---------------------------
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelPreferences {
		@JsonProperty("hints")
		private List<ModelHint> hints;
		
		@JsonProperty("costPriority")
		private Double costPriority;
		
		@JsonProperty("speedPriority")
		private Double speedPriority;
		
		@JsonProperty("intelligencePriority")
		private Double intelligencePriority;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ModelHint {
		@JsonProperty("name")
		private String name;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SamplingMessage {
		@JsonProperty("role")
		private Role role;
		
		@JsonProperty("content")
		private Content content;
	}

	// Sampling and Message Creation
	@Data
//	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageRequest implements Request {
		@JsonProperty("messages")
		private List<SamplingMessage> messages;
		
		@JsonProperty("modelPreferences")
		private ModelPreferences modelPreferences;
		
		@JsonProperty("systemPrompt")
		private String systemPrompt;
		
		@JsonProperty("includeContext")
		private ContextInclusionStrategy includeContext;
		
		@JsonProperty("temperature")
		private Double temperature;
		
		@JsonProperty("maxTokens")
		private int maxTokens;
		
		@JsonProperty("stopSequences")
		private List<String> stopSequences;
		
		@JsonProperty("metadata")
		private Map<String, Object> metadata;

		public enum ContextInclusionStrategy {
			@JsonProperty("none") NONE,
			@JsonProperty("thisServer") THIS_SERVER,
			@JsonProperty("allServers") ALL_SERVERS
		}
		
		 public static Builder builder() {
		 	return new Builder();
		 }

		 public static class Builder {
		 	private List<SamplingMessage> messages;
		 	private ModelPreferences modelPreferences;
		 	private String systemPrompt;
		 	private ContextInclusionStrategy includeContext;
		 	private Double temperature;
		 	private int maxTokens;
		 	private List<String> stopSequences;
		 	private Map<String, Object> metadata;

		 	public Builder messages(List<SamplingMessage> messages) {
		 		this.messages = messages;
		 		return this;
		 	}

		 	public Builder modelPreferences(ModelPreferences modelPreferences) {
		 		this.modelPreferences = modelPreferences;
		 		return this;
		 	}

		 	public Builder systemPrompt(String systemPrompt) {
		 		this.systemPrompt = systemPrompt;
		 		return this;
		 	}

		 	public Builder includeContext(ContextInclusionStrategy includeContext) {
		 		this.includeContext = includeContext;
		 		return this;
		 	}

		 	public Builder temperature(Double temperature) {
		 		this.temperature = temperature;
		 		return this;
		 	}

		 	public Builder maxTokens(int maxTokens) {
		 		this.maxTokens = maxTokens;
		 		return this;
		 	}

		 	public Builder stopSequences(List<String> stopSequences) {
		 		this.stopSequences = stopSequences;
		 		return this;
		 	}

		 	public Builder metadata(Map<String, Object> metadata) {
		 		this.metadata = metadata;
		 		return this;
		 	}

		 	public CreateMessageRequest build() {
		 		return new CreateMessageRequest(messages, modelPreferences, systemPrompt,
		 			includeContext, temperature, maxTokens, stopSequences, metadata);
		 	}
		 }
	}

	@Data
//	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CreateMessageResult {
		@JsonProperty("role")
		private Role role;
		
		@JsonProperty("content")
		private Content content;
		
		@JsonProperty("model")
		private String model;
		
		@JsonProperty("stopReason")
		private StopReason stopReason;

		public enum StopReason {
			@JsonProperty("endTurn") END_TURN,
			@JsonProperty("stopSequence") STOP_SEQUENCE,
			@JsonProperty("maxTokens") MAX_TOKENS
		}

		 public static Builder builder() {
		 	return new Builder();
		 }

		 public static class Builder {
		 	private Role role = Role.ASSISTANT;
		 	private Content content;
		 	private String model;
		 	private StopReason stopReason = StopReason.END_TURN;

		 	public Builder role(Role role) {
		 		this.role = role;
		 		return this;
		 	}

		 	public Builder content(Content content) {
		 		this.content = content;
		 		return this;
		 	}

		 	public Builder model(String model) {
		 		this.model = model;
		 		return this;
		 	}

		 	public Builder stopReason(StopReason stopReason) {
		 		this.stopReason = stopReason;
		 		return this;
		 	}

		 	public Builder message(String message) {
		 		this.content = new TextContent(message);
		 		return this;
		 	}

		 	public CreateMessageResult build() {
		 		return new CreateMessageResult(role, content, model, stopReason);
		 	}
		 }
	}

	// ---------------------------
	// Pagination Interfaces
	// ---------------------------
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedRequest {
		@JsonProperty("cursor")
		private String cursor;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PaginatedResult {
		@JsonProperty("nextCursor")
		private String nextCursor;
	}

	// ---------------------------
	// Progress and Logging
	// ---------------------------
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProgressNotification {
		@JsonProperty("progressToken")
		private String progressToken;
		
		@JsonProperty("progress")
		private double progress;
		
		@JsonProperty("total")
		private Double total;
	}

	/**
	 * The Model Context Protocol (MCP) provides a standardized way for servers to send
	 * structured log messages to clients. Clients can control logging verbosity by
	 * setting minimum log levels, with servers sending notifications containing severity
	 * levels, optional logger names, and arbitrary JSON-serializable data.
	 *
	 * @param level The severity levels. The minimum log level is set by the client.
	 * @param logger The logger that generated the message.
	 * @param data JSON-serializable logging data.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LoggingMessageNotification {
		@JsonProperty("level")
		private LoggingLevel level;
		
		@JsonProperty("logger")
		private String logger;
		
		@JsonProperty("data")
		private String data;

		public LoggingLevel level() {
			return this.level;
		}

		public String logger() {
			return this.logger;
		}

		public String data() {
			return this.data;
		}
	}

	public enum LoggingLevel {
		@JsonProperty("debug") DEBUG(0),
		@JsonProperty("info") INFO(1),
		@JsonProperty("notice") NOTICE(2),
		@JsonProperty("warning") WARNING(3),
		@JsonProperty("error") ERROR(4),
		@JsonProperty("critical") CRITICAL(5),
		@JsonProperty("alert") ALERT(6),
		@JsonProperty("emergency") EMERGENCY(7);

		private final int level;

		LoggingLevel(int level) {
			this.level = level;
		}

		public int level() {
			return level;
		}

	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SetLevelRequest {
		@JsonProperty("level")
		private LoggingLevel level;

		public LoggingLevel level() {
			return this.level;
		}
	}

	// ---------------------------
	// Autocomplete
	// ---------------------------
	public interface CompleteReference {

		String type();

		String identifier();

	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PromptReference implements CompleteReference {
		@JsonProperty("type")
		private String type = "ref/prompt";
		
		@JsonProperty("name")
		private String name;

		public PromptReference() {
		}

		public PromptReference(String name) {
			this.name = name;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String identifier() {
			return name;
		}

		public String name() {
			return this.name;
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ResourceReference implements CompleteReference {
		@JsonProperty("type")
		private String type = "resource";
		
		@JsonProperty("uri")
		private String uri ;

		public ResourceReference() {
		}

		public ResourceReference(String uri) {
			this.uri = uri;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String identifier() {
			return uri;
		}

		public String uri() {
			return this.uri;
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CompleteRequest implements Request {
		@JsonProperty("ref")
		private CompleteReference ref;
		
		@JsonProperty("argument")
		private CompleteArgument argument;

		public CompleteReference ref() {
			return this.ref;
		}

		public CompleteArgument argument() {
			return this.argument;
		}

		@Data
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class CompleteArgument {
			@JsonProperty("name")
			private String name;
			
			@JsonProperty("value")
			private String value;

			public String name() {
				return this.name;
			}

			public String value() {
				return this.value;
			}
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CompleteResult {
		@JsonProperty("completion")
		private CompleteCompletion completion;

		@Data
		@JsonInclude(JsonInclude.Include.NON_ABSENT)
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class CompleteCompletion {
			@JsonProperty("values")
			private List<String> values;
			
			@JsonProperty("total")
			private Integer total;
			
			@JsonProperty("hasMore")
			private Boolean hasMore;
		}
	}

	// ---------------------------
	// Content Types
	// ---------------------------
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
			@JsonSubTypes.Type(value = ImageContent.class, name = "image"),
			@JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
	public interface Content {

		default String type() {
			if (this instanceof TextContent) {
				return "text";
			}
			else if (this instanceof ImageContent) {
				return "image";
			}
			else if (this instanceof EmbeddedResource) {
				return "resource";
			}
			throw new IllegalArgumentException("Unknown content type: " + this);
		}

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TextContent implements Content {
		@JsonProperty("audience")
		private List<Role> audience;
		
		@JsonProperty("priority")
		private Double priority;
		
		@JsonProperty("text")
		private String text;

		public TextContent(String content) {
			this(null, null, content);
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ImageContent implements Content {
		@JsonProperty("audience")
		private List<Role> audience;
		
		@JsonProperty("priority")
		private Double priority;
		
		@JsonProperty("data")
		private String data;
		
		@JsonProperty("mimeType")
		private String mimeType;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EmbeddedResource implements Content {
		@JsonProperty("audience")
		private List<Role> audience;
		
		@JsonProperty("priority")
		private Double priority;
		
		@JsonProperty("resource")
		private ResourceContents resource;

		@Override
		public String type() {
			return "resource";
		}
	}

	// ---------------------------
	// Roots
	// ---------------------------
	/**
	 * Represents a root directory or file that the server can operate on.
	 *
	 * @param uri The URI identifying the root. This *must* start with file:// for now.
	 * This restriction may be relaxed in future versions of the protocol to allow other
	 * URI schemes.
	 * @param name An optional name for the root. This can be used to provide a
	 * human-readable identifier for the root, which may be useful for display purposes or
	 * for referencing the root in other parts of the application.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Root {
		@JsonProperty("uri")
		private String uri;
		
		@JsonProperty("name")
		private String name;

		public Root(String uri, String name) {
			this.uri = uri;
			this.name = name;
		}

		public String uri() {
			return uri;
		}

		public String name() {
			return name;
		}
	}

	/**
	 * The client's response to a roots/list request from the server. This result contains
	 * an array of Root objects, each representing a root directory or file that the
	 * server can operate on.
	 *
	 * @param roots An array of Root objects, each representing a root directory or file
	 * that the server can operate on.
	 */
	@Data
	@JsonInclude(JsonInclude.Include.NON_ABSENT)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ListRootsResult {
		@JsonProperty("roots")
		private List<Root> roots;
	}

}
