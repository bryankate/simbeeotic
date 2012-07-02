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

package harvard.robobees.simbeeotic.algorithms;


public class BayesFilter {


    public float update(float prevProbability){

        //double resultingPosterior = .9*prevProbability + .1*(1-prevProbability);
        double probSenseOccGivenOcc = .6;
        double probSenseOccGivenFree = .2;
        //double bayesOccupied = probSenseOccGivenOcc * resultingPosterior;
        //double bayesFree = probSenseOccGivenFree * resultingPosterior;
        double bayesOccupied = probSenseOccGivenOcc * prevProbability;
        double bayesFree = probSenseOccGivenFree * (1-prevProbability);
        double eta = 1/(bayesFree + bayesOccupied);
        float probOccupied = (float)(bayesOccupied*eta);
        return probOccupied;
    }

    public float downdate(float prevProbability){

        //double resultingPosterior = .1*prevProbability + .9*(1-prevProbability);
        double probSenseFreeGivenOcc = .2;
        double probSenseFreeGivenFree = .6;
        //double bayesOccupied = probSenseFreeGivenOcc * resultingPosterior;
        //double bayesFree = probSenseFreeGivenFree * resultingPosterior;
        double bayesOccupied = probSenseFreeGivenOcc * prevProbability;
        double bayesFree = probSenseFreeGivenFree * (1-prevProbability);
        double eta = 1/(bayesFree + bayesOccupied);
        float probOccupied = (float)(bayesOccupied*eta);
        return probOccupied;
    }

}
