package com.cognition.android.mailboxapp.Summary_Utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import com.cognition.android.mailboxapp.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.util.Span;

import static java.lang.Math.*;

public class CalculateScores {
    
    private ArrayList<SentenceTokensScores>allSentences;
    private ArrayList<String>subject ;
    private ArrayList<String>numbers;
    private ArrayList<String>domains;
    private HashMap<String,Integer>wordScores ;
    private String regexNumber;
    private int subjectSize,domainSize;
    private NameFinderME dateFinder;
    private POSTaggerME posTagger;
    
    public CalculateScores(Context ctx) throws FileNotFoundException, IOException
    {
        AssetManager a = ctx.getAssets();
        this.numbers = new ArrayList(Arrays.asList("one","two","three","four","five","six","seven","eight","nine","ten"));
        this.regexNumber = new String("^\\d+$");
        this.domains = new ArrayList(Arrays.asList(".com",".edu",".gov",".net",".org",".in",".us",".uk"));
        allSentences = new ArrayList();
        allSentences = TokenizedStemmedSentences.sentences;
        //System.out.println(allSentences.get(0));

        subject = new ArrayList();
        subject = TokenizedStemmedSentences.subjectTokens;
        this.subjectSize = subject.size();
        this.domainSize = domains.size();

        //String dateFilePath = "C:\\Users\\Arnav Desai\\AndroidStudioProjects\\Mailbox\\app\\src\\main\\assets\\en-ner-date.bin";
        Resources resources = ctx.getResources();
        InputStream inputStreamDateFinder = resources.openRawResource(R.raw.en_ner_date);
        TokenNameFinderModel dateModel = new TokenNameFinderModel(inputStreamDateFinder);
        dateFinder = new NameFinderME(dateModel);

        InputStream inputStreamPosTagger = resources.openRawResource(R.raw.en_pos_maxent);
        POSModel posModel = new POSModel(inputStreamPosTagger);
        posTagger = new POSTaggerME(posModel);

        ArrayList<String>tokens = new ArrayList();
        wordScores = new HashMap<String,Integer>();

        for(int i=0;i<allSentences.size();i++)
        {
            tokens.addAll(allSentences.get(i).tokens);
        }

        for(int i=0;i<tokens.size();i++)
        {
            String currentToken = tokens.get(i);
            if(wordScores.containsKey(currentToken))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wordScores.replace(currentToken,wordScores.get(currentToken)+1);
                }
            }
            else
                wordScores.put(currentToken, 1);
        }
    }
    
    public void calculate() throws IOException
    {
        for(int i=0;i<allSentences.size();i++)
        {
            calculateTitleFeature(allSentences.get(i));
            calculateNumericalDataFeature(allSentences.get(i));
            calculateLinkFeature(allSentences.get(i));
            calculateDateTimeFeature(allSentences.get(i));
            calculateProperNounsFeature(allSentences.get(i));
            calculateTopicalWordsFeature(allSentences.get(i));
            
            allSentences.get(i).setTotal();
        }
    }
    
    private void calculateTitleFeature(SentenceTokensScores sentence)
    {
        //Stemming and then matching
        PorterStemmer porterStemmer = new PorterStemmer();
        int numberOfMatchingWords = 0;
        //System.out.println(sentence.tokens);
        for(int i=0;i<subjectSize;i++)     
        {
            String subjectToken = porterStemmer.stem(subject.get(i));
            for(int j=0; j<sentence.tokens.size();j++)
            {
                if(porterStemmer.stem(sentence.tokens.get(j)).contains(subjectToken))
                {
                    numberOfMatchingWords++;
                }
            }
        }
        //System.out.println(subjectSize);
        sentence.titleFeature = (double) numberOfMatchingWords /subjectSize;
        //System.out.println(sentence.titleFeature);
    }
    
    private void calculateNumericalDataFeature(SentenceTokensScores sentence)       //counting numerical data values in the sentence
    {
        int numericalData = 0;
        for(int i=0;i<sentence.tokens.size();i++)
        {
            String token = sentence.tokens.get(i);
            if(token.matches(regexNumber) || numbers.contains(token)) numericalData++;
        }
        Double numericalDataDouble = (double) numericalData /10;
        sentence.numericalDataFeature = numericalDataDouble;
    }
    
    private void calculateLinkFeature(SentenceTokensScores sentence)
    {
        int flag = 0;
        for(int i=0;i<sentence.tokens.size();i++)
        {
            String token = sentence.tokens.get(i);
            for(int j=0;j<domainSize;j++)
            {
                if(token.contains(domains.get(j)))    
                {
                    sentence.linkFeature = 0.4; flag=1; break; 
                }
            }
            if(flag==1) break;
        }
        //System.out.println(sentence.linkFeature);
    }
    
    private void calculateDateTimeFeature(SentenceTokensScores sentence)
    {
        for(int i=0;i<sentence.tokens.size();i++)
        {
            String token = sentence.tokens.get(i);
            if(token.compareToIgnoreCase("A.M.")==0 || token.compareToIgnoreCase("P.M.")==0 || token.compareToIgnoreCase("AM")==0 || token.compareToIgnoreCase("PM")==0)
            {
                sentence.dateTimeFeature = sentence.dateTimeFeature+0.4;
                break;
            }   
        }
        
        Object[] array = sentence.tokens.toArray();
        String[] tokens = Arrays.copyOf(array, array.length,String[].class);
        Span[] dates = dateFinder.find(tokens);
        
        String[] datesArray=Span.spansToStrings(dates, tokens);
        
        if(datesArray.length>0)
        {
            sentence.dateTimeFeature = sentence.dateTimeFeature+0.4;
        }
    }

    private void calculateProperNounsFeature(SentenceTokensScores sentence) {
       Object[] array = sentence.tokens.toArray();
       String[] tokens = Arrays.copyOf(array, array.length,String[].class);
       String[] tags = posTagger.tag(tokens);
       
       for(int i=0;i<tags.length;i++)
       {
           if(tags[i].contains("NNP")) sentence.properNounFeature+=0.4; 
       }
    }

    private void calculateTopicalWordsFeature(SentenceTokensScores sentence) {
    
        Double score = 0.0;
        
        for(int i=0;i<sentence.tokens.size();i++)
        {
            score+= (wordScores.get(sentence.tokens.get(i)));
        }

        sentence.topicalWordFeature = round((score/allSentences.size()) * 100.0) / 100.0;
        //System.out.println(sentence.tokens+"   "+sentence.topicalWordFeature);
    }
    
    public ArrayList<Integer> getTopSentences(){
        
        //creating copy of original sentences to topsentences.
        ArrayList<SentenceTokensScores>topSentences = new ArrayList<SentenceTokensScores>();
        for(int i=0;i<allSentences.size();i++)
        {
            topSentences.add(allSentences.get(i));
        }
        
        //sorting based on total scores
        SentenceTokensScores temp = new SentenceTokensScores(0);
        for(int i=0;i<topSentences.size();i++)
        {
            //System.out.print("aaaaaa");
            for(int j=i+1;j<topSentences.size();j++)
            {
                if(topSentences.get(i).total<topSentences.get(j).total)
                {
                    //swap
                    temp = topSentences.get(i);
                    topSentences.set(i, topSentences.get(j));
                    topSentences.set(j, temp);
                }
            }    
        }
        //getting top size/2 sentences in topsentences1
        List<SentenceTokensScores>topSentences1 = topSentences.subList(0,allSentences.size()/2);
        
        //copying top sentence positions to summaryPositions
        ArrayList<Integer>summaryPositions = new ArrayList<Integer>();
        for(int i=0;i<topSentences1.size();i++)
        {   //-1 since counting from 0 and not from 1
            summaryPositions.add(topSentences1.get(i).sentencePosition-1);
        }
        //sorting summaryPositions ascending
        Collections.sort(summaryPositions);

        return summaryPositions;
        
    }
}