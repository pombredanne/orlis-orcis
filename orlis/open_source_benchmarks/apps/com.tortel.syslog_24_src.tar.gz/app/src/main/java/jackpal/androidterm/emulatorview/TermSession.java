/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jackpal.androidterm.emulatorview;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import android.os.Handler;
import android.os.Message;

import com.tortel.syslog.utils.Log;

import eu.chainfire.libsuperuser.Shell;

/**
 * A terminal session, consisting of a VT100 terminal emulator and its
 * input and output streams.
 * <p>
 * You need to supply an {@link InputStream} and {@link OutputStream} to
 * provide input and output to the terminal.  For a locally running
 * program, these would typically point to a tty; for a telnet program
 * they might point to a network socket.  Reader and writer threads will be
 * spawned to do I/O to these streams.  All other operations, including
 * processing of input and output in {@link #processInput processInput} and
 * {@link #write(byte[], int, int) write}, will be performed on the main thread.
 * <p>
 * Call {@link #setTermIn} and {@link #setTermOut} to connect the input and
 * output streams to the emulator.  When all of your initialization is
 * complete, your initial screen size is known, and you're ready to
 * start VT100 emulation, call {@link #initializeEmulator} or {@link
 * #updateSize} with the number of rows and columns the terminal should
 * initially have.  (If you attach the session to an {@link EmulatorView},
 * the view will take care of setting the screen size and initializing the
 * emulator for you.)
 * <p>
 * When you're done with the session, you should call {@link #finish} on it.
 * This frees emulator data from memory, stops the reader and writer threads,
 * and closes the attached I/O streams.
 */
public class TermSession {
    private ColorScheme mColorScheme = BaseTextRenderer.defaultColorScheme;
    private UpdateCallback mNotify;

    private String mTitle;

    private TranscriptScreen mTranscriptScreen;
    private TerminalEmulator mEmulator;

    private boolean mDefaultUTF8Mode = true;

    private Thread mReaderThread;

    // Number of rows in the transcript
    private static final int TRANSCRIPT_ROWS = 500;

    private static final int NEW_INPUT = 1;

    private Shell.Interactive shell;
    private static final String NEW_LINE = "line";

    /**
     * Callback to be invoked when a {@link TermSession} finishes.
     *
     * @see TermSession#setUpdateCallback
     */
    public interface FinishCallback {
        /**
         * Callback function to be invoked when a {@link TermSession} finishes.
         *
         * @param session The <code>TermSession</code> which has finished.
         */
        void onSessionFinish(TermSession session);
    }
    private FinishCallback mFinishCallback;

