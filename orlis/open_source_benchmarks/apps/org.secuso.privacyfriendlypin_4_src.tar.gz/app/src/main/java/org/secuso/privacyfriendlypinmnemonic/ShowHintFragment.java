package org.secuso.privacyfriendlypinmnemonic;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.secuso.privacyfriendlypin.R;
import org.secuso.privacyfriendlypinmnemonic.pinhelpers.CheckPin;
import org.secuso.privacyfriendlypinmnemonic.pinhelpers.DrawView;

public class ShowHintFragment extends Fragment {

    private int[] input = new int[4];
    private String practicePIN;
    Activity activity;
    View rootView;
    boolean securityReset;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        this.securityReset = false;

        container.removeAllViews();
        View rootView = inflater.inflate(R.layout.fragment_show_hint, container, false);
        this.rootView = rootView;

        doFirstRun();

        ((ActionBarActivity) getActivity()).getSupportActionBar().setSubtitle(R.string.action_pin_mnemonics);

        Bundle bundle = this.getArguments();

        String pin = "";

        if (bundle != null) {
            pin = bundle.getString("pin");
            practicePIN = pin;
        }

        //secure against Screenshot
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        //Buttons
        final Button[] numpad = new Button[10];
        numpad[0] = (Button) rootView.findViewById(R.id.button_zero_hint);
        numpad[1] = (Button) rootView.findViewById(R.id.button_one_hint);
        numpad[2] = (Button) rootView.findViewById(R.id.button_two_hint);
        numpad[3] = (Button) rootView.findViewById(R.id.button_three_hint);
        numpad[4] = (Button) rootView.findViewById(R.id.button_four_hint);
        numpad[5] = (Button) rootView.findViewById(R.id.button_five_hint);
        numpad[6] = (Button) rootView.findViewById(R.id.button_six_hint);
        numpad[7] = (Button) rootView.findViewById(R.id.button_seven_hint);
        numpad[8] = (Button) rootView.findViewById(R.id.button_eight_hint);
        numpad[9] = (Button) rootView.findViewById(R.id.button_nine_hint);

        Button dummyButtonLeft = (Button) rootView.findViewById(R.id.button_zero_space_left);
        Button dummyButtonRight = (Button) rootView.findViewById(R.id.button_zero_space_right);

        SpannableString dummyString = new SpannableString(" 0 \n +");
        dummyString.setSpan(new RelativeSizeSpan(1.8f), 0, 2, 0);

        dummyButtonLeft.setText(dummyString);
        dummyButtonRight.setText(dummyString);

        SpannableString[] spannables = createSetSpannables();

        for (int i=0; i<spannables.length; i++) {
            spannables[i].setSpan(new RelativeSizeSpan(1.8f), 0, 2, 0);
            numpad[i].setText(spannables[i]);
        }

