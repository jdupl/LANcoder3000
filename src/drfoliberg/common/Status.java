package drfoliberg.common;

public enum Status {
	//public static final int NOT_CONECTED = 0, FREE = 1, WORKING = 2, PAUSED = 3;
	NOT_CONNECTED, FREE, WORKING, PAUSED, // status of Node
	JOB_COMPLETED, JOB_TODO, JOB_COMPUTING, JOB_CANCELED // status of Task
}
