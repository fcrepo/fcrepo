package org.fcrepo;

import java.beans.Transient;
import java.util.Date;
import java.util.UUID;

import javax.jcr.Session;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement(name = "transaction")
@XmlAccessorType(XmlAccessType.FIELD)
public class Transaction {

	public enum State {
		DIRTY, NEW, COMMITED, ROLLED_BACK;
	}

	@XmlTransient
	private final Session session;

	@XmlAttribute(name = "id")
	private final String id;

	@XmlAttribute(name = "created")
	private final Date created;

	private State state = State.NEW;
	
	private Transaction(){
		this.session = null;
		this.created = null;
		this.id = null;
	}

	public Transaction(Session session) {
		super();
		this.session = session;
		this.created = new Date();
		this.id = UUID.randomUUID().toString();
	}

	public Session getSession() {
		return session;
	}

	public Date getCreated() {
		return created;
	}

	public String getId() {
		return id;
	}

	public void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

}
