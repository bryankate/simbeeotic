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

import java.util.ArrayList;
import java.util.List;


/**
 * A looping variable that holds a single constant value. This is bit of a misnomer in that the value may not be
 * constant if it is in fact a reference to another variable that is changing, but it will always hold a single value at
 * any given time.
 *
 * @author bkate
 */
public class ConstantVariable extends AbstractLoopingVariable {

    protected String value = null;


    /**
     * Default constructor - takes the initial value string.
     *
     * @param value The value of the variable - may be a placeholder reference to another variable (but not null).
     */
    public ConstantVariable(String value) {

        this.value = value;

        addDepIfNeeded(value);
    }


    /**
     * Gets the value of the variable.
     *
     * @return A list containing one item - the current value of this variable.
     */
    protected List<String> calculateValues() throws VariableCalculationException {

        if (value == null) {
            throw new VariableCalculationException("The value of a 'constant' looping variable must be non-null.");
        }

        List<String> results = new ArrayList<String>();

        String val = value;

        // see if it needs to be resolved
        if (DocUtil.isPlaceholder(value)) {
            val = getDependencyValue(DocUtil.extractPlaceholderName(value));
        }

        results.add(val);

        return results;
    }
}

