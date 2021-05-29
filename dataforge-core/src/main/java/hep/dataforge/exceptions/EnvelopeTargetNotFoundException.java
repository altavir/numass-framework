/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.exceptions;

import hep.dataforge.meta.Meta;

/**
 * An exception thrown in case message target is not found by current {@code Responder}
 * @author Alexander Nozik
 */
public class EnvelopeTargetNotFoundException extends RuntimeException {
    
    private String targetType;
    private String targetName;
    private Meta targetMeta;

    public EnvelopeTargetNotFoundException(String targetName) {
        this.targetName = targetName;
    }

    public EnvelopeTargetNotFoundException(String targetType, String targetName) {
        this.targetType = targetType;
        this.targetName = targetName;
    }

    public EnvelopeTargetNotFoundException(Meta targetMeta) {
        this.targetMeta = targetMeta;
    }
    
    public EnvelopeTargetNotFoundException(String targetName, Meta targetMeta) {
        this.targetName = targetName;
        this.targetMeta = targetMeta;
    }

    public EnvelopeTargetNotFoundException(String targetType, String targetName, Meta targetMeta) {
        this.targetType = targetType;
        this.targetName = targetName;
        this.targetMeta = targetMeta;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public Meta getTargetMeta() {
        return targetMeta;
    }

    
}
