package com.cognition.android.mailboxapp.Summary_Utils;

import android.content.Context;
import android.content.res.Resources;
import com.cognition.android.mailboxapp.R;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class Summary {
    
    public static String mail;
    public static String subject;
    //static String sentenceDetectorFilePath = "C:\\Users\\Arnav Desai\\AndroidStudioProjects\\Mailbox\\app\\src\\main\\assets\\en-sent.bin";
    static SentenceDetectorME sentenceDetector;
    private static File file;

    public static String summarize(String mail, String subject, Context ctx) throws IOException {

        Summary.mail = mail;
        Summary.subject = subject;
        Preprocessor preProcessor = new Preprocessor(mail, subject, ctx);
        preProcessor.splitSentences();
        int flag = preProcessor.tokenizeStem();
        if(flag==1) return mail;     //for summary generation condition
        
        CalculateScores calculateScores = new CalculateScores(ctx);
        calculateScores.calculate();

        ArrayList<Integer>summaryPositions;
        //getting sentences positions which will constitute the summary
        summaryPositions = calculateScores.getTopSentences();
        
        //sentences detection model loading
        prepareSentenceDetectionModel(ctx);
	    String[] sentences = sentenceDetector.sentDetect(mail);
        
        String summary = "";
        for(int i=0;i<summaryPositions.size();i++)
        {
            summary = summary.concat(sentences[summaryPositions.get(i)]);
        }
        
        return summary;
    }    

    private static void prepareSentenceDetectionModel(Context ctx) throws IOException {
        Resources resources = ctx.getResources();
        InputStream is = resources.openRawResource(R.raw.en_sent);
        SentenceModel model = new SentenceModel(is);
        sentenceDetector = new SentenceDetectorME(model);
    }
}