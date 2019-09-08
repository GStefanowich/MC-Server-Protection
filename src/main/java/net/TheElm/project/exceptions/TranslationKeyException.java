package net.TheElm.project.exceptions;

public final class TranslationKeyException extends Exception {
    
    private final String translationKey;
    
    public TranslationKeyException(String key) {
        this.translationKey = key;
    }
    
    public String getKey() {
        return this.translationKey;
    }
    
}
