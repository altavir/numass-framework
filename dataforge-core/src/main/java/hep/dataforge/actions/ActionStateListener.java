/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.actions;

/**
 *
 * @author Alexander Nozik
 */
public interface ActionStateListener {
    void notifyActionStarted(Action action, Object argument);
    void notifyActionFinished(Action action, Object result);
    /**
     * Notify action progress
     * @param action
     * @param progress the value between 0 and 1; negative values are ignored
     * @param message 
     */
    void notifyAcionProgress(Action action, double progress, String message);
    
}
