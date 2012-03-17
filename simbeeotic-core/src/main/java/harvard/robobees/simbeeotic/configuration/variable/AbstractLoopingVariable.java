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
package harvard.robobees.simbeeotic.configuration.variable;


import harvard.robobees.simbeeotic.util.DocUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A class that aggregates some base functionality of a scenario looping variable. It enables implementations of
 * variables to have dependency checking and resolution methods available without re-implementation.
 *
 * @author bkate
 */
public abstract class AbstractLoopingVariable {

    private String name = null;
    private List<String> values = null;

    private boolean dirty = false;
    private Set<String> dependencies = new HashSet<String>();
    private Map<String, String> dependencyMap = new HashMap<String, String>();


    /**
     * Get a list of the values associated with the variable. Implementers are reminded that this list could change if a
     * dependency changes, so the method is only called when a new list is desired. The dependency values should be
     * checked before generating the return list.
     *
     * @return A list of string values associated with the variable.
     *
     * @throws VariableCalculationException If there is an error while calculating the variable values.
     */
    protected abstract List<String> calculateValues() throws VariableCalculationException;


    /**
     * Gets the values for the looping variable. This checks to see if any dependencies were recently changed and
     * recalculates the values if so. If there are no changes, an old value list is returned, and if all the required
     * dependencies are not satisfied, an exception is thrown.
     *
     * @return The list of values for this variable. Will remain static for the lifetime of the variable.
     *
     * @throws VariableCalculationException Thrown if there is an error calculating the variables.
     * @throws VariableDependencyException Thrown if there is an unresolved dependency in this variable.
     */
    public List<String> getValues() throws VariableCalculationException, VariableDependencyException {

        // get pre-calculated variables if nothing has changed
        if ((values != null) && !dirty) {
            return values;
        }

        // only get values if all the dependencies are there
        if (isResolved()) {

            values = calculateValues();
            dirty = false;

            if (values == null) {
                throw new VariableCalculationException("Calculated variable values are null.");
            }

            return values;
        }

        throw new VariableDependencyException("Cannot calculate variable values: missing dependencies.");
    }


    /**
     * Gets a set of variable deendencies (names of other variables upon which this is dependent).
     *
     * @return A set of dependency variable names.
     */
    public Set<String> getDependencies() {
        return dependencies;
    }


    /**
     * Adds a dependency to the set of dependencies for this variable.
     *
     * @param name the name of the enabling variable.
     */
    protected void addDependency(String name) {

        if (name != null) {
            dependencies.add(name);
        }
    }


    /**
     * Sets the value of a variable upon which this is dependent.
     *
     * @param name The name of the enabling variable.
     * @param value The variable value.
     */
    public void setDependencyValue(String name, String value) {

        if ((name != null) && (value != null)) {

            dependencyMap.put(name, value);
            dirty = true;
        }
    }


    /**
     * Gets the value of a variable upon which this variable is dependent.
     *
     * @param name The name of the enabling variable.
     *
     * @return The value of the variable, or null if no value is set.
     */
    protected String getDependencyValue(String name) {

        if (name != null) {
            return dependencyMap.get(name);
        }

        return null;
    }


    /**
     * Determines if all dependencies have been satisfied.
     *
     * @return True if the required variable dependencies have been resolved, false otherwise.
     */
    private boolean isResolved() {
        return dependencies.equals(dependencyMap.keySet());
    }


    /**
     * Sets the name of this variable.
     *
     * @param name The desired name of the variable.
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets the name of the variable.
     *
     * @return The variable's name.
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the dirty flag for this variable. This should be used sparingly since most of the dependency resolution is
     * confined to this class.
     *
     * @param dirty Indicates if a variable has been dirtied since the last derivation of variable values.
     */
    protected void setDirty(boolean dirty) {
        this.dirty = dirty;
    }


    /**
     * Pass all parameter values to this method.  They will be checked to see if they are placeholders.  If they are,
     * that placeholder will be added to the internal dependency list.
     *
     * @param paramValue The parameter value to check and add.  May be <code>null</code>.
     */
    protected void addDepIfNeeded(String paramValue) {

        if ((paramValue != null) && DocUtil.isPlaceholder(paramValue)) {
            addDependency(DocUtil.extractPlaceholderName(paramValue));
        }
    }


    /**
     * Helper method for parsing int values out of a string.  The String provided will be checked if it is a placeholder
     * and will be expanded if needed.
     *
     * @param value The raw value to parse.
     *
     * @return The int that was in the String or in the param value.  If the value is null or empty, defaultValue will
     *         be returned.
     */
    protected int intParam(String value, int defaultValue) {
        try {
            return intParam(value);
        }
        catch (VariableCalculationException e) {
            return defaultValue;
        }
    }


