package org.fcrepo.modeshape.observer;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;
import static org.fcrepo.modeshape.utils.EventType.getEventType;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.xml.transform.stream.StreamSource;

import org.joda.time.DateTime;
import org.modeshape.common.SystemFailureException;

import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

@Path("/rss")
public class RSSPublisher {

	@Context
	UriInfo uriInfo;

	private static final Integer FEED_LENGTH = 10;

	private static final String FEED_TYPE = "rss_2.0";

	private static final String FEED_TITLE = "What happened in Fedora";

	private static final String FEED_DESCRIPTION = "What happened in Fedora";

	@Inject
	EventBus eventBus;

	@Inject
	private Repository repo;

	private BlockingQueue<Event> feedQueue = new ArrayBlockingQueue<Event>(
			FEED_LENGTH);

	private SyndFeed feed = new SyndFeedImpl();

	@GET
	@Path("")
	@Produces("application/rss+xml")
	public StreamSource getFeed() throws FeedException {
		feed.setLink(uriInfo.getBaseUri().toString());
		feed.setEntries(transform(copyOf(feedQueue), event2entry));
		// TODO ought to make this stream, not go through a string
		return new StreamSource(new ByteArrayInputStream(new SyndFeedOutput()
				.outputString(feed).getBytes()));
	}

	private Function<Event, SyndEntry> event2entry = new Function<Event, SyndEntry>() {
		@Override
		public SyndEntry apply(Event event) {
			SyndEntry entry = new SyndEntryImpl();
			try {
				entry.setTitle(event.getIdentifier());
				entry.setLink(event.getPath());
				entry.setPublishedDate(new DateTime(event.getDate()).toDate());
				SyndContent description = new SyndContentImpl();
				description.setType("text/plain");
				description.setValue(getEventType(event.getType()).toString());
				entry.setDescription(description);
			} catch (RepositoryException e) {
				throw new SystemFailureException(e);
			}
			return entry;
		}

	};

	@PostConstruct
	public void initialize() {
		eventBus.register(this);
		feed.setFeedType(FEED_TYPE);
		feed.setTitle(FEED_TITLE);
		feed.setDescription(FEED_DESCRIPTION);
	}

	@Subscribe
	public void newEvent(Event event) {
		if (feedQueue.remainingCapacity() > 0) {
			feedQueue.offer(event);
		} else {
			feedQueue.poll();
			feedQueue.offer(event);
		}
	}

}
