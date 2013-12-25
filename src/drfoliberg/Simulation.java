package drfoliberg;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import drfoliberg.common.task.Job;
import drfoliberg.common.task.JobType;
import drfoliberg.common.task.Task;
import drfoliberg.master.Master;
import drfoliberg.master.Node;
import drfoliberg.worker.Worker;

public class Simulation extends Thread {

	/**
	 * This simulation runs so we have a client-server in the same console.
	 * 
	 */
	public Simulation() throws IOException {

	}

	public void run() {

		InetAddress masterIp;
		try {
			masterIp = InetAddress.getByName("127.0.0.1");
			System.out.println("SIM: Creating a job");
			Job j =new Job("testname", "My.Movie.mkv", JobType.BITRATE_2_PASS_JOB, 5*60*1000, 120*60*1000+11548);
			System.out.println("SIM: Creating first worker now,");
			Worker worker1 = new Worker("worker1", masterIp, 1337, 1338);
			worker1.start();			
			System.out.println("SIM: Faking that master is not up... waiting 5 seconds to start master.");
			sleep(5000);
			System.out.println("SIM: Starting master now");
			Master m = new Master();
			
			// m.start();
			// System.out.println("SIM: Forcing master to disconnect his worker in 2 seconds");
			// sleep(2000);
			// System.out.println("SIM: disconnecting worker now");
			// m.disconnectNode(new Node(masterIp));
			
			sleep(2000);
			//System.out.println("SIM: sending a task to the worker!");
			//m.dispatch(new Task(), new Node(masterIp, 1338));
			System.out.println("SIM: adding a job to master's queue !");
			m.addJob(j);
			System.out.println("SIM: simulation completed !");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
