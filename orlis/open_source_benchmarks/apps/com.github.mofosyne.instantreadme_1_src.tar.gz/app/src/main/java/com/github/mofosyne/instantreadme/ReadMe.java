package com.github.mofosyne.instantreadme;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadMe extends AppCompatActivity {

    TextView tv;
    boolean markdownEnable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        Creating the layout programmically http://stackoverflow.com/questions/4979212/programmatically-creating-a-relativelayout-in-android
        */

        // Creating a new RelativeLayout
        RelativeLayout relativeLayout = new RelativeLayout(this);
        // Defining the RelativeLayout layout parameters.
        // In this case I want to fill its parent
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        // Creating a new TextView
        tv = new TextView(this);
        tv.setText("Empty...");
        tv.setTextIsSelectable(true);
        tv.setAutoLinkMask(Linkify.ALL); // List of possible mask value https://developer.android.com/reference/android/text/util/Linkify.html#ALL

        // Defining the layout parameters of the TextView
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Setting the parameters on the TextView
        tv.setLayoutParams(lp);

        // Adding the TextView to the RelativeLayout as a child
        relativeLayout.addView(tv);

        // Setting the RelativeLayout as our content view
        setContentView(relativeLayout, rlp);

        /* Show the return/close button in actionbar */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        /*
         Load Readme and Style it!
         This essentially runs the text loader, and push it though a text styler.
          */
        if (markdownEnable) {
            updateMainDisplay(readTxt());
        } else{
            // PLAIN TEXT DISPLAY ONLY
            // TextView mTextView = (TextView) findViewById(R.id.readme_info);
            TextView mTextView = tv;
            // Let's update the main display
            mTextView.setText(readTxt(), TextView.BufferType.SPANNABLE);
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /*
        TEXT LOADER
        This load the text from the resource folder
     */

    // Using http://android-er.blogspot.com.au/2010/07/display-text-file-in-resraw_01.html
    private String readTxt(){

        InputStream inputStream = getResources().openRawResource(R.raw.readme);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int i;
        try {
            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toString();
    }




    /*
        Text Styler
        A crappy psudo markdown styler. Could do with a total revamp.
     */

    /*
    * Styling the textview for easier readability
    * */
    private void updateMainDisplay(String text) {
        //TextView mTextView = (TextView) findViewById(R.id.readme_info);
        TextView mTextView = tv;

        // Let's update the main display
        // Needs to set as spannable otherwise http://stackoverflow.com/questions/16340681/fatal-exception-string-cant-be-cast-to-spannable
        mTextView.setText(text, TextView.BufferType.SPANNABLE);
        // Let's prettify it!
        changeLineinView_TITLESTYLE(mTextView, "# ", 0xfff4585d, 2f); // Primary Header
        changeLineinView(mTextView, "\n# ", 0xFFF4A158, 1.5f); // Secondary Header
        changeLineinView(mTextView, "\n## ", 0xFFF4A158, 1.2f); // Secondary Header
        changeLineinView(mTextView, "\n---", 0xFFF4A158, 1.2f); // Horizontal Rule
        changeLineinView(mTextView, "\n>",   0xFF89e24d, 0.9f); // Block Quotes
        changeLineinView(mTextView, "\n - ", 0xFFA74DE3, 1f);   // Classic Markdown List
        changeLineinView(mTextView, "\n- ", 0xFFA74DE3, 1f);   // NonStandard List

        //spanSetterInView(String startTarget, String endTarget, int typefaceStyle, String fontFamily,TextView tv, int colour, float size)
        // Limitation of spanSetterInView. Well its not a regular expression... so can't exactly have * list, and *bold* at the same time.
        spanSetterInView(mTextView, "\n```\n", "\n```\n",   Typeface.BOLD,        "monospace",  0xFF45c152,  0.8f, false); // fenced code Blocks ( endAtLineBreak=false since this is a multiline block operator)
        spanSetterInView(mTextView,   " **"  ,     "** ",   Typeface.BOLD,        "",  0xFF89e24d,  1f, true); // Bolding
        spanSetterInView(mTextView,    " *"  ,      "* ",   Typeface.ITALIC,      "",  0xFF4dd8e2,  1f, true); // Italic
        spanSetterInView(mTextView,  " ***"  ,    "*** ",   Typeface.BOLD_ITALIC, "",  0xFF4de25c,  1f, true); // Bold and Italic
        spanSetterInView(mTextView,    " `"  ,      "` ",   Typeface.BOLD,        "monospace",  0xFF45c152,  0.8f, true); // inline code
        spanSetterInView(mTextView, "\n    " ,      "\n",   Typeface.BOLD,        "monospace",  0xFF45c152,  0.7f, true); // classic indented code
    }

    private void changeLineinView(TextView tv, String target, int colour, float size) {
        String vString = (String) tv.getText().toString();
        int startSpan = 0, endSpan = 0;
        //Spannable spanRange = new SpannableString(vString);
        Spannable spanRange = (Spannable) tv.getText();
        while (true) {
            startSpan = vString.indexOf(target, endSpan-1);     // (!@#$%) I want to check a character behind in case it is a newline
            endSpan = vString.indexOf("\n", startSpan+1);       // But at the same time, I do not want to read the point found by startSpan. This is since startSpan may point to a initial newline.
            ForegroundColorSpan foreColour = new ForegroundColorSpan(colour);
            // Need a NEW span object every loop, else it just moves the span
            // Fix: -1 in startSpan or endSpan, indicates that the indexOf has already searched the entire string with not valid match (Lack of endspan check, occoured because of the inclusion of endTarget, which added extra complications)
            if ( (startSpan < 0) || ( endSpan < 0 ) ) break;// Need a NEW span object every loop, else it just moves the span
            // Need to make sure that start range is always smaller than end range. (Solved! Refer to few lines above with (!@#$%) )
            if (endSpan > startSpan) {
                //endSpan = startSpan + target.length();
                spanRange.setSpan(foreColour, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Also wannna bold the span too
                spanRange.setSpan(new RelativeSizeSpan(size), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new StyleSpan(Typeface.BOLD), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tv.setText(spanRange);
    }

    private void changeLineinView_TITLESTYLE(TextView tv, String target, int colour, float size) {
        String vString = (String) tv.getText().toString();
        int startSpan = 0, endSpan = 0;
        //Spannable spanRange = new SpannableString(vString);
        Spannable spanRange = (Spannable) tv.getText();
        /*
        * Had to do this, since there is something wrong with this overlapping the "##" detection routine
        * Plus you only really need one title.
         */
        //while (true) {
        startSpan = vString.substring(0,target.length()).indexOf(target, endSpan-1); //substring(target.length()) since we only want the first line
        endSpan = vString.indexOf("\n", startSpan+1);
        ForegroundColorSpan foreColour = new ForegroundColorSpan(colour);
        // Need a NEW span object every loop, else it just moves the span
            /*
            if (startSpan < 0)
                break;
                */
        if ( !(startSpan < 0) ) { // hacky I know, but its to cater to the case where there is no header text
            // Need to make sure that start range is always smaller than end range.
            if (endSpan > startSpan) {
                //endSpan = startSpan + target.length();
                spanRange.setSpan(foreColour, startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Also wannna bold the span too
                spanRange.setSpan(new RelativeSizeSpan(size), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        //}
        tv.setText(spanRange);
    }


    private void spanSetterInView(TextView tv, String startTarget, String endTarget, int typefaceStyle, String fontFamily, int colour, float size, boolean endAtLineBreak) {
        String vString = (String) tv.getText().toString();
        int startSpan = 0, endSpan = 0;
        //Spannable spanRange = new SpannableString(vString);
        Spannable spanRange = (Spannable) tv.getText();
        while (true) {
            startSpan = vString.indexOf(startTarget, endSpan-1);     // (!@#$%) I want to check a character behind in case it is a newline
            endSpan = vString.indexOf(endTarget, startSpan+1+startTarget.length());     // But at the same time, I do not want to read the point found by startSpan. This is since startSpan may point to a initial newline. We also need to avoid the first patten matching a token from the second pattern.
            // Since this is pretty powerful, we really want to avoid overmatching it, and limit any problems to a single line. Especially if people forget to type in the closing symbol (e.g. * in bold)
            if (endAtLineBreak){
                int endSpan_linebreak = vString.indexOf("\n", startSpan+1+startTarget.length());
                if ( endSpan_linebreak < endSpan ) { endSpan = endSpan_linebreak; }
            }
            // Fix: -1 in startSpan or endSpan, indicates that the indexOf has already searched the entire string with not valid match (Lack of endspan check, occoured because of the inclusion of endTarget, which added extra complications)
            if ( (startSpan < 0) || ( endSpan < 0 ) ) break;// Need a NEW span object every loop, else it just moves the span
            // We want to also include the end "** " characters
            endSpan += endTarget.length();
            // If all is well, we shall set the styles and etc...
            if (endSpan > startSpan) {// Need to make sure that start range is always smaller than end range. (Solved! Refer to few lines above with (!@#$%) )
                spanRange.setSpan(new ForegroundColorSpan(colour), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new RelativeSizeSpan(size), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanRange.setSpan(new StyleSpan(typefaceStyle), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                // Default to normal font family if settings is empty
                if( !fontFamily.equals("") )  spanRange.setSpan(new TypefaceSpan(fontFamily), startSpan, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tv.setText(spanRange);
    }
}
