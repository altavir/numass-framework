/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.content.Named;
import javafx.scene.control.Tab;

/**
 * A tab which contains output of task or action. 
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public abstract class OutputTab extends Tab implements Named {

    private String name;

    public OutputTab(String name) {
        super(name);
    }

    public OutputTab(String name, String title) {
        super(title);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public abstract void close();

    public abstract void clear();
}
