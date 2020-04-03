package com.cognition.android.mailboxapp.Summary_Utils;

import java.util.ArrayList;

public class SentenceTokensScores {
    
    int sentencePosition;
    public Double total;
    public Double titleFeature;
    public Double dateTimeFeature;
    public Double properNounFeature;
    public Double numericalDataFeature;
    public Double linkFeature;
    public Double topicalWordFeature;
    
    ArrayList<String>tokens;
    
    public SentenceTokensScores(int position)
    {
        sentencePosition = position;
        tokens = new ArrayList();
        dateTimeFeature=0.0;
        linkFeature=0.0;
        numericalDataFeature=0.0;
        properNounFeature=0.0;
        titleFeature=0.0;
        topicalWordFeature = 0.0;
    }
    
    public void setScore(String featureName , Double score)
    {
        switch (featureName)
        {
            case "titleFeature": this.titleFeature = score; break;
            case "eventFeature": this.dateTimeFeature = score; break;
            case "properNounFeature": this.properNounFeature = score; break;
            case "numericalDataFeature": this.numericalDataFeature = score; break;
            case "linkFeature": this.linkFeature = score; break;
            case "topicalWordFeature": this.topicalWordFeature = score; break;
        }
    }
    
    public void setTotal()
    {
        total = titleFeature+dateTimeFeature+properNounFeature
                +numericalDataFeature+linkFeature+topicalWordFeature;
    }
}
