package drfoliberg.common.job;

import java.io.Serializable;
import java.util.ArrayList;

public class JobConfig implements Serializable {

	private static final long serialVersionUID = -8201664961243820323L;

	protected String sourceFile;
	protected RateControlType rateControlType;
	protected int rate; // kbps or crf TODO use BiterateControl ?
	protected int passes;
	protected FFmpegPreset preset;
	protected ArrayList<String> extraEncoderArgs; // TODO usage this to allow --slow-first-pass and other overrides

	public JobConfig(String sourceFile, RateControlType rateControlType, int rate, int passes, FFmpegPreset preset,
			ArrayList<String> extraEncoderArgs) {
		super();
		this.sourceFile = sourceFile;
		this.rateControlType = rateControlType;
		this.rate = rate;
		this.passes = passes;
		this.preset = preset;
		this.extraEncoderArgs = extraEncoderArgs;
	}

	public JobConfig(JobConfig config) {
		this.sourceFile = config.sourceFile;
		this.rateControlType = config.rateControlType;
		this.rate = config.rate;
		this.passes = config.passes;
		this.preset = config.preset;
		this.extraEncoderArgs = config.extraEncoderArgs;
	}

	public int getPasses() {
		return passes;
	}

	public void setPasses(int passes) {
		this.passes = passes;
	}

	public FFmpegPreset getPreset() {
		return preset;
	}

	public void setPreset(FFmpegPreset preset) {
		this.preset = preset;
	}

	public RateControlType getRateControlType() {
		return rateControlType;
	}

	public void setRateControlType(RateControlType rateControlType) {
		this.rateControlType = rateControlType;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

	public ArrayList<String> getExtraEncoderArgs() {
		return extraEncoderArgs;
	}

	public void setExtraEncoderArgs(ArrayList<String> extraEncoderArgs) {
		this.extraEncoderArgs = extraEncoderArgs;
	}

}