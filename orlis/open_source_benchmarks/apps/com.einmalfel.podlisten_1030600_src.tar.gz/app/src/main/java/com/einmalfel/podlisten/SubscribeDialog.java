package com.einmalfel.podlisten;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.nononsenseapps.filepicker.FilePickerActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class SubscribeDialog extends AppCompatDialogFragment implements View.OnClickListener {
  private static final String TAG = "SDF";
  private static final String URL_ARG = "com.einmalfel.podlisten.url_arg";
  private static final int SEARCH_REQUEST_ID = 1;
  private static final int FILE_REQUEST_ID = 0;
  private static final String FRAG_TAG = "subscribe dialog";

  private EditText urlText;
  private Provider.RefreshMode refreshMode = Provider.RefreshMode.WEEK;

  static void show(@Nullable Uri uri, @NonNull FragmentManager fm) {
    AppCompatDialogFragment oldDialog = (AppCompatDialogFragment) fm.findFragmentByTag(FRAG_TAG);
    if (oldDialog != null) {
      oldDialog.dismiss();
    }
    SubscribeDialog newDialog = new SubscribeDialog();
    if (uri != null) {
      Bundle dialogArguments = new Bundle(1);
      dialogArguments.putString(URL_ARG, uri.toString());
      newDialog.setArguments(dialogArguments);
    }
    newDialog.show(fm, FRAG_TAG);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      switch (requestCode) {
        case FILE_REQUEST_ID:
          urlText.setText(data.getData().toString());
          break;
        case SEARCH_REQUEST_ID:
          urlText.setText(data.getStringExtra(SearchActivity.RSS_URL_EXTRA));
          break;
      }
    }
  }

  /** subscribe button onClick */
  @Override
  public void onClick(View v) {
    // Dialog may become invisible after this click. Attach snackbars to activity root view.
    final View acRoot = getActivity().getWindow().getDecorView().findViewById(R.id.tabbed_frame);

    String link = completeUrlString(urlText.getText());
    try {
      // First, try to process it as local OPML file
      InputStream inputStream = getActivity().getContentResolver().openInputStream(Uri.parse(link));
      if (inputStream == null) {
        throw new FileNotFoundException();
      }
      List<String> urls = parseOPML(inputStream);
      if (urls.isEmpty()) {
        Snackbar.make(v, R.string.subscribe_dialog_opml_empty, Snackbar.LENGTH_LONG);
        return;
      }
      int subscribed = urls.size();
      for (final String feedUrl : urls) {
        if (PodcastHelper.getInstance().trySubscribe(feedUrl, acRoot, refreshMode) == 0) {
          subscribed--;
        }
      }
      PodlistenAccount.getInstance().refresh(0);
      if (subscribed > 0) {
        Snackbar.make(
            acRoot,
            getString(R.string.subscribe_dialog_opml_subscriptions_added, subscribed, urls.size()),
            Snackbar.LENGTH_LONG).show();
        dismiss();
      } else {
        Snackbar.make(
            v, R.string.subscribe_dialog_opml_failed, Snackbar.LENGTH_LONG).show();
      }
    } catch (XmlPullParserException | IOException exception) {
      // IOException means url doesn't point to local file or content provider.
      // Parser exception means this is not well formed OPML
      // Subscribe button is only enabled if urlText contains valid url, so treat it as direct link
      // to feed
      long id = PodcastHelper.getInstance().trySubscribe(link, v, refreshMode);
      if (id != 0) {
        PodlistenAccount.getInstance().refresh(id);
        dismiss();
      }
    }
  }

  @NonNull
  private List<String> parseOPML(@NonNull InputStream inputStream)
      throws XmlPullParserException, IOException {
    List<String> result = new LinkedList<>();
    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
    parser.setInput(inputStream, null);
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
      if (parser.getEventType() == XmlPullParser.START_TAG && "outline".equals(parser.getName())) {
        String url = parser.getAttributeValue(XmlPullParser.NO_NAMESPACE, "xmlUrl");
        if (url != null) {
          result.add(url);
        }
      }
    }
    return result;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View result = inflater.inflate(R.layout.subscribe_dialog, container);

    final Button subscribeButton = (Button) result.findViewById(R.id.subscribe_button);
    subscribeButton.setOnClickListener(this);

    urlText = (EditText) result.findViewById(R.id.url_text);
    urlText.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean valid = URLUtil.isValidUrl(completeUrlString(s));
        subscribeButton.setEnabled(valid);
        subscribeButton.setAlpha(valid ? 1f : .3f);
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });
    final Bundle arguments = getArguments();
    urlText.setText(arguments == null ? "" : arguments.getString(URL_ARG, ""));


    Button searchButton = (Button) result.findViewById(R.id.search_button);
    searchButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivityForResult(new Intent(getContext(), SearchActivity.class), SEARCH_REQUEST_ID);
      }
    });

    Button openFileButton = (Button) result.findViewById(R.id.open_file_button);
    openFileButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(getContext(), FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        startActivityForResult(intent, FILE_REQUEST_ID);
      }
    });

    Spinner spinner = (Spinner) result.findViewById(R.id.refresh_mode);
    spinner.setAdapter(new ArrayAdapter<>(
        getContext(), R.layout.subscribe_spinner_item, Provider.RefreshMode.values()));
    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        refreshMode = (Provider.RefreshMode) parent.getItemAtPosition(position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        Log.e(TAG, "Nothing selected in spinner", new AssertionError());
      }
    });
    // colorize little triangle selector on the right side of spinner
    Drawable spinnerDrawable = spinner.getBackground().getConstantState().newDrawable();
    spinnerDrawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.accent_primary),
                                   PorterDuff.Mode.SRC_ATOP);
    spinner.setBackground(spinnerDrawable);

    getDialog().setTitle(R.string.subscribe_dialog_title);
    return result;
  }

  private static final Pattern URL_SCHEME = Pattern.compile(".*://.*");

  @NonNull
  private static String completeUrlString(@NonNull CharSequence sequence) {
    if (sequence.toString().trim().isEmpty()) {
      return "";
    } else {
      return (URL_SCHEME.matcher(sequence).matches() ? sequence : "http://" + sequence).toString();
    }
  }
}