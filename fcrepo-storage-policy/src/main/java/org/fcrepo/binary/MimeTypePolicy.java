package org.fcrepo.binary;

import org.modeshape.jcr.value.binary.StrategyHint;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

public class MimeTypePolicy implements Policy {

	private static final Logger logger = getLogger(MimeTypePolicy.class);

	private final String mimeType;
	private final StrategyHint hint;

	public MimeTypePolicy(final String mimeType, final StrategyHint hint) {
		this.mimeType = mimeType;
		this.hint = hint;
	}

	public StrategyHint evaluatePolicy(final Node n) {
		logger.debug("Evaluating MimeTypePolicy for {} -> {}", mimeType, hint.toString());
		try {
			final String nodeMimeType = n.getNode(JCR_CONTENT).getProperty(JCR_MIME_TYPE).getString();
			logger.debug("Found mime type {}", nodeMimeType);
			if(nodeMimeType.equals(mimeType)) {
				return hint;
			}
		} catch (RepositoryException e) {
			logger.warn("Got Exception evaluating policy: {}", e);
			return null;
		}

		return null;
	}
}
