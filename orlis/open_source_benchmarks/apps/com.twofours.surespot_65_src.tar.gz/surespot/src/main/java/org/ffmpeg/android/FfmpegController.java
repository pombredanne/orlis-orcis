package org.ffmpeg.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.ffmpeg.android.ShellUtils.ShellCallback;

import android.content.Context;
import android.util.Log;

public class FfmpegController {

	File mFileBinDir;
	Context mContext;
	private String ffmpegBin;

	private final static String TAG = "FFMPEG";

	public FfmpegController(Context context) throws FileNotFoundException, IOException {
		mContext = context;

		//copy PIE ffmpeg binary if we're on lollipop
		if (android.os.Build.VERSION.SDK_INT < 21) {
			checkBinary(false);
		}
		else {
			checkBinary(true);
		}

	}

	private void checkBinary(boolean pie) throws FileNotFoundException, IOException {
		mFileBinDir = mContext.getDir("bin", 0);
		File fileBin = new File(mFileBinDir, "ffmpeg" + (pie ? "pie" : ""));

		if (!fileBin.exists()) {
			BinaryInstaller bi = new BinaryInstaller(mContext, mFileBinDir);
			bi.installFromRaw(pie);
		}

		ffmpegBin = fileBin.getCanonicalPath();

	}


	private void execFFMPEG(List<String> cmd, ShellCallback sc, File fileExec) throws IOException, InterruptedException {
		String binary = "ffmpeg";
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			binary += "pie";
		}
		String ffmpegBin = new File(mFileBinDir, binary).getCanonicalPath();
		Runtime.getRuntime().exec("chmod 777 " + ffmpegBin);

		execProcess(cmd, sc, fileExec);
	}

	private void execFFMPEG(List<String> cmd, ShellCallback sc) throws IOException, InterruptedException {
		execFFMPEG(cmd, sc, mFileBinDir);
	}

	private int execProcess(List<String> cmds, ShellCallback sc, File fileExec) throws IOException, InterruptedException {

		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(fileExec);

		StringBuffer cmdlog = new StringBuffer();

		for (String cmd : cmds) {
			cmdlog.append(cmd);
			cmdlog.append(' ');
		}

		Log.d(TAG, cmdlog.toString());

		pb.redirectErrorStream(true);
		// pb.redirectErrorStream(true);
		Process process = pb.start();

		// any error message?
		/*
		 * StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR", sc);
		 */

		// any output?
		StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT", sc);

		// kick them off
		// errorGobbler.start();
		outputGobbler.start();

		int exitVal = process.waitFor();

		sc.processComplete(exitVal);

		return exitVal;

	}

	public class FFMPEGArg {
		String key;
		String value;

		public static final String ARG_VIDEOCODEC = "-vcodec";
		public static final String ARG_AUDIOCODEC = "-acodec";

		public static final String ARG_VIDEOBITSTREAMFILTER = "-vbsf";
		public static final String ARG_AUDIOBITSTREAMFILTER = "-absf";

		public static final String ARG_VERBOSITY = "-v";
		public static final String ARG_FILE_INPUT = "-i";
		public static final String ARG_SIZE = "-s";
		public static final String ARG_FRAMERATE = "-r";
		public static final String ARG_FORMAT = "-f";
		public static final String ARG_BITRATE_VIDEO = "-b:v";

		public static final String ARG_BITRATE_AUDIO = "-b:a";
		public static final String ARG_CHANNELS_AUDIO = "-ac";
		public static final String ARG_FREQ_AUDIO = "-ar";

		public static final String ARG_STARTTIME = "-ss";
		public static final String ARG_DURATION = "-t";

	}

	public void convertWavToMp4(String wavIn, String m4aFile, ShellCallback shellCallback) throws IOException, InterruptedException {
		ArrayList<String> cmd = new ArrayList<String>();

		cmd.add(ffmpegBin);
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(new File(wavIn).getCanonicalPath());

		cmd.add("-c:a");
		cmd.add("aac");

		cmd.add("-b:a");
		cmd.add("24k");

		cmd.add("-cutoff");
		cmd.add("5000");

		cmd.add("-strict");
		cmd.add("-2");

		cmd.add(m4aFile);

		execFFMPEG(cmd, shellCallback);
	}

	class StreamGobbler extends Thread {
		InputStream is;
		String type;
		ShellCallback sc;

		StreamGobbler(InputStream is, String type, ShellCallback sc) {
			this.is = is;
			this.type = type;
			this.sc = sc;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					if (sc != null)
						sc.shellOut(line);

			}
			catch (IOException ioe) {
				Log.e(TAG, "error reading shell slog", ioe);
			}
		}
	}

}

