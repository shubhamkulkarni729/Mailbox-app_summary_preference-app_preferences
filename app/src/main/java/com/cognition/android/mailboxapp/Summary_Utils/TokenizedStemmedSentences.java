package com.cognition.android.mailboxapp.Summary_Utils;

import java.util.ArrayList;

//Contains arrayList of all the tokenized stemmed sentences with scores
public class TokenizedStemmedSentences {
    
    public static ArrayList<SentenceTokensScores>sentences;       //contains all sentences tokens and sentence numbers and scores    
    public static ArrayList<String>subjectTokens;
    
    public TokenizedStemmedSentences(int numberOfSentences)
    {
        this.sentences = new ArrayList<SentenceTokensScores>(numberOfSentences);
        this.subjectTokens = new ArrayList<String>();
    }
}
