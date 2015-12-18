/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import inr.numass.models.LossCalculator

LossCalculator loss = new LossCalculator();

def X = 0.6

def lossProbs = loss.getGunLossProbabilities(X);

printf("%8s\t%8s\t%8s\t%8s\t%n",
    "eps",
    "p1",
    "p2",
    "p3"
)

def singleScatter = loss.getSingleScatterFunction();

for(double d = 0; d < 30; d += 0.3){
    double ei = 18500;
    double ef = ei-d;
    printf("%8f\t%8f\t%8f\t%8f\t%n",
        d,
        lossProbs[1]*loss.getLossValue(1,ei,ef),
        lossProbs[2]*loss.getLossValue(2,ei,ef),
        lossProbs[3]*loss.getLossValue(3,ei,ef)
    )
}

for(double d = 30; d < 100; d += 1){
    double ei = 18500;
    double ef = ei-d;
    printf("%8f\t%8f\t%8f\t%8f\t%n",
        d,
        lossProbs[1]*loss.getLossValue(1,ei,ef),
        lossProbs[2]*loss.getLossValue(2,ei,ef),
        lossProbs[3]*loss.getLossValue(3,ei,ef)
    )
}
