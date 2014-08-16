package drfoliberg.common.job;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import drfoliberg.common.codecs.Codec;
import drfoliberg.common.file_components.FileInfo;
import drfoliberg.common.file_components.streams.AudioStream;
import drfoliberg.common.file_components.streams.Stream;
import drfoliberg.common.file_components.streams.VideoStream;
import drfoliberg.common.status.JobState;
import drfoliberg.common.status.TaskState;
import drfoliberg.common.task.Task;
import drfoliberg.common.task.audio.AudioEncodingTask;
import drfoliberg.common.task.video.VideoEncodingTask;

/**
 * A job is the whole process of taking the source file, splitting it if necessary, encoding it and merge back all
 * tracks. Tasks will be dispatched to nodes by the master.
 * 
 * @author justin
 * 
 */
public class Job extends JobConfig implements Comparable<Job> {

	private static final long serialVersionUID = -3817299446490049451L;

	private String jobId;
	private String jobName;
	private JobState jobStatus = JobState.JOB_TODO;
	private int lengthOfTasks;
	private long lengthOfJob;
	private int frameCount;
	private double frameRate;
	private int priority;
	/**
	 * Output path of this job, relative to absolute shared directory
	 */
	private String outputFolder;
	/**
	 * Filename for final output file relative to outputFolder
	 */
	private String outputFileName;
	/**
	 * The folder in which to store the parts before muxing
	 */
	private String partsFolderName;

	private FileInfo fileInfo;

	private ArrayList<VideoEncodingTask> videoTasks = new ArrayList<>();
	private ArrayList<AudioEncodingTask> audioTasks = new ArrayList<>();
	private int taskCount = 0;

