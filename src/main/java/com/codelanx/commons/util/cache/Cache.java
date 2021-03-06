/*
 * Copyright (C) 2016 Codelanx, All Rights Reserved
 *
 * This work is licensed under a Creative Commons
 * Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 *
 * This program is protected software: You are free to distrubute your
 * own use of this software under the terms of the Creative Commons BY-NC-ND
 * license as published by Creative Commons in the year 2015 or as published
 * by a later date. You may not provide the source files or provide a means
 * of running the software outside of those licensed to use it.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the Creative Commons BY-NC-ND license
 * long with this program. If not, see <https://creativecommons.org/licenses/>.
 */
package com.codelanx.commons.util.cache;

/**
 * Represents a single variable which will be dynamically updated upon regular
 * intervals of retrieval
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 * 
 * @param <E> The type of the cached variable
 */
public abstract class Cache<E> {

    private volatile E value;
    private final long timeMS;
    private long nextCache = -1;

    /**
     * Notes the timed intervals in milliseconds for cache refreshing, and
     * refreshes the cache for the first time
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param timeMS The time to wait between refreshes in milliseconds
     */
    public Cache(long timeMS) {
        this(timeMS, false);
    }

    /**
     * Notes the timed intervals in milliseconds for cache refreshing, and
     * refreshes the cache for the first time
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param timeMS The time to wait between refreshes in milliseconds
     * @param forceRefresh {@code true} to refresh and retrieve the cached
     *                     instance upon construction, instead of on the first
     *                     call to {@link Cache#get()}
     */
    public Cache(long timeMS, boolean forceRefresh) {
        this.timeMS = timeMS;
        if (forceRefresh) {
            this.forceRefresh();
        }
    }


    /**
     * Calls a check to see if the cache needs to be updated, and returns the
     * stored variable
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The stored value of this {@link Cache}
     */
    public final E get() {
        this.checkCache();
        return this.value;
    }

    /**
     * Checks if it is time to refresh the current variable
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    private synchronized void checkCache() {
        if (this.nextCache < 0 || System.currentTimeMillis() > this.nextCache) {
            this.setNextCache();
        }
    }

    /**
     * Forcibly updates the variable and sets the next point in time for an
     * update
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    private void setNextCache() {
        this.update();
        this.nextCache = System.currentTimeMillis() + this.timeMS;
    }

    /**
     * Called when a variable needs to be updated.
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    protected abstract void update();

    /**
     * Returns the currently in-use variable without checking the cache
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The current value of this {@link Cache}
     */
    protected E getCurrentValue() {
        return this.value;
    }

    /**
     * Sets the value of this {@link Cache}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param value The new value to set
     */
    protected void setCurrentValue(E value) {
        this.value = value;
    }

    /**
     * Forcibly refreshes the current value of this {@link Cache}
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    public final void forceRefresh() {
        this.setNextCache();
    }

}
