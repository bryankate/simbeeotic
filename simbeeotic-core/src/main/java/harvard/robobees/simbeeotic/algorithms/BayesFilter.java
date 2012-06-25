package harvard.robobees.simbeeotic.algorithms;

/**
 * Created with IntelliJ IDEA.
 * User: markus
 * Date: 6/19/12
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
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
