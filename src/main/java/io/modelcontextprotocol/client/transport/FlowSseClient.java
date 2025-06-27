/*
* Copyright 2024 - 2024 the original author or authors.
*/
package io.modelcontextprotocol.client.transport;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * A Server-Sent Events (SSE) client implementation using Spring WebFlux for reactive
 * stream processing. This client establishes a connection to an SSE endpoint and
 * processes the incoming event stream, parsing SSE-formatted messages into structured
 * events.
 *
 * <p>
 * The client supports standard SSE event fields including:
 * <ul>
 * <li>event - The event type (defaults to "message" if not specified)</li>
 * <li>id - The event ID</li>
 * <li>data - The event payload data</li>
 * </ul>
 *
 * <p>
 * Events are delivered to a provided {@link SseEventHandler} which can process events and
 * handle any errors that occur during the connection.
 *
 * @author Christian Tzolov
 * @see SseEventHandler
 * @see SseEvent
 */
public class FlowSseClient {

	private final WebClient webClient;

	/**
	 * Pattern to extract the data content from SSE data field lines. Matches lines
	 * starting with "data:" and captures the remaining content.
	 */
	private static final Pattern EVENT_DATA_PATTERN = Pattern.compile("^data:(.+)$", Pattern.MULTILINE);

	/**
	 * Pattern to extract the event ID from SSE id field lines. Matches lines starting
	 * with "id:" and captures the ID value.
	 */
	private static final Pattern EVENT_ID_PATTERN = Pattern.compile("^id:(.+)$", Pattern.MULTILINE);

	/**
	 * Pattern to extract the event type from SSE event field lines. Matches lines
	 * starting with "event:" and captures the event type.
	 */
	private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("^event:(.+)$", Pattern.MULTILINE);

	/**
	 * Class representing a Server-Sent Event with its standard fields.
	 */
	public static class SseEvent {
		private final String id;
		private final String type;
		private final String data;

		public SseEvent(String id, String type, String data) {
			this.id = id;
			this.type = type;
			this.data = data;
		}

		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getData() {
			return data;
		}
	}

	/**
	 * Interface for handling SSE events and errors. Implementations can process received
	 * events and handle any errors that occur during the SSE connection.
	 */
	public interface SseEventHandler {
		/**
		 * Called when an SSE event is received.
		 * @param event the received SSE event containing id, type, and data
		 */
		void onEvent(SseEvent event);

		/**
		 * Called when an error occurs during the SSE connection.
		 * @param error the error that occurred
		 */
		void onError(Throwable error);
	}

	/**
	 * Creates a new FlowSseClient with the default WebClient.
	 */
	public FlowSseClient() {
		this(WebClient.create());
	}

	/**
	 * Creates a new FlowSseClient with the specified WebClient.
	 * @param webClient the WebClient instance to use for SSE connections
	 */
	public FlowSseClient(WebClient webClient) {
		this.webClient = webClient;
	}

	/**
	 * Subscribes to an SSE endpoint and processes the event stream.
	 *
	 * <p>
	 * This method establishes a connection to the specified URL and begins processing the
	 * SSE stream. Events are parsed and delivered to the provided event handler. The
	 * connection remains active until either an error occurs or the server closes the
	 * connection.
	 * @param url the SSE endpoint URL to connect to
	 * @param eventHandler the handler that will receive SSE events and error
	 * notifications
	 */
	public void subscribe(String url, SseEventHandler eventHandler) {
		StringBuilder eventBuilder = new StringBuilder();
		AtomicReference<String> currentEventId = new AtomicReference<>();
		AtomicReference<String> currentEventType = new AtomicReference<>("message");

		webClient.get()
			.uri(url)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.retrieve()
			.bodyToFlux(String.class)
			.subscribe(new Subscriber<String>() {
				private Subscription subscription;

				@Override
				public void onSubscribe(Subscription subscription) {
					this.subscription = subscription;
					subscription.request(Long.MAX_VALUE);
				}

				@Override
				public void onNext(String line) {
					if (line.isEmpty()) {
						// Empty line means end of event
						if (eventBuilder.length() > 0) {
							String eventData = eventBuilder.toString();
							SseEvent event = new SseEvent(currentEventId.get(), currentEventType.get(), eventData.trim());
							eventHandler.onEvent(event);
							eventBuilder.setLength(0);
						}
					} else {
						if (line.startsWith("data:")) {
							java.util.regex.Matcher matcher = EVENT_DATA_PATTERN.matcher(line);
							if (matcher.find()) {
								eventBuilder.append(matcher.group(1).trim()).append("\n");
							}
						} else if (line.startsWith("id:")) {
							java.util.regex.Matcher matcher = EVENT_ID_PATTERN.matcher(line);
							if (matcher.find()) {
								currentEventId.set(matcher.group(1).trim());
							}
						} else if (line.startsWith("event:")) {
							java.util.regex.Matcher matcher = EVENT_TYPE_PATTERN.matcher(line);
							if (matcher.find()) {
								currentEventType.set(matcher.group(1).trim());
							}
						}
					}
					subscription.request(1);
				}

				@Override
				public void onError(Throwable throwable) {
					eventHandler.onError(throwable);
				}

				@Override
				public void onComplete() {
					// Handle any remaining event data
					if (eventBuilder.length() > 0) {
						String eventData = eventBuilder.toString();
						SseEvent event = new SseEvent(currentEventId.get(), currentEventType.get(), eventData.trim());
						eventHandler.onEvent(event);
					}
				}
			});
	}
}
