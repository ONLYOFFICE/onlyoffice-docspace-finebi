package com.asc.fr.docspace.application.port.input;

/**
 * A {@link ScheduledJob} that must run on only one node per tick in a clustered deployment. The
 * runner guards each run with a cluster-wide lock keyed on {@link #name()}, so exactly one node
 * executes it while the others skip that tick; in standalone mode the lock is always granted. Use
 * this for work that hits shared external state (e.g. registering the DocSpace webhook) where
 * running on every node would be redundant or racy. Plain {@link ScheduledJob}s keep running
 * per-node.
 */
public interface ScheduledClusterJob extends ScheduledJob {}
