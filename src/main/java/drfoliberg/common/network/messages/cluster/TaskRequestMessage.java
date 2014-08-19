package drfoliberg.common.network.messages.cluster;

import drfoliberg.common.network.messages.ClusterProtocol;
import drfoliberg.common.task.Task;

public class TaskRequestMessage extends Message {

	private static final long serialVersionUID = -994042578899999534L;

	public Task task;

	/**
	 * Sent from master to workers
	 * 
	 * @param task
	 *            The task to request to the worker
	 */
	public TaskRequestMessage(Task task) {
		super(ClusterProtocol.TASK_REQUEST);
		this.task = task;
	}

	public Task getTask() {
		return task;
	}
}
