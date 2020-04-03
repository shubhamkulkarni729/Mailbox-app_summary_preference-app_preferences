package com.cognition.android.mailboxapp.Summary_Utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import com.cognition.android.mailboxapp.R;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class Preprocessor {
    
    String originalMail , subject;
    String[] sentences;
    String[] tokens = null , tokensSubject = null;
    //String sentenceDetectionFilePath = "C:\\Users\\Arnav Desai\\AndroidStudioProjects\\Mailbox\\app\\src\\main\\assets\\en-sent.bin";
    //String tokenizerFilePath = "C:\\Users\\Arnav Desai\\AndroidStudioProjects\\Mailbox\\app\\src\\main\\assets\\en-token.bin";
    //String dateFilePath = "C:\\Users\\Arnav Desai\\AndroidStudioProjects\\Mailbox\\app\\src\\main\\assets\\en-ner-date.bin";
    int totalTokens = 0;

    ArrayList<String>stopwords = new ArrayList(Arrays.asList("a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "can", "can't", "cannot", "cause", "causes", "certain", "certainly", "changes", "clearly","concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldn't", "currently", "dear", "definitely", "despite", "did", "didn't", "different", "do", "does", "doesn't", "doing", "don't", "done", "down", "downwards", "during", "each", "e.g.","either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc.", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere","exactly", "example", "except", "far", "few", "followed", "following", "follows", "for", "former", "formerly", "forth","from", "further", "furthermore", "get", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadn't", "happens", "hardly", "has", "hasn't", "have", "haven't", "having", "he", "hello", "help", "hence", "her", "here", "here's", "hereafter", "hereby", "herein", "hereupon", "her's", "herself", "hi", "him", "himself", "his", "hey", "hello", "hi", "hither", "hopefully", "how", "howbeit", "however", "i", "i'm", "i've", "i.e.", "if", "ignored", "immediate","immediately", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isn't", "it", "it'd", "it'll", "its", "it's", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "let's", "like", "liked", "likely", "little", "look", "looking", "looks", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must","mustn't", "my", "myself", "name", "namely", "nearly","nearby", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next","no", "nobody", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once","ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "quite", "qv", "rather", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously","several", "shall","shan't", "she", "should", "shouldn't", "since", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "take", "taken", "tell", "tends", "than", "thank", "thanks", "thanx", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "there's","theirs", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "they'd", "they'll", "they're", "they've", "think", "this", "thorough", "thoroughly", "those", "though", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz.","viz", "vs","versus", "want", "wants", "was", "wasn't", "way", "we", "wed", "well", "were", "we've", "welcome", "well", "went", "were", "weren't", "what", "what's", "whatever", "when", "whence", "whenever", "where", "where's", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whose","who's", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "won't", "wonder", "would", "wouldn't", "yes", "yet", "you", "you'd", "you'll", "youre", "youve", "your", "yours", "yourself", "yourselves"));
    private Context ctx;
    AssetManager a;

    public Preprocessor(String mail , String subject, Context ctx){
        this.subject = subject;
        originalMail = mail;
        this.ctx = ctx;
        a = ctx.getAssets();
    }

    //split whole mail into sentences.
    public void splitSentences() throws FileNotFoundException, IOException{
        Resources resources = ctx.getResources();

        InputStream is = resources.openRawResource(R.raw.en_sent);
        SentenceModel model = new SentenceModel(is);
        
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
 
	sentences = sentenceDetector.sentDetect(originalMail);
    }
    
    //tokenize
    public int tokenizeStem() throws FileNotFoundException, IOException{
        Resources resources = ctx.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.en_token);
        TokenizerModel tokenModel = new TokenizerModel(inputStream); 
        TokenizerME tokenizer = new TokenizerME(tokenModel);

        InputStream inputStreamDateFinder = resources.openRawResource(R.raw.en_ner_date);
        TokenNameFinderModel dateModel = new TokenNameFinderModel(inputStreamDateFinder);
        NameFinderME dateFinder = new NameFinderME(dateModel);
        
        TokenizedStemmedSentences tokenizedStemmedSentences = new TokenizedStemmedSentences(sentences.length);
       
        
        //Preprocessing subject
        tokens = tokenizer.tokenize(subject);
        
        for(int j=0;j<tokens.length;j++)
            {
                if(!stopwords.contains(tokens[j].toLowerCase()) && tokens[j].length()>1)    //if token is not a stopword then add to new sentence.
                {
                    //String token = porterStemmer.stem(tokens[j]);      //stemming on non stopwords
                    TokenizedStemmedSentences.subjectTokens.add(tokens[j]); //adding token to arraylist of sentencetokensscores class
                }
            }
        
        //tokenizing each sentence of the sentences string array (mail body)
        for(int i=0;i<sentences.length;i++) {
            tokens = tokenizer.tokenize(sentences[i]);
            Span[] dates = dateFinder.find(tokens);
            
            SentenceTokensScores sentenceTokensScores = new SentenceTokensScores(i+1);
                        
            for(int j=0;j<tokens.length;j++)
            {
                if(!stopwords.contains(tokens[j].toLowerCase()) && tokens[j].length()>1)    //if token is not a stopword then add to new sentence.
                {
                    totalTokens++;
                    //String token = porterStemmer.stem(tokens[j]);      //stemming on non stopwords
                    sentenceTokensScores.tokens.add(tokens[j]);     //adding token to arraylist of sentencetokensscores class 
                    //System.out.println(token);
                }
            }
            sentenceTokensScores.dateTimeFeature = dates.length*0.4;
            tokenizedStemmedSentences.sentences.add(sentenceTokensScores);
        }
        
        //checking number of total tokens
        if(totalTokens<150) 
        {
            return 1;
        }   
        return 0;
    }
}
 
