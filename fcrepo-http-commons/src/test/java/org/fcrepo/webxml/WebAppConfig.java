package org.fcrepo.webxml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.fcrepo.webxml.bind.ContextParam;
import org.fcrepo.webxml.bind.Displayable;
import org.fcrepo.webxml.bind.Filter;
import org.fcrepo.webxml.bind.FilterMapping;
import org.fcrepo.webxml.bind.Listener;
import org.fcrepo.webxml.bind.Servlet;
import org.fcrepo.webxml.bind.ServletMapping;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

@XmlRootElement(namespace="http://java.sun.com/xml/ns/javaee", name="web-app")
public class WebAppConfig extends Displayable {
	
	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="context-param")})
	List<ContextParam> contextParams;

	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="listener")})
	List<Listener> listeners;

	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="servlet")})
	List<Servlet> servlets;

	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="filter")})
	List<Filter> filters;

	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="servlet-mapping")})
	List<ServletMapping> servletMappings;

	@XmlElements(value = {@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="filter-mapping")})
	List<FilterMapping> filterMappings;

	public Collection<ServletMapping> servletMappings(String servletName) {
		return Collections2.filter(servletMappings, new SMapFilter(servletName));
	}
	
	public Collection<FilterMapping> filterMappings(String filterName) {
		return Collections2.filter(filterMappings, new FMapFilter(filterName));
	}
	
	private static List<ContextParam> NO_CP =
			Collections.unmodifiableList(new ArrayList<ContextParam>(0));
	
	public Collection<ContextParam> contextParams() {
		return (contextParams != null) ? contextParams : NO_CP;
	}
	
	private static List<Servlet> NO_S =
			Collections.unmodifiableList(new ArrayList<Servlet>(0));
	
	public Collection<Servlet> servlets() {
		return (servlets != null) ? servlets : NO_S;
	}

	private static List<Filter> NO_F =
			Collections.unmodifiableList(new ArrayList<Filter>(0));
	
	public Collection<Filter> filters() {
		return (filters != null) ? filters : NO_F;
	}

	private static List<Listener> NO_L =
			Collections.unmodifiableList(new ArrayList<Listener>(0));
	
	public Collection<Listener> listeners() {
		return (listeners != null) ? listeners : NO_L;
	}

	private static class SMapFilter implements Predicate<ServletMapping> {
		String servletName;

		SMapFilter(String sName) {
			servletName = sName;
		}
		
		@Override
		public boolean apply(ServletMapping input) {
			return (servletName == null) ? input.servletName() == null :
				servletName.equals(input.servletName());
		}
		
		
	}
	private static class FMapFilter implements Predicate<FilterMapping> {
		String filterName;

		FMapFilter(String sName) {
			filterName = sName;
		}
		
		@Override
		public boolean apply(FilterMapping input) {
			return (filterName == null) ? input.filterName() == null :
				filterName.equals(input.filterName());
		}
		
		
	}
}