    /**
     * This is a variation of {@link #intParam(String, int)} that throws an exception if the string doesn't represent a
     * valid value.
     *
     * @param value The unparsed value: either a String representation of the primitive type being parsed or a valid
     * placeholder value.
     *
     * @return The parsed value
     */
    protected int intParam(String value) throws VariableCalculationException {
        if (value == null) {
            throw new VariableCalculationException("Can't expand null value");
        }

        try {
            if (DocUtil.isPlaceholder(value)) {
                return Integer.parseInt(getDependencyValue(DocUtil.extractPlaceholderName(value)));
            }
            else {
                return Integer.parseInt(value);
            }
        }
        catch (NumberFormatException e) {
            throw new VariableCalculationException("Couldn't parse value", e);
        }

    }


    /**
     * Helper method for parsing long values out of a string.  The String provided will be checked if it is a
     * placeholder and will be expanded if needed.
     *
     * @param value The raw value to parse.
     *
     * @return The double that was in the String or in the param value.  If the value is null or empty, defaultValue
     *         will be returned.
     */
    protected long longParam(String value, long defaultValue) {
        try {
            return longParam(value);
        }
        catch (VariableCalculationException e) {
            return defaultValue;
        }
    }


    /**
     * This is a variation of {@link #longParam(String, long)} that throws an exception if the string doesn't represent
     * a valid value.
     *
     * @param value The unparsed value: either a String representation of the primitive type being parsed or a valid
     * placeholder value.
     *
     * @return The parsed value
     */
    protected long longParam(String value) throws VariableCalculationException {
        if (value == null) {
            throw new VariableCalculationException("Can't expand null value");
        }

        try {
            if (DocUtil.isPlaceholder(value)) {
                return Long.parseLong(getDependencyValue(DocUtil.extractPlaceholderName(value)));
            }
            else {
                return Long.parseLong(value);
            }
        }
        catch (NumberFormatException e) {
            throw new VariableCalculationException("Couldn't parse value", e);
        }

    }


    /**
     * Helper method for parsing double values out of a string.  The String provided will be checked if it is a
     * placeholder and will be expanded if needed.
     *
     * @param value The raw value to parse.
     *
     * @return The double that was in the String or in the param value.  If the value is null or empty, defaultValue
     *         will be returned.
     */
    protected double doubleParam(String value, double defaultValue) {
        try {
            return doubleParam(value);
        }
        catch (VariableCalculationException e) {
            return defaultValue;
        }
    }


    /**
     * This is a variation of {@link #doubleParam(String, double)} that throws an exception if the string doesn't
     * represent a valid value.
     *
     * @param value The unparsed value: either a String representation of the primitive type being parsed or a valid
     * placeholder value.
     *
     * @return The parsed value
     */
    protected double doubleParam(String value) throws VariableCalculationException {
        if (value == null) {
            throw new VariableCalculationException("Can't expand null value");
        }

        try {
            if (DocUtil.isPlaceholder(value)) {
                return Double.parseDouble(getDependencyValue(DocUtil.extractPlaceholderName(value)));
            }
            else {
                return Double.parseDouble(value);
            }
        }
        catch (NumberFormatException e) {
            throw new VariableCalculationException("Couldn't parse value", e);
        }

    }


    /**
     * Helper method for parsing String values out of a string.  The String provided will be checked if it is a
     * placeholder and will be expanded if needed.
     *
     * @param value The raw value to parse.
     *
     * @return The String that was in the String or in the param value.  If the value is null or empty, defaultValue
     *         will be returned.
     */
    protected String stringParam(String value, String defaultValue) {
        try {
            return stringParam(value);
        }
        catch (VariableCalculationException e) {
            return defaultValue;
        }
    }


    /**
     * This is a variation of {@link #stringParam(String, String)} that throws an exception if the string doesn't
     * represent a valid value (which only occurs in the case of a null string).
     *
     * @param value The unparsed value: either a String literal or a valid placeholder value.
     *
     * @return The parsed value
     */
    protected String stringParam(String value) throws VariableCalculationException {
        if (value == null) {
            throw new VariableCalculationException("Can't expand null value");
        }


        if (DocUtil.isPlaceholder(value)) {
            return getDependencyValue(DocUtil.extractPlaceholderName(value));
        }
        else {
            return value;
        }
    }

}
