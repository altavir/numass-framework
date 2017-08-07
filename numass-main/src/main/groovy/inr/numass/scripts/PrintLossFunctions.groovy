/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import inr.numass.models.LossCalculator

LossCalculator loss = LossCalculator.instance()

def X = 0.36

def lossProbs = loss.getGunLossProbabilities(X);

printf("%8s\t%8s\t%8s\t%8s\t%n",
        "eps",
        "p1",
        "p2",
        "p3"
)

/*
'exPos'	= 12.587 ± 0.049
'ionPos'	= 11.11 ± 0.50
'exW'	= 1.20 ± 0.12
'ionW'	= 11.02 ± 0.68
'exIonRatio'	= 2.43 ± 0.42
 */

def singleScatter = loss.getSingleScatterFunction(
        12.860,
        16.62,
        1.71,
        12.09,
        4.59
);

for (double d = 0; d < 30; d += 0.3) {
    double ei = 18500;
    double ef = ei - d;
    printf("%8f\t%8f\t%8f\t%8f\t%n",
            d,
            lossProbs[1] * singleScatter.value(ei - ef),
            lossProbs[2] * loss.getLossValue(2, ei, ef),
            lossProbs[3] * loss.getLossValue(3, ei, ef)
    )
}

for (double d = 30; d < 100; d += 1) {
    double ei = 18500;
    double ef = ei - d;
    printf("%8f\t%8f\t%8f\t%8f\t%n",
            d,
            lossProbs[1] * singleScatter.value(ei - ef),
            lossProbs[2] * loss.getLossValue(2, ei, ef),
            lossProbs[3] * loss.getLossValue(3, ei, ef)
    )
}
