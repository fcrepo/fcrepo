package org.fcrepo.binary;

import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.value.binary.NamedHint;
import org.modeshape.jcr.value.binary.StrategyHint;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_CONTENTTYPE;

public class MimeTypePolicy implements Policy {
	private final String mimeType;
	private final StrategyHint hint;

	public MimeTypePolicy(String mimeType, StrategyHint hint) {
		this.mimeType = mimeType;
		this.hint = hint;
	}

	public StrategyHint evaluatePolicy(Node n) {
		try {
			if(n.getProperty(FEDORA_CONTENTTYPE).getString().equals(mimeType)) {
				return hint;
			}
		} catch (RepositoryException e) {
			return null;
		}

		return null;
	}
}