    private boolean mIsRunning = false;
    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mIsRunning) {
                return;
            }
            if (msg.what == NEW_INPUT) {
                newLineFromProcess(msg.getData().getString(NEW_LINE));
            }
        }
    };

    private UpdateCallback mTitleChangedListener;

    public TermSession() {
        mReaderThread = new Thread() {

            @Override
            public void run() {
                try {
                    Shell.Builder builder = new Shell.Builder();
                    builder.useSU();
                    builder.setOnSTDOUTLineListener(new Shell.OnCommandLineListener() {
                        @Override
                        public void onCommandResult(int commandCode, int exitCode) {
                            sendLine("Logcat process ended.");
                        }

                        @Override
                        public void onLine(String line) {
                            sendLine(line);
                        }

                        /**
                         * Send a line to be printed. A newline will be added to the line automatically
                         * @param line
                         */
                        private void sendLine(String line){
                            Message message = mMsgHandler.obtainMessage(NEW_INPUT);
                            message.getData().putString(NEW_LINE, line + "\n\r");

                            mMsgHandler.sendMessage(message);
                        }
                    });
                    shell = builder.open();
                    shell.addCommand("logcat");
                } catch(Exception e){
                    Log.e("Some exception occured when starting logcat thread", e);
                    Message message = mMsgHandler.obtainMessage(NEW_INPUT);
                    message.getData().putString(NEW_LINE, "Exception starting logcat thread");
                }
            }
        };
        mReaderThread.setName("TermSession input reader");
    }

    /**
     * Set the terminal emulator's window size and start terminal emulation.
     *
     * @param columns The number of columns in the terminal window.
     * @param rows The number of rows in the terminal window.
     */
    public void initializeEmulator(int columns, int rows) {
        mTranscriptScreen = new TranscriptScreen(columns, TRANSCRIPT_ROWS, rows, mColorScheme);
        mEmulator = new TerminalEmulator(this, mTranscriptScreen, columns, rows, mColorScheme);
        mEmulator.setDefaultUTF8Mode(mDefaultUTF8Mode);

        mIsRunning = true;
        mReaderThread.start();
    }

    /**
     * Write data to the terminal output.  The written data will be consumed by
     * the emulation client as input.
     * <p>
     * <code>write</code> itself runs on the main thread.  The default
     * implementation writes the data into a circular buffer and signals the
     * writer thread to copy it from there to the {@link OutputStream}.
     * <p>
     * Subclasses may override this method to modify the output before writing
     * it to the stream, but implementations in derived classes should call
     * through to this method to do the actual writing.
     *
     * @param data An array of bytes to write to the terminal.
     * @param offset The offset into the array at which the data starts.
     * @param count The number of bytes to be written.
     */
    public void write(byte[] data, int offset, int count) {
        // Not using
    }

    /**
     * Write the UTF-8 representation of a String to the terminal output.  The
     * written data will be consumed by the emulation client as input.
     * <p>
     * This implementation encodes the String and then calls
     * {@link #write(byte[], int, int)} to do the actual writing.  It should
     * therefore usually be unnecessary to override this method; override
     * {@link #write(byte[], int, int)} instead.
     *
     * @param data The String to write to the terminal.
     */
    public void write(String data) {
        // Not using
    }

    /**
     * Write the UTF-8 representation of a single Unicode code point to the
     * terminal output.  The written data will be consumed by the emulation
     * client as input.
     * <p>
     * This implementation encodes the code point and then calls
     * {@link #write(byte[], int, int)} to do the actual writing.  It should
     * therefore usually be unnecessary to override this method; override
     * {@link #write(byte[], int, int)} instead.
     *
     * @param codePoint The Unicode code point to write to the terminal.
     */
    public void write(int codePoint) {
        // Not using
    }

    /**
     * @return Whether the terminal emulation is currently running.
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    public void stopLogcat() {
        if(shell != null && shell.isRunning()){
            shell.kill();
        }
    }

    TranscriptScreen getTranscriptScreen() {
        return mTranscriptScreen;
    }

    TerminalEmulator getEmulator() {
        return mEmulator;
    }

    /**
     * Set an {@link UpdateCallback} to be invoked when the terminal emulator's
     * screen is changed.
     *
     * @param notify The {@link UpdateCallback} to be invoked on changes.
     */
    public void setUpdateCallback(UpdateCallback notify) {
        mNotify = notify;
    }

    /**
     * Notify the {@link UpdateCallback} registered by {@link
     * #setUpdateCallback setUpdateCallback} that the screen has changed.
     */
    protected void notifyUpdate() {
        if (mNotify != null) {
            mNotify.onUpdate();
        }
    }

    /**
     * Get the terminal session's title (may be null).
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Change the terminal session's title.
     */
    public void setTitle(String title) {
        mTitle = title;
        notifyTitleChanged();
    }

    /**
     * Set an {@link UpdateCallback} to be invoked when the terminal emulator's
     * title is changed.
     *
     * @param listener The {@link UpdateCallback} to be invoked on changes.
     */
    public void setTitleChangedListener(UpdateCallback listener) {
        mTitleChangedListener = listener;
    }

    /**
     * Notify the UpdateCallback registered for title changes, if any, that the
     * terminal session's title has changed.
     */
    protected void notifyTitleChanged() {
        UpdateCallback listener = mTitleChangedListener;
        if (listener != null) {
            listener.onUpdate();
        }
    }

    /**
     * Change the terminal's window size.  Will call {@link #initializeEmulator}
     * if the emulator is not yet running.
     * <p>
     * You should override this method if your application needs to be notified
     * when the screen size changes (for example, if you need to issue
     * <code>TIOCSWINSZ</code> to a tty to adjust the window size).  <em>If you
     * do override this method, you must call through to the superclass
     * implementation.</em>
     *
     * @param columns The number of columns in the terminal window.
     * @param rows The number of rows in the terminal window.
     */
    public void updateSize(int columns, int rows) {
        if (mEmulator == null) {
            initializeEmulator(columns, rows);
        } else {
            mEmulator.updateSize(columns, rows);
        }
    }

    /**
     * Retrieve the terminal's screen and scrollback buffer.
     *
     * @return A {@link String} containing the contents of the screen and
     *         scrollback buffer.
     */
    public String getTranscriptText() {
        return mTranscriptScreen.getTranscriptText();
    }

    /**
     * Show the process output
     */
    private void newLineFromProcess(String line) {

        // Give subclasses a chance to process the read data
        byte[] bytes = line.getBytes();
        processInput(bytes, 0, bytes.length);
        notifyUpdate();
    }

    /**
     * Process input and send it to the terminal emulator.  This method is
     * invoked on the main thread whenever new data is read from the
     * InputStream.
     * <p>
     * The default implementation sends the data straight to the terminal
     * emulator without modifying it in any way.  Subclasses can override it to
     * modify the data before giving it to the terminal.
     *
     * @param data A byte array containing the data read.
     * @param offset The offset into the buffer where the read data begins.
     * @param count The number of bytes read.
     */
    protected void processInput(byte[] data, int offset, int count) {
        mEmulator.append(data, offset, count);
    }

    /**
     * Set the terminal emulator's color scheme (default colors).
     *
     * @param scheme The {@link ColorScheme} to be used (use null for the
     *               default scheme).
     */
    public void setColorScheme(ColorScheme scheme) {
        if (scheme == null) {
            scheme = BaseTextRenderer.defaultColorScheme;
        }
        mColorScheme = scheme;
        if (mEmulator == null) {
            return;
        }
        mEmulator.setColorScheme(scheme);
    }

    /**
     * Set whether the terminal emulator should be in UTF-8 mode by default.
     * <p>
     * In UTF-8 mode, the terminal will handle UTF-8 sequences, allowing the
     * display of text in most of the world's languages, but applications must
     * encode C1 control characters and graphics drawing characters as the
     * corresponding UTF-8 sequences.
     *
     * @param utf8ByDefault Whether the terminal emulator should be in UTF-8
     *                      mode by default.
     */
    public void setDefaultUTF8Mode(boolean utf8ByDefault) {
        mDefaultUTF8Mode = utf8ByDefault;
        if (mEmulator == null) {
            return;
        }
        mEmulator.setDefaultUTF8Mode(utf8ByDefault);
    }

    /**
     * Get whether the terminal emulator is currently in UTF-8 mode.
     *
     * @return Whether the emulator is currently in UTF-8 mode.
     */
    public boolean getUTF8Mode() {
        if (mEmulator == null) {
            return mDefaultUTF8Mode;
        } else {
            return mEmulator.getUTF8Mode();
        }
    }

    /**
     * Set an {@link UpdateCallback} to be invoked when the terminal emulator
     * goes into or out of UTF-8 mode.
     *
     * @param utf8ModeNotify The {@link UpdateCallback} to be invoked.
     */
    public void setUTF8ModeUpdateCallback(UpdateCallback utf8ModeNotify) {
        if (mEmulator != null) {
            mEmulator.setUTF8ModeUpdateCallback(utf8ModeNotify);
        }
    }

    /**
     * Reset the terminal emulator's state.
     */
    public void reset() {
        mEmulator.reset();
        notifyUpdate();
    }

    /**
     * Set a {@link FinishCallback} to be invoked once this terminal session is
     * finished.
     *
     * @param callback The {@link FinishCallback} to be invoked on finish.
     */
    public void setFinishCallback(FinishCallback callback) {
        mFinishCallback = callback;
    }

    /**
     * Finish this terminal session.  Frees resources used by the terminal
     * emulator and closes the attached <code>InputStream</code> and
     * <code>OutputStream</code>.
     */
    public void finish() {
        stopLogcat();
        mIsRunning = false;
        mEmulator.finish();
        if (mTranscriptScreen != null) {
            mTranscriptScreen.finish();
        }

        if (mFinishCallback != null) {
            mFinishCallback.onSessionFinish(this);
        }
    }
}
