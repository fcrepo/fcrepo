package org.fcrepo.binary;

import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.value.binary.NamedHint;
import org.modeshape.jcr.value.binary.StrategyHint;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_CONTENTTYPE;
import static org.slf4j.LoggerFactory.getLogger;

public class MimeTypePolicy implements Policy {

	private static final Logger logger = getLogger(MimeTypePolicy.class);

	private final String mimeType;
	private final StrategyHint hint;

	public MimeTypePolicy(String mimeType, StrategyHint hint) {
		this.mimeType = mimeType;
		this.hint = hint;
	}

	public StrategyHint evaluatePolicy(Node n) {
		logger.debug("Evaluating MimeTypePolicy for {} -> {}", mimeType, hint.toString());
		try {
			final String nodeMimeType = n.getProperty(FEDORA_CONTENTTYPE).getString();
			logger.debug("Found mime type {}", nodeMimeType);
			if(nodeMimeType.equals(mimeType)) {
				return hint;
			}
		} catch (RepositoryException e) {
			return null;
		}

		return null;
	}
}
