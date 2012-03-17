/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic.model;


import java.util.Map;
import java.util.HashMap;
import java.util.Collections;


/**
 * A class that aggregates values together into a summary. The values can be itemized
 * into subclasses, if desired. For example, one could aggregate energy usage (as a whole)
 * and itemize the total by category or subcomponent (e.g. radio, sensor).
 *
 * Aggregators can be arranged hierarchically, so that values aggregated in lower levels are
 * also passed to higher levels.
 *
 * @author bkate
 */
public class Aggregator {

    private Aggregator parent;

    private Map<String, Double> aggregate = new HashMap<String, Double>();
    private Map<String, Map<String, Double>> itemized = new HashMap<String, Map<String, Double>>();


    /**
     * Adds a value to the sum for a given key (cannot be null).
     *
     * @param key The key under which the value is to be tallied.
     * @param value The value being tallied.
     */
    public void addValue(String key, double value) {
        addValue(key, "", value);
    }


    /**
     * Adds a value to the sum for a given key (cannot be null).
     *
     * @param key The key under which the value is to be tallied.
     * @param item The itemized subcontext of the key under which the value is to be tallied.
     * @param value The value being tallied.
     */
    public void addValue(String key, String item, double value) {

        if (key == null) {
            throw new IllegalArgumentException("The aggregator key is null.");
        }

        if (item == null) {
            throw new IllegalArgumentException("The aggregator item is null.");
        }

        if (!aggregate.containsKey(key)) {
            aggregate.put(key, 0.0);
        }

        if (!itemized.containsKey(key)) {
            itemized.put(key, new HashMap<String, Double>());
        }

        if (!itemized.get(key).containsKey(item)) {
            itemized.get(key).put(item, 0.0);
        }

        aggregate.put(key, aggregate.get(key) + value);
        itemized.get(key).put(item, itemized.get(key).get(item) + value);


        // if this is a child aggregator, inform the parent of the update
        if (parent != null) {
            parent.addValue(key, item, value);
        }
    }


    /**
     * Gets the aggregated value that corresponds to a key.
     *
     * @param key The key identifying the value being aggregated (cannot be null).
     *
     * @return The aggregated value corresponding to the given key.
     */
    public double getAggregateValue(String key) {

        if (key == null) {
            throw new IllegalArgumentException("The aggregator key is null.");
        }

        if (aggregate.containsKey(key)) {
            return aggregate.get(key);
        }

        return 0;
    }


    /**
     * Gets the aggregated value that corresponds to a key and item identifier.
     *
     * @param key The key identifying the value being aggregated (cannot be null).
     * @param item The item identifier, signifying a subcontext within the key (cannot be null).
     *
     * @return The aggregated value corresponding to the given key, item pair.
     */
    public double getItemizedValue(String key, String item) {

        if (key == null) {
            throw new IllegalArgumentException("The aggregator key is null.");
        }

        if (item == null) {
            throw new IllegalArgumentException("The aggregator item is null.");
        }

        if (itemized.containsKey(key) && itemized.get(key).containsKey(item)) {
            return itemized.get(key).get(item);
        }

        return 0;
    }


    /**
     * Gets the aggregated values that corresponds to the item subcontexts within the given key.
     *
     * @param key The key identifying the value being aggregated (cannot be null).
     *
     * @return The aggregated values indexed by item identifier.
     */
    public Map<String, Double> getItemizedValues(String key) {

        if (key == null) {
            throw new IllegalArgumentException("The aggregator key is null.");
        }

        if (itemized.containsKey(key)) {
            return Collections.unmodifiableMap(itemized.get(key));
        }

        return null;
    }


    /**
     * Indicates that this aggregator is a child of another. All values will be forwarded to the parent
     * for aggregation in addition to being aggregated locally.
     *
     * @param parent The parent aggregator.
     */
    public void setParentAggregator(Aggregator parent) {

        // todo: this would ideally be done with events between models
        this.parent = parent;
    }


    /**
     * Clears a key from the aggregator.
     *
     * @param key The key to clear.
     */
    public void clear(String key) {

        if (key == null) {
            throw new IllegalArgumentException("The aggregator key is null.");
        }

        aggregate.remove(key);
        itemized.remove(key);
    }


    /**
     * Clears an item belonging to a specific key in the aggregator.
     *
     * @param key The key to clear.
     */
    public void clear(String key, String item) {

        if (key == null) {
            throw new IllegalArgumentException("The aggregator key is null.");
        }

        if (item == null) {
            throw new IllegalArgumentException("The aggregator item is null.");
        }

        if (aggregate.containsKey(key) && itemized.containsKey(key) && itemized.get(key).containsKey(item)) {

            double val = itemized.get(key).get(item);

            itemized.get(key).remove(item);
            aggregate.put(key, aggregate.get(key) - val);
        }
    }
}