        Button toPractiseButton = (Button) rootView.findViewById(R.id.practiseButton);
        toPractiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickPractiseButton();
            }
        });

        for (int i = 0; i < 4; i++) {
            input[i] = Integer.parseInt(Character.toString(pin.charAt(i)));
        }

        int[] multiples = containsMultiples(input);

        for (int j = 0; j < input.length; j++) {
            colorNumpad(multiples[j], numpad[input[j]]);
        }

        for (int i = 0; i < 4; i++) {
            if (i < 3) {
                drawArrow(numpad[input[i]], numpad[input[i + 1]], input[i], input[i + 1]);
            }
        }

        CheckPin checkPin = new CheckPin(pin, activity.getBaseContext());

        TextView wordTextView = (TextView) rootView.findViewById(R.id.word);
        TextView dateFrameTextView = (TextView) rootView.findViewById(R.id.date);
        TextView mathFrameTextView = (TextView) rootView.findViewById(R.id.math);
        TextView pinTextView = (TextView) rootView.findViewById(R.id.pin);

        pinTextView.setText(pin);

        wordTextView.setText(checkPin.determineWord());
        dateFrameTextView.setText(checkPin.determineDate());

        mathFrameTextView.setText(checkPin.determineCalculation());

        ImageView[] symbolRow = new ImageView[4];
        symbolRow[0] = (ImageView) rootView.findViewById(R.id.symbolImageView1);
        symbolRow[1] = (ImageView) rootView.findViewById(R.id.symbolImageView2);
        symbolRow[2] = (ImageView) rootView.findViewById(R.id.symbolImageView3);
        symbolRow[3] = (ImageView) rootView.findViewById(R.id.symbolImageView4);

        for (int i=0; i<symbolRow.length; i++) {
            assignSymbol(input[i], symbolRow[i]);
        }

        return rootView;

    }

    /**
     *
     * @param first button where arrow starts
     * @param second button where arrow finishes
     * @param digitOne digit on first button
     * @param digitTwo digit on second button
     */
    public void drawArrow(Button first, Button second, int digitOne, int digitTwo) {
        DrawView drawView = new DrawView(activity, first, second, digitOne, digitTwo);

        Display display = activity.getWindowManager().getDefaultDisplay();
        int temp = display.getWidth() / 80;
        drawView.setStrokeWidth(temp);
        RelativeLayout numpadLayout = (RelativeLayout) rootView.findViewById(R.id.numpadFrame);
        numpadLayout.addView(drawView);
    }

    public int[] containsMultiples(int[] input) {

        int[] multiples = new int[4];

        for (int i = 0; i < multiples.length; i++) {
            multiples[i] = 1;
        }

        for (int j = 0; j < input.length; j++) {
            for (int k = 0; k < input.length; k++) {
                if (k == j) {
                    continue;
                } else if (input[j] == input[k]) {
                    multiples[j]++;
                }
            }
        }
        for (int l = 0; l < input.length; l++) {
            System.out.println("Input Array: " + Integer.toString(input[l]));
            System.out.println("Multiples Array: " + Integer.toString(multiples[l]));
        }
        return multiples;
    }

    public void clickPractiseButton() {

        Bundle bundle = new Bundle();
        bundle.putString("pin", practicePIN);

        PractiseFragment practiseFragment = new PractiseFragment();
        practiseFragment.setArguments(bundle);

        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, practiseFragment, "PractiseFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void doFirstRun() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedPreferences.edit().putString("firstShow", "").commit();
        SharedPreferences settings = activity.getSharedPreferences("firstShow", activity.getBaseContext().MODE_PRIVATE);
        if (settings.getBoolean("isFirstRun", true)) {
            ((RootActivity)getActivity()).tutorialDialogMnemonics();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("isFirstRun", false);
        editor.commit();
         }
    }



    public SpannableString[] createSetSpannables() {

        SpannableString[] spannables = new SpannableString[10];

        spannables[0] = new SpannableString(" 0 \n +");
        spannables[1] = new SpannableString(" 1 \n ");
        spannables[2] = new SpannableString(" 2 \n abc");
        spannables[3] = new SpannableString(" 3 \n def");
        spannables[4] = new SpannableString(" 4 \n ghi");
        spannables[5] = new SpannableString(" 5 \n jkl");
        spannables[6] = new SpannableString(" 6 \n mno");
        spannables[7] = new SpannableString(" 7 \n pqrs");
        spannables[8] = new SpannableString(" 8 \n tuv");
        spannables[9] = new SpannableString(" 9 \n wxyz");

        return spannables;
    }

    public void colorNumpad(int multiple, Button button) {

        switch (multiple) {
            case 1:
                button.setBackgroundResource(R.drawable.numpad_highlighted);
                //System.out.println(Integer.toString(multiple) + " BLUE");
                break;
            case 2:
                button.setBackgroundResource(R.drawable.numpad_highlighted_two);
                //System.out.println(Integer.toString(multiple) + " YELLOW");
                break;
            case 3:
                button.setBackgroundResource(R.drawable.numpad_highlighted_three);
                //System.out.println(Integer.toString(multiple) + " BLACK");
                break;
            case 4:
                button.setBackgroundResource(R.drawable.numpad_highlighted_four);
                //System.out.println(Integer.toString(multiple) + " WHITE");
                break;
            default:
                button.setBackgroundResource(R.drawable.mnemonic_numpad_button);
                break;
        }
    }

    public void assignSymbol(int digit, ImageView imageView) {
        switch (digit) {
            case 0:
                imageView.setImageResource(R.drawable.story_zero);
                break;
            case 1:
                imageView.setImageResource(R.drawable.story_one);
                break;
            case 2:
                imageView.setImageResource(R.drawable.story_two);
                break;
            case 3:
                imageView.setImageResource(R.drawable.story_three);
                break;
            case 4:
                imageView.setImageResource(R.drawable.story_four);
                break;
            case 5:
                imageView.setImageResource(R.drawable.story_five);
                break;
            case 6:
                imageView.setImageResource(R.drawable.story_six);
                break;
            case 7:
                imageView.setImageResource(R.drawable.story_seven);
                break;
            case 8:
                imageView.setImageResource(R.drawable.story_eight);
                break;
            case 9:
                imageView.setImageResource(R.drawable.story_nine);
                break;
            default:
                imageView.setImageResource(0);
                break;
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    public void onResume() {
        super.onResume();
        if (securityReset) {
            ((RootActivity)getActivity()).guiClearDialog();
        }
    }

    public void onPause() {
        super.onPause();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            public void run() {
                securityReset = true;
            }
        }, 300000);
    }

}
