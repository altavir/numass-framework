/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.test;

import hep.dataforge.actions.ActionController;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Alexander Nozik
 */
public class TestLazyData {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ActionController lock = new ActionController();
        CompletableFuture<String> future = lock.hold(() -> {
            System.out.println(" I am initialized");
            return "abcd";
        });
        future = future.<String>thenCompose((String res) -> CompletableFuture.supplyAsync(() -> {
            System.out.println(" I am capitalized");
            return res.toUpperCase();
        }));
        future = future.handleAsync((res, err) -> {
            System.out.println(" I am handled");
            return "handled " + res;
        });
        System.out.println("Releasing hold");
        lock.release();

//        dat1.getInFuture().thenRunAsync(() -> System.out.println(" I am finished"));
//        dat1.getInFuture().thenAcceptAsync((res) -> System.out.println(" I am finished"));
    }

}
