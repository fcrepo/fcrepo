/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.api;

/**
 * @author dbernstein
 */
public interface Stats {
    StatsResults getStatistics(StatsParameters statsParams);
}
