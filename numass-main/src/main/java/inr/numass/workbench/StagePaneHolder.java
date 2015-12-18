/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public interface StagePaneHolder {
    StagePane getStagePane(String stageName);
    void clearStage(String stageName);
}
