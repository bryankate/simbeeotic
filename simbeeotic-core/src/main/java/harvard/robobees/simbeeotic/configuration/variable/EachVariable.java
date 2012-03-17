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


import java.util.ArrayList;
import java.util.List;


/**
 * A looping variable that allows a list of items to be specified. Each item in the list can be a variable reference.
 *
 * @author bkate
 */
public class EachVariable extends AbstractLoopingVariable {

    private List<String> picks = null;
    private String firstDraw = null;
    private String numDraws = null;


    /**
     * Default constructor - takes the list of pick values.
     *
     * @param picks The list of values for this variable to pick from - cannot be null, but may include references.
     */
    public EachVariable(List<String> picks, String firstDraw, String numDraws) {

        this.picks = picks;
        this.firstDraw = firstDraw;
        this.numDraws = numDraws;


        if (picks != null) {

            for (String pick : picks) {

                // look for dependencies
                addDepIfNeeded(pick);
            }
        }

        addDepIfNeeded(firstDraw);
        addDepIfNeeded(numDraws);
    }


    /**
     * Calculate the current list of values. This list may be constant if there are no dependencies.
     *
     * @return The list of values that this variable represents.
     */
    @Override
    protected List<String> calculateValues() throws VariableCalculationException {

        if ((picks == null) || picks.isEmpty()) {
            throw new VariableCalculationException("There must be at least one value in the 'each' looping variable.");
        }

        List<String> results = new ArrayList<String>();

        int firstDraw = intParam(this.firstDraw, 1);
        int numDraws = intParam(this.numDraws, -1);

        int i = 0;
        int numDrawn = 0;
        for (String pick : picks) {
            if (i < firstDraw - 1) {
                continue;
            }


            // resolve any dependencies in the list
            results.add(stringParam(pick));


            if (numDraws != -1 && numDrawn >= numDraws) {
                break;
            }
        }

        return results;
    }
}

