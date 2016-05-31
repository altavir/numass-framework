/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.actions;

import inr.numass.utils.ExpressionUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;

import org.junit.Test;

/**
 *
 * @author Alexander Nozik
 */
public class PrepareDataActionTest {

    public PrepareDataActionTest() {
    }

    @Test
    public void testExpression() {
        Map<String, Double> exprParams = new HashMap<>();
        exprParams.put("U", 18000d);
        double correctionFactor = ExpressionUtils.evaluate("1 + 13.265 * exp(- U / 2343.4)", exprParams);
        Assert.assertEquals("Testing expression calculation", 1.006125, correctionFactor, 1e-5);
    }

}
