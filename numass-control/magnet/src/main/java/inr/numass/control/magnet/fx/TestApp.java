/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.magnet.fx;

/**
 *
 * @author darksnake
 */
public class TestApp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MagnetControllerApp.main(new String[]{"--port=192.168.111.31:4001"});
//MagnetControllerApp.main(new String[]{"--port=192.168.111.31:4001", "--logLevel=DEBUG"});        
    }
    
}