	public Job(JobConfig config, String jobName, int lengthOfTasks, String encodingOutputFolder, FileInfo fileInfo) {
		super(config);
		this.jobName = jobName;
		this.lengthOfTasks = lengthOfTasks;
		this.lengthOfJob = fileInfo.getDuration();
		this.frameRate = fileInfo.getMainVideoStream().getFramerate();
		this.fileInfo = fileInfo;
		this.partsFolderName = "parts"; // TODO Why would this change ? Perhaps move to constant.

		// Estimate the frame count from the frame rate and length
		this.frameCount = (int) Math.floor((lengthOfJob / 1000 * frameRate));
		// Get source' filename
		File source = new File(config.getSourceFile());
		// Set output's filename
		this.outputFileName = String.format("%s.mkv", FilenameUtils.removeExtension(source.getName()));
		// Get /sharedFolder/LANcoder/jobsOutput/jobName/ (without the shared folder)
		File relativeEncodingOutput = FileUtils.getFile(encodingOutputFolder, jobName);
		this.outputFolder = relativeEncodingOutput.getPath();

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] byteArray = md.digest((sourceFile + jobName + System.currentTimeMillis()).getBytes());
			String result = "";
			for (int i = 0; i < byteArray.length; i++) {
				result += Integer.toString((byteArray[i] & 0xff) + 0x100, 16).substring(1);
			}
			this.jobId = result;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			// even if the algorithm is not available, don't crash
			this.jobId = String.valueOf(System.currentTimeMillis());
		}
		createTasks();
	}

	/**
	 * Creates tasks of the job with good handling of paths. TODO add subtitles to the job
	 */
	private void createTasks() {
		for (Stream stream : this.fileInfo.getStreams()) {
			if (stream instanceof VideoStream) {
				this.videoTasks.addAll(createVideoTasks((VideoStream) stream));
			} else if (stream instanceof AudioStream) {
				this.audioTasks.add(createAudioTask((AudioStream) stream));
			}
		}
	}

	/**
	 * Process a VideoStream and split into multiple VideoEncodingTasks
	 * 
	 * @param stream
	 *            The stream to process
	 * @return The VideoEncodingTasks that will be encoded
	 */
	private ArrayList<VideoEncodingTask> createVideoTasks(VideoStream stream) {
		long currentMs = 0;
		ArrayList<VideoEncodingTask> tasks = new ArrayList<>();

		long remaining = fileInfo.getDuration();

		// Get relative (to absolute shared directory) output folder for this job's tasks
		File relativeTasksOutput = FileUtils.getFile(getOutputFolder(), getPartsFolderName());
		while (remaining > 0) {
			VideoEncodingTask task = new VideoEncodingTask(taskCount++, getJobId(), this, stream);
			task.setEncodingStartTime(currentMs);
			if ((((double) remaining - getLengthOfTasks()) / getLengthOfJob()) <= 0.15) {
				task.setEncodingEndTime(getLengthOfJob());
				remaining = 0;
			} else {
				task.setEncodingEndTime(currentMs + lengthOfTasks);
				remaining -= lengthOfTasks;
				currentMs += lengthOfTasks;
			}
			long ms = task.getEncodingEndTime() - task.getEncodingStartTime();
			task.setEstimatedFramesCount((long) Math.floor((ms / 1000 * stream.getFramerate())));
			// Set task output file
			File relativeTaskOutputFile = FileUtils.getFile(relativeTasksOutput,
					String.format("part-%d.mpeg.ts", task.getTaskId())); // TODO get extension from codec
			task.setOutputFile(relativeTaskOutputFile.getPath());
			tasks.add(task);
		}
		return tasks;
	}

	/**
	 * Process an AudioStream and create an AudioEncodingTask (with hardcoded vorbis settings). TODO handle multiple
	 * audio codec.
	 * 
	 * @param stream
	 *            The stream to encode
	 * @return The AudioEncodingTask
	 */
	private AudioEncodingTask createAudioTask(AudioStream stream) {
		int nextTaskId = taskCount++;
		File output = FileUtils.getFile(getOutputFolder(), String.valueOf(nextTaskId));
		return new AudioEncodingTask(Codec.VORBIS, 2, 44100, 3, RateControlType.CRF, getSourceFile(), output.getPath(),
				getJobId(), nextTaskId, stream);
	}

	/**
	 * Returns next task to encode. Changes Job status if job is not started yet.
	 * 
	 * @return The task or null if no task is available
	 */
	public synchronized VideoEncodingTask getNextTask() {
		if (getTaskRemainingCount() == 0) {
			return null;
		}
		if (this.getJobStatus() == JobState.JOB_TODO) {
			// TODO move this to job manager
			this.setJobStatus(JobState.JOB_COMPUTING);
		}
		for (VideoEncodingTask task : this.videoTasks) {
			if (task.getTaskState() == TaskState.TASK_TODO) {
				return task;
			}
		}
		return null;
	}

	/**
	 * Counts if necessary the tasks currently not processed. A task being processed by a node counts as processed.
	 * 
	 * @return The count of tasks left to dispatch
	 */
	public synchronized int getTaskRemainingCount() {
		switch (this.getJobStatus()) {
		case JOB_COMPLETED:
			return 0;
		case JOB_TODO:
			return this.videoTasks.size();
		default:
			int count = 0;
			for (VideoEncodingTask task : this.videoTasks) {
				if (task.getTaskState() == TaskState.TASK_TODO) {
					++count;
				}
			}
			return count;
		}
	}

	/**
	 * Compare job accordingly to priority, tasks remaining and job length.
	 * 
	 * @return 1 if this is bigger, -1 otherwise
	 */
	public int compareTo(Job other) {
		if (this.priority != other.priority) {
			return Integer.compare(this.priority, other.priority);
		} else if (this.getTaskRemainingCount() != other.getTaskRemainingCount()) {
			return Integer.compare(this.getTaskRemainingCount(), other.getTaskRemainingCount());
		} else {
			return Long.compare(this.getLengthOfJob(), other.getLengthOfJob());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Job)) {
			return false;
		}
		Job other = (Job) obj;
		return other.getJobId().equals(this.getJobId());
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}

	public JobState getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(JobState jobStatus) {
		this.jobStatus = jobStatus;
	}

	public long getFrameCount() {
		return frameCount;
	}

	public double getFrameRate() {
		return frameRate;
	}

	public int getLengthOfTasks() {
		return lengthOfTasks;
	}

	public void setLengthOfTasks(int lengthOfTasks) {
		this.lengthOfTasks = lengthOfTasks;
	}

	public long getLengthOfJob() {
		return lengthOfJob;
	}

	public void setLengthOfJob(long lengthOfJob) {
		this.lengthOfJob = lengthOfJob;
	}

	public ArrayList<Task> getTasks() {
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.addAll(audioTasks);
		tasks.addAll(videoTasks);
		return tasks;
	}

	public void setTasks(ArrayList<VideoEncodingTask> tasks) {
		this.videoTasks = tasks;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public RateControlType getRateContolType() {
		return rateControlType;
	}

	public void setRateContolType(RateControlType rateContolType) {
		this.rateControlType = rateContolType;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public String getPartsFolderName() {
		return partsFolderName;
	}

	public void setPartsFolderName(String partsFolderName) {
		this.partsFolderName = partsFolderName;
	}

	public ArrayList<AudioEncodingTask> getAudioTasks() {
		return audioTasks;
	}

	public ArrayList<VideoEncodingTask> getVideoTasks() {
		return videoTasks;
	}

}