/*
 * Main options: -L show license -h show help -? show help -help show help --help show help -version show version -formats show available formats -codecs show
 * available codecs -bsfs show available bit stream filters -protocols show available protocols -filters show available filters -pix_fmts show available pixel
 * formats -sample_fmts show available audio sample formats -loglevel loglevel set libav* logging level -v loglevel set libav* logging level -debug flags set
 * debug flags -report generate a report -f fmt force format -i filename input file name -y overwrite output files -n do not overwrite output files -c codec
 * codec name -codec codec codec name -pre preset preset name -t duration record or transcode "duration" seconds of audio/video -fs limit_size set the limit
 * file size in bytes -ss time_off set the start time offset -itsoffset time_off set the input ts offset -itsscale scale set the input ts scale -timestamp time
 * set the recording timestamp ('now' to set the current time) -metadata string=string add metadata -dframes number set the number of data frames to record
 * -timelimit limit set max runtime in seconds -target type specify target file type ("vcd", "svcd", "dvd", "dv", "dv50", "pal-vcd", "ntsc-svcd", ...) -xerror
 * exit on error -frames number set the number of frames to record -tag fourcc/tag force codec tag/fourcc -filter filter_list set stream filterchain -stats
 * print progress report during encoding -attach filename add an attachment to the output file -dump_attachment filename extract an attachment into a file -bsf
 * bitstream_filters A comma-separated list of bitstream filters -dcodec codec force data codec ('copy' to copy stream)
 * 
 * Advanced options: -map file.stream[:syncfile.syncstream] set input stream mapping -map_channel file.stream.channel[:syncfile.syncstream] map an audio channel
 * from one stream to another -map_meta_data outfile[,metadata]:infile[,metadata] DEPRECATED set meta data information of outfile from infile -map_metadata
 * outfile[,metadata]:infile[,metadata] set metadata information of outfile from infile -map_chapters input_file_index set chapters mapping -benchmark add
 * timings for benchmarking -dump dump each input packet -hex when dumping packets, also dump the payload -re read input at native frame rate -loop_input
 * deprecated, use -loop -loop_output deprecated, use -loop -vsync video sync method -async audio sync method -adrift_threshold threshold audio drift threshold
 * -copyts copy timestamps -copytb source copy input stream time base when stream copying -shortest finish encoding within shortest input -dts_delta_threshold
 * threshold timestamp discontinuity delta threshold -copyinkf copy initial non-keyframes -q q use fixed quality scale (VBR) -qscale q use fixed quality scale
 * (VBR) -streamid streamIndex:value set the value of an outfile streamid -muxdelay seconds set the maximum demux-decode delay -muxpreload seconds set the
 * initial demux-decode delay -fpre filename set options from indicated preset file
 * 
 * Video options: -vframes number set the number of video frames to record -r rate set frame rate (Hz value, fraction or abbreviation) -s size set frame size
 * (WxH or abbreviation) -aspect aspect set aspect ratio (4:3, 16:9 or 1.3333, 1.7777) -bits_per_raw_sample number set the number of bits per raw sample
 * -croptop size Removed, use the crop filter instead -cropbottom size Removed, use the crop filter instead -cropleft size Removed, use the crop filter instead
 * -cropright size Removed, use the crop filter instead -padtop size Removed, use the pad filter instead -padbottom size Removed, use the pad filter instead
 * -padleft size Removed, use the pad filter instead -padright size Removed, use the pad filter instead -padcolor color Removed, use the pad filter instead -vn
 * disable video -vcodec codec force video codec ('copy' to copy stream) -sameq use same quantizer as source (implies VBR) -same_quant use same quantizer as
 * source (implies VBR) -pass n select the pass number (1 or 2) -passlogfile prefix select two pass log file name prefix -vf filter list video filters -b
 * bitrate video bitrate (please use -b:v) -dn disable data
 * 
 * Advanced Video options: -pix_fmt format set pixel format -intra use only intra frames -vdt n discard threshold -rc_override override rate control override
 * for specific intervals -deinterlace deinterlace pictures -psnr calculate PSNR of compressed frames -vstats dump video coding statistics to file -vstats_file
 * file dump video coding statistics to file -intra_matrix matrix specify intra matrix coeffs -inter_matrix matrix specify inter matrix coeffs -top
 * top=1/bottom=0/auto=-1 field first -dc precision intra_dc_precision -vtag fourcc/tag force video tag/fourcc -qphist show QP histogram -force_fps force the
 * selected framerate, disable the best supported framerate selection -force_key_frames timestamps force key frames at specified timestamps -vbsf video
 * bitstream_filters deprecated -vpre preset set the video options to the indicated preset
 * 
 * Audio options: -aframes number set the number of audio frames to record -aq quality set audio quality (codec-specific) -ar rate set audio sampling rate (in
 * Hz) -ac channels set number of audio channels -an disable audio -acodec codec force audio codec ('copy' to copy stream) -vol volume change audio volume
 * (256=normal) -rmvol volume rematrix volume (as factor)
 */

