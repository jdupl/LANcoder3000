package drfoliberg.worker.converter;

import drfoliberg.common.network.Cause;
import drfoliberg.common.task.Task;
import drfoliberg.worker.WorkerConfig;

/**
 * TODO Refactor to abstract class and inherit service class ?
 *
 */
public interface ConverterListener {

	public void workStarted(Task task);

	public void workCompleted(Task task);

	public void workFailed(Task task);

	public void nodeCrash(Cause cause);

	public WorkerConfig getConfig();
}