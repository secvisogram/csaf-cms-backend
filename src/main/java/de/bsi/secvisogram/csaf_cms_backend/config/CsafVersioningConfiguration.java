package de.bsi.secvisogram.csaf_cms_backend.config;

public class CsafVersioningConfiguration {

    private int levenshtein;

    public int getLevenshtein() {
        return levenshtein;
    }

    public CsafVersioningConfiguration setLevenshtein(int levenshtein) {
        this.levenshtein = levenshtein;
        return this;
    }
}