/*
 * //./ffmpeg -y -i test.mp4 -vframes 999999 -vf 'redact=blurbox.txt [out] [d], [d]nullsink' -acodec copy outputa.mp4
 * 
 * //ffmpeg -v 10 -y -i /sdcard/org.witness.sscvideoproto/videocapture1042744151.mp4 -vcodec libx264 //-b 3000k -s 720x480 -r 30 -acodec copy -f mp4 -vf
 * 'redact=/data/data/org.witness.sscvideoproto/redact_unsort.txt' ///sdcard/org.witness.sscvideoproto/new.mp4
 * 
 * //"-vf" , "redact=" + Environment.getExternalStorageDirectory().getPath() + "/" + PACKAGENAME + "/redact_unsort.txt",
 * 
 * 
 * // Need to make sure this will create a legitimate mp4 file //"-acodec", "ac3", "-ac", "1", "-ar", "16000", "-ab", "32k",
 * 
 * 
 * String[] ffmpegCommand = {"/data/data/"+PACKAGENAME+"/ffmpeg", "-v", "10", "-y", "-i", recordingFile.getPath(), "-vcodec", "libx264", "-b", "3000k", "-vpre",
 * "baseline", "-s", "720x480", "-r", "30", //"-vf", "drawbox=10:20:200:60:red@0.5", "-vf" , "\"movie="+ overlayImage.getPath()
 * +" [logo];[in][logo] overlay=0:0 [out]\"", "-acodec", "copy", "-f", "mp4", savePath.getPath()+"/output.mp4"};
 * 
 * 
 * 
 * 
 * //ffmpeg -i source-video.avi -s 480x320 -vcodec mpeg4 -acodec aac -ac 1 -ar 16000 -r 13 -ab 32000 -aspect 3:2 output-video.mp4/
 */

/*
 * concat doesn't seem to work cmd.add("-i");
 * 
 * StringBuffer concat = new StringBuffer();
 * 
 * for (int i = 0; i < videos.size(); i++) { if (i > 0) concat.append("|");
 * 
 * concat.append(out.path + '.' + i + ".wav");
 * 
 * }
 * 
 * cmd.add("concat:\"" + concat.toString() + "\"");
 */

