/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.viewer;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public interface ProgressUpdateCallback {
    void setProgressText(String text);
    void setProgress(double progress);
}